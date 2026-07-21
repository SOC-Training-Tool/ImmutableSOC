package soc.base.actions

import game.{InventorySet, NoInput}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import soc.base.*
import soc.base.DevelopmentCards.*
import soc.base.state.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.Resources.*
import soc.core.state.*

class LargestArmySpec extends AnyFunSpec with Matchers:

  private def input(
    counts: PlayerArmyCount = PlayerArmyCount(Map.empty),
    holder: LargestArmyPlayer = LargestArmyPlayer(None)
  ) = KnightInput(Turn(1), counts, holder)

  describe("PlayPerfectKnightAction") {
    it("awards largest army and +2 points on the third knight") {
      val action = PlayPerfectKnightAction()
      val move   = PerfectInfoPlayKnightResult(PerfectInfoRobberMoveResult[Resource](0, 5, None))
      val result = action(move, input(counts = PlayerArmyCount(Map(0 -> 2))))

      result.playerArmyCountChange shouldBe Some(SpecialCounts.Increment(0))
      result.largestArmyPlayerChange shouldBe List(SpecialPlayer.Set(0))
      result.largestArmyPointChanges shouldBe List(PlayerPoints.Increment(0), PlayerPoints.Increment(0))
    }

    it("keeps the award with the first player to reach three knights on a tie") {
      val action = PlayPerfectKnightAction()
      val move   = PerfectInfoPlayKnightResult(PerfectInfoRobberMoveResult[Resource](1, 5, None))
      val counts = PlayerArmyCount(Map(0 -> 3, 1 -> 2))
      val holder = LargestArmyPlayer(Some(0))
      val result = action(move, input(counts, holder))

      result.playerArmyCountChange shouldBe Some(SpecialCounts.Increment(1))
      result.largestArmyPlayerChange shouldBe Nil
      result.largestArmyPointChanges shouldBe Nil
    }

    it("moves the award when a player strictly exceeds the current holder") {
      val action = PlayPerfectKnightAction()
      val move   = PerfectInfoPlayKnightResult(PerfectInfoRobberMoveResult[Resource](1, 5, None))
      val counts = PlayerArmyCount(Map(0 -> 3, 1 -> 3))
      val holder = LargestArmyPlayer(Some(0))
      val result = action(move, input(counts, holder))

      result.playerArmyCountChange shouldBe Some(SpecialCounts.Increment(1))
      result.largestArmyPlayerChange shouldBe List(SpecialPlayer.Remove, SpecialPlayer.Set(1))
      result.largestArmyPointChanges shouldBe List(
        PlayerPoints.Decrement(0), PlayerPoints.Decrement(0),
        PlayerPoints.Increment(1), PlayerPoints.Increment(1)
      )
    }
  }

  describe("PlayPublicKnightAction") {
    it("mirrors the perfect-info largest army deltas without exposing robber steals") {
      val action = PlayPublicKnightAction()
      val move   = PlayKnightResult[Resource](RobberMoveResult[Resource](0, 5, None))
      val result = action(move, input(counts = PlayerArmyCount(Map(0 -> 2))))

      result.playerArmyCountChange shouldBe Some(SpecialCounts.Increment(0))
      result.largestArmyPlayerChange shouldBe List(SpecialPlayer.Set(0))
      result.largestArmyPointChanges shouldBe List(PlayerPoints.Increment(0), PlayerPoints.Increment(0))
      result.steal shouldBe None
    }
  }
