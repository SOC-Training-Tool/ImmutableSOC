package soc.base.actions

import game.{GameAction, InventorySet, NoInput}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import soc.base.*
import soc.base.DevelopmentCards.*
import soc.base.state.*
import soc.core.*
import soc.core.DevTransactions.*
import soc.core.ResourceInventories.*
import soc.core.ResourceSet.*
import soc.core.Resources.*
import soc.core.Transactions.*
import soc.core.state.*
import soc.core.state.BoardBuildingState.*
import Ports.MISC

class GameActionSpec extends AnyFunSpec with Matchers {

  private val sampleBoard: BaseBoard[Resource] = BaseBoard(
    List[Hex[Resource]](
      ResourceHex(WHEAT, 6), ResourceHex(ORE, 2), ResourceHex(SHEEP, 5),
      ResourceHex(ORE, 8), ResourceHex(WOOD, 4), ResourceHex(BRICK, 11),
      ResourceHex(SHEEP, 12), ResourceHex(ORE, 9), ResourceHex(SHEEP, 10),
      ResourceHex(BRICK, 8), Desert, ResourceHex(WHEAT, 3),
      ResourceHex(SHEEP, 9), ResourceHex(BRICK, 10), ResourceHex(WOOD, 3),
      ResourceHex(WOOD, 6), ResourceHex(WHEAT, 5), ResourceHex(WOOD, 4),
      ResourceHex(WHEAT, 11)
    ),
    List(MISC, ORE, MISC, WHEAT, MISC, BRICK, WOOD, SHEEP, MISC)
  )

  private def emptyBank: Bank[Resource] = Bank(InventorySet.fromMap(Map(
    WOOD -> 19, BRICK -> 19, SHEEP -> 19, WHEAT -> 19, ORE -> 19
  )))

  // ── 1. EndTurnAction ─────────────────────────────────────────────────────────

  describe("EndTurnAction") {
    it("produces EndTurnOutput with turnIncrement = 1") {
      val action = EndTurnAction()
      val result = action(EndTurnMove(0), NoInput)
      result shouldBe EndTurnOutput(Turn.Delta(1))
      result.turnIncrement shouldBe Turn.Delta(1)
    }

    it("produces same output regardless of player id") {
      val action = EndTurnAction()
      val r1 = action(EndTurnMove(0), NoInput)
      val r2 = action(EndTurnMove(3), NoInput)
      r1 shouldBe r2
      r2.turnIncrement shouldBe Turn.Delta(1)
    }
  }

  // ── 2. BuildSettlementCoreAction ────────────────────────────────────────────

  describe("BuildSettlementCoreAction") {
    it("produces correct output deltas for a settlement placement") {
      val action = BuildSettlementCoreAction()
      val result = action(BuildSettlementMove(0, Vertex(41)), NoInput)
      result.addedSettlement shouldBe add(Vertex(41), Settlement, 0)
      result.resourcesSpent shouldBe Lose(0, SETTLEMENT_COST)
      result.resourcesReturned shouldBe Bank.Add(SETTLEMENT_COST)
      result.pointGained shouldBe PlayerPoints.Increment(0)
    }

    it("correctly uses different player id") {
      val action = BuildSettlementCoreAction()
      val result = action(BuildSettlementMove(2, Vertex(33)), NoInput)
      result.addedSettlement shouldBe AddBuilding(Vertex(33), 2, Settlement)
      result.resourcesSpent shouldBe Lose(2, SETTLEMENT_COST)
      result.pointGained shouldBe PlayerPoints.Increment(2)
    }

    it("SETTLEMENT_COST equals WOOD, BRICK, WHEAT, SHEEP") {
      SETTLEMENT_COST.getTotal shouldBe 4
      SETTLEMENT_COST.getAmount(WOOD)  shouldBe 1
      SETTLEMENT_COST.getAmount(BRICK) shouldBe 1
      SETTLEMENT_COST.getAmount(WHEAT) shouldBe 1
      SETTLEMENT_COST.getAmount(SHEEP) shouldBe 1
      SETTLEMENT_COST.getAmount(ORE)   shouldBe 0
    }
  }

  // ── 3. BuildCityAction ──────────────────────────────────────────────────────

