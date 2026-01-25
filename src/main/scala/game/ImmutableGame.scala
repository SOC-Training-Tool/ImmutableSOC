package game

import game.Delta.ApplyDeltas
import game.ImmutableGame.{ExtractAction, FetchActions, MovePoly, TypeBox, builder}
import shapeless.ops.{coproduct, hlist}
import shapeless.{:+:, ::, CNil, Coproduct, Generic, HList, HNil, Poly0, Poly1, Poly2}
import util.DependsOn
import util.opext.CoproductUnion

trait ImmutableGame[MOVES <: Coproduct, STATE <: HList] {
  self =>

  type DELTA <: Coproduct

  def applyMove(move: MOVES, state: STATE): (List[DELTA], STATE)

  def apply[M, S <: HList, R <: HList](move: M, state: S)(implicit inject: coproduct.Inject[MOVES, M], remove: hlist.RemoveAll.Aux[S, STATE, (STATE, R)]): (List[DELTA], S) = {
    val (s, remainder) = remove.apply(state)
    val (deltas, post) = applyMove(inject.apply(move), s)
    val newState       = remove.reinsert((post, remainder))
    (deltas, newState)
  }

  case class AlignApply[M <: Coproduct, S <: HList]() {
    def apply[R <: HList]()(implicit basis: coproduct.Basis[MOVES, M], ra: hlist.RemoveAll.Aux[S, STATE, (STATE, R)]): ImmutableGame.Aux[M, S, DELTA] = new ImmutableGame[M, S] {

      override type DELTA = self.DELTA

      override def applyMove(move: M, state: S): (List[DELTA], S) = {
        val (s, remainder) = ra.apply(state)
        val (deltas, post) = self.applyMove(move.embed[MOVES], s)
        (deltas, ra.reinsert((post, remainder)))
      }
    }
  }

  def align[M <: Coproduct, S <: HList]: AlignApply[M, S] = AlignApply[M, S]()

  //  def addGlobalAction[M, S <: HList, A, OutS <: HList](action: A)(implicit ev1: Any =:= M, ev2: A <:< GameAction[M, S], un: hlist.Union.Aux[STATE, S, OutS], dep1: DependsOn[OutS, STATE], dep2: DependsOn[OutS, S]): ImmutableGame[MOVES, OutS] = {
  //    (move: MOVES, state: OutS) => {
  //      val s1 = dep1.updateAll(state)(self.applyMove(move, _))
  //      dep2.updateAll(s1)(action.apply(ev1.apply(()), _))
  //    }
  //  }
}

class ImmutableGameBuilder[ACTIONS <: HList, GS](actions: ACTIONS, globalActions: GS) {

  def addAction[A](action: A)(implicit ev: A <:< GameAction[Any, Any, Any]) = new ImmutableGameBuilder[A :: ACTIONS, GS](action :: actions, globalActions)

  def addMove[M] = AddMovesApply[M :+: CNil]()

  def addMoves[MOVES <: Coproduct] = AddMovesApply[MOVES]()

  final case class AddMovesApply[MOVES <: Coproduct]() {
    def apply[ML <: HList]()(implicit fetch: FetchActions.Aux[MOVES, ML], prep: hlist.Prepend[ACTIONS, ML]) = new ImmutableGameBuilder[prep.Out, GS](prep.apply(actions, fetch.instances), globalActions)
  }

  def addGlobalAction[S1, D1, S2, D2, OutS, OutD](gAction: GameAction[Any, S2, D2])(implicit ev: GS <:< GameAction[Any, S1, D1], ms: MergeState.Aux[S1, S2, OutS], md: MergeDelta.Aux[D1, D2, OutD]) = {
    new ImmutableGameBuilder(actions, globalActions.andThen(gAction))
  }

