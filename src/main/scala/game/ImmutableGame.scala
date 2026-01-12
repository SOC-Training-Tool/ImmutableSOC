package game

import game.Delta.LiftAllDelta.Aux
import game.Delta.{ApplyDeltas, DeltaApply, ExtractState, LiftAllDelta}
import game.ImmutableGame.ApplyActionOnMove
import shapeless.ops.hlist.Union
import shapeless.ops.{coproduct, hlist}
import shapeless.{:+:, ::, <:!<, CNil, Coproduct, DepFn1, DepFn2, Generic, HList, HNil, Nat, Poly, Poly0, Poly1, Poly2}
import util.DependsOn
import util.opext.Embedder

trait ImmutableGame[MOVES <: Coproduct, STATE <: HList] {
  self =>

  type DELTA <: Coproduct

  def applyMove(move: MOVES, state: STATE): (List[DELTA], STATE)

    def apply[M, S <: HList, R <: HList, D <: Coproduct](move: M, state: S)(implicit inject: coproduct.Inject[MOVES, M], remove: hlist.RemoveAll.Aux[S, STATE, (STATE, R)], liftAllDelta: LiftAllDelta.Aux[S, D], embed: coproduct.Basis[D, DELTA]): (List[D], S) = {
      val (s, remainder) = remove.apply(state)
      val (deltas, post) = applyMove(inject.apply(move), s)
      val newState = remove.reinsert((post, remainder))
      val newDeltas =deltas.map(_.embed[D])
      (newDeltas, newState)
    }
  //
  //  case class AlignApply[M <: Coproduct, S <: HList]() {
  //    def apply[R <: HList]()(implicit basis: coproduct.Basis[MOVES, M], ra: hlist.RemoveAll.Aux[S, STATE, (STATE, R)]): ImmutableGame[M, S] = { (move: M, state: S) =>
  //      val (s, remainder) = ra.apply(state)
  //      val post = applyMove(move.embed[MOVES], s)
  //      ra.reinsert((post, remainder))
  //    }
  //  }
  //
  //  def align[M <: Coproduct, S <: HList]: AlignApply[M, S] = AlignApply[M, S]()
  //
  //  def addGlobalAction[M, S <: HList, A, OutS <: HList](action: A)(implicit ev1: Any =:= M, ev2: A <:< GameAction[M, S], un: hlist.Union.Aux[STATE, S, OutS], dep1: DependsOn[OutS, STATE], dep2: DependsOn[OutS, S]): ImmutableGame[MOVES, OutS] = {
  //    (move: MOVES, state: OutS) => {
  //      val s1 = dep1.updateAll(state)(self.applyMove(move, _))
  //      dep2.updateAll(s1)(action.apply(ev1.apply(()), _))
  //    }
  //  }
}

