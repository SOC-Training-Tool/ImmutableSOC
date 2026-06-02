package game

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import soc.core.state.MoveCount

class ImmutableGameSpec extends AnyFunSpec with Matchers:

  case class M1(i: Int)
  case class M2()

  case class Foo(i: Int) extends GameState[Foo]:
    type Delta = Int
    override def apply(delta: Int): Foo = Foo(i + delta)

  case class Bar(s: String) extends GameState[Bar]:
    type Delta = String
    override def apply(delta: String): Bar = Bar(s + delta)

  case class TestState(foo: Foo, bar: Bar)

  // Named delta wrappers
  case class DeltaFoo(d: Int)
  case class DeltaBar(d: String)
  type TestDelta = DeltaFoo | DeltaBar | DeltaMoveCount

  case class DeltaMoveCount(d: Int)

  type MOVES = M1 | M2

  val game: ImmutableGame[MOVES, TestState, TestDelta] =
    new ImmutableGame[MOVES, TestState, TestDelta]:
      def applyMove(move: MOVES, state: TestState): (List[TestDelta], TestState) =
        val actionDeltas: List[TestDelta] = move match
          case M1(i)  => List(DeltaFoo(state.bar.s.length + i))
          case _: M2  => List(DeltaBar(state.foo.i.toString), DeltaFoo(state.foo.i))
        val allDeltas = actionDeltas :+ DeltaMoveCount(1)
        val newState  = allDeltas.foldLeft(state)(applyUpdate)
        (allDeltas, newState)

      private def applyUpdate(state: TestState, d: TestDelta): TestState = d match
        case DeltaFoo(dd)       => state.copy(foo = state.foo(dd))
        case DeltaBar(dd)       => state.copy(bar = state.bar(dd))
        case DeltaMoveCount(_)  => state  // not tracked in TestState

  describe("ImmutableGame"):

    it("should update the state on M1 action"):
      val initState = TestState(Foo(0), Bar("0"))
      val (deltas, state) = game.applyMove(M1(1), initState)
      state.foo shouldBe Foo(2)  // bar.s.length("0") = 1, i = 1, so Foo(0 + 1 + 1) = Foo(2)
      deltas.exists { case DeltaFoo(_) => true; case _ => false } shouldBe true

    it("should update the state on M2 action"):
      val initState = TestState(Foo(5), Bar("hello"))
      val (_, state) = game.applyMove(M2(), initState)
      state.bar shouldBe Bar("hello5")  // appended "5" (foo.i.toString)
      state.foo shouldBe Foo(10)        // added foo.i=5 again