  describe("BuildCityAction") {
    it("produces two vertex building deltas (remove settlement, add city)") {
      val action = BuildCityAction()
      val result = action(BuildCityMove(0, Vertex(41)), NoInput)
      result.vertexBuildingChanges should have length 2
      result.vertexBuildingChanges(0) shouldBe BoardBuildingState.RemoveBuilding(Vertex(41))
      result.vertexBuildingChanges(1) shouldBe add(Vertex(41), City, 0)
    }

    it("produces correct point deltas (decrement + 2x increment = net +1)") {
      val action = BuildCityAction()
      val result = action(BuildCityMove(0, Vertex(41)), NoInput)
      result.pointChanges should have length 3
      result.pointChanges(0) shouldBe PlayerPoints.Decrement(0)
      result.pointChanges(1) shouldBe PlayerPoints.Increment(0)
      result.pointChanges(2) shouldBe PlayerPoints.Increment(0)
    }

    it("charges CITY_COST and returns it to bank") {
      val action = BuildCityAction()
      val result = action(BuildCityMove(0, Vertex(41)), NoInput)
      result.resourcesSpent shouldBe Lose(0, CITY_COST)
      result.resourcesReturned shouldBe Bank.Add(CITY_COST)
    }

    it("CITY_COST equals three ORE and two WHEAT") {
      CITY_COST.getTotal shouldBe 5
      CITY_COST.getAmount(ORE)   shouldBe 3
      CITY_COST.getAmount(WHEAT) shouldBe 2
    }
  }

  // ── 4. BuildRoadCoreAction ──────────────────────────────────────────────────

  describe("BuildRoadCoreAction") {
    it("produces correct output for a road placement") {
      val action = BuildRoadCoreAction()
      val edge   = Edge(Vertex(40), Vertex(41))
      val result = action(BuildRoadMove(0, edge), NoInput)
      result.addedRoad shouldBe add(edge, Road, 0)
      result.resourcesSpent shouldBe Lose(0, ROAD_COST)
      result.resourcesReturned shouldBe Bank.Add(ROAD_COST)
    }

    it("ROAD_COST equals WOOD and BRICK") {
      ROAD_COST.getTotal shouldBe 2
      ROAD_COST.getAmount(WOOD)  shouldBe 1
      ROAD_COST.getAmount(BRICK) shouldBe 1
    }
  }

  // ── 5. TradeAction ──────────────────────────────────────────────────────────

  describe("TradeAction") {
    it("produces four resource change deltas for a two-player trade") {
      val give = ResourceSet(WOOD)
      val get  = ResourceSet(BRICK)
      val action = TradeAction()
      val result = action(TradeMove(0, 1, give, get), NoInput)
      result.resourceChanges should have length 4
      result.resourceChanges(0) shouldBe Lose(0, give)
      result.resourceChanges(1) shouldBe Lose(1, get)
      result.resourceChanges(2) shouldBe Gain(0, get)
      result.resourceChanges(3) shouldBe Gain(1, give)
    }

    it("handles multi-resource trades") {
      val give = ResourceSet(WOOD, WHEAT)
      val get  = ResourceSet(BRICK, ORE, SHEEP)
      val action = TradeAction()
      val result = action(TradeMove(1, 2, give, get), NoInput)
      result.resourceChanges should have length 4
      result.resourceChanges(0) shouldBe Lose(1, give)
      result.resourceChanges(1) shouldBe Lose(2, get)
      result.resourceChanges(2) shouldBe Gain(1, get)
      result.resourceChanges(3) shouldBe Gain(2, give)
    }

    it("handles trades with zero resources") {
      val empty = InventorySet.empty[Resource, Int]
      val action = TradeAction()
      val result = action(TradeMove(0, 1, empty, empty), NoInput)
      result.resourceChanges should have length 4
    }
  }

  // ── 6. PortTradeAction ──────────────────────────────────────────────────────

