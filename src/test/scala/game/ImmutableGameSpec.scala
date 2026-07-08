package game

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import soc.base.{Applier, ImmutableGameBuilder}

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

  given Applier[TestState, Int] with
    def apply(s: TestState, d: Int): TestState = s.copy(foo = s.foo(d))

  given Applier[TestState, String] with
    def apply(s: TestState, d: String): TestState = s.copy(bar = s.bar(d))

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
