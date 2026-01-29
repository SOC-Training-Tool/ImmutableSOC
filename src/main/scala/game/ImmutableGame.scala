package game

import game.Delta.ApplyDeltas
import game.ImmutableGame.{AddGlobalActionPoly, TypeBox}
import shapeless.ops.{coproduct, hlist}
import shapeless.{:+:, ::, CNil, Coproduct, Generic, HList, HNil, Poly0, Poly1}
import util.opext.{CoproductDistinct, CoproductUnion, HListDistinct}

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
    def apply[ML <: HList]()(implicit fetch: utils.FetchActions.Aux[MOVES, ML], prep: hlist.Prepend[ACTIONS, ML]) = new ImmutableGameBuilder[prep.Out, GS](prep.apply(actions, fetch.instances), globalActions)
  }

  def addGlobalAction[S1, D1, S2, D2, OutS, OutD](gAction: GameAction[Any, S2, D2])(implicit ev: GS <:< GameAction[Any, S1, D1], ms: MergeState.Aux[S1, S2, OutS], md: MergeDelta.Aux[D1, D2, OutD]): ImmutableGameBuilder[ACTIONS, GameAction[Any, OutS, OutD]] = {
    new ImmutableGameBuilder(actions, globalActions.andThen(gAction))
  }

  def build[MOVES <: Coproduct, STATE <: HList, DOut <: Coproduct, Z1 <: HList, AL <: HList, Z2 <: HList, AL2 <: HList, Z3 <: Coproduct]()(
    implicit
    zipper1: hlist.ZipConst.Aux[GS, ACTIONS, Z1],
    mapper: hlist.Mapper.Aux[AddGlobalActionPoly.type, Z1, AL],
    ea: utils.ExtractMoves.Aux[AL, MOVES],
    es: utils.ExtractState.Aux[AL, STATE],
    ed: utils.ExtractDelta.Aux[AL, DOut],
    zipper2: hlist.ZipConst.Aux[(TypeBox[STATE], TypeBox[DOut]), AL, Z2],
    liftAll: hlist.Mapper.Aux[utils.LiftAllPoly.type, Z2, AL2],
    zipper3: coproduct.ZipWith.Aux[AL2, MOVES, Z3],
    onMove: coproduct.Folder.Aux[utils.ApplyMovePoly.type, Z3, List[STATE => List[DOut]]],
    applyDelta: coproduct.LeftFolder.Aux[DOut, STATE, ApplyDeltas.type, STATE]
  ): ImmutableGame.Aux[MOVES, STATE, DOut] = new ImmutableGame[MOVES, STATE] {
    override type DELTA = DOut

    override def applyMove(move: MOVES, state: STATE): (List[DELTA], STATE) = {
      val allActions = mapper.apply(actions.zipConst(globalActions))
      val unifiedActions = liftAll.apply(allActions.zipConst((TypeBox[STATE](), TypeBox[DOut]())))
      val stateFuncs: List[STATE => List[DOut]] = onMove.apply(move.zipWith(unifiedActions))
      stateFuncs.foldLeft[(STATE, List[DOut])]((state, Nil)) {
        case ((state, deltas), f) =>
          val dl: List[DOut] = f(state)
          val s2: STATE   = dl.foldLeft(state) { case (s, d) => applyDelta.apply(d, s) }
          (s2, deltas ++ dl)
      }.swap
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

  object AddGlobalActionPoly extends Poly1 {

    implicit def onMove[M, S1, D1, S2, D2, SOut, DOut](implicit ms: MergeState.Aux[S1, S2, SOut], md: MergeDelta.Aux[D1, D2, DOut]): Case.Aux[(GameAction[M, S1, D1], GameAction[Any, S2, D2]), GameAction[M, SOut, DOut]] =
      at[(GameAction[M, S1, D1], GameAction[Any, S2, D2])] { case (action, ga) => action.andThen(ga.compose[M](identity))

      }

  }

  case class TypeBox[C]()

//  object MovePoly extends Poly1 {
//
//    implicit def onMove[M, S <: HList, D <: Coproduct, DELTA <: Coproduct, STATE <: HList](
//      implicit
//      dep1: DependsOn[STATE, S],
//      applyDelta: coproduct.LeftFolder.Aux[D, STATE, ApplyDeltas.type, STATE],
//      basis: coproduct.Basis[DELTA, D]
//    ): Case.Aux[((M, GameAction[M, S, D]), (STATE, TypeBox[DELTA])), (STATE, List[DELTA])] =
//      at[((M, GameAction[M, S, D]), (STATE, TypeBox[DELTA]))] { case ((move, action), (state, _)) =>
//        val (s, ds) = action.actions.foldLeft[(STATE, List[D])]((state, Nil)) {
//          case ((state, deltas), f) =>
//            val dl: List[D] = f(move, dep1.getAll(state))
//            val s2: STATE   = dl.foldLeft(state) { case (s, d) => applyDelta.apply(d, s) }
//            (s2, deltas ++ dl)
//        }
//        (s, ds.map(_.embed[DELTA]))
//      }
//  }
//
//  implicit def cnilLeftFolder[S, DELTA]: coproduct.LeftFolder.Aux[CNil, (S, TypeBox[DELTA]), MovePoly.type, (S, List[DELTA])] =
//    new coproduct.LeftFolder[CNil, (S, TypeBox[DELTA]), MovePoly.type] {
//      override type Out = (S, List[DELTA])
//
//      def apply(t: CNil, u: (S, TypeBox[DELTA])): Out = (u._1, Nil)
//    }

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

object utils {

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

  trait ExtractMoves[L <: HList] {
    type Out <: Coproduct
  }

  object ExtractMoves {
    type Aux[L <: HList, Out0 <: Coproduct] = ExtractMoves[L] { type Out = Out0 }

    def apply[L <: HList](implicit em: ExtractMoves[L]): Aux[L, em.Out] = em

    implicit def recur[H, S, D, T <: HList](implicit next: ExtractMoves[T]): Aux[GameAction[H, S, D] :: T, H :+: next.Out] = new ExtractMoves[GameAction[H, S, D] :: T] {
      type Out = H :+: next.Out
    }

    implicit val hnil: Aux[HNil, CNil] =  new ExtractMoves[HNil] { type Out = CNil }
  }

  trait ExtractState[L <: HList] {
    type Out <: HList
  }

  object ExtractState {
    type Aux[L <: HList, Out0 <: HList] = ExtractState[L] { type Out = Out0}

    def apply[L <: HList](implicit es: ExtractState[L]): Aux[L, es.Out] = es

    implicit def recur[H, S, D, T <: HList, FS <: HList, OutS <: HList](implicit fullState: FullState.Aux[GameAction[H, S, D], FS], next: ExtractState.Aux[T, OutS], un: hlist.Union[FS, OutS]): Aux[GameAction[H, S, D] :: T, un.Out] = new ExtractState[GameAction[H, S, D] :: T] {
      override type Out = un.Out
    }

    implicit val hnil: Aux[HNil, HNil] = new ExtractState[HNil] { type Out = HNil}
  }

  trait ExtractDelta[L <: HList] {
    type Out <: Coproduct
  }

  object ExtractDelta {
    type Aux[L <: HList, Out0 <: Coproduct] = ExtractDelta[L] { type Out = Out0}

    def apply[L <: HList](implicit ed: ExtractDelta[L]): Aux[L, ed.Out] = ed

    implicit def recur[H, S, D <: Coproduct, T <: HList, ND <: Coproduct](implicit next: Aux[T, ND], un: CoproductUnion[D, ND]): Aux[GameAction[H, S, D] :: T, un.Out] = new ExtractDelta[GameAction[H, S, D] :: T] {
      type Out = un.Out
    }

    implicit val hnil: Aux[HNil, CNil] = new ExtractDelta[HNil] { type Out = CNil}
  }

  object LiftAllPoly extends Poly1 {

    implicit def onAction[M, S, D, STATE, DELTA](implicit liftS: Lift[S, STATE], liftD: Lift[DELTA, D]): Case.Aux[(GameAction[M, S, D], (TypeBox[STATE], TypeBox[DELTA])), GameAction[M, STATE, DELTA]] = at[(GameAction[M, S, D], (TypeBox[STATE], TypeBox[DELTA]))] { case (action, _) =>
      action.liftState[STATE].liftDelta[DELTA]
    }
  }

  object ApplyMovePoly extends Poly1 {
    implicit def onMove[M, STATE, DELTA]: Case.Aux[(M, GameAction[M, STATE, DELTA]), List[STATE => List[DELTA]]] =
      at[(M, GameAction[M, STATE, DELTA])] { case (move, action) => action.actions.map { f => f(move, _) }
    }
  }
}