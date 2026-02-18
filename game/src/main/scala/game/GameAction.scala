package game

class GameAction[MOVE, S <: Tuple, DELTA](val actions: List[(MOVE, S) => List[DELTA]]):

  def liftState[S2 <: Tuple](using lift: Lift[S, S2]): GameAction[MOVE, S2, DELTA] =
    new GameAction(actions.map(f => (m: MOVE, s: S2) => f(m, lift(s))))

  def liftDelta[D2](using lift: Lift[D2, DELTA]): GameAction[MOVE, S, D2] =
    new GameAction(actions.map(f => (m: MOVE, s: S) => f(m, s).map(lift.apply)))

  def compose[M](f: M => MOVE): GameAction[M, S, DELTA] =
    new GameAction(actions.map(action => (m: M, s: S) => action(f(m), s)))

  def composeS[M, S2 <: Tuple](f: (M, S2) => MOVE)(using merge: MergeState[S, S2]): GameAction[M, merge.Out, DELTA] =
    new GameAction(actions.map { action =>
      (move: M, state: merge.Out) =>
        val (state1, state2) = merge.split(state)
        action(f(move, state2), state1)
    })

  def map[D2](f: DELTA => D2): GameAction[MOVE, S, D2] =
    new GameAction(actions.map(action => (move: MOVE, state: S) => action(move, state).map(f)))

  // Scala 3 macro-based composition - computes types at compile time
  inline def andThen[S2 <: Tuple, D2](ga: GameAction[MOVE, S2, D2]): GameAction[MOVE, ?, ?] =
    util.GameActionCompositionMacro.andThenMacro(this, ga)


object GameAction:

  def empty: GameAction[Any, EmptyTuple, CNil] = GameAction.apply[Any](_ => Nil)

  def fromState[Move, S <: Tuple]: FromStateApply[Move, S] = FromStateApply[Move, S]()

  def apply[MOVE]: GameActionApply[MOVE] = GameActionApply[MOVE]()

  case class FromStateApply[MOVE, S <: Tuple]():
    def apply[DL](f: (MOVE, S) => List[DL]): GameAction[MOVE, S, DL] =
      new GameAction(List(f))

  case class GameActionApply[MOVE]():
    def apply[DL](f: MOVE => List[DL]): GameAction[MOVE, EmptyTuple, DL] =
      new GameAction(List((m, _) => f(m)))


trait MergeState[S1 <: Tuple, S2 <: Tuple]:
  self =>
  type Out <: Tuple
  def split(s: Out): (S1, S2)

  def flip: MergeState.Aux[S2, S1, Out] = new MergeState[S2, S1]:
    type Out = self.Out
    def split(s: self.Out): (S2, S1) = self.split(s).swap

object MergeState:
  type Aux[S1 <: Tuple, S2 <: Tuple, O <: Tuple] = MergeState[S1, S2] { type Out = O }

  given [S1 <: Tuple, S2 <: Tuple](using
    sa1: TupleSelectAll[TupleOps.Union[S1, S2], S1],
    sa2: TupleSelectAll[TupleOps.Union[S1, S2], S2]
  ): MergeState[S1, S2] with
    type Out = TupleOps.Union[S1, S2]
    def split(s: TupleOps.Union[S1, S2]): (S1, S2) = (sa1(s), sa2(s))


trait MergeDelta[D1, D2]:
  self =>
  type Out
  def applyLeft(left: D1): Out
  def applyRight(right: D2): Out

  def flip: MergeDelta.Aux[D2, D1, Out] = new MergeDelta[D2, D1]:
    type Out = self.Out
    def applyLeft(left: D2): self.Out = self.applyRight(left)
    def applyRight(right: D1): self.Out = self.applyLeft(right)

object MergeDelta:
  type Aux[D1, D2, O] = MergeDelta[D1, D2] { type Out = O }

  given [D1, D2](using un: CoproductUnionOp[D1, D2]): MergeDelta[D1, D2] with
    type Out = un.Out
    def applyLeft(left: D1): un.Out = un.applyLeft(left)
    def applyRight(right: D2): un.Out = un.applyRight(right)


/** Lift[A, B] converts B to A.
  * For Tuples: selects A's elements from B.
  * For Coproducts: embeds B into A. */
trait Lift[A, B] extends (B => A)

object Lift:
  given tupleLift[A <: Tuple, B <: Tuple](using sa: TupleSelectAll[B, A]): Lift[A, B] with
    def apply(b: B): A = sa(b)

  given coproductLift[A, B](using basis: CoproductBasis[A, B]): Lift[A, B] with
    def apply(b: B): A = basis.embed(b)