//case class GameBuilder[S <: HList, GS <: HList](actions: S, globalActions: GS) {
//
//  def addAction[A, Out <: HList](action: A)(implicit add: utils2.AddActions.Aux[S, A :: HNil, Out]): GameBuilder[Out, GS] = {
//    GameBuilder(add.apply(actions, action :: HNil), globalActions)
//  }
//
//  def addMultiMoveAction[MS <: Coproduct]: AddMultiMoveActionApply[MS] = AddMultiMoveActionApply[MS]()
//
//  final case class AddMultiMoveActionApply[MS <: Coproduct]() {
//    def apply[A, OutM <: HList](action: A)(implicit
//                                           multiAdd: utils2.MultiMoveAction.Aux[MS, A, OutM],
//                                           add: utils2.AddActions[S, OutM]
//    ): GameBuilder[add.Out, GS] = {
//      val newActions = add.apply(actions, multiAdd.apply(action))
//      GameBuilder(newActions, globalActions)
//    }
//  }
//
//  def addCompositeFunction[M, M2, S0 <: HList, A, AL <: HList, Out <: HList](f: M => M2)(implicit
//                                                                                         get: utils2.GetActionsForMoves.Aux[S, M2 :: HNil, AL],
//                                                                                         isHCons: hlist.IsHCons.Aux[AL, A, HNil],
//                                                                                         ev: A <:< GameAction[M2, S0],
//                                                                                         add: utils2.AddActions.Aux[S, GameAction[M, S0] :: HNil, Out]
//  ): GameBuilder[Out, GS] = {
//    val action = get.apply(actions).head
//    //val action = get.apply(actions).at[N]
//    val composeAction = action.compose(f)
//    GameBuilder(add.apply(actions, composeAction :: HNil), globalActions)
//  }
//
//  def addGlobalAction[S2 <: HList, GA, M, Out <: HList](gAction: GA)(implicit
//      ev1: GA <:< GameAction[M, S2],
//      ev2: M =:= Any,
//      prep: hlist.Prepend.Aux[GS, GA :: HNil, Out]
//  ) = GameBuilder(actions, prep.apply(globalActions, gAction :: HNil))
//
//
//  //  def merge[S2 <: HList, S2F <: HList, GS2 <: HList, C2 <: HList, SOut <: HList, GSOut <: HList, COut <: HList](other: GameBuilder2[S2, GS2, C2])(implicit
//  //      flatten: utils.Flatten.Aux[S2, S2F],
//  //      addActions: hlist.LeftFolder.Aux[S2F, S, utils2.AddActionsPoly.type, SOut],
//  //      gaPre: hlist.Prepend.Aux[GS, GS2, GSOut],
//  //      compPre: hlist.Prepend.Aux[COMP, C2, COut]
//  //  ): GameBuilder2[SOut, GSOut, COut] = {
//  //    GameBuilder2(addActions.apply(flatten.apply(other.actions), actions), gaPre.apply(globalActions, other.globalActions), compPre.apply(compositeFunctions, other.compositeFunctions))
//  //  }
//
//  def build[H0, T0 <: HList, GOut, GZOut <: HList, MOut <: HList, MS <: HList, MOVES <: Coproduct, STATE <: HList, Z1 <: Coproduct, Z2 <: Coproduct]()(implicit
//    isHCons: hlist.IsHCons.Aux[GS, H0, T0],
//    globalFold: hlist.LeftFolder.Aux[T0, H0, utils2.CollapseGlobalActions.type, GOut],
//    zip1: hlist.ZipConst.Aux[GOut, S, GZOut],
//    mapper: hlist.Mapper.Aux[utils2.MapGlobalActions.type, GZOut, MOut],
//    getMoves: utils2.GetMoves.Aux[MOut, MS],
//    toCoproduct: hlist.ToCoproduct.Aux[MS, MOVES],
//    fullState: utils2.StateHList.Aux[MOut, STATE],
//    zip2: coproduct.ZipWith.Aux[MOut, MOVES, Z1],
//    zip3: coproduct.ZipConst.Aux[STATE, Z1, Z2],
//    applyFolder: coproduct.Folder.Aux[utils2.ApplyActionFolder.type, Z2, STATE]
//  ): ImmutableGame[MOVES, STATE] = {
//    val ga = globalFold.apply(globalActions.tail, globalActions.head)
//    val as = mapper.apply(zip1.apply(ga, actions))
//    (moves: MOVES, state: STATE) =>
//      moves.zipWith(as).zipConst(state).fold(utils2.ApplyActionFolder)
//  }
//}

object ImmutableGame {

  // val builder = new GameBuilder[HNil, HNil](HNil, HNil)

  type Aux[M <: Coproduct, S <: HList, D <: Coproduct] = ImmutableGame[M, S] { type DELTA = D}

  object ApplyActionOnMove extends Poly1 {
    implicit def onMove[M, STATE <: HList, DELTA <: Coproduct, S <: HList, DL <: Coproduct](
      implicit
      action: GAction.Aux[M, S, DL],
      dep: DependsOn[STATE, S]
    ): Case.Aux[(M, STATE), List[DL]] = at { case (move, state) =>
      action.apply().foldLeft((move, state, Nil)) { case ((move, state, deltas), f) =>
        val newDeltas =

      }
    }
  }

  object EmbedDeltasPoly extends Poly1 {

    implicit def onDeltas[DL <: Coproduct, DELTA <: Coproduct]: Case.Aux[(List[DL], coproduct.Basis[DELTA, DL]), List[DELTA]] =
      at { case (deltas: List[DL], basis: coproduct.Basis[DELTA, DL]) => deltas.map(_.embed[DELTA](basis)) }
  }

  trait EmbedDeltas[DL <: Coproduct, DELTA <: Coproduct] {
    type Out <: HList

    def instances: Out
  }

  object EmbedDeltas {
    type Aux[DL <: Coproduct, DELTA <: Coproduct, Out0 <: HList] = EmbedDeltas[DL, DELTA] {type Out = Out0}

