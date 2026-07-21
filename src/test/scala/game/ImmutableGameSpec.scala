package game

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import soc.base.ImmutableGameBuilder

class ImmutableGameSpec extends AnyFunSpec with Matchers:

  case class M1(i: Int)
  case class M2()
  case class M3()

  case class Foo(i: Int) extends GameState[Foo]:
    type Delta = Int
    override def apply(delta: Int): Foo = Foo(i + delta)

  case class Bar(s: String) extends GameState[Bar]:
    type Delta = String
    override def apply(delta: String): Bar = Bar(s + delta)

  case class TestState(foo: Foo, bar: Bar)

  case class M1Output(delta: Int)
  case class M2Output(fooDelta: Int, barDelta: String)

  class M1Action extends GameAction[M1, NoInput.type, M1Output]:
    def apply(move: M1, input: NoInput.type): M1Output = M1Output(move.i)

  class M2Action extends GameAction[M2, TestState, M2Output]:
    def apply(move: M2, input: TestState): M2Output =
      M2Output(input.foo.i, input.foo.i.toString)

  val game = ImmutableGameBuilder[TestState]
    .register(M1Action())
    .register(M2Action())
    .build

  describe("ImmutableGameBuilder"):

    it("should register M1 and return typed M1Output"):
      val initState = TestState(Foo(0), Bar("hello"))
      val (out, state) = game.applyMove(M1(3), initState)
      out shouldBe M1Output(3)
      state shouldBe TestState(Foo(3), Bar("hello"))

    it("narrows the return type to the specific action output"):
      val initState = TestState(Foo(5), Bar("hello"))
      val (out, state) = game.applyMove(M2(), initState)
      out shouldBe M2Output(5, "5")
      state shouldBe TestState(Foo(10), Bar("hello5"))

    it("applyMoveAny accepts a union-typed move list and returns a union output"):
      val initState = TestState(Foo(0), Bar("hello"))
      val moves: List[M1 | M2] = List(M1(3), M2(), M1(2))
      val finalState = moves.foldLeft(initState) { (s, m) =>
        game.applyMoveAny(m, s)._2
      }
      finalState shouldBe TestState(Foo(8), Bar("hello3"))

    it("AllOutputs is the union of the registered action output types"):
      summon[game.AllOutputs <:< (M1Output | M2Output)]
      summon[(M1Output | M2Output) <:< game.AllOutputs]

    it("applyMoveAny result can be pattern-matched exhaustively on its output union"):
      val initState = TestState(Foo(0), Bar("hello"))
      val move: M1 | M2 = M1(3)
      val (out, state) = game.applyMoveAny(move, initState)
      out match
        case _: M1Output => state shouldBe TestState(Foo(3), Bar("hello"))
        case _: M2Output => fail("expected M1Output")

    it("applyMoveAny returns the correct delta value for M1"):
      val initState = TestState(Foo(0), Bar("hello"))
      val move: M1 | M2 = M1(3)
      val (out, state) = game.applyMoveAny(move, initState)
      out match
        case M1Output(delta) =>
          delta shouldBe 3
          state shouldBe TestState(Foo(3), Bar("hello"))
        case _ => fail("expected M1Output")

    it("applyMoveAny returns the correct delta values for M2"):
      val initState = TestState(Foo(5), Bar("hello"))
      val move: M1 | M2 = M2()
      val (out, state) = game.applyMoveAny(move, initState)
      out match
        case M2Output(fooDelta, barDelta) =>
          fooDelta shouldBe 5
          barDelta shouldBe "5"
          state shouldBe TestState(Foo(10), Bar("hello5"))
        case _ => fail("expected M2Output")

    it("applyMoveAny returns the same output and state as applyMove"):
      val initState = TestState(Foo(7), Bar("hi"))
      val move = M1(4)
      val (typedOut, typedState) = game.applyMove(move, initState)
      val (anyOut, anyState) = game.applyMoveAny(move, initState)
      anyState shouldBe typedState
      anyOut match
        case o: M1Output => o shouldBe typedOut
        case _           => fail("expected M1Output")