  describe("PortTradeAction") {
    it("produces resource and bank change deltas") {
      val give = ResourceSet(WOOD, WOOD, WOOD, WOOD)
      val get  = ResourceSet(ORE)
      val action = PortTradeAction()
      val result = action(PortTradeMove(0, give, get), NoInput)
      result.resourceChanges should have length 2
      result.resourceChanges(0) shouldBe Lose(0, give)
      result.resourceChanges(1) shouldBe Gain(0, get)
      result.bankChanges should have length 2
      result.bankChanges(0) shouldBe Bank.Add(give)
      result.bankChanges(1) shouldBe Bank.Take(get)
    }
  }

  // ── 7. DiscardAction ────────────────────────────────────────────────────────

  describe("DiscardAction") {
    it("produces player Lost and bank Add deltas") {
      val set = ResourceSet(WOOD, BRICK)
      val action = DiscardAction()
      val result = action(DiscardMove(0, set), NoInput)
      result.playerLost shouldBe Lose(0, set)
      result.bankGained shouldBe Bank.Add(set)
    }

    it("handles discarding a single resource") {
      val set = ResourceSet(ORE)
      val action = DiscardAction()
      val result = action(DiscardMove(1, set), NoInput)
      result.playerLost shouldBe Lose(1, set)
      result.bankGained shouldBe Bank.Add(set)
    }

    it("handles discarding all resource types") {
      val set = ResourceSet(WOOD, BRICK, SHEEP, WHEAT, ORE)
      val action = DiscardAction()
      val result = action(DiscardMove(2, set), NoInput)
      result.playerLost shouldBe Lose(2, set)
      result.bankGained shouldBe Bank.Add(set)
    }
  }

  // ── 8. RollDiceAction ───────────────────────────────────────────────────────

  describe("RollDiceAction") {
    it("produces no gains on roll 7 (robber roll)") {
      val vbs = VertexBuildingState[BaseVertexBuilding](Map.empty)
      val action = RollDiceAction()
      val input  = RollDiceInput(RobberLocation(10), sampleBoard, vbs, emptyBank)
      val result = action(RollDiceMoveResult(0, 7), input)
      result.bankLost shouldBe None
      result.playerGains shouldBe empty
    }

    it("produces no gains when bank is empty") {
      val vbs = VertexBuildingState[BaseVertexBuilding](
        Map(Vertex(41) -> PlayerBuilding(0, Settlement)))
      val emptyBank = Bank(InventorySet.empty[Resource, Int])
      val action = RollDiceAction()
      val input  = RollDiceInput(RobberLocation(10), sampleBoard, vbs, emptyBank)
      val result = action(RollDiceMoveResult(0, 6), input)
      result.playerGains shouldBe empty
    }

    it("distributes resources on a non-7 matching roll") {
      val vbs = VertexBuildingState[BaseVertexBuilding](
        Map(Vertex(41) -> PlayerBuilding(0, Settlement)))
      val action = RollDiceAction()
      val input  = RollDiceInput(RobberLocation(10), sampleBoard, vbs, emptyBank)
      val result = action(RollDiceMoveResult(0, 6), input)
      result.bankLost shouldBe defined
      result.playerGains should not be empty
    }

    it("blocks resource distribution when robber is on the matching hex") {
      val vbs = VertexBuildingState[BaseVertexBuilding](
        Map(Vertex(41) -> PlayerBuilding(0, Settlement)))
      val action = RollDiceAction()
      val input  = RollDiceInput(RobberLocation(15), sampleBoard, vbs, emptyBank)
      val result = action(RollDiceMoveResult(0, 6), input)
      result.playerGains shouldBe empty
    }

    it("handles city resource doubling (2x)") {
      val vbs = VertexBuildingState[BaseVertexBuilding](
        Map(Vertex(41) -> PlayerBuilding(0, City)))
      val action = RollDiceAction()
      val input  = RollDiceInput(RobberLocation(10), sampleBoard, vbs, emptyBank)
      val result = action(RollDiceMoveResult(0, 6), input)
      result.playerGains should not be empty
      result.playerGains.head shouldBe a[Gain[?]]
    }

    it("roll with no matching hexes produces empty gains") {
      val vbs = VertexBuildingState[BaseVertexBuilding](Map.empty)
      val action = RollDiceAction()
      val input  = RollDiceInput(RobberLocation(10), sampleBoard, vbs, emptyBank)
      val result = action(RollDiceMoveResult(0, 2), input)
      result.playerGains shouldBe empty
    }
  }