    def apply[DL <: Coproduct, DELTA <: Coproduct](implicit ed: EmbedDeltas[DL, DELTA]): Aux[DL, DELTA, ed.Out] = ed

    implicit def recur[H <: Coproduct, T <: Coproduct, DELTA <: Coproduct](implicit basis: coproduct.Basis[DELTA, H], next: EmbedDeltas[T, DELTA]): Aux[List[H] :+: T, DELTA, coproduct.Basis[DELTA, H] :: next.Out] = new EmbedDeltas[List[H] :+: T, DELTA] {

      override type Out = coproduct.Basis[DELTA, H] :: next.Out

      override def instances: Out = basis :: next.instances
    }

    implicit def cnil[C <: Coproduct]: Aux[CNil, C, HNil] = new EmbedDeltas[CNil, C] {
      type Out = HNil

      override def instances: Out = HNil
    }

  }

  final case class MakeApply[MOVE <: Coproduct, STATE <: HList, D <: Coproduct]() {
    def apply[Z1 <: Coproduct, MOut <: Coproduct, EM <: HList, Z2 <: Coproduct, Z3 <: Coproduct, F1 <: shapeless.Poly, F2 <: shapeless.Poly, F3 <: shapeless.Poly]()(
      implicit
      zipper1: coproduct.ZipConst.Aux[STATE, MOVE, Z1], // coproduct of move state tuples
      mapper1: coproduct.Mapper.Aux[F1, Z1, MOut], // coproduct of List of Sub Delta coproducts
      embedder: EmbedDeltas.Aux[MOut, D, EM], // hlist of coproduct.Basis for each Sub Delta coproduct with DELTA super
      zipper2: coproduct.ZipWith.Aux[EM, MOut, Z2], // coproduct of  List of Sub Delta coproducts and basis tuples
      mapper2: coproduct.Folder.Aux[F2, Z2, List[D]],
      zipper3: coproduct.ZipConst.Aux[STATE, D, Z3],
      folder2: coproduct.Folder.Aux[F3, Z3, STATE],
    ): Aux[MOVE, STATE, D] = new ImmutableGame[MOVE, STATE] {
      override type DELTA = D

      override def applyMove(move: MOVE, state: STATE): (List[DELTA], STATE) = {
        val zip1     : Z1      = zipper1.apply(state, move)
        val subDeltas: MOut    = mapper1.apply(zip1)
        val basis    : EM      = embedder.instances
        val zip2     : Z2      = zipper2.apply(basis, subDeltas)
        val deltas   : List[D] = mapper2.apply(zip2)
        val result   : STATE   = deltas.foldLeft(state) { case (s, delta) => folder2.apply(delta.zipConst(s)) }
        (deltas, result)
      }
    }
  }

  final case class ApplyApply[MOVE <: Coproduct]() {
    def apply[AL <: HList, STATE <: HList, D <: Coproduct, MOut <: Coproduct, EM <: HList, Z1 <: Coproduct, Z2 <: Coproduct, Z3 <: Coproduct]()(
      implicit
      actions: CollectActionState.Aux[MOVE, STATE],
      lift: LiftAllDelta.Aux[STATE, D],
      zipper1: coproduct.ZipConst.Aux[STATE, MOVE, Z1], // coproduct of move state tuples
      mapper1: coproduct.Mapper.Aux[ApplyActionOnMove.type, Z1, MOut], // coproduct of List of Sub Delta coproducts
      embedder: EmbedDeltas.Aux[MOut, D, EM], // hlist of coproduct.Basis for each Sub Delta coproduct with DELTA super
      zipper2: coproduct.ZipWith.Aux[EM, MOut, Z2], // coproduct of  List of Sub Delta coproducts and basis tuples
      mapper2: coproduct.Folder.Aux[EmbedDeltasPoly.type, Z2, List[D]],
      zipper3: coproduct.ZipConst.Aux[STATE, D, Z3],
      folder2: coproduct.Folder.Aux[ApplyDeltas.type, Z3, STATE],
    ): Aux[MOVE, STATE, D] = make[MOVE, STATE, D]()
  }

  def make[MOVE <: Coproduct, STATE <: HList, DELTA <: Coproduct] = MakeApply[MOVE, STATE, DELTA]()

  def apply[MOVE <: Coproduct] = new ApplyApply[MOVE]


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