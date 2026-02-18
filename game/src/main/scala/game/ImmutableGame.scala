package game

// ====== Match types for game type computation ======
// Replaces ComputeGameTypesMacro — pure type-level, no macro needed

object GameTypes:
  /** Collect move types from an actions tuple into a Coproduct */
  type CollectMoves[A <: Tuple] = A match
    case GameAction[m, _, _] *: t => m :+: CollectMoves[t]
    case EmptyTuple => CNil

  /** Collect all state types from actions, accumulating with dedup */
  type CollectState[A <: Tuple, Acc <: Tuple] <: Tuple = A match
    case EmptyTuple => Acc
    case GameAction[_, s, d] *: t =>
      CollectState[t, AddActionContrib[Acc, s, d]]

  /** Add an action's state and delta-inner types to the accumulator */
  type AddActionContrib[Acc <: Tuple, S <: Tuple, D] =
    TupleOps.Union[TupleOps.Union[Acc, S], CoproductOps.DeltaInnerTypes[D]]

  /** Add global action's contribution */
  type AddGSContrib[Acc <: Tuple, GS] <: Tuple = GS match
    case GameAction[_, s, d] => AddActionContrib[Acc, s, d]

  /** Compute full STATE from ACTIONS tuple and GS global action */
  type ComputeState[A <: Tuple, GS] = AddGSContrib[CollectState[A, EmptyTuple], GS]


// ====== Runtime coproduct helpers ======

private[game] def coproductIndex(c: Any): (Any, Int) = c match
  case Inl(h) => (h, 0)
  case Inr(t) =>
    val (v, i) = coproductIndex(t)
    (v, i + 1)

private[game] def applyOneDelta(delta: Any, state: Any): Any =
  val tup = state.asInstanceOf[Tuple]
  val (rawDelta, idx) = coproductIndex(delta)
  val d = rawDelta.asInstanceOf[Delta[?]]
  val arr = tup.toArray
  val stateElem = arr(idx).asInstanceOf[GameState[Any]]
  arr(idx) = stateElem.apply(d.delta.asInstanceOf[stateElem.Delta]).asInstanceOf[Object]
  Tuple.fromArray(arr)


// ====== Lift all actions to full STATE ======
// Replaces BuildGameImplMacro — given-based, no macro needed

trait LiftAllActions[ACTIONS <: Tuple, STATE <: Tuple]:
  def lift(actions: ACTIONS): Vector[List[(Any, STATE) => List[TupleOps.DeltaOf[STATE]]]]

object LiftAllActions:
  given [STATE <: Tuple]: LiftAllActions[EmptyTuple, STATE] with
    def lift(actions: EmptyTuple) = Vector.empty

  given [M, S <: Tuple, D, Rest <: Tuple, STATE <: Tuple](using
    liftS: Lift[S, STATE],
    embedD: CoproductBasis[TupleOps.DeltaOf[STATE], D],
    rest: LiftAllActions[Rest, STATE]
  ): LiftAllActions[GameAction[M, S, D] *: Rest, STATE] with
    def lift(actions: GameAction[M, S, D] *: Rest) =
      val head = actions.head
      val liftedFuncs: List[(Any, STATE) => List[TupleOps.DeltaOf[STATE]]] =
        head.actions.map { f => (m: Any, st: STATE) =>
          val s = liftS(st)
          f(m.asInstanceOf[M], s).map(d => embedD.embed(d))
        }
      liftedFuncs +: rest.lift(actions.tail)


// ====== Lift global action ======

trait LiftGlobalAction[GS, STATE <: Tuple]:
  def lift(gs: GS): List[(Any, STATE) => List[TupleOps.DeltaOf[STATE]]]

object LiftGlobalAction:
  given [S <: Tuple, D, STATE <: Tuple](using
    liftS: Lift[S, STATE],
    embedD: CoproductBasis[TupleOps.DeltaOf[STATE], D]
  ): LiftGlobalAction[GameAction[Any, S, D], STATE] with
    def lift(gs: GameAction[Any, S, D]) =
      gs.actions.map { f => (m: Any, st: STATE) =>
        val s = liftS(st)
        f(m, s).map(d => embedD.embed(d))
      }


// ====== ImmutableGame trait ======

trait ImmutableGame[MOVES, STATE <: Tuple]:
  self =>

  def applyMove(move: MOVES, state: STATE): (List[TupleOps.DeltaOf[STATE]], STATE)

  def apply[M, S <: Tuple](move: M, state: S)(using
    inject: CoproductInject[MOVES, M],
    ra: TupleRemoveAll[S, STATE]
  ): (List[TupleOps.DeltaOf[STATE]], S) =
    val (innerState, remainder) = ra(state)
    val (deltas, post) = applyMove(inject(move), innerState)
    (deltas, ra.reinsert((post, remainder)))

  def align[M, S <: Tuple](using
    basis: CoproductBasis[MOVES, M],
    ra: TupleRemoveAll[S, STATE],
    embedDelta: CoproductBasis[TupleOps.DeltaOf[S], TupleOps.DeltaOf[STATE]]
  ): ImmutableGame[M, S] = new ImmutableGame[M, S]:
    def applyMove(move: M, state: S): (List[TupleOps.DeltaOf[S]], S) =
      val (innerState, remainder) = ra(state)
      val (deltas, newInnerState) = self.applyMove(basis.embed(move), innerState)
      val newState = ra.reinsert((newInnerState, remainder))
      (deltas.map(embedDelta.embed), newState)


