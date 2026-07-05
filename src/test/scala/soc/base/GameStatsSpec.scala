/*
package soc.base

import game.InventorySet
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import soc.base.BaseGame.*
import soc.base.BaseGameFixtures.perfectInfoGame
import soc.base.DevelopmentCards.*
import soc.base.state.*
import soc.base.stats.*
import soc.core.*
import soc.core.DevTransactions.*
import soc.core.ResourceInventories.*
import soc.core.ResourceSet.*
import soc.core.Resources.*
import soc.core.state.*

class GameStatsSpec extends AnyFunSpec with Matchers:

  private val base = perfectInfoGame.initPerfectInfoState

  private def withResources(s: PerfectInfoState, player: Int, inv: Resources): PerfectInfoState =
    s.copy(privateInventories = PrivateInventories(s.privateInventories.m + (player -> inv)))

  private def withSettlement(s: PerfectInfoState, player: Int, v: Vertex): PerfectInfoState =
    s.copy(vertexBuildingState =
      VertexBuildingState(s.vertexBuildingState.map + (v -> PlayerBuilding(player, Settlement))))

  // Hex 15 (WOOD, 6) has vertex 41 — settlement at Vertex(41) with roll 6 gives player 0 exactly 1 WOOD
  private val rollSixState = base.copy(
    privateInventories  = PrivateInventories(Map(0 -> InventorySet.empty[Resource, Int])),
    vertexBuildingState = VertexBuildingState(Map(Vertex(41) -> PlayerBuilding(0, Settlement)))
  )

  // ── BuildStats ──────────────────────────────────────────────────────────────

  describe("BuildStats"):

    it("counts settlement for player 0"):
      val s = withResources(base, 0, ResourceSet(WOOD, BRICK, WHEAT, SHEEP))
      val (_, deltas) = PerfectInfoGame.buildSettlement.applyFull(BuildSettlementMove(0, Vertex(41)), s)
      val stats = BuildStats.update[PerfectInfoDelta](BuildStats.empty, deltas)
      stats.settlementCount(0) shouldBe 1
      stats.cityCount(0) shouldBe 0
      stats.roadCount(0) shouldBe 0

    it("counts city for player 0 (and does not double-count the replaced settlement)"):
      val s = withResources(withSettlement(base, 0, Vertex(41)), 0, ResourceSet(ORE, ORE, ORE, WHEAT, WHEAT))
        .copy(playerPoints = PlayerPoints(Map(0 -> 1)))
      val (_, deltas) = PerfectInfoGame.buildCity.applyFull(BuildCityMove(0, Vertex(41)), s)
      val stats = BuildStats.update[PerfectInfoDelta](BuildStats.empty, deltas)
      stats.cityCount(0) shouldBe 1
      // RemoveBuilding for the settlement is not counted
      stats.settlementCount(0) shouldBe 0

    it("counts road for player 0"):
      val s = withResources(base, 0, ResourceSet(WOOD, BRICK))
      val edge = Edge(Vertex(40), Vertex(41))
      val (deltas, _) = PerfectInfoGame.game.applyMove(BuildRoadMove(0, edge), s)
      val stats = BuildStats.update[PerfectInfoDelta](BuildStats.empty, deltas)
      stats.roadCount(0) shouldBe 1
      stats.settlementCount(0) shouldBe 0
      stats.cityCount(0) shouldBe 0

    it("accumulates across multiple actions"):
      val s = withResources(base, 0, ResourceSet(WOOD, BRICK, WHEAT, SHEEP, WOOD, BRICK))
      val (d1, _) = PerfectInfoGame.game.applyMove(BuildSettlementMove(0, Vertex(41)), s)
      val (d2, _) = PerfectInfoGame.game.applyMove(BuildRoadMove(0, Edge(Vertex(40), Vertex(41))), s)
      val stats = BuildStats.update[PerfectInfoDelta](
        BuildStats.update[PerfectInfoDelta](BuildStats.empty, d1), d2)
      stats.settlementCount(0) shouldBe 1
      stats.roadCount(0) shouldBe 1

  // ── DevCardStats ─────────────────────────────────────────────────────────────

  describe("DevCardStats"):

    it("tracks bought card for player 0"):
      val s = withResources(base, 0, ResourceSet(ORE, WHEAT, SHEEP)).copy(
        privateDevCardInv = PrivateDevCardInv(Map(0 -> Seq.empty)),
        turn = Turn(1)
      )
      val (_, deltas) = PerfectInfoGame.buyDevCard.applyFull(
        PerfectInfoBuyDevelopmentCardMoveResult(0, KNIGHT), s)
      val stats = DevCardStats.update(DevCardStats.empty, deltas)
      stats.boughtByPlayer(0) shouldBe Seq(KNIGHT)
      stats.playedByPlayer(0) shouldBe Nil

    it("tracks played knight for player 0"):
      val s = base.copy(
        privateDevCardInv = PrivateDevCardInv(Map(0 -> Seq((KNIGHT, 0)))),
        turn = Turn(1)
      )
      val (_, deltas) = PerfectInfoGame.playKnight.applyFull(
        PerfectInfoPlayKnightResult(PerfectInfoRobberMoveResult(0, 5, None)), s)
      val stats = DevCardStats.update(DevCardStats.empty, deltas)
      stats.playedByPlayer(0) should contain(KNIGHT)

    it("tracks both bought and played in sequence"):
      val buyState = withResources(base, 0, ResourceSet(ORE, WHEAT, SHEEP)).copy(
        privateDevCardInv = PrivateDevCardInv(Map(0 -> Seq.empty)),
        turn = Turn(1)
      )
      val (_, buyDeltas) = PerfectInfoGame.buyDevCard.applyFull(
        PerfectInfoBuyDevelopmentCardMoveResult(0, MONOPOLY), buyState)
      val playState = base.copy(
        privateDevCardInv = PrivateDevCardInv(Map(0 -> Seq((MONOPOLY, 0)))),
        turn = Turn(1)
      )
      val (_, playDeltas) = PerfectInfoGame.playMonopoly.applyFull(
        PlayMonopolyMoveResult(0, WHEAT, Map.empty), playState)
      val stats = DevCardStats.update(
        DevCardStats.update(DevCardStats.empty, buyDeltas), playDeltas)
      stats.boughtByPlayer(0) shouldBe Seq(MONOPOLY)
      stats.playedByPlayer(0) should contain(MONOPOLY)

  // ── ResourceLedger ───────────────────────────────────────────────────────────

  describe("ResourceLedger"):

    it("records loss and gain from port trade"):
      val give = ResourceSet(WOOD, WOOD, WOOD, WOOD)
      val get  = ResourceSet(ORE)
      val s = withResources(base, 0, give)
      val (_, deltas) = PerfectInfoGame.portTrade.applyFull(PortTradeMove(0, give, get), s)
      val ledger = ResourceLedger.update[PerfectInfoDelta](ResourceLedger.empty, deltas)
      ledger.lost.getOrElse(0, ResourceSet.empty[Int]).getAmount(WOOD) shouldBe 4
      ledger.gained.getOrElse(0, ResourceSet.empty[Int]).getAmount(ORE)  shouldBe 1

    it("records gain from dice roll"):
      val (_, deltas) = PerfectInfoGame.rollDice.applyFull(RollDiceMoveResult(0, 6), rollSixState)
      val ledger = ResourceLedger.update[PerfectInfoDelta](ResourceLedger.empty, deltas)
      ledger.gained.getOrElse(0, ResourceSet.empty[Int]).getAmount(WOOD) shouldBe 1

    it("netGain is zero when gained and lost cancel"):
      val give = ResourceSet(WOOD, WOOD, WOOD, WOOD)
      val get  = ResourceSet(WOOD, WOOD, WOOD, WOOD)
      val s = withResources(base, 0, give)
      val (_, deltas) = PerfectInfoGame.portTrade.applyFull(PortTradeMove(0, give, get), s)
      val ledger = ResourceLedger.update[PerfectInfoDelta](ResourceLedger.empty, deltas)
      ledger.netGain(0).getAmount(WOOD) shouldBe 0

  // ── GameStats ─────────────────────────────────────────────────────────────────

  describe("GameStats"):

    it("after roll 6 — diceRolls and resources gain WOOD, builds and devCards stay empty"):
      val (_, deltas) = PerfectInfoGame.rollDice.applyFull(RollDiceMoveResult(0, 6), rollSixState)
      val stats = GameStats.update(GameStats.empty, deltas)
      stats.diceRolls.byPlayer(0).getAmount(WOOD) shouldBe 1
      stats.resources.gained.getOrElse(0, ResourceSet.empty[Int]).getAmount(WOOD) shouldBe 1
      stats.builds.settlementCount(0) shouldBe 0
      stats.devCards.boughtByPlayer(0) shouldBe Nil

    it("after buildSettlement — builds updated, others unchanged"):
      val s = withResources(base, 0, ResourceSet(WOOD, BRICK, WHEAT, SHEEP))
      val (_, deltas) = PerfectInfoGame.buildSettlement.applyFull(BuildSettlementMove(0, Vertex(41)), s)
      val stats = GameStats.update(GameStats.empty, deltas)
      stats.builds.settlementCount(0) shouldBe 1
      stats.diceRolls.byPlayer(0).getTotal shouldBe 0

    it("accumulates across multiple delta lists"):
      val rollState = rollSixState
      val buildState = withResources(base, 0, ResourceSet(WOOD, BRICK, WHEAT, SHEEP))
      val (_, rollDeltas) = PerfectInfoGame.rollDice.applyFull(RollDiceMoveResult(0, 6), rollState)
      val (_, buildDeltas) = PerfectInfoGame.buildSettlement.applyFull(BuildSettlementMove(0, Vertex(41)), buildState)
      val stats = GameStats.update(
        GameStats.update(GameStats.empty, rollDeltas), buildDeltas)
      stats.diceRolls.byPlayer(0).getAmount(WOOD) shouldBe 1
      stats.builds.settlementCount(0) shouldBe 1
*/