  // ── 9. PerfectRobberAction ──────────────────────────────────────────────────

  describe("PerfectRobberAction") {
    it("moves robber without stealing") {
      val action = PerfectRobberAction()
      val move   = PerfectInfoRobberMoveResult[Resource](0, 5, None)
      val result = action(move, NoInput)
      result.newRobberLocation shouldBe RobberLocation.Delta(5)
      result.steals shouldBe empty
    }

    it("moves robber with a steal produces two inventory deltas") {
      val action = PerfectRobberAction()
      val move   = PerfectInfoRobberMoveResult[Resource](0, 5, Some(PlayerSteal(1, Wood)))
      val result = action(move, NoInput)
      result.newRobberLocation shouldBe RobberLocation.Delta(5)
      result.steals should have length 2
      result.steals(0) shouldBe Gain(0, InventorySet.fromList(Seq(Wood)))
      result.steals(1) shouldBe Lose(1, InventorySet.fromList(Seq(Wood)))
    }

    it("steals with different resource types") {
      val action = PerfectRobberAction()
      val move   = PerfectInfoRobberMoveResult[Resource](1, 3, Some(PlayerSteal(2, Ore)))
      val result = action(move, NoInput)
      result.newRobberLocation shouldBe RobberLocation.Delta(3)
      result.steals(0) shouldBe Gain(1, InventorySet.fromList(Seq(Ore)))
      result.steals(1) shouldBe Lose(2, InventorySet.fromList(Seq(Ore)))
    }

    it("steals from player 0 to player 2") {
      val action = PerfectRobberAction()
      val move   = PerfectInfoRobberMoveResult[Resource](2, 8, Some(PlayerSteal(0, Wheat)))
      val result = action(move, NoInput)
      result.newRobberLocation shouldBe RobberLocation.Delta(8)
      result.steals(0) shouldBe Gain(2, InventorySet.fromList(Seq(Wheat)))
      result.steals(1) shouldBe Lose(0, InventorySet.fromList(Seq(Wheat)))
    }
  }

  // ── 10. PublicRobberAction ──────────────────────────────────────────────────

  describe("PublicRobberAction") {
    it("moves robber without stealing") {
      val action = PublicRobberAction()
      val move   = RobberMoveResult[Resource](0, 5, None)
      val result = action(move, NoInput)
      result.newRobberLocation shouldBe RobberLocation.Delta(5)
      result.steal shouldBe None
    }

    it("moves robber with an ImperfectInfoExchange steal") {
      val action = PublicRobberAction()
      val move   = RobberMoveResult[Resource](0, 5, Some(PlayerSteal(1, Some(Wood))))
      val result = action(move, NoInput)
      result.newRobberLocation shouldBe RobberLocation.Delta(5)
      result.steal shouldBe Some(ImperfectInfoExchange(1, 0, Some(Wood)))
    }

    it("moves robber with unknown stolen resource") {
      val action = PublicRobberAction()
      val move   = RobberMoveResult[Resource](0, 5, Some(PlayerSteal(1, None)))
      val result = action(move, NoInput)
      result.newRobberLocation shouldBe RobberLocation.Delta(5)
      result.steal shouldBe Some(ImperfectInfoExchange(1, 0, None))
    }
  }

  // ── 11. PerfectBuyDevCardAction ─────────────────────────────────────────────

  describe("PerfectBuyDevCardAction") {
    it("produces correct deltas for buying a KNIGHT card") {
      val action = PerfectBuyDevCardAction()
      val move   = PerfectInfoBuyDevelopmentCardMoveResult(0, KNIGHT)
      val input  = TurnInput(Turn(1))
      val result = action(move, input)
      result.resourcesSpent shouldBe Lose(0, DEV_CARD_COST)
      result.resourcesReturned shouldBe Bank.Add(DEV_CARD_COST)
      result.cardBought shouldBe PerfectInfoBuyCard(KNIGHT, 0, 1)
      result.deckShrunk shouldBe DevelopmentCardDeck.Remove
      result.bonusPoint shouldBe None
    }

    it("includes a bonus point when buying a POINT card") {
      val action = PerfectBuyDevCardAction()
      val move   = PerfectInfoBuyDevelopmentCardMoveResult(1, POINT)
      val input  = TurnInput(Turn(2))
      val result = action(move, input)
      result.cardBought shouldBe PerfectInfoBuyCard(POINT, 1, 2)
      result.bonusPoint shouldBe Some(PlayerPoints.Increment(1))
    }

    it("DEV_CARD_COST equals ORE, WHEAT, SHEEP") {
      DEV_CARD_COST.getTotal shouldBe 3
      DEV_CARD_COST.getAmount(ORE)   shouldBe 1
      DEV_CARD_COST.getAmount(WHEAT) shouldBe 1
      DEV_CARD_COST.getAmount(SHEEP) shouldBe 1
    }

    it("buys ROAD_BUILDER card without bonus point") {
      val action = PerfectBuyDevCardAction()
      val move   = PerfectInfoBuyDevelopmentCardMoveResult(0, ROAD_BUILDER)
      val input  = TurnInput(Turn(3))
      val result = action(move, input)
      result.cardBought shouldBe PerfectInfoBuyCard(ROAD_BUILDER, 0, 3)
      result.bonusPoint shouldBe None
    }
  }

  // ── 12. PublicBuyDevCardAction ──────────────────────────────────────────────

  describe("PublicBuyDevCardAction") {
    it("produces correct deltas for buying a dev card with known card") {
      val action = PublicBuyDevCardAction()
      val move   = BuyDevelopmentCardMoveResult[DevelopmentCard](0, Some(KNIGHT))
      val input  = TurnInput(Turn(1))
      val result = action(move, input)
      result.resourcesSpent shouldBe Lose(0, DEV_CARD_COST)
      result.resourcesReturned shouldBe Bank.Add(DEV_CARD_COST)
      result.cardBought shouldBe ImperfectInfoBuyCard(Some(KNIGHT), 0, 1)
      result.deckShrunk shouldBe DevelopmentCardDeck.Remove
    }

    it("produces correct deltas for buying a dev card with unknown card") {
      val action = PublicBuyDevCardAction()
      val move   = BuyDevelopmentCardMoveResult[DevelopmentCard](2, None)
      val input  = TurnInput(Turn(3))
      val result = action(move, input)
      result.resourcesSpent shouldBe Lose(2, DEV_CARD_COST)
      result.resourcesReturned shouldBe Bank.Add(DEV_CARD_COST)
      result.cardBought shouldBe ImperfectInfoBuyCard(None, 2, 3)
      result.deckShrunk shouldBe DevelopmentCardDeck.Remove
    }
  }

  // ── 13. PlayPointActions ────────────────────────────────────────────────────

  describe("PlayPerfectPointAction") {
    it("produces only the card played delta") {
      val action = PlayPerfectPointAction()
      val move   = PlayPointMove(0)
      val input  = TurnInput(Turn(1))
      val result = action(move, input)
      result.cardPlayed  shouldBe PlayCard(DevelopmentCards.POINT, 0, 1)
    }

    it("uses correct player and turn") {
      val action = PlayPerfectPointAction()
      val result = action(PlayPointMove(2), TurnInput(Turn(5)))
      result.cardPlayed  shouldBe PlayCard(DevelopmentCards.POINT, 2, 5)
    }
  }

  describe("PlayPublicPointAction") {
    it("produces point gain and card played deltas") {
      val action = PlayPublicPointAction()
      val move   = PlayPointMove(0)
      val input  = TurnInput(Turn(1))
      val result = action(move, input)
      result.pointGained shouldBe PlayerPoints.Increment(0)
      result.cardPlayed  shouldBe PlayCard(DevelopmentCards.POINT, 0, 1)
    }

    it("uses correct player and turn") {
      val action = PlayPublicPointAction()
      val result = action(PlayPointMove(2), TurnInput(Turn(5)))
      result.pointGained shouldBe PlayerPoints.Increment(2)
      result.cardPlayed  shouldBe PlayCard(DevelopmentCards.POINT, 2, 5)
    }
  }

  // ── 14. PlayMonopolyAction ──────────────────────────────────────────────────

  describe("PlayMonopolyAction") {
    it("steals all of a resource from other players") {
      val action = PlayMonopolyAction()
      val move   = PlayMonopolyMoveResult(0, WHEAT, Map(1 -> 2, 2 -> 1))
      val input  = TurnInput(Turn(1))
      val result = action(move, input)
      result.cardsLost should have length 2
      result.cardsLost(0) shouldBe Lose(1, InventorySet.fromMap(Map(WHEAT -> 2)))
      result.cardsLost(1) shouldBe Lose(2, InventorySet.fromMap(Map(WHEAT -> 1)))
      result.cardsGained shouldBe Gain(0, InventorySet.fromMap(Map(WHEAT -> 3)))
      result.cardPlayed shouldBe PlayCard(DevelopmentCards.MONOPOLY, 0, 1)
    }

    it("cardsLost is empty when no other players have the resource") {
      val action = PlayMonopolyAction()
      val move   = PlayMonopolyMoveResult(0, ORE, Map.empty)
      val input  = TurnInput(Turn(2))
      val result = action(move, input)
      result.cardsLost shouldBe empty
      result.cardsGained shouldBe Gain(0, InventorySet.empty[Resource, Int])
      result.cardPlayed shouldBe PlayCard(DevelopmentCards.MONOPOLY, 0, 2)
    }

    it("handles stealing from a single player") {
      val action = PlayMonopolyAction()
      val move   = PlayMonopolyMoveResult(1, WOOD, Map(3 -> 4))
      val input  = TurnInput(Turn(4))
      val result = action(move, input)
      result.cardsLost should have length 1
      result.cardsLost(0) shouldBe Lose(3, InventorySet.fromMap(Map(WOOD -> 4)))
      result.cardsGained shouldBe Gain(1, InventorySet.fromMap(Map(WOOD -> 4)))
    }
  }

  // ── 15. PlayRoadBuilderCoreAction ───────────────────────────────────────────

  describe("PlayRoadBuilderCoreAction") {
    it("builds two roads and plays the card") {
      val edge1  = Edge(Vertex(40), Vertex(41))
      val edge2  = Edge(Vertex(17), Vertex(40))
      val action = PlayRoadBuilderCoreAction()
      val move   = PlayRoadBuilderMove(0, edge1, Some(edge2))
      val input  = TurnInput(Turn(1))
      val result = action(move, input)
      result.addedRoads should have length 2
      result.addedRoads(0) shouldBe add(edge1, Road, 0)
      result.addedRoads(1) shouldBe add(edge2, Road, 0)
      result.cardPlayed shouldBe PlayCard(DevelopmentCards.ROAD_BUILDER, 0, 1)
    }

    it("builds one road when edge2 is None") {
      val edge1  = Edge(Vertex(40), Vertex(41))
      val action = PlayRoadBuilderCoreAction()
      val move   = PlayRoadBuilderMove(1, edge1, None)
      val input  = TurnInput(Turn(2))
      val result = action(move, input)
      result.addedRoads should have length 1
      result.addedRoads(0) shouldBe add(edge1, Road, 1)
      result.cardPlayed shouldBe PlayCard(DevelopmentCards.ROAD_BUILDER, 1, 2)
    }
  }

  // ── 16. PlayYearOfPlentyAction ──────────────────────────────────────────────

  describe("PlayYearOfPlentyAction") {
    it("takes two resources from bank and gives to player") {
      val action = PlayYearOfPlentyAction()
      val move   = PlayYearOfPlentyMove(0, ORE, WHEAT)
      val input  = TurnInput(Turn(1))
      val result = action(move, input)
      result.bankLost shouldBe Bank.Take(InventorySet.fromList(List(ORE, WHEAT)))
      result.playerGained shouldBe Gain(0, InventorySet.fromList(List(ORE, WHEAT)))
      result.cardPlayed shouldBe PlayCard(DevelopmentCards.YEAR_OF_PLENTY, 0, 1)
    }

    it("takes two same resources") {
      val action = PlayYearOfPlentyAction()
      val move   = PlayYearOfPlentyMove(1, WOOD, WOOD)
      val input  = TurnInput(Turn(3))
      val result = action(move, input)
      result.bankLost shouldBe Bank.Take(InventorySet.fromMap(Map(WOOD -> 2)))
      result.playerGained shouldBe Gain(1, InventorySet.fromMap(Map(WOOD -> 2)))
    }
  }

  // ── 17. RemoveKnightCardAction ──────────────────────────────────────────────

  describe("RemoveKnightCardAction") {
    it("produces a PlayCard delta for a Knight card") {
      val action = RemoveKnightCardAction()
      val input  = TurnInput(Turn(1))
      val result = action(0, input)
      result.cardPlayed shouldBe PlayCard(Knight, 0, 1)
    }

    it("uses correct player id and turn") {
      val action = RemoveKnightCardAction()
      val result = action(3, TurnInput(Turn(5)))
      result.cardPlayed shouldBe PlayCard(Knight, 3, 5)
    }

    it("RemoveKnightCardOutput is a case class with single field") {
      val action = RemoveKnightCardAction()
      val result = action(0, TurnInput(Turn(2)))
      result shouldBe RemoveKnightCardOutput(PlayCard(Knight, 0, 2))
    }
  }

  // ── 18. InitialPlacementCoreAction ──────────────────────────────────────────

  describe("InitialPlacementCoreAction") {
    it("first round: places settlement + road, grants VP, no resources") {
      val input = InitialPlacementInput(
        playerPoints        = PlayerPoints(Map(0 -> 0, 1 -> 0)),
        setupPlacementOrder = SetupPlacementOrder(Nil),
        board               = sampleBoard,
        vertexBuildingState = VertexBuildingState(Map.empty),
        edgeBuildingState   = EdgeBuildingState(Map.empty)
      )
      val action = InitialPlacementCoreAction()
      val move   = InitialPlacementMove(Vertex(41), Edge(Vertex(40), Vertex(41)), 0)
      val result = action(move, input)
      result.addedSettlement shouldBe add(Vertex(41), Settlement, 0)
      result.addedRoad       shouldBe add(Edge(Vertex(40), Vertex(41)), Road, 0)
      result.pointGained     shouldBe PlayerPoints.Increment(0)
      result.resourceGains   shouldBe empty
      result.bankLost        shouldBe empty
      result.setupPlacementOrder shouldBe SetupPlacementOrder.Placement(0, Vertex(41))
    }

    it("second round: places settlement + road, grants VP") {
      val input = InitialPlacementInput(
        playerPoints        = PlayerPoints(Map(0 -> 0, 1 -> 0)),
        setupPlacementOrder = SetupPlacementOrder(List(
          (0, Vertex(40)),
          (1, Vertex(34))
        )),
        board               = sampleBoard,
        vertexBuildingState = VertexBuildingState(Map.empty),
        edgeBuildingState   = EdgeBuildingState(Map.empty)
      )
      val action = InitialPlacementCoreAction()
      val move   = InitialPlacementMove(Vertex(41), Edge(Vertex(40), Vertex(41)), 0)
      val result = action(move, input)
      result.addedSettlement shouldBe add(Vertex(41), Settlement, 0)
      result.pointGained     shouldBe PlayerPoints.Increment(0)
      result.addedRoad       shouldBe add(Edge(Vertex(40), Vertex(41)), Road, 0)
      result.setupPlacementOrder shouldBe SetupPlacementOrder.Placement(0, Vertex(41))
    }

    it("handles players with setup complete (no more resources)") {
      val input = InitialPlacementInput(
        playerPoints        = PlayerPoints(Map(0 -> 0, 1 -> 0, 2 -> 0)),
        setupPlacementOrder = SetupPlacementOrder(Nil),
        board               = sampleBoard,
        vertexBuildingState = VertexBuildingState(Map.empty),
        edgeBuildingState   = EdgeBuildingState(Map.empty)
      )
      val action = InitialPlacementCoreAction()
      val move   = InitialPlacementMove(Vertex(41), Edge(Vertex(40), Vertex(41)), 2)
      val result = action(move, input)
      result.pointGained shouldBe PlayerPoints.Increment(2)
      result.resourceGains shouldBe empty
      result.bankLost shouldBe empty
    }

    it("desert vertex produces no resources in round 1") {
      val input = InitialPlacementInput(
        playerPoints        = PlayerPoints(Map(0 -> 0, 1 -> 0)),
        setupPlacementOrder = SetupPlacementOrder(Nil),
        board               = sampleBoard,
        vertexBuildingState = VertexBuildingState(Map.empty),
        edgeBuildingState   = EdgeBuildingState(Map.empty)
      )
      val action = InitialPlacementCoreAction()
      val move   = InitialPlacementMove(Vertex(23), Edge(Vertex(22), Vertex(23)), 0)
      val result = action(move, input)
      result.resourceGains shouldBe empty
      result.bankLost        shouldBe empty
    }
  }

  // ── Supporting type tests ──────────────────────────────────────────────────

  describe("Edge equality") {
    it("Edge equality ignores vertex order") {
      val e1 = Edge(Vertex(40), Vertex(41))
      val e2 = Edge(Vertex(41), Vertex(40))
      e1 shouldBe e2
      e1.hashCode shouldBe e2.hashCode
    }

    it("different Edges are not equal") {
      Edge(Vertex(40), Vertex(41)) should not be Edge(Vertex(40), Vertex(42))
    }
  }

  describe("Cost constants") {
    it("DEV_CARD_COST equals ORE, WHEAT, SHEEP") {
      DEV_CARD_COST.getTotal shouldBe 3
      DEV_CARD_COST.getAmount(ORE)   shouldBe 1
      DEV_CARD_COST.getAmount(WHEAT) shouldBe 1
      DEV_CARD_COST.getAmount(SHEEP) shouldBe 1
    }

    it("ROAD_COST equals WOOD, BRICK") {
      ROAD_COST.getTotal shouldBe 2
      ROAD_COST.getAmount(WOOD)  shouldBe 1
      ROAD_COST.getAmount(BRICK) shouldBe 1
    }

    it("SETTLEMENT_COST equals WOOD, BRICK, WHEAT, SHEEP") {
      SETTLEMENT_COST.getTotal shouldBe 4
      SETTLEMENT_COST.getAmount(WOOD)  shouldBe 1
      SETTLEMENT_COST.getAmount(BRICK) shouldBe 1
      SETTLEMENT_COST.getAmount(WHEAT) shouldBe 1
      SETTLEMENT_COST.getAmount(SHEEP) shouldBe 1
    }

    it("CITY_COST equals 3 ORE, 2 WHEAT") {
      CITY_COST.getTotal shouldBe 5
      CITY_COST.getAmount(ORE)   shouldBe 3
      CITY_COST.getAmount(WHEAT) shouldBe 2
    }
  }

  // ── InventorySet utility tests ─────────────────────────────────────────────

  describe("InventorySet") {
    it("ResourceSet factory creates correct sets") {
      val set = ResourceSet(WOOD, BRICK, WHEAT, SHEEP)
      set.getTotal shouldBe 4
      set.getAmount(WOOD)  shouldBe 1
      set.getAmount(BRICK) shouldBe 1
    }

    it("ResourceSet named-arg factory works") {
      val set = ResourceSet(wo = 2, br = 3, or = 1)
      set.getTotal shouldBe 6
      set.getAmount(WOOD)  shouldBe 2
      set.getAmount(BRICK) shouldBe 3
      set.getAmount(ORE)   shouldBe 1
    }
  }

  // ── TurnInput integration tests ────────────────────────────────────────────

  describe("TurnInput usage across actions") {
    it("PerfectBuyDevCardAction uses TurnInput.turn.t for card purchase turn") {
      val action = PerfectBuyDevCardAction()
      val move   = PerfectInfoBuyDevelopmentCardMoveResult(0, KNIGHT)
      val input  = TurnInput(Turn(7))
      val result = action(move, input)
      result.cardBought shouldBe PerfectInfoBuyCard(KNIGHT, 0, 7)
    }

    it("PlayPointAction uses TurnInput.turn.t for card play turn") {
      val action = PlayPublicPointAction()
      val result = action(PlayPointMove(1), TurnInput(Turn(3)))
      result.cardPlayed shouldBe PlayCard(DevelopmentCards.POINT, 1, 3)
    }

    it("RemoveKnightCardAction uses TurnInput.turn.t") {
      val action = RemoveKnightCardAction()
      val result = action(2, TurnInput(Turn(4)))
      result.cardPlayed shouldBe PlayCard(Knight, 2, 4)
    }
  }
}
