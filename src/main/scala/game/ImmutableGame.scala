package game

import game.Delta.LiftAllDelta.Aux
import game.Delta.{ApplyDeltas, DeltaApply, ExtractState, LiftAllDelta}
import shapeless.ops.hlist.Union
import shapeless.ops.{coproduct, hlist}
import shapeless.{:+:, ::, <:!<, CNil, Coproduct, DepFn1, DepFn2, Generic, HList, HNil, Nat, Poly, Poly0, Poly1, Poly2}
import util.DependsOn
import util.opext.Embedder

trait ImmutableGame[MOVES <: Coproduct, STATE <: HList] {
  self =>

  type DELTA <: Coproduct

  def applyMove(move: MOVES, state: STATE): (List[DELTA], STATE)

  //  def apply[M, S <: HList, R <: HList](move: M, state: S)(implicit inject: coproduct.Inject[MOVES, M], remove: hlist.RemoveAll.Aux[S, STATE, (STATE, R)]): S = {
  //    val (s, remainder) = remove.apply(state)
  //    val post = applyMove(inject.apply(move), s)
  //    remove.reinsert((post, remainder))
  //  }
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

  object ApplyActionOnMove extends Poly1 {
    implicit def onMove[M, STATE <: HList, DELTA <: Coproduct, S <: HList, DL <: Coproduct](
      implicit
      action: GAction.Aux[M, S, DL],
      basis: coproduct.Basis[DELTA, DL],
      dep: DependsOn[STATE, S]
    ): Case.Aux[(M, STATE), List[DELTA]] = at { case (move, state) =>
      action.apply(move, dep.getAll(state)).map(_.embed[DELTA])
    }
  }

  //
  //      at { case (move, state) => dep.updateAll(state)(s => Delta.applyState(s, action.apply(move, dep2.getAll(s))))}
  //    }


  final case class MakeApply[MOVE <: Coproduct, STATE <: HList]() {
    def apply[D <: Coproduct, Z1 <: Coproduct, Z2 <: Coproduct, F1 <: shapeless.Poly, F2 <: shapeless.Poly]()(
      implicit
      zipper1: coproduct.ZipConst.Aux[STATE, MOVE, Z1],
      folder1: coproduct.Folder.Aux[F1, Z1, List[D]],
      zipper2: coproduct.ZipConst.Aux[STATE, D, Z2],
      folder2: coproduct.Folder.Aux[F2, Z2, STATE],
    ): ImmutableGame[MOVE, STATE] = new ImmutableGame[MOVE, STATE] {
      override type DELTA = D

      override def applyMove(move: MOVE, state: STATE): (List[DELTA], STATE) = {
        val deltas: List[D] = folder1.apply(zipper1.apply(state, move))
        val result          = deltas.foldLeft(state) { case (s, delta) => folder2.apply(delta.zipConst(s)) }
        (deltas, result)
      }
    }
  }

  final case class ApplyApply[MOVE <: Coproduct]() {
    def apply[AL <: HList, STATE <: HList, D <: Coproduct, Z1 <: Coproduct, Z2 <: Coproduct]()(
      implicit
      actions: CollectActions.Aux[MOVE, AL],


      collect: utils2.CollectState.Aux[M, STATE],
      lift: LiftAllDelta.Aux[STATE, D],
      z1: coproduct.ZipConst.Aux[STATE, MOVE, Z1],
      folder1: coproduct.Folder.Aux[ApplyActionOnMove.type, Z1, List[D]],
      z2: coproduct.ZipConst.Aux[STATE, D, Z2],
      folder2: coproduct.Folder.Aux[ApplyDeltas.type, Z2, STATE]
    ): ImmutableGame[MOVE, STATE] = make[MOVE, STATE]()
  }

  def make[MOVE <: Coproduct, STATE <: HList] = MakeApply[MOVE, STATE]()

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

object utils2 {

  trait CollectState[L <: HList] extends Serializable {
    type Out <: HList
  }