  def build[MOVES <: Coproduct, STATE <: HList, DOut <: Coproduct, S1, D1, S2, D2, FS, Z1 <: Coproduct, Z2 <: Coproduct]()(
    implicit
    ex: ExtractAction.Aux[ACTIONS, MOVES, S1, D1],
    ev: GS <:< GameAction[Any, S2, D2],
    fullState: FullState.Aux[GS, FS],
    ms: MergeState.Aux[S1, S2, STATE],
    md: MergeDelta.Aux[D1, D2, DOut],
    zipper1: coproduct.ZipWith.Aux[ACTIONS, MOVES, Z1],
    zipper2: coproduct.ZipConst.Aux[(STATE, GS, TypeBox[DOut]), Z1, Z2],
    onMove: coproduct.Folder.Aux[MovePoly.type, Z2, (STATE, List[DOut])]
  ): ImmutableGame.Aux[MOVES, STATE, DOut] = new ImmutableGame[MOVES, STATE] {
    override type DELTA = DOut

    override def applyMove(move: MOVES, state: STATE): (List[DELTA], STATE) = {
      val z1 = move.zipWith(actions)
      val z2 = z1.zipConst((state, globalActions, TypeBox[DELTA]()))
      onMove.apply(z2).swap
    }
  }
}


object ImmutableGame {

  val builder = new ImmutableGameBuilder(HNil, GameAction.empty)


  MergeState[HNil, String :: HNil]

  type Aux[M <: Coproduct, S <: HList, D <: Coproduct] = ImmutableGame[M, S] {type DELTA = D}

  //def make[MOVE <: Coproduct, STATE <: HList, DELTA <: Coproduct] = MakeApply[MOVE, STATE, DELTA]()

  def apply[MOVE <: Coproduct]: builder.AddMovesApply[MOVE] = builder.addMoves[MOVE]


  //  final case class ApplyApply[MOVE <: Coproduct]() {
  //    def apply[STATE <: HList, DELTA <: Coproduct, Z <: Coproduct, MOut <: Coproduct]()(
  //      implicit
  //      actions: CollectActionState.Aux[MOVE, STATE, DELTA],
  //      onMove: coproduct.LeftFolder.Aux[MOVE, (STATE, TypeBox[DELTA]), MovePoly.type, (STATE, List[DELTA])], // Coproduct of (STATE, List[D]) tuples
  //    ): Aux[MOVE, STATE, DELTA] = make[MOVE, STATE, DELTA].apply()
  //  }
  //
  //  final case class MakeApply[MOVE <: Coproduct, STATE <: HList, D <: Coproduct]() {
  //
  //    def apply()(
  //      implicit onMove: coproduct.LeftFolder.Aux[MOVE, (STATE, TypeBox[D]), MovePoly.type, (STATE, List[D])],
  //    ): Aux[MOVE, STATE, D] = {
  //      new ImmutableGame[MOVE, STATE] {
  //        override type DELTA = D
  //
  //        override def applyMove(move: MOVE, state: STATE): (List[DELTA], STATE) = {
  //          onMove.apply(move, (state, TypeBox[DELTA]())).swap
  //        }
  //      }
  //    }
  //  }

  case class TypeBox[C]()

  object MovePoly extends Poly1 {

    implicit def onMove[M, DELTA <: Coproduct, STATE <: HList, ACTION, GA, S1, D1, S2, D2, SOut <: HList, DOut <: Coproduct](
      implicit
      ev1: ACTION <:< GameAction[M, S1, D1],
      ev2: GA <:< GameAction[Any, S2, D2],
      ms: MergeState.Aux[S1, S2, SOut],
      md: MergeDelta.Aux[D1, D2, DOut],
      dep1: DependsOn[STATE, SOut],
      applyDelta: coproduct.LeftFolder.Aux[DOut, STATE, ApplyDeltas.type, STATE],
      basis: coproduct.Basis[DELTA, DOut]
    ): Case.Aux[((M, ACTION), (STATE, GA, TypeBox[DELTA])), (STATE, List[DELTA])] =
      at[((M, ACTION), (STATE, GA, TypeBox[DELTA]))] { case ((move, action), (state, ga, _)) =>
        val (s, ds) = action.andThen(ga.compose[M](identity)).actions.foldLeft[(STATE, List[DOut])]((state, Nil)) {
          case ((state, deltas), f) =>
            val dl: List[DOut] = f(move, dep1.getAll(state))
            val s2: STATE      = dl.foldLeft(state) { case (s, d) => applyDelta.apply(d, s) }
            (s2, deltas ++ dl)
        }
        (s, ds.map(_.embed[DELTA]))
      }
  }

  implicit def cnilLeftFolder[S, DELTA]: coproduct.LeftFolder.Aux[CNil, (S, TypeBox[DELTA]), MovePoly.type, (S, List[DELTA])] =
    new coproduct.LeftFolder[CNil, (S, TypeBox[DELTA]), MovePoly.type] {
      override type Out = (S, List[DELTA])

      def apply(t: CNil, u: (S, TypeBox[DELTA])): Out = (u._1, Nil)
    }

  //  trait MergeDelta[L <: Coproduct] {
  //    type Out <: Coproduct
  //  }
  //
  //  object MergeDelta {
  //
  //    type Aux[L <: Coproduct, Out0 <: Coproduct] = MergeDelta[L] {type Out = Out0}
  //
  //    def apply[L <: Coproduct](implicit m: MergeDelta[L]): Aux[L, m.Out] = m
  //
  //    implicit def recur[S, D <: Coproduct, T <: Coproduct, Out0 <: Coproduct](implicit next: MergeDelta.Aux[T, Out0], un: CoproductUnion[D, Out0]): Aux[(S, List[D]) :+: T, un.Out] =
  //      new MergeDelta[(S, List[D]) :+: T] {
  //        type Out = un.Out
  //      }
  //
  //    implicit val cnil: Aux[CNil, CNil] =
  //      new MergeDelta[CNil] {
  //        type Out = CNil
  //      }
  //  }

  trait FetchActions[C <: Coproduct] {
    type Out <: HList

    def instances: Out
  }

  object FetchActions {

    type Aux[C <: Coproduct, Out0 <: HList] = FetchActions[C] {type Out = Out0}

    def apply[C <: Coproduct](implicit f: FetchActions[C]): Aux[C, f.Out] = f

    implicit def recur[H, T <: Coproduct, S, D](implicit ga: GameAction[H, S, D], next: FetchActions[T]): Aux[H :+: T, GameAction[H, S, D] :: next.Out] =
      new FetchActions[H :+: T] {
        override type Out = GameAction[H, S, D] :: next.Out

        override def instances: Out = ga :: next.instances
      }

    implicit val cnil: Aux[CNil, HNil] = new FetchActions[CNil] {
      type Out = HNil

      override def instances: Out = HNil
    }
  }

  trait ExtractAction[L <: HList] {
    type MOVES <: Coproduct
    type STATE
    type DELTA
  }

  object ExtractAction {

    type Aux[L <: HList, M <: Coproduct, S, D] = ExtractAction[L] {
      type MOVES = M
      type STATE = S
      type DELTA = D
    }

    def apply[L <: HList](implicit ex: ExtractAction[L]): Aux[L, ex.MOVES, ex.STATE, ex.DELTA] = ex

    implicit def recur[M, S, D, T <: HList, ML <: Coproduct, S2, D2, FS](implicit next: ExtractAction.Aux[T, ML, S2, D2], fullState: FullState.Aux[GameAction[M, S, D], FS], ms: MergeState[S2, FS], md: MergeDelta[D, D2]): Aux[GameAction[M, S, D] :: T, M :+: next.MOVES, ms.Out, md.Out] =
      new ExtractAction[GameAction[M, S, D] :: T] {
        type MOVES = M :+: next.MOVES
        type STATE = ms.Out
        type DELTA = md.Out
      }

    implicit def hnil: Aux[HNil, CNil, HNil, CNil] = new ExtractAction[HNil] {
      type MOVES = CNil
      type STATE = HNil
      type DELTA = CNil
    }
  }

  trait StateInitializer[T] extends shapeless.DepFn0 {
    override type Out = T
  }

  def initialize[S <: HList](implicit fw: hlist.FillWith[InitializeOp.type, S]): S = HList.fillWith[S](InitializeOp)

  object InitializeOp extends Poly0 {

    implicit val initInt   : Case0[Int]    = at(0)
    implicit val initDouble: Case0[Double] = at(0.0)
    implicit val initString: Case0[String] = at("")

    implicit def initList[A]: Case0[List[A]] = at(List.empty[A])

    implicit def initMap[K, V]: Case0[Map[K, V]] = at(Map.empty[K, V])

    implicit def initOpt[A]: Case0[Option[A]] = at(None)

    implicit def initSet[A, T: Numeric]: Case0[InventorySet[A, T]] = at(InventorySet.empty[A, T])

    implicit def initHList[L <: HList](implicit fill: hlist.FillWith[InitializeOp.type, L]): Case0[L] = at[L](HList.fillWith[L](InitializeOp))

    implicit def initObj[A, Repr <: HList](implicit gen: Generic.Aux[A, Repr], c: Case0[Repr]): Case0[A] = at[A] {
      gen.from(c.apply())

    }
  }
}