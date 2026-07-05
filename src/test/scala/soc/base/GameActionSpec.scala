/*
package soc.base

import game.{Delta, InventorySet}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import soc.base.BaseGame.*
import soc.base.BaseGameFixtures.perfectInfoGame
import soc.base.BaseGameFixtures.imperfectInfoGame
import soc.base.DevelopmentCards.*
import soc.base.state.*
import soc.base.stats.{DiceRollStats, GainDeltas, GameStats}
import soc.core.*
import soc.core.DevTransactions.*
import soc.core.ResourceInventories.*
import soc.core.ResourceSet.*
import soc.core.Resources.*
import soc.core.state.*

class GameActionSpec extends AnyFunSpec with Matchers:

  // ── helpers ─────────────────────────────────────────────────────────────────

  private val board = perfectInfoGame.board
  private val base  = perfectInfoGame.initPerfectInfoState

  // Produce a state after the first N test moves (used to get populated board state)
  private def stateAfter(n: Int): PerfectInfoState =
    perfectInfoGame.testMoveResults.take(n).foldLeft(base) { (s, m) =>
      PerfectInfoGame.game.applyMove(m, s)._2
    }

  private def withResources(s: PerfectInfoState, player: Int, inv: Resources): PerfectInfoState =
    s.copy(privateInventories = PrivateInventories(s.privateInventories.m + (player -> inv)))

  private def withSettlement(s: PerfectInfoState, player: Int, v: Vertex): PerfectInfoState =
    s.copy(vertexBuildingState =
      VertexBuildingState(s.vertexBuildingState.map + (v -> PlayerBuilding(player, Settlement))))

  private val postPlacements: PerfectInfoState = stateAfter(8)  // after all 8 initial placements

  // ── PerfectInfoGame ─────────────────────────────────────────────────────────

  describe("PerfectInfoGame.endTurn"):

    it("increments Turn by 1"):
      val (newState, deltas) = PerfectInfoGame.endTurn.applyFull(EndTurnMove(0), base)
      newState.turn shouldBe Turn(1)
      deltas should have length 1

    it("produces a Delta[Turn]"):
      val (_, deltas) = PerfectInfoGame.endTurn.applyFull(EndTurnMove(0), base)
      deltas.head shouldBe a[Delta[?]]

  describe("PerfectInfoGame.buildSettlement"):

    it("places settlement, deducts inventory, adds point"):
      val settleCost = ResourceSet(WOOD, BRICK, WHEAT, SHEEP)
      val s = withResources(base, 0, settleCost)
      val (newState, deltas) = PerfectInfoGame.buildSettlement.applyFull(
        BuildSettlementMove(0, Vertex(41)), s)

      newState.vertexBuildingState.map.get(Vertex(41)) shouldBe
        Some(PlayerBuilding(0, Settlement))
      newState.playerPoints.points.getOrElse(0, 0) shouldBe 1
      newState.privateInventories.m(0).getTotal shouldBe 0
      newState.bank.b.getTotal shouldBe (base.bank.b.getTotal + settleCost.getTotal)

    it("produces VBS, PrivI, Bank, PP deltas"):
      val s = withResources(base, 0, ResourceSet(WOOD, BRICK, WHEAT, SHEEP))
      val (_, deltas) = PerfectInfoGame.buildSettlement.applyFull(BuildSettlementMove(0, Vertex(41)), s)
      def hasType(runtimeClass: Class[?]) =
        deltas.exists { case d: Delta[?] => d.ct.runtimeClass == runtimeClass; case _ => false }
      hasType(classOf[VertexBuildingState[?]]) shouldBe true
      hasType(classOf[PrivateInventories[?]])  shouldBe true
      hasType(classOf[Bank[?]])                shouldBe true
      hasType(classOf[PlayerPoints])           shouldBe true

  describe("PerfectInfoGame.buildCity"):

    it("upgrades settlement to city, net +1 VP, deducts CITY_COST"):
      val cityCost = ResourceSet(ORE, ORE, ORE, WHEAT, WHEAT)
      val s = withResources(withSettlement(base, 0, Vertex(41)), 0, cityCost)
        .copy(playerPoints = PlayerPoints(Map(0 -> 1)))  // 1 pt for the pre-existing settlement
      val (newState, _) = PerfectInfoGame.buildCity.applyFull(BuildCityMove(0, Vertex(41)), s)

      newState.vertexBuildingState.map.get(Vertex(41)) shouldBe
        Some(PlayerBuilding(0, City))
      // Decrement (settlement→city downgrade), then 2× Increment (city = 2 pts)
      newState.playerPoints.points.getOrElse(0, 0) shouldBe
        (s.playerPoints.points.getOrElse(0, 0) + 1)
      newState.privateInventories.m(0).getTotal shouldBe 0

    it("returns bank resources equal to city cost"):
      val s = withResources(withSettlement(base, 0, Vertex(41)), 0, ResourceSet(ORE, ORE, ORE, WHEAT, WHEAT))
      val (newState, _) = PerfectInfoGame.buildCity.applyFull(BuildCityMove(0, Vertex(41)), s)
      newState.bank.b.getTotal shouldBe (base.bank.b.getTotal + 5)

  describe("PerfectInfoGame.buildRoad"):

    it("places road, deducts ROAD_COST"):
      val roadCost = ResourceSet(WOOD, BRICK)
      val s = withResources(base, 0, roadCost)
      val edge = Edge(Vertex(40), Vertex(41))
      val (newState, _) = PerfectInfoGame.buildRoad.applyFull(BuildRoadMove(0, edge), s)

      newState.edgeBuildingState.map.get(edge) shouldBe Some(PlayerBuilding(0, Road))
      newState.privateInventories.m(0).getTotal shouldBe 0
      newState.bank.b.getTotal shouldBe (base.bank.b.getTotal + 2)

  describe("PerfectInfoGame.portTrade"):

    it("exchanges player resources with bank"):
      val give = ResourceSet(WOOD, WOOD, WOOD, WOOD)
      val get  = ResourceSet(ORE)
      val s = withResources(base, 0, give)
      val (newState, deltas) = PerfectInfoGame.portTrade.applyFull(PortTradeMove(0, give, get), s)

      newState.privateInventories.m(0).getAmount(WOOD) shouldBe 0
      newState.privateInventories.m(0).getAmount(ORE)  shouldBe 1
      newState.bank.b.getAmount(WOOD) shouldBe (base.bank.b.getAmount(WOOD) + 4)
      newState.bank.b.getAmount(ORE)  shouldBe (base.bank.b.getAmount(ORE)  - 1)
      deltas should have length 4  // Lose, Add, Take, Gain

  describe("PerfectInfoGame.discard"):

    it("removes resources from player inventory, adds to bank"):
      val set = ResourceSet(WOOD, BRICK)
      val s = withResources(base, 0, set)
      val (newState, deltas) = PerfectInfoGame.discard.applyFull(DiscardMove(0, set), s)

      newState.privateInventories.m(0).getTotal shouldBe 0
      newState.bank.b.getTotal shouldBe (base.bank.b.getTotal + 2)
      deltas should have length 2  // Delta[PrivI], Delta[Bank]

  describe("PerfectInfoGame.rollDice"):

    it("distributes resources when players have settlements (roll 5)"):
      // postPlacements has settlements at vertices adjacent to various hexes
      val bankBefore = postPlacements.bank.b.getTotal
      val (newState, _) = PerfectInfoGame.rollDice.applyFull(RollDiceMoveResult(0, 5), postPlacements)
      // Roll 5 hits SHEEP(5) and WHEAT(5) hexes — at least one player gains resources
      newState.bank.b.getTotal should be <= bankBefore

    it("produces no resource deltas on roll 7 (robber roll)"):
      val (_, deltas) = PerfectInfoGame.rollDice.applyFull(RollDiceMoveResult(0, 7), postPlacements)
      deltas shouldBe empty

    it("produces no resource deltas when bank is empty of that resource"):
      val emptyBank = base.copy(bank = Bank(InventorySet.empty[Resource, Int]))
      val s = withSettlement(emptyBank, 0, Vertex(41))
        .copy(privateInventories = PrivateInventories(Map(0 -> InventorySet.empty[Resource, Int])))
      val (newState, deltas) = PerfectInfoGame.rollDice.applyFull(RollDiceMoveResult(0, 6), s)
      deltas shouldBe empty

  describe("PerfectInfoGame.trade"):

    it("transfers resources between two players"):
      val give = ResourceSet(WOOD)
      val get  = ResourceSet(BRICK)
      val s = base.copy(
        privateInventories = PrivateInventories(Map(
          0 -> InventorySet.fromList(List(WOOD)),
          1 -> InventorySet.fromList(List(BRICK))))
      )
      val (newState, _) = PerfectInfoGame.trade.applyFull(TradeMove(0, 1, give, get), s)

      newState.privateInventories.m(0).getAmount(WOOD)  shouldBe 0
      newState.privateInventories.m(0).getAmount(BRICK) shouldBe 1
      newState.privateInventories.m(1).getAmount(BRICK) shouldBe 0
      newState.privateInventories.m(1).getAmount(WOOD)  shouldBe 1

  describe("PerfectInfoGame.perfectRobber"):

    it("moves robber without steal"):
      val (newState, deltas) = PerfectInfoGame.perfectRobber.applyFull(
        PerfectInfoRobberMoveResult(0, 5, None), base)
      newState.robberLocation shouldBe RobberLocation(5)
      deltas should have length 1

    it("moves robber and transfers stolen resource"):
      val s = base.copy(
        privateInventories = PrivateInventories(Map(
          0 -> InventorySet.empty[Resource, Int],
          1 -> InventorySet.fromList(List(WOOD))))
      )
      val (newState, deltas) = PerfectInfoGame.perfectRobber.applyFull(
        PerfectInfoRobberMoveResult(0, 5, Some(PlayerSteal(1, WOOD))), s)

      newState.robberLocation shouldBe RobberLocation(5)
      newState.privateInventories.m(0).getAmount(WOOD) shouldBe 1
      newState.privateInventories.m(1).getAmount(WOOD) shouldBe 0
      deltas should have length 3  // RobberLocation + Gain[PrivI] + Lose[PrivI]

  describe("PerfectInfoGame.buyDevCard"):

    it("deducts DEV_CARD_COST, adds card to hand, shrinks deck"):
      val devCost = ResourceSet(ORE, WHEAT, SHEEP)
      val s = withResources(base, 0, devCost).copy(
        privateDevCardInv = PrivateDevCardInv(Map(0 -> Seq.empty)),
        turn = Turn(1)
      )
      val (newState, _) = PerfectInfoGame.buyDevCard.applyFull(
        PerfectInfoBuyDevelopmentCardMoveResult(0, KNIGHT), s)

      newState.privateInventories.m(0).getTotal shouldBe 0
      newState.developmentCardDeck.cards.length shouldBe
        (base.developmentCardDeck.cards.length - 1)
      newState.privateDevCardInv.m(0).map(_._1) should contain(KNIGHT)

    it("adds a VP when buying a POINT card"):
      val s = withResources(base, 0, ResourceSet(ORE, WHEAT, SHEEP)).copy(
        playerPoints      = PlayerPoints(Map(0 -> 0)),
        privateDevCardInv = PrivateDevCardInv(Map(0 -> Seq.empty)),
        turn = Turn(1)
      )
      val (newState, _) = PerfectInfoGame.buyDevCard.applyFull(
        PerfectInfoBuyDevelopmentCardMoveResult(0, POINT), s)
      newState.playerPoints.points(0) shouldBe 1

  describe("PerfectInfoGame.playKnight"):

    it("moves robber, removes knight from hand, increments army count"):
      val s = base.copy(
        privateDevCardInv = PrivateDevCardInv(Map(0 -> Seq((KNIGHT, 0)))),
        turn = Turn(1)
      )
      val (newState, _) = PerfectInfoGame.playKnight.applyFull(
        PerfectInfoPlayKnightResult(PerfectInfoRobberMoveResult(0, 5, None)), s)

      newState.robberLocation shouldBe RobberLocation(5)
      newState.privateDevCardInv.m(0).map(_._1) should not contain KNIGHT
      newState.playerArmyCount.m.getOrElse(0, 0) shouldBe 1

    it("awards largest army (2 points) when army reaches 3"):
      val s = base.copy(
        privateDevCardInv = PrivateDevCardInv(Map(0 -> Seq((KNIGHT, 0)))),
        playerArmyCount   = PlayerArmyCount(Map(0 -> 2)),
        playerPoints      = PlayerPoints(Map(0 -> 0)),
        turn = Turn(3)
      )
      val (newState, _) = PerfectInfoGame.playKnight.applyFull(
        PerfectInfoPlayKnightResult(PerfectInfoRobberMoveResult(0, 5, None)), s)

      newState.largestArmyPlayer.player shouldBe Some(0)
      newState.playerPoints.points(0) shouldBe 2

  describe("PerfectInfoGame.playMonopoly"):

    it("takes all of a resource from other players"):
      val s = base.copy(
        privateInventories = PrivateInventories(Map(
          0 -> InventorySet.empty[Resource, Int],
          1 -> InventorySet.fromList(List(WHEAT, WHEAT)),
          2 -> InventorySet.fromList(List(WHEAT)))),
        privateDevCardInv = PrivateDevCardInv(Map(0 -> Seq((MONOPOLY, 0)))),
        turn = Turn(1)
      )
      val (newState, _) = PerfectInfoGame.playMonopoly.applyFull(
        PlayMonopolyMoveResult(0, WHEAT, Map(1 -> 2, 2 -> 1)), s)

      newState.privateInventories.m(0).getAmount(WHEAT) shouldBe 3
      newState.privateInventories.m(1).getAmount(WHEAT) shouldBe 0
      newState.privateInventories.m(2).getAmount(WHEAT) shouldBe 0
      newState.privateDevCardInv.m(0).map(_._1) should not contain MONOPOLY

  describe("PerfectInfoGame.playRoadBuilder"):

    it("builds two roads for free, removes card from hand"):
      val edge1 = Edge(Vertex(40), Vertex(41))
      val edge2 = Edge(Vertex(17), Vertex(40))
      val s = postPlacements.copy(
        privateDevCardInv = PrivateDevCardInv(Map(0 -> Seq((ROAD_BUILDER, 0)))),
        turn = Turn(1)
      )
      val (newState, _) = PerfectInfoGame.playRoadBuilder.applyFull(
        PlayRoadBuilderMove(0, edge1, Some(edge2)), s)

      newState.edgeBuildingState.map.get(edge1) shouldBe Some(PlayerBuilding(0, Road))
      newState.edgeBuildingState.map.get(edge2) shouldBe Some(PlayerBuilding(0, Road))
      newState.privateDevCardInv.m(0).map(_._1) should not contain ROAD_BUILDER
      // bank unchanged — road builder is free
      newState.bank.b.getTotal shouldBe postPlacements.bank.b.getTotal

  describe("PerfectInfoGame.playYearOfPlenty"):

    it("gives player two resources from bank"):
      val s = base.copy(
        privateInventories = PrivateInventories(Map(0 -> InventorySet.empty[Resource, Int])),
        privateDevCardInv  = PrivateDevCardInv(Map(0 -> Seq((YEAR_OF_PLENTY, 0)))),
        turn = Turn(1)
      )
      val (newState, _) = PerfectInfoGame.playYearOfPlenty.applyFull(
        PlayYearOfPlentyMove(0, ORE, WHEAT), s)

      newState.privateInventories.m(0).getAmount(ORE)  shouldBe 1
      newState.privateInventories.m(0).getAmount(WHEAT) shouldBe 1
      newState.bank.b.getAmount(ORE)  shouldBe (base.bank.b.getAmount(ORE)  - 1)
      newState.bank.b.getAmount(WHEAT) shouldBe (base.bank.b.getAmount(WHEAT) - 1)
      newState.privateDevCardInv.m(0).map(_._1) should not contain YEAR_OF_PLENTY

  describe("PerfectInfoGame.initialPlacement"):

    it("first round: places settlement + road, grants VP, no resources"):
      val bankBefore = base.bank.b.getTotal
      val (newState, _) = PerfectInfoGame.initialPlacement.applyFull(
        InitialPlacementMove(Vertex(41), Edge(Vertex(40), Vertex(41)), 0), base)

      newState.vertexBuildingState.map.get(Vertex(41)) shouldBe Some(PlayerBuilding(0, Settlement))
      newState.edgeBuildingState.map.get(Edge(Vertex(40), Vertex(41))) shouldBe
        Some(PlayerBuilding(0, Road))
      newState.playerPoints.points.getOrElse(0, 0) shouldBe 1
      newState.bank.b.getTotal shouldBe bankBefore  // no resources in round 1

    it("second round: places settlement + road, grants VP, distributes adjacent resources"):
      // After 4 placements (1 per player) the second round starts (moveCount >= numPlayers)
      val s = stateAfter(4)
      val bankBefore = s.bank.b.getTotal
      // Player 3's second round placement — they get resources from adjacent hexes
      val (newState, _) = PerfectInfoGame.initialPlacement.applyFull(
        InitialPlacementMove(Vertex(31), Edge(Vertex(2), Vertex(31)), 3), s)

      newState.vertexBuildingState.map.get(Vertex(31)) shouldBe Some(PlayerBuilding(3, Settlement))
      newState.playerPoints.points.getOrElse(3, 0) should be >=
        s.playerPoints.points.getOrElse(3, 0) + 1
      // Resources may or may not be granted depending on adjacent hexes; bank should not increase
      newState.bank.b.getTotal should be <= bankBefore

  // ── PublicInfoGame ──────────────────────────────────────────────────────────

  private val pubBase = imperfectInfoGame.initPublicInfoState
  private val pubBoard = imperfectInfoGame.initPublicInfoState.board

  private def pubWithResources(s: PublicInfoState, player: Int, total: Int): PublicInfoState =
    s.copy(publicInventories = PublicInventories(s.publicInventories.m + (player -> total)))

  private val pubPostPlacements: PublicInfoState =
    imperfectInfoGame.testMoveResults.take(8).foldLeft(pubBase) { (s, m) =>
      PublicInfoGame.game.applyMove(m, s)._2
    }

  describe("PublicInfoGame.endTurn"):
    it("increments Turn"):
      val (newState, _) = PublicInfoGame.endTurn.applyFull(EndTurnMove(0), pubBase)
      newState.turn shouldBe Turn(1)

  describe("PublicInfoGame.buildSettlement"):
    it("places settlement, deducts public resource count, adds point"):
      val s = pubWithResources(pubBase, 0, 4)
      val (newState, _) = PublicInfoGame.buildSettlement.applyFull(
        BuildSettlementMove(0, Vertex(33)), s)
      newState.vertexBuildingState.map.get(Vertex(33)) shouldBe Some(PlayerBuilding(0, Settlement))
      newState.playerPoints.points.getOrElse(0, 0) shouldBe 1
      newState.publicInventories.m(0) shouldBe 0

  describe("PublicInfoGame.buildCity"):
    it("upgrades settlement, net +1 VP"):
      val s = pubWithResources(
        pubBase.copy(
          vertexBuildingState = VertexBuildingState(Map(Vertex(33) -> PlayerBuilding(0, Settlement))),
          playerPoints        = PlayerPoints(Map(0 -> 1))),  // 1 pt for the pre-existing settlement
        0, 5)
      val (newState, _) = PublicInfoGame.buildCity.applyFull(BuildCityMove(0, Vertex(33)), s)
      newState.vertexBuildingState.map.get(Vertex(33)) shouldBe Some(PlayerBuilding(0, City))
      newState.playerPoints.points.getOrElse(0, 0) shouldBe 2  // started at 1 (settlement), net +1 = 2

  describe("PublicInfoGame.buildRoad"):
    it("places road, deducts 2 public resources"):
      val s = pubWithResources(pubBase, 0, 2)
      val edge = Edge(Vertex(4), Vertex(33))
      val (newState, _) = PublicInfoGame.buildRoad.applyFull(BuildRoadMove(0, edge), s)
      newState.edgeBuildingState.map.get(edge) shouldBe Some(PlayerBuilding(0, Road))
      newState.publicInventories.m(0) shouldBe 0

  describe("PublicInfoGame.portTrade"):
    it("updates public resource count"):
      val s = pubWithResources(pubBase, 0, 4)
      val (newState, _) = PublicInfoGame.portTrade.applyFull(
        PortTradeMove(0, ResourceSet(WOOD, WOOD, WOOD, WOOD), ResourceSet(ORE)), s)
      newState.publicInventories.m(0) shouldBe 1

  describe("PublicInfoGame.discard"):
    it("removes resources from public count, adds to bank"):
      val s = pubWithResources(pubBase, 0, 4)
      val (newState, _) = PublicInfoGame.discard.applyFull(
        DiscardMove(0, ResourceSet(WOOD, WOOD)), s)
      newState.publicInventories.m(0) shouldBe 2
      newState.bank.b.getTotal shouldBe (pubBase.bank.b.getTotal + 2)

  describe("PublicInfoGame.rollDice"):
    it("distributes resources on non-7 roll"):
      val bankBefore = pubPostPlacements.bank.b.getTotal
      val (newState, _) = PublicInfoGame.rollDice.applyFull(
        RollDiceMoveResult(0, 5), pubPostPlacements)
      newState.bank.b.getTotal should be <= bankBefore

    it("produces no deltas on roll 7"):
      val (_, deltas) = PublicInfoGame.rollDice.applyFull(
        RollDiceMoveResult(0, 7), pubPostPlacements)
      deltas shouldBe empty

  describe("PublicInfoGame.trade"):
    it("swaps public resource counts"):
      val s = pubBase.copy(publicInventories = PublicInventories(Map(0 -> 1, 1 -> 1)))
      val (newState, _) = PublicInfoGame.trade.applyFull(
        TradeMove(0, 1, ResourceSet(WOOD), ResourceSet(BRICK)), s)
      newState.publicInventories.m(0) shouldBe 1
      newState.publicInventories.m(1) shouldBe 1

  describe("PublicInfoGame.publicRobber"):
    it("moves robber and updates public inventory on steal"):
      val s = pubBase.copy(publicInventories = PublicInventories(Map(0 -> 0, 1 -> 1)))
      val (newState, _) = PublicInfoGame.publicRobber.applyFull(
        RobberMoveResult[Resource](0, 5, Some(PlayerSteal(1, Some(WOOD)))), s)
      newState.robberLocation shouldBe RobberLocation(5)
      newState.publicInventories.m(0) shouldBe 1
      newState.publicInventories.m(1) shouldBe 0

    it("moves robber without steal"):
      val (newState, deltas) = PublicInfoGame.publicRobber.applyFull(
        RobberMoveResult[Resource](0, 5, None), pubBase)
      newState.robberLocation shouldBe RobberLocation(5)
      deltas should have length 1

  describe("PublicInfoGame.buyDevCard"):
    it("deducts public resources, shrinks deck, increments dev card count"):
      val s = pubWithResources(pubBase, 0, 3).copy(
        publicDevCardInv = PublicDevCardInv(Map(0 -> 0)),
        turn = Turn(1)
      )
      val (newState, _) = PublicInfoGame.buyDevCard.applyFull(
        BuyDevelopmentCardMoveResult(0, None), s)
      newState.publicInventories.m(0) shouldBe 0
      newState.developmentCardDeckSize.size shouldBe (pubBase.developmentCardDeckSize.size - 1)
      newState.publicDevCardInv.m.getOrElse(0, 0) shouldBe 1

  describe("PublicInfoGame.playPoint"):
    it("increments VP, removes card from public dev card count"):
      val s = pubBase.copy(
        playerPoints     = PlayerPoints(Map(0 -> 0)),
        publicDevCardInv = PublicDevCardInv(Map(0 -> 1)),
        turn = Turn(1)
      )
      val (newState, _) = PublicInfoGame.playPoint.applyFull(PlayPointMove(0), s)
      newState.playerPoints.points(0) shouldBe 1
      newState.publicDevCardInv.m.getOrElse(0, 0) shouldBe 0

  describe("PublicInfoGame.playKnight"):
    it("moves robber, increments army count, removes card"):
      val s = pubBase.copy(
        publicDevCardInv = PublicDevCardInv(Map(0 -> 1)),
        turn = Turn(1)
      )
      val (newState, _) = PublicInfoGame.playKnight.applyFull(
        PlayKnightResult(RobberMoveResult[Resource](0, 5, None)), s)
      newState.robberLocation shouldBe RobberLocation(5)
      newState.playerArmyCount.m.getOrElse(0, 0) shouldBe 1
      newState.publicDevCardInv.m.getOrElse(0, 0) shouldBe 0

  describe("PublicInfoGame.playMonopoly"):
    it("updates public resource counts"):
      val s = pubBase.copy(
        publicInventories = PublicInventories(Map(0 -> 0, 1 -> 3)),
        publicDevCardInv  = PublicDevCardInv(Map(0 -> 1)),
        turn = Turn(1)
      )
      val (newState, _) = PublicInfoGame.playMonopoly.applyFull(
        PlayMonopolyMoveResult(0, WHEAT, Map(1 -> 3)), s)
      newState.publicInventories.m(0) shouldBe 3
      newState.publicInventories.m(1) shouldBe 0
      newState.publicDevCardInv.m.getOrElse(0, 0) shouldBe 0

  describe("PublicInfoGame.playRoadBuilder"):
    it("builds two roads, removes card from public count"):
      val edge1 = Edge(2, 31)
      val edge2 = Edge(2, 3)
      val s = pubPostPlacements.copy(
        publicDevCardInv = PublicDevCardInv(Map(3 -> 1)),
        turn = Turn(1)
      )
      val (newState, _) = PublicInfoGame.playRoadBuilder.applyFull(
        PlayRoadBuilderMove(3, edge1, Some(edge2)), s)
      newState.edgeBuildingState.map.get(edge1) shouldBe Some(PlayerBuilding(3, Road))
      newState.edgeBuildingState.map.get(edge2) shouldBe Some(PlayerBuilding(3, Road))
      newState.publicDevCardInv.m.getOrElse(3, 0) shouldBe 0

  describe("PublicInfoGame.playYearOfPlenty"):
    it("grants two resources to player from bank"):
      val s = pubBase.copy(
        publicInventories = PublicInventories(Map(0 -> 0)),
        publicDevCardInv  = PublicDevCardInv(Map(0 -> 1)),
        turn = Turn(1)
      )
      val (newState, _) = PublicInfoGame.playYearOfPlenty.applyFull(
        PlayYearOfPlentyMove(0, ORE, WHEAT), s)
      newState.publicInventories.m(0) shouldBe 2
      newState.bank.b.getAmount(ORE)   shouldBe (pubBase.bank.b.getAmount(ORE)   - 1)
      newState.bank.b.getAmount(WHEAT) shouldBe (pubBase.bank.b.getAmount(WHEAT) - 1)
      newState.publicDevCardInv.m.getOrElse(0, 0) shouldBe 0

  describe("PublicInfoGame.initialPlacement"):
    it("first round: places buildings, grants VP, no bank change"):
      val bankBefore = pubBase.bank.b.getTotal
      val (newState, _) = PublicInfoGame.initialPlacement.applyFull(
        InitialPlacementMove(Vertex(33), Edge(Vertex(4), Vertex(33)), 0), pubBase)
      newState.vertexBuildingState.map.get(Vertex(33)) shouldBe Some(PlayerBuilding(0, Settlement))
      newState.playerPoints.points.getOrElse(0, 0) shouldBe 1
      newState.bank.b.getTotal shouldBe bankBefore

    it("second round: places buildings, grants VP, bank can decrease"):
      val s = imperfectInfoGame.testMoveResults.take(4).foldLeft(pubBase) { (st, m) =>
        PublicInfoGame.game.applyMove(m, st)._2
      }
      val bankBefore = s.bank.b.getTotal
      val (newState, _) = PublicInfoGame.initialPlacement.applyFull(
        InitialPlacementMove(Vertex(39), Edge(Vertex(14), Vertex(39)), 3), s)
      newState.vertexBuildingState.map.get(Vertex(39)) shouldBe Some(PlayerBuilding(3, Settlement))
      newState.bank.b.getTotal should be <= bankBefore

  // ── apply_ demonstration ────────────────────────────────────────────────────

  describe("GameAction.apply_"):

    it("endTurn works with ActionState[Unit, Tuple1[Turn]] = Tuple1[Turn]"):
      val actionState = Tuple1(Turn(3))
      val (newState, deltas) = PerfectInfoGame.endTurn.apply_(EndTurnMove(0), actionState)
      newState shouldBe Tuple1(Turn(4))
      deltas should have length 1

    it("buildCity works with ActionState[Unit, (VBS, PrivI, Bank, PP)]"):
      val vbs   = VertexBuildingState[BaseVertexBuilding](
        Map(Vertex(41) -> PlayerBuilding(0, Settlement)))
      val privI = PrivateInventories[Resource](
        Map(0 -> InventorySet.fromMap(Map(ORE -> 3, WHEAT -> 2))))
      val bank  = Bank(InventorySet.fromMap(
        Map[Resource, Int](WOOD -> 19, BRICK -> 19, SHEEP -> 19, WHEAT -> 19, ORE -> 19)))
      val pp    = PlayerPoints(Map(1 -> 0))

      val actionState = (vbs, privI, bank, pp)
      val (newAS, _) = PerfectInfoGame.buildCity.apply_(BuildCityMove(0, Vertex(41)), actionState)

      newAS(0).map.get(Vertex(41)) shouldBe Some(PlayerBuilding(0, City))
      newAS(1).m(0).getTotal shouldBe 0
      newAS(2).b.getAmount(ORE) shouldBe 22

    it("discard works with ActionState[Unit, (PrivI, Bank)]"):
      val privI = PrivateInventories[Resource](
        Map(0 -> InventorySet.fromList(List(WOOD, BRICK))))
      val bank  = Bank(InventorySet.fromMap(
        Map[Resource, Int](WOOD -> 19, BRICK -> 19, SHEEP -> 19, WHEAT -> 19, ORE -> 19)))

      val actionState = (privI, bank)
      val (newAS, deltas) = PerfectInfoGame.discard.apply_(
        DiscardMove(0, ResourceSet(WOOD, BRICK)), actionState)

      newAS(0).m(0).getTotal shouldBe 0
      newAS(1).b.getTotal shouldBe 97
      deltas should have length 2

  // ── DiceRollStats ────────────────────────────────────────────────────────────

  // Hex 15 (WOOD, 6) has vertex 41 in its adjacency list.
  // A settlement at Vertex(41) with roll 6 gives player 0 exactly 1 WOOD.
  private val rollSixState = base.copy(
    privateInventories  = PrivateInventories(Map(0 -> InventorySet.empty[Resource, Int])),
    vertexBuildingState = VertexBuildingState(Map(Vertex(41) -> PlayerBuilding(0, Settlement)))
  )

  describe("DiceRollStats"):

    it("tracks WOOD gain for player 0 on roll 6"):
      val (_, deltas) = PerfectInfoGame.rollDice.applyFull(RollDiceMoveResult(0, 6), rollSixState)
      val stats = DiceRollStats.update[PerfectInfoDelta](DiceRollStats.empty, deltas)
      stats.byPlayer(0).getAmount(WOOD) shouldBe 1

    it("byResource returns correct per-player count"):
      val (_, deltas) = PerfectInfoGame.rollDice.applyFull(RollDiceMoveResult(0, 6), rollSixState)
      val stats = DiceRollStats.update[PerfectInfoDelta](DiceRollStats.empty, deltas)
      stats.byResource(WOOD)(0) shouldBe 1

    it("produces empty stats on roll 7 (robber — no resources distributed)"):
      val (_, deltas) = PerfectInfoGame.rollDice.applyFull(RollDiceMoveResult(0, 7), rollSixState)
      DiceRollStats.update[PerfectInfoDelta](DiceRollStats.empty, deltas) shouldBe DiceRollStats.empty

    it("accumulates gains across multiple rolls"):
      val (_, d1) = PerfectInfoGame.rollDice.applyFull(RollDiceMoveResult(0, 6), rollSixState)
      val (_, d2) = PerfectInfoGame.rollDice.applyFull(RollDiceMoveResult(0, 6), rollSixState)
      val stats = DiceRollStats.update[PerfectInfoDelta](
        DiceRollStats.update[PerfectInfoDelta](DiceRollStats.empty, d1), d2)
      stats.byPlayer(0).getAmount(WOOD) shouldBe 2

    // Vertex(41) is adjacent to hex 7 (ORE, 9) = 4 pips, hex 15 (WOOD, 6) = 5 pips,
    // hex 16 (WHEAT, 5) = 4 pips.  Robber starts at hex 10 (Desert) — no conflict.

    it("expectedByPlayer gives correct pips per resource for a settlement"):
      val stats = DiceRollStats.recordExpected(
        DiceRollStats.empty, board, rollSixState.vertexBuildingState, rollSixState.robberLocation)
      stats.expectedByPlayer(0).getAmount(WOOD)  shouldBe 5  // 6 → 5 pips
      stats.expectedByPlayer(0).getAmount(ORE)   shouldBe 4  // 9 → 4 pips
      stats.expectedByPlayer(0).getAmount(WHEAT) shouldBe 4  // 5 → 4 pips

    it("baseExpectedByPlayer matches expectedByPlayer when robber is not on an adjacent hex"):
      val stats = DiceRollStats.recordExpected(
        DiceRollStats.empty, board, rollSixState.vertexBuildingState, rollSixState.robberLocation)
      stats.baseExpectedByPlayer(0) shouldBe stats.expectedByPlayer(0)

    it("robberImpact shows blocked pips when robber is on an adjacent hex"):
      // Move robber to hex 15 (WOOD, 6) — blocks 5 WOOD pips for player 0
      val blockedState = rollSixState.copy(robberLocation = RobberLocation(15))
      val stats = DiceRollStats.recordExpected(
        DiceRollStats.empty, board, blockedState.vertexBuildingState, blockedState.robberLocation)
      stats.expectedByPlayer(0).getAmount(WOOD)  shouldBe 0  // hex 15 blocked
      stats.expectedByPlayer(0).getAmount(ORE)   shouldBe 4  // unaffected
      stats.baseExpectedByPlayer(0).getAmount(WOOD) shouldBe 5
      stats.robberImpact(0).getAmount(WOOD)          shouldBe 5
      stats.robberImpact(0).getAmount(ORE)            shouldBe 0

    it("city contributes double pips"):
      val cityState = base.copy(
        vertexBuildingState = VertexBuildingState(Map(Vertex(41) -> PlayerBuilding(0, City))))
      val stats = DiceRollStats.recordExpected(
        DiceRollStats.empty, board, cityState.vertexBuildingState, base.robberLocation)
      stats.expectedByPlayer(0).getAmount(WOOD) shouldBe 10  // 6 → 5 pips × 2 for city

    it("pips accumulate across multiple recordExpected calls (two rolls worth)"):
      val stats = DiceRollStats.recordExpected(
        DiceRollStats.recordExpected(
          DiceRollStats.empty, board, rollSixState.vertexBuildingState, rollSixState.robberLocation),
        board, rollSixState.vertexBuildingState, rollSixState.robberLocation)
      stats.expectedByPlayer(0).getAmount(WOOD) shouldBe 10  // 5 pips × 2 rolls

    it("GameStats.recordRoll populates diceRolls expectedPips"):
      val gs1 = GameStats.recordRoll(
        GameStats.empty, board, rollSixState.vertexBuildingState, rollSixState.robberLocation)
      gs1.diceRolls.expectedByPlayer(0).getAmount(WOOD) shouldBe 5
*/