  object CollectState {

    type Aux[L <: HList, Out0 <: HList] = CollectState[L] {type Out = Out0}

    def apply[L <: HList](implicit collect: CollectState[L]): Aux[L, collect.Out] = collect

    implicit def recur[H, T <: HList, S1 <: HList, DL <: Coproduct, S2 <: HList, S3 <: HList, S4 <: HList](
      implicit
      action: GAction.Aux[H, S1, DL],
      extractState: ExtractState.Aux[DL, S2],
      un1: Union.Aux[S1, S2, S3],
      next: Aux[T, S4],
      un2: Union[S3, S4]
    ): Aux[H :: T, un2.Out] = new CollectState[H :: T] {
      type Out = un2.Out
    }

    implicit val hnil: Aux[HNil, HNil] = new CollectState[HNil] {
      type Out = HNil
    }
  }

  trait GetMoves[L <: HList] extends Serializable {
    type Out <: HList
  }

  object GetMoves {

    type Aux[L <: HList, Out0 <: HList] = GetMoves[L] {type Out = Out0}

    def apply[L <: HList](implicit get: GetMoves[L]): Aux[L, get.Out] = get

    implicit def recur[H, T <: HList, M, S <: HList](implicit ev: H <:< GameAction[M, S], next: GetMoves[T]): Aux[H :: T, M :: next.Out] = new GetMoves[H :: T] {
      override type Out = M :: next.Out
    }

    implicit val hnil: Aux[HNil, HNil] = new GetMoves[HNil] {
      type Out = HNil
    }
  }

  trait GetActionsForMoves[L <: HList, M <: HList] extends DepFn1[L] {
    type Out <: HList
  }

  object GetActionsForMoves {

    type Aux[L <: HList, M <: HList, Out0 <: HList] = GetActionsForMoves[L, M] {type Out = Out0}

    def apply[L <: HList, M <: HList](implicit get: GetActionsForMoves[L, M]): Aux[L, M, get.Out] = get

    implicit def move[L <: HList, H, T <: HList, ML <: HList, N <: Nat, A](implicit moves: GetMoves.Aux[L, ML], at1: hlist.At.Aux[ML, N, H], at2: hlist.At.Aux[L, N, A], next: GetActionsForMoves[L, T]): Aux[L, H :: T, A :: next.Out] = new GetActionsForMoves[L, H :: T] {
      override type Out = A :: next.Out

      override def apply(t: L): Out = at2.apply(t) :: next.apply(t)
    }

    implicit def hnil[L <: HList]: Aux[L, HNil, HNil] = new GetActionsForMoves[L, HNil] {
      override type Out = HNil

      override def apply(t: L): Out = HNil
    }
  }

  trait AddActions[L <: HList, AL <: HList] extends DepFn2[L, AL] {
    override type Out <: HList
  }

  object AddActions {

    object ComposeActionMapper extends Poly2 {

      implicit def atCase[A1, A2, M, S1 <: HList, S2 <: HList, SOut <: HList](
        implicit
        ev1: A1 <:< GameAction[M, S1],
        ev2: A2 <:< GameAction[M, S2],
        un: Union.Aux[S1, S2, SOut],
        dep1: DependsOn[SOut, S1],
        dep2: DependsOn[SOut, S2]) = at { (a1: A1, a2: A2) => a1.extend(a2) }
    }

    type Aux[AL <: HList, L <: HList, Out0 <: HList] = AddActions[AL, L] {type Out = Out0}

    def apply[AL <: HList, L <: HList](implicit add: AddActions[AL, L]): Aux[AL, L, add.Out] = add

