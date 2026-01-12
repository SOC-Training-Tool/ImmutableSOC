package game

import game.Delta.DeltaGen._
import org.scalatest.{FunSpec, Matchers}
import shapeless.{:+:, ::, CNil, HNil}

class ImmutableGameSpec extends FunSpec with Matchers {

  case class M1(i: Int)

  case class M2()

  case class Foo(i: Int) extends GameState[Foo] {
    type Delta = Double :+: String :+: CNil

    override def apply(delta: :+:[Double, String :+: CNil]): Foo = Foo(i + delta.select[String].fold(0)(_.toInt))
  }

  case class Bar(s: String) extends GameState[Bar] {
    type Delta = String :+: Long :+: Int :+: CNil

    override def apply(delta: :+:[String, Long :+: Int :+: CNil]): Bar = Bar(s + delta.select[String].getOrElse(""))
  }

  implicit val m1Action: GameAction[M1, Bar :: HNil, Delta[Foo] :+: CNil]                = GameAction[M1, Bar :: HNil] { case (m1, state) =>
    DeltaList().add[Foo](state.select[Bar].s + m1.i.toString).apply()
  }
  implicit val m2Action: GameAction[M2, Foo :: HNil, Delta[Foo] :+: Delta[Bar] :+: CNil] = GameAction[M2, Foo :: HNil] { case (_, state) =>
    val foo = state.select[Foo]
    DeltaList()
      .add[Bar]()
      .add[Foo](foo.i.toDouble)
      .add[Bar](foo.i)
      .apply()
  }

  val game: ImmutableGame.Aux[M1 :+: M2 :+: CNil, Foo :: Bar :: HNil, Delta[Foo] :+: Delta[Bar] :+: CNil] = ImmutableGame.apply[M1 :+: M2 :+: CNil]()