// ====== ImmutableGameBuilder ======

class ImmutableGameBuilder[ACTIONS <: Tuple, GS](actions: ACTIONS, globalActions: GS):

  def addAction[A](action: A)(using ev: A <:< GameAction[?, ?, ?]): ImmutableGameBuilder[A *: ACTIONS, GS] =
    new ImmutableGameBuilder(action *: actions, globalActions)

  def addMove[M]: ImmutableGame.AddMovesApply[M :+: CNil, ACTIONS, GS] =
    ImmutableGame.AddMovesApply(actions, globalActions)

  def addMoves[MOVES]: ImmutableGame.AddMovesApply[MOVES, ACTIONS, GS] =
    ImmutableGame.AddMovesApply(actions, globalActions)

  def addGlobalAction[S1 <: Tuple, D1, S2 <: Tuple, D2](gAction: GameAction[Any, S2, D2])(using
    ev: GS <:< GameAction[Any, S1, D1],
    ms: MergeState[S1, S2],
    md: MergeDelta[D1, D2]
  ): ImmutableGameBuilder[ACTIONS, GameAction[Any, ms.Out, md.Out]] =
    val ga1: GameAction[Any, S1, D1] = ev(globalActions)
    val ga2: GameAction[Any, S2, D2] = gAction

    val left = ga1.actions.map { action =>
      (move: Any, state: ms.Out) =>
        val (state1, _) = ms.split(state)
        action(move, state1).map(md.applyLeft)
    }

    val right = ga2.actions.map { action =>
      (move: Any, state: ms.Out) =>
        val (_, state2) = ms.split(state)
        action(move, state2).map(md.applyRight)
    }

    new ImmutableGameBuilder(actions, new GameAction[Any, ms.Out, md.Out](left ++ right))

  def build()(using
    liftAll: LiftAllActions[ACTIONS, GameTypes.ComputeState[ACTIONS, GS]],
    liftGS: LiftGlobalAction[GS, GameTypes.ComputeState[ACTIONS, GS]]
  ): ImmutableGame[GameTypes.CollectMoves[ACTIONS], GameTypes.ComputeState[ACTIONS, GS]] =
    type STATE = GameTypes.ComputeState[ACTIONS, GS]
    type DELTA = TupleOps.DeltaOf[STATE]
    type MOVES = GameTypes.CollectMoves[ACTIONS]
    val table = liftAll.lift(actions)
    val gsFuncs = liftGS.lift(globalActions)
    new ImmutableGame[MOVES, STATE]:
      def applyMove(move: MOVES, state: STATE): (List[DELTA], STATE) =
        val (moveVal, idx) = coproductIndex(move)
        val moveFuncs = table(idx)
        val allFuncs: List[STATE => List[DELTA]] =
          moveFuncs.map(f => (st: STATE) => f(moveVal, st)) ++
          gsFuncs.map(f => (st: STATE) => f(moveVal, st))
        allFuncs.foldLeft((state, List.empty[DELTA])) { case ((st, deltas), f) =>
          val dl = f(st)
          val s2 = dl.foldLeft[STATE](st) { case (s, d) => applyOneDelta(d, s).asInstanceOf[STATE] }
          (s2, deltas ++ dl)
        }.swap


// ====== ImmutableGame companion ======

object ImmutableGame:

  val builder: ImmutableGameBuilder[EmptyTuple, GameAction[Any, EmptyTuple, CNil]] =
    new ImmutableGameBuilder(EmptyTuple, GameAction.empty)

  def apply[MOVES]: AddMovesApply[MOVES, EmptyTuple, GameAction[Any, EmptyTuple, CNil]] =
    AddMovesApply(EmptyTuple, GameAction.empty)

  final case class AddMovesApply[MOVES, AL <: Tuple, GS](actions: AL, gs: GS):
    def apply()(using fetch: FetchActions[MOVES]): ImmutableGameBuilder[Tuple.Concat[AL, fetch.Out], GS] =
      val combined = Tuple.fromArray(actions.toArray ++ fetch.instances.toArray)
        .asInstanceOf[Tuple.Concat[AL, fetch.Out]]
      new ImmutableGameBuilder(combined, gs)

  def initialize[S <: Tuple](using fill: TupleFill[S]): S = fill()

  trait StateInitializer[T]:
    def apply(): T

  given stateInitDefault[T](using si: StateInitializer[T]): DefaultValue[T] with
    def value: T = si()

  case class TypeBox[C]()


// ====== FetchActions ======

trait FetchActions[C]:
  type Out <: Tuple
  def instances: Out

object FetchActions:
  type Aux[C, O <: Tuple] = FetchActions[C] { type Out = O }

  given FetchActions[CNil] with
    type Out = EmptyTuple
    def instances = EmptyTuple

  given [H, T, S <: Tuple, D](using ga: GameAction[H, S, D], next: FetchActions[T]): FetchActions[H :+: T] with
    type Out = GameAction[H, S, D] *: next.Out
    def instances = ga *: next.instances