    implicit def add[L <: HList, AL <: HList, ML <: HList, AML <: HList, Shared <: HList, R1 <: HList, R2 <: HList, O1 <: HList, O2 <: HList, O3 <: HList, O4 <: HList, Z <: HList, P1 <: HList](
      implicit
      allMoves: GetMoves.Aux[L, ML],
      addMoves: GetMoves.Aux[AL, AML],
      shared: hlist.Intersection.Aux[ML, AML, Shared],
      remove1: hlist.RemoveAll.Aux[AML, Shared, (Shared, R1)],
      remove2: hlist.RemoveAll.Aux[ML, Shared, (Shared, R2)],
      get1: GetActionsForMoves.Aux[L, Shared, O1],
      get2: GetActionsForMoves.Aux[AL, Shared, O2],
      get3: GetActionsForMoves.Aux[L, R2, O3],
      get4: GetActionsForMoves.Aux[AL, R1, O4],
      zip: hlist.ZipWith.Aux[O1, O2, ComposeActionMapper.type, Z],
      prep1: hlist.Prepend.Aux[O4, Z, P1],
      prep2: hlist.Prepend[P1, O3]
    ): Aux[L, AL, prep2.Out] = new AddActions[L, AL] {
      override type Out = prep2.Out

      override def apply(t: L, u: AL): Out = {
        val a = get1.apply(t)
        val b = get2.apply(u)
        val c = get3.apply(t)
        val d = get4.apply(u)
        val e = zip.apply(a, b)
        val f = prep1.apply(d, e)
        prep2.apply(f, c)
      }
    }
  }

  trait ApplyComposite[L <: HList, A, B] extends DepFn2[L, A => B] {
    type Out <: HList
  }

  object ApplyComposite {

    type Aux[L <: HList, A, B, Out0 <: HList] = ApplyComposite[L, A, B] {type Out = Out0}

    def apply[L <: HList, A, B](implicit ac: ApplyComposite[L, A, B]): Aux[L, A, B, ac.Out] = ac

    implicit def composite[H, T <: HList, A, B, S <: HList](
      implicit ev1: H <:< GameAction[B, S]
    ): ApplyComposite.Aux[H :: T, A, B, H :: GameAction[A, S] :: T] = new ApplyComposite[H :: T, A, B] {
      override type Out = H :: GameAction[A, S] :: T

      override def apply(t: H :: T, u: A => B): Out = {
        val action = t.head
        action :: action.compose(u) :: t.tail
      }
    }

    implicit def recur[H, T <: HList, M, A, B, S <: HList](
      implicit
      ev1: H <:< GameAction[M, S],
      ev2: M <:!< B,
      next: ApplyComposite[T, A, B]
    ): ApplyComposite.Aux[H :: T, A, B, H :: next.Out] = new ApplyComposite[H :: T, A, B] {
      override type Out = H :: next.Out

      override def apply(t: H :: T, u: A => B): Out = t.head :: next.apply(t.tail, u)
    }

    implicit def hnil[A, B]: ApplyComposite[HNil, A, B] = new ApplyComposite[HNil, A, B] {
      override type Out = HNil

      override def apply(t: HNil, u: A => B): HNil = HNil
    }
  }

  final case class CompositeFunction[A, B](f: A => B)

  object FoldCompositeFunctions extends Poly2 {

    implicit def onComp[L <: HList, H, A, B, Out <: HList](implicit ev: H <:< CompositeFunction[A, B], applyComp: ApplyComposite.Aux[L, A, B, Out]): Case.Aux[L, H, Out] = at { (l, comp) =>
      applyComp.apply(l, comp.f)
    }
  }

  trait MovesCoproduct[L <: HList] extends Serializable {
    type Out <: Coproduct
  }

  object MovesCoproduct {

    type Aux[L <: HList, Out0 <: Coproduct] = MovesCoproduct[L] {type Out = Out0}

    def apply[L <: HList](implicit mc: MovesCoproduct[L]): Aux[L, mc.Out] = mc

    implicit def recur[H, T <: HList, M, S <: HList](implicit ev: H <:< GameAction[M, S], next: MovesCoproduct[T]): Aux[H :: T, M :+: next.Out] = new MovesCoproduct[H :: T] {
      type Out = M :+: next.Out
    }