  describe("Immutable Game") {

    it("should update the state on action") {
      val move = M1(1)
      val initState = Foo(0) :: Bar("0") :: HNil

      val result = game.apply(move, initState)
      result._2.select[Foo] shouldBe Foo(1)
    }

  }


}
//
//  case object IntAction extends GameAction[Int, Int :: HNil] {
//    override def applyMove[GAME_STATE <: HList](move: Int, state: GAME_STATE)(implicit dep: DependsOn[GAME_STATE, Int :: HNil]) = {
//      val i = dep.get[Int](state)
//      dep.update(i + move, state)
//    }
//  }
//
//  case object StringAction extends GameAction[String, String :: HNil] {
//    override def applyMove[GAME_STATE <: HList](move: String, state: GAME_STATE)(implicit dep: DependsOn[GAME_STATE, String :: HNil]) = {
//      val s = dep.get[String](state)
//      dep.update(s"$s$move", state)
//    }
//  }
//
//  val stringExtension: ActionExtension[String, Double :: HNil] =
//    new ActionExtension[String, Double :: HNil] {
//      override def apply(move: String, pre: STATE, post: STATE): STATE =
//        post.updatedElem(move.toDouble)
//    }
//
//  val globalBooleanExtenstion: ActionExtension[Any, Boolean :: HNil] = ActionExtension.extension[Boolean :: HNil](booleanState => booleanState.updatedElem(true))
//
//  type GM = String :+: Int :+: CNil
//
//  describe("ImmutableGame") {
//
//    it("should update the state") {
//      val game = ImmutableGame.builder
//        .addAction(IntAction)
//        .addAction(StringAction)
//        .build
//
//      val state = "" :: 0 :: HNil
//
//      val result1 = game.apply(Coproduct[GM](1), state)
//
//      result1.select[Int] shouldBe 1
//      result1.select[String] shouldBe ""
//    }
//
//    it("should update the state with an extended action") {
//      val game = ImmutableGame.builder
//        .addAction(IntAction)
//        .addAction(StringAction.extend(stringExtension))
//        .build
//
//      val state = ImmutableGame.initialize[Double :: String :: Int :: HNil]
//
//      val result2 = game.apply(Coproduct[GM]("0.1"), state)
//
//      result2.select[Int] shouldBe 0
//      result2.select[String] shouldBe "0.1"
//      result2.select[Double] shouldBe 0.1
//    }
//
//    trait GetMoveCoproduct[H <: HList] {
//      type MOVES <: Coproduct
//      type STATE <: HList
//      type MOVE_ACTION <: Coproduct
//    }
//
//    trait BuildExtensions[M, S <: HList, MOVES <: Coproduct] extends DepFn1[ActionExtension[M, S]] {
//      type Out <: HNil
//    }
//
//    object BuildExtensions {
//
//      type Aux[M, S <: HList, MOVES <: Coproduct, Out0 <: HNil] = BuildExtensions[M, S, MOVES] { type Out = Out0 }
//
//      implicit def buildExtension[M, S <: HList, MOVES <: Coproduct, N <: Nat, LENGTH <: Nat, FILL_OUT <: HList, Out0 <: HNil](implicit
//          length: coproduct.Length.Aux[MOVES, LENGTH],
//          cat: coproduct.At.Aux[MOVES, N, M],
//          fill: hlist.Fill.Aux[LENGTH#N, Ext[Any, HNil], FILL_OUT],
//          modifier: hlist.ModifierAt.Aux[FILL_OUT, N, ActionExtension[M, HNil], ActionExtension[M, S], Out0],
//          union: hlist.Union.Aux[S, HNil, S],
//          dep: DependsOn[S, S]
//      ): Aux[M, S, MOVES, Out0] =
//        new BuildExtensions[M, S, MOVES] {
//            type Out = Out0
//            def apply(extension: ActionExtension[M, S]): Out0 = {
//                val l: LENGTH = length.apply()
//                val noopExts  = HList.fill(l)(ActionExtension.noop)
//                val exts      = modifier.apply(noopExts, _.extend(extension))
//                exts
//            }
//        }
//    }
//
//    object ExtendActionPoly extends Poly1 {
//      type Aux[M1, M2, S1 <: HList, S2 <: HList, U <: HList] = ExtendActionPoly.Case.Aux[(GameAction[M1, S1], Ext[M2, S2]), GameAction[M1, U]]
//      implicit def extend[M1, M2, S1 <: HList, S2 <: HList, U <: HList](implicit
//          ev: M1 <:< M2,
//          union: hlist.Union.Aux[S2, S1, U],
//          dep1: DependsOn[U, S1],
//          dep2: DependsOn[U, S2]
//      ): Aux[M1, M2, S1, S2, U] =
//        at { case (action, extension) => action.extend(extension) }
//    }
//
//    final case class FindMoveApply[Move, STATE <: HList](extension: ActionExtension[Move, STATE]) {
//      def apply[Actions <: HList, M <: Coproduct, S <: HList, MA <: Coproduct, N <: Nat, N_SIZE <: Nat, FILL_OUT <: HList, R <: HList, Z <: HList, OUT <: HList](actions: Actions)(implicit
//          get: GetMoveCoproduct.Aux[Actions, M, S, MA],
//          at: coproduct.At.Aux[M, N, Move],
//          length: hlist.Length.Aux[Actions, N_SIZE],
//          fill: hlist.Fill.Aux[N_SIZE#N, Ext[Any, HNil], FILL_OUT],
//          replacer: hlist.ReplaceAt.Aux[FILL_OUT, N, Ext[Move, STATE], (Ext[Any, HNil], R)],
//          zipper: hlist.Zip.Aux[Actions :: R :: HNil, Z],
//          mapper: hlist.Mapper.Aux[ExtendActionPoly.type, Z, OUT]
//      ) = {
//        val l: N_SIZE = length.apply()
//        val noopExts  = HList.fill(l)(ActionExtension.noop)
//        val exts      = replacer.apply(noopExts, extension)
//        actions.zip(exts._2).map(ExtendActionPoly)
//      }
//    }
//
//    def findMove[Move, STATE <: HList](extension: ActionExtension[Move, STATE]) = FindMoveApply[Move, STATE](extension)
//
//    it("tester") {
//      val actions: GameAction[String, String :: HNil] :: GameAction[Int, Int :: HNil] :: HNil = StringAction :: IntAction :: HNil
//
//      //val s ]= GetMoveCoproduct[GameAction[String, Double :: String :: HNil] :: GameAction[Int, Int :: HNil] :: GameAction[String, String :: HNil] :: HNil]
//
//      //val a1 = findMove[Int](actions)
//      val a2: GameAction[String, Double :: String :: HNil] :: GameAction[Int, Int :: HNil] :: HNil = findMove(stringExtension).apply(actions)
//      a2.updatedAt
//
//      // => MOVES, STATE, (MOVE -> ACTION)
//
//    }
//
////    it("condense extensions") {
////      val a: ActionExtension[String, Double :: HNil] = ActionExtension.noop.extend(stringExtension)
////      val b: Ext[String, state.MoveCount :: Double :: HNil] = stringExtension.extend(MoveCountExtension.extension)
////      val extensions = stringExtension :: globalBooleanExtenstion :: MoveCountExtension.extension :: HNil
////
////      val c = StringAction.extend(MoveCountExtension.extension)
////
////      implicitly[String <:< Any]
////
////      val f: Ext[String, state.MoveCount :: Boolean :: Double :: HNil] = ActionExtension.condense(extensions)
////    }
//  }
//}
