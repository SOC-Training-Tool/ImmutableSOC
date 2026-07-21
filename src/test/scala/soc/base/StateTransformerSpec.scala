package soc.base

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class StateTransformerSpec extends AnyFunSpec with Matchers {

  private final case class OptionalIntDeltaOutput(delta: Option[IntGameState.Delta])
  private final case class IntDeltaListOutput(deltas: List[IntGameState.Delta])
  private final case class UnionDeltaListOutput(deltas: List[Int | String])
  private final case class OrderedUnionOutput(first: List[Int | String], second: Option[Int | String])
  private final case class AtomicIntDeltaOutput(delta: IntGameState.Delta)

  describe("TupleUpdater") {
    it("head match: applies a delta to the first element when its Delta type matches") {
      val updater = summon[TupleUpdater[IntGameState *: StringGameState *: EmptyTuple, IntGameState.Delta]]
      updater.update(
        IntGameState(1) *: StringGameState("a") *: EmptyTuple,
        IntGameState.Delta(3)
      ).shouldBe(IntGameState(4) *: StringGameState("a") *: EmptyTuple)
    }

    it("tail recursion: skips a non-matching head and updates the matching tail element") {
      val updater = summon[TupleUpdater[IntGameState *: StringGameState *: EmptyTuple, StringGameState.Delta]]
      updater.update(
        IntGameState(1) *: StringGameState("a") *: EmptyTuple,
        StringGameState.Delta("b")
      ).shouldBe(IntGameState(1) *: StringGameState("b") *: EmptyTuple)
    }

    it("matches a UnionGameState head when the delta is an Int") {
      val updater = summon[TupleUpdater[UnionGameState *: IntGameState *: EmptyTuple, Int]]
      updater.update(
        UnionGameState(1, "a") *: IntGameState(10) *: EmptyTuple,
        5
      ).shouldBe(UnionGameState(6, "a") *: IntGameState(10) *: EmptyTuple)
    }

    it("matches a UnionGameState tail when the delta is a String") {
      val updater = summon[TupleUpdater[IntGameState *: UnionGameState *: EmptyTuple, String]]
      updater.update(
        IntGameState(10) *: UnionGameState(1, "a") *: EmptyTuple,
        "z"
      ).shouldBe(IntGameState(10) *: UnionGameState(1, "az") *: EmptyTuple)
    }

    it("single-element tuple: applies a delta to the sole element") {
      val updater = summon[TupleUpdater[IntGameState *: EmptyTuple, IntGameState.Delta]]
      updater.update(
        IntGameState(5) *: EmptyTuple,
        IntGameState.Delta(2)
      ).shouldBe(IntGameState(7) *: EmptyTuple)
    }
  }

  describe("BulkTupleUpdater") {
    it("empty deltas: returns the tuple unchanged") {
      val bulk = summon[BulkTupleUpdater[IntGameState *: StringGameState *: EmptyTuple, EmptyTuple]]
      bulk.updateAll(
        IntGameState(1) *: StringGameState("a") *: EmptyTuple,
        EmptyTuple
      ).shouldBe(IntGameState(1) *: StringGameState("a") *: EmptyTuple)
    }

    it("recursive: applies a tuple of distinct deltas in order") {
      val bulk = summon[BulkTupleUpdater[
        IntGameState *: StringGameState *: EmptyTuple,
        IntGameState.Delta *: StringGameState.Delta *: EmptyTuple
      ]]
      bulk.updateAll(
        IntGameState(1) *: StringGameState("a") *: EmptyTuple,
        IntGameState.Delta(3) *: StringGameState.Delta("z") *: EmptyTuple
      ).shouldBe(IntGameState(4) *: StringGameState("z") *: EmptyTuple)
    }

    it("partial deltas: applies fewer deltas than elements, leaving the rest untouched") {
      val bulk = summon[BulkTupleUpdater[
        IntGameState *: StringGameState *: EmptyTuple,
        IntGameState.Delta *: EmptyTuple
      ]]
      bulk.updateAll(
        IntGameState(1) *: StringGameState("a") *: EmptyTuple,
        IntGameState.Delta(3) *: EmptyTuple
      ).shouldBe(IntGameState(4) *: StringGameState("a") *: EmptyTuple)
    }
  }

  describe("compile-time negative scenarios") {
    it("assertTypeError: no TupleUpdater instance when the delta type does not match any element") {
      assertTypeError("""summon[TupleUpdater[StringGameState *: EmptyTuple, IntGameState.Delta]]""")
    }

    it("assertTypeError: no TupleUpdater instance for an EmptyTuple-only state with a mismatched delta") {
      assertTypeError("""summon[TupleUpdater[EmptyTuple.type, IntGameState.Delta]]""")
    }

    it("assertTypeError: no BulkTupleUpdater instance when a trailing delta has no matching element") {
      assertTypeError(
        """summon[BulkTupleUpdater[IntGameState *: EmptyTuple, IntGameState.Delta *: StringGameState.Delta *: EmptyTuple]]"""
      )
    }
  }

  describe("applyDelta on Tuple") {
    it("single delta overload: applies a delta to the head element") {
      val state = IntGameState(1) *: StringGameState("a") *: EmptyTuple
      state.applyDelta(IntGameState.Delta(3))
        .shouldBe(IntGameState(4) *: StringGameState("a") *: EmptyTuple)
    }

    it("single delta overload: applies a delta to a tail element") {
      val state = IntGameState(1) *: StringGameState("a") *: EmptyTuple
      state.applyDelta(StringGameState.Delta("z"))
        .shouldBe(IntGameState(1) *: StringGameState("z") *: EmptyTuple)
    }

    it("tuple-of-deltas overload: applies distinct deltas in order") {
      val state = IntGameState(1) *: StringGameState("a") *: EmptyTuple
      val deltas = IntGameState.Delta(3) *: StringGameState.Delta("z") *: EmptyTuple
      state.applyDelta(deltas)
        .shouldBe(IntGameState(4) *: StringGameState("z") *: EmptyTuple)
    }

    it("applyDeltaProduct overload: converts a product of deltas and applies it") {
      val state = IntGameState(1) *: StringGameState("a") *: EmptyTuple
      state.applyDeltaProduct(StatePairDeltas(IntGameState.Delta(3), StringGameState.Delta("z")))
        .shouldBe(IntGameState(4) *: StringGameState("z") *: EmptyTuple)
    }
  }

  describe("applyDelta on Product container") {
    it("single delta overload: applies a delta to the head field and rebuilds the case class") {
      val state = StatePair(IntGameState(1), StringGameState("a"))
      state.applyDelta(IntGameState.Delta(3))
        .shouldBe(StatePair(IntGameState(4), StringGameState("a")))
    }

    it("single delta overload: applies a delta to a non-head field and rebuilds the case class") {
      val state = StatePair(IntGameState(1), StringGameState("a"))
      state.applyDelta(StringGameState.Delta("z"))
        .shouldBe(StatePair(IntGameState(1), StringGameState("z")))
    }

    it("tuple-of-deltas overload: applies a tuple of deltas and rebuilds the case class") {
      val state = StatePair(IntGameState(1), StringGameState("a"))
      val deltas = IntGameState.Delta(3) *: StringGameState.Delta("z") *: EmptyTuple
      state.applyDelta(deltas)
        .shouldBe(StatePair(IntGameState(4), StringGameState("z")))
    }

    it("applyDeltaProduct overload: applies a product of deltas and rebuilds the case class") {
      val state = StatePair(IntGameState(1), StringGameState("a"))
      state.applyDeltaProduct(StatePairDeltas(IntGameState.Delta(3), StringGameState.Delta("z")))
        .shouldBe(StatePair(IntGameState(4), StringGameState("z")))
    }
  }

  describe("compile-time negative scenarios for applyDelta") {
    it("assertTypeError: applying a non-matching delta via applyDelta does not compile") {
      assertTypeError("""{
        val state = IntGameState(1) *: IntGameState(2) *: EmptyTuple
        state.applyDelta(StringGameState.Delta("x"))
      }""")
    }
  }

  describe("StateTransformer.updateFlexible") {
    it("applies a Some delta field") {
      val update = StateTransformer.updateFlexible[Unit, StatePair, OptionalIntDeltaOutput](
        (_, _) => OptionalIntDeltaOutput(Some(IntGameState.Delta(3)))
      )

      update((), StatePair(IntGameState(1), StringGameState("a"))).shouldBe((
        OptionalIntDeltaOutput(Some(IntGameState.Delta(3))),
        StatePair(IntGameState(4), StringGameState("a"))
      ))
    }

    it("leaves state unchanged for a None delta field") {
      val update = StateTransformer.updateFlexible[Unit, StatePair, OptionalIntDeltaOutput](
        (_, _) => OptionalIntDeltaOutput(None)
      )

      update((), StatePair(IntGameState(1), StringGameState("a"))).shouldBe((
        OptionalIntDeltaOutput(None),
        StatePair(IntGameState(1), StringGameState("a"))
      ))
    }

    it("leaves state unchanged for an empty delta list") {
      val update = StateTransformer.updateFlexible[Unit, StatePair, IntDeltaListOutput](
        (_, _) => IntDeltaListOutput(Nil)
      )

      update((), StatePair(IntGameState(1), StringGameState("a"))).shouldBe((
        IntDeltaListOutput(Nil),
        StatePair(IntGameState(1), StringGameState("a"))
      ))
    }

    it("applies a single-element delta list") {
      val update = StateTransformer.updateFlexible[Unit, StatePair, IntDeltaListOutput](
        (_, _) => IntDeltaListOutput(List(IntGameState.Delta(3)))
      )

      update((), StatePair(IntGameState(1), StringGameState("a"))).shouldBe((
        IntDeltaListOutput(List(IntGameState.Delta(3))),
        StatePair(IntGameState(4), StringGameState("a"))
      ))
    }

    it("applies a multi-element delta list in order") {
      val update = StateTransformer.updateFlexible[Unit, UnionGameState *: EmptyTuple, UnionDeltaListOutput](
        (_, _) => UnionDeltaListOutput(List(2, "a", 3, "b"))
      )

      update((), UnionGameState(1, "") *: EmptyTuple).shouldBe((
        UnionDeltaListOutput(List(2, "a", 3, "b")),
        UnionGameState(6, "ab") *: EmptyTuple
      ))
    }

    it("applies output product fields in declaration order") {
      val update = StateTransformer.updateFlexible[Unit, UnionGameState *: EmptyTuple, OrderedUnionOutput](
        (_, _) => OrderedUnionOutput(List("a"), Some("b"))
      )

      update((), UnionGameState(1, "") *: EmptyTuple).shouldBe((
        OrderedUnionOutput(List("a"), Some("b")),
        UnionGameState(1, "ab") *: EmptyTuple
      ))
    }

    it("applies a delta case class atomically rather than decomposing its fields") {
      val update = StateTransformer.updateFlexible[Unit, StatePair, AtomicIntDeltaOutput](
        (_, _) => AtomicIntDeltaOutput(IntGameState.Delta(3))
      )

      update((), StatePair(IntGameState(1), StringGameState("a"))).shouldBe((
        AtomicIntDeltaOutput(IntGameState.Delta(3)),
        StatePair(IntGameState(4), StringGameState("a"))
      ))
    }

    it("assertTypeError: Option containing a non-delta does not compile") {
      assertTypeError("""{
        final case class BadDelta(by: Int)
        summon[FieldApplier[StatePair, Option[BadDelta]]]
      }""")
    }

    it("assertTypeError: List containing a non-delta does not compile") {
      assertTypeError("""{
        final case class BadDelta(by: Int)
        summon[FieldApplier[StatePair, List[BadDelta]]]
      }""")
    }
  }
}