    implicit val hnilMove: Aux[HNil, CNil] = new MovesCoproduct[HNil] {
      type Out = CNil
    }
  }

  trait StateHList[L <: HList] extends Serializable {
    type Out <: HList
  }

  object StateHList {

    type Aux[L <: HList, Out0 <: HList] = StateHList[L] {type Out = Out0}

    def apply[L <: HList](implicit sh: StateHList[L]): Aux[L, sh.Out] = sh

    implicit def row[H, T <: HList, M, S <: HList, TOut <: HList, Out0 <: HList](
      implicit
      ev: H <:< GameAction[M, S],
      next: StateHList.Aux[T, TOut],
      un: hlist.Union.Aux[S, TOut, Out0]
    ): Aux[H :: T, Out0] = new StateHList[H :: T] {
      type Out = Out0
    }

    implicit val hnilState: Aux[HNil, HNil] = new StateHList[HNil] {
      type Out = HNil
    }
  }

  trait ComposeMove[A, B] extends Serializable {
    type S <: HList

    def apply(a: A, s: S): B
  }

  object ComposeMove {

    type Aux[A, B, S0 <: HList] = ComposeMove[A, B] {type S = S0}

    def apply[A, B](implicit comp: ComposeMove[A, B]): Aux[A, B, comp.S] = comp

    def at[A, B](f: A => B): Aux[A, B, HNil] = new ComposeMove[A, B] {
      type S = HNil

      def apply(a: A, s: S): B = f(a)
    }

    def at[A, B, S0 <: HList](f: (A, S0) => B): Aux[A, B, S0] = new ComposeMove[A, B] {
      override type S = S0

      override def apply(a: A, s: S0): B = f(a, s)
    }

  }

  trait MultiMoveAction[MOVES <: Coproduct, A] extends DepFn1[A] {
    type Out <: HList
  }

  object MultiMoveAction {


    type Aux[MOVES <: Coproduct, A, Out0 <: HList] = MultiMoveAction[MOVES, A] {type Out = Out0}

    def apply[MOVES <: Coproduct, A](implicit m: MultiMoveAction[MOVES, A]): Aux[MOVES, A, m.Out] = m

    implicit def cnil[A]: Aux[CNil, A, HNil] = new MultiMoveAction[CNil, A] {
      type Out = HNil

      def apply(a: A): HNil = HNil
    }

    implicit def recur[H, T <: Coproduct, A, M, S <: HList, S0 <: HList, Out0 <: HList](
      implicit
      ev: A <:< GameAction[M, S],
      folder: ComposeMove.Aux[H, M, S0],
      un: hlist.Union.Aux[S, S0, Out0],
      s1: hlist.SelectAll[Out0, S],
      s2: hlist.SelectAll[Out0, S0],
      next: MultiMoveAction[T, A]
    ): Aux[H :+: T, A, GameAction[H, Out0] :: next.Out] = new MultiMoveAction[H :+: T, A] {
      type Out = GameAction[H, Out0] :: next.Out

      def apply(a: A): GameAction[H, Out0] :: next.Out =
        a.composeS[H, S0, Out0](folder.apply) :: next.apply(a)
    }
  }

  object CollapseGlobalActions extends Poly2 {
    implicit def extend[A, B, Out](implicit ex: ExtendAction.Aux[A, B, Out]): Case.Aux[A, B, Out] = at(ex.apply)
  }

  object MapGlobalActions extends Poly1 {
    implicit def extend[A, B, Out](implicit ex: ExtendAction.Aux[A, B, Out]): Case.Aux[(A, B), Out] = at((ex.apply _).tupled)
  }

  object ApplyActionFolder extends Poly1 {
    implicit def applyMove[M, S <: HList, S0 <: HList, A](implicit ev: A <:< GameAction[M, S], dep: DependsOn[S0, S]): Case.Aux[((M, A), S0), S0] =
      at[((M, A), S0)] { case ((move, action), state) => dep.updateAll(state)(action.apply(move, _)) }

  }
}
