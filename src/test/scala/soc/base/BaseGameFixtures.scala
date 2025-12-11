package soc.base

import game.{ImmutableGame, InventorySet}
import shapeless.{Coproduct, HNil}
import soc.ToPublicInfo
import soc.base.DevelopmentCards._
import soc.base.state._
import soc.core.Resources._
import soc.core._
import soc.core.state._
import BaseGame.PublicInfoGame.{MOVES => PublicInfoMoves, STATE => PublicInfoState}
import BaseGame.PerfectInfoGame.{MOVES => PerfectInfoMoves, STATE => PerfectInfoState}
import util.DependsOn.HListDependsOnOp

object BaseGameFixtures {

  private val bank = InventorySet(Map(WOOD -> 19, BRICK -> 19, SHEEP -> 19, WHEAT -> 19, ORE -> 19))

  object perfectInfoGame {

    private val devDeck = List(
      KNIGHT,
      POINT,
      KNIGHT,
      POINT,
      POINT,
      KNIGHT,
      KNIGHT,
      ROAD_BUILDER,
      POINT,
      KNIGHT,
      MONOPOLY,
      YEAR_OF_PLENTY,
      YEAR_OF_PLENTY,
      KNIGHT,
      KNIGHT,
      KNIGHT,
      ROAD_BUILDER,
      MONOPOLY,
      KNIGHT,
      KNIGHT,
      KNIGHT,
      POINT,
      KNIGHT,
      KNIGHT,
      KNIGHT
    )

    val ports = {
      import Ports._
      List(MISC, ORE, MISC, WHEAT, MISC, BRICK, WOOD, SHEEP, MISC)
    }

    val board: BaseBoard[Resource] = BaseBoard(
      List[Hex[Resource]](
        ResourceHex(WHEAT, 6),
        ResourceHex(ORE, 2),
        ResourceHex(SHEEP, 5),
        ResourceHex(ORE, 8),
        ResourceHex(WOOD, 4),
        ResourceHex(BRICK, 11),
        ResourceHex(SHEEP, 12),
        ResourceHex(ORE, 9),
        ResourceHex(SHEEP, 10),
        ResourceHex(BRICK, 8),
        Desert,
        ResourceHex(WHEAT, 3),
        ResourceHex(SHEEP, 9),
        ResourceHex(BRICK, 10),
        ResourceHex(WOOD, 3),
        ResourceHex(WOOD, 6),
        ResourceHex(WHEAT, 5),
        ResourceHex(WOOD, 4),
        ResourceHex(WHEAT, 11)
      ),
      ports
    )

    val robberLocation = RobberLocation(10)

    val testMoveResults: List[PerfectInfoMoves] = List(
      Coproduct[PerfectInfoMoves](InitialPlacementMove(Vertex(41), Edge(Vertex(40), Vertex(41)), 0)),
      Coproduct[PerfectInfoMoves](InitialPlacementMove(Vertex(34), Edge(Vertex(7), Vertex(34)), 1)),
      Coproduct[PerfectInfoMoves](InitialPlacementMove(Vertex(44), Edge(Vertex(44), Vertex(45)), 2)),
      Coproduct[PerfectInfoMoves](InitialPlacementMove(Vertex(36), Edge(Vertex(9), Vertex(36)), 3)),
      Coproduct[PerfectInfoMoves](InitialPlacementMove(Vertex(31), Edge(Vertex(2), Vertex(31)), 3)),
      Coproduct[PerfectInfoMoves](InitialPlacementMove(Vertex(47), Edge(Vertex(30), Vertex(47)), 2)),
      Coproduct[PerfectInfoMoves](InitialPlacementMove(Vertex(48), Edge(Vertex(48), Vertex(49)), 1)),
      Coproduct[PerfectInfoMoves](InitialPlacementMove(Vertex(22), Edge(Vertex(21), Vertex(22)), 0)),
      // 0
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(0, 5)),
      Coproduct[PerfectInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(1, 6)),
      Coproduct[PerfectInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(2, 4)),
      Coproduct[PerfectInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(3, 4)),
      Coproduct[PerfectInfoMoves](PerfectInfoBuyDevelopmentCardMoveResult(3, KNIGHT)),
      Coproduct[PerfectInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(0, 9)),
      Coproduct[PerfectInfoMoves](PerfectInfoBuyDevelopmentCardMoveResult(0, POINT)),
      Coproduct[PerfectInfoMoves](BuildRoadMove(0, Edge(Vertex(17), Vertex(40)))),
      Coproduct[PerfectInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(1, 8)),
      Coproduct[PerfectInfoMoves](PerfectInfoBuyDevelopmentCardMoveResult(1, KNIGHT)),
      Coproduct[PerfectInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(2, 9)),
      Coproduct[PerfectInfoMoves](PortTradeMove(2, ResourceSet(WOOD, WOOD, WOOD, WOOD), ResourceSet(ORE))),
      Coproduct[PerfectInfoMoves](PerfectInfoBuyDevelopmentCardMoveResult(2, POINT)),
      Coproduct[PerfectInfoMoves](BuildRoadMove(2, Edge(Vertex(24), Vertex(45)))),
      Coproduct[PerfectInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(3, 7)),
      Coproduct[PerfectInfoMoves](PerfectInfoRobberMoveResult(3, 9, Some(PlayerSteal(3, BRICK)))),
      Coproduct[PerfectInfoMoves](PerfectInfoBuyDevelopmentCardMoveResult(3, POINT)),
      Coproduct[PerfectInfoMoves](BuildRoadMove(3, Edge(Vertex(1), Vertex(2)))),
      Coproduct[PerfectInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(0, 6)),
      Coproduct[PerfectInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(1, 5)),
      Coproduct[PerfectInfoMoves](PortTradeMove(1, ResourceSet(SHEEP, SHEEP, SHEEP, SHEEP), ResourceSet(WOOD))),
      Coproduct[PerfectInfoMoves](BuildRoadMove(1, Edge(Vertex(49), Vertex(50)))),
      Coproduct[PerfectInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(2, 3)),
      Coproduct[PerfectInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(3, 10)),
      Coproduct[PerfectInfoMoves](PerfectInfoPlayKnightResult(PerfectInfoRobberMoveResult(3, 13, Some(PlayerSteal(1, BRICK))))),
      Coproduct[PerfectInfoMoves](BuildSettlementMove(3, Vertex(1))),
      Coproduct[PerfectInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(0, 10)),
      Coproduct[PerfectInfoMoves](PerfectInfoBuyDevelopmentCardMoveResult(0, KNIGHT)),
      Coproduct[PerfectInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PerfectInfoMoves](PerfectInfoPlayKnightResult(PerfectInfoRobberMoveResult(1, 0, Some(PlayerSteal(3, WOOD))))),
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(1, 8)),
      Coproduct[PerfectInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(2, 9)),
      Coproduct[PerfectInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(3, 7)),
      Coproduct[PerfectInfoMoves](PerfectInfoRobberMoveResult(3, 9, Some(PlayerSteal(0, SHEEP)))),
      Coproduct[PerfectInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PerfectInfoMoves](PerfectInfoPlayKnightResult(PerfectInfoRobberMoveResult(0, 3, Some(PlayerSteal(3, BRICK))))),
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(0, 5)),
      Coproduct[PerfectInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(1, 8)),
      Coproduct[PerfectInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(2, 10)),
      Coproduct[PerfectInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(3, 11)),
      Coproduct[PerfectInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(0, 10)),
      Coproduct[PerfectInfoMoves](BuildSettlementMove(0, Vertex(17))),
      Coproduct[PerfectInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(1, 11)),
      Coproduct[PerfectInfoMoves](BuildSettlementMove(1, Vertex(50))),
      Coproduct[PerfectInfoMoves](PerfectInfoBuyDevelopmentCardMoveResult(1, KNIGHT)),
      Coproduct[PerfectInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(2, 5)),
      Coproduct[PerfectInfoMoves](PortTradeMove(2, ResourceSet(WHEAT, WHEAT, WHEAT, WHEAT), ResourceSet(WOOD))),
      Coproduct[PerfectInfoMoves](BuildSettlementMove(2, Vertex(24))),
      Coproduct[PerfectInfoMoves](PortTradeMove(2, ResourceSet(SHEEP, SHEEP), ResourceSet(WOOD))),
      Coproduct[PerfectInfoMoves](BuildRoadMove(2, Edge(Vertex(29), Vertex(30)))),
      Coproduct[PerfectInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(3, 3)),
      Coproduct[PerfectInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(0, 10)),
      Coproduct[PerfectInfoMoves](PerfectInfoBuyDevelopmentCardMoveResult(0, ROAD_BUILDER)),
      Coproduct[PerfectInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PerfectInfoMoves](PerfectInfoPlayKnightResult(PerfectInfoRobberMoveResult(1, 9, Some(PlayerSteal(2, WHEAT))))),
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(1, 9)),
      Coproduct[PerfectInfoMoves](BuildRoadMove(1, Edge(Vertex(7), Vertex(8)))),
      Coproduct[PerfectInfoMoves](PortTradeMove(1, ResourceSet(br = 4), ResourceSet(wo = 1))),
      Coproduct[PerfectInfoMoves](BuildSettlementMove(1, Vertex(8))),
      Coproduct[PerfectInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(2, 5)),
      Coproduct[PerfectInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(3, 8)),
      Coproduct[PerfectInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(0, 5)),
      Coproduct[PerfectInfoMoves](BuildCityMove(0, Vertex(41))),
      Coproduct[PerfectInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(1, 5)),
      Coproduct[PerfectInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(2, 8)),
      Coproduct[PerfectInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(3, 11)),
      Coproduct[PerfectInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(0, 9)),
      Coproduct[PerfectInfoMoves](BuildCityMove(0, Vertex(22))),
      Coproduct[PerfectInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(1, 5)),
      Coproduct[PerfectInfoMoves](BuildCityMove(1, Vertex(34))),
      Coproduct[PerfectInfoMoves](PortTradeMove(1, ResourceSet(sh = 3), ResourceSet(wh = 1))),
      Coproduct[PerfectInfoMoves](PerfectInfoBuyDevelopmentCardMoveResult(1, POINT)),
      Coproduct[PerfectInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(2, 8)),
      Coproduct[PerfectInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(3, 8)),
      Coproduct[PerfectInfoMoves](PortTradeMove(3, ResourceSet(sh = 3), ResourceSet(br = 1))),
      Coproduct[PerfectInfoMoves](BuildRoadMove(3, Edge(Vertex(9), Vertex(10)))),
      Coproduct[PerfectInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(0, 7)),
      Coproduct[PerfectInfoMoves](DiscardMove(1, ResourceSet(or = 3, br = 1))),
      Coproduct[PerfectInfoMoves](PerfectInfoRobberMoveResult(0, 3, Some(PlayerSteal(1, ORE)))),
      Coproduct[PerfectInfoMoves](PerfectInfoBuyDevelopmentCardMoveResult(0, KNIGHT)),
      Coproduct[PerfectInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(1, 5)),
      Coproduct[PerfectInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(2, 8)),
      Coproduct[PerfectInfoMoves](PortTradeMove(2, ResourceSet(wh = 4), ResourceSet(wo = 1))),
      Coproduct[PerfectInfoMoves](BuildSettlementMove(2, Vertex(29))),
      Coproduct[PerfectInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(3, 5)),
      Coproduct[PerfectInfoMoves](PortTradeMove(3, ResourceSet(or = 3), ResourceSet(wh = 1))),
      Coproduct[PerfectInfoMoves](PerfectInfoBuyDevelopmentCardMoveResult(3, MONOPOLY)),
      Coproduct[PerfectInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(0, 10)),
      Coproduct[PerfectInfoMoves](PlayRoadBuilderMove(0, Edge(Vertex(20), Vertex(21)), Some(Edge(Vertex(39), Vertex(40))))),
      Coproduct[PerfectInfoMoves](PortTradeMove(0, ResourceSet(br = 2), ResourceSet(wo = 1))),
      Coproduct[PerfectInfoMoves](BuildSettlementMove(0, Vertex(20))),
      Coproduct[PerfectInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(1, 5)),
      Coproduct[PerfectInfoMoves](PortTradeMove(1, ResourceSet(sh = 3), ResourceSet(wh = 1))),
      Coproduct[PerfectInfoMoves](PerfectInfoBuyDevelopmentCardMoveResult(1, YEAR_OF_PLENTY)),
      Coproduct[PerfectInfoMoves](PortTradeMove(1, ResourceSet(br = 3), ResourceSet(wh = 1))),
      Coproduct[PerfectInfoMoves](PerfectInfoBuyDevelopmentCardMoveResult(1, YEAR_OF_PLENTY)),
      Coproduct[PerfectInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(2, 8)),
      Coproduct[PerfectInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(3, 6)),
      Coproduct[PerfectInfoMoves](PlayMonopolyMoveResult(3, WHEAT, Map(0 -> 6, 2 -> 3))),
      Coproduct[PerfectInfoMoves](PortTradeMove(3, ResourceSet(wh = 3), ResourceSet(or = 1))),
      Coproduct[PerfectInfoMoves](PortTradeMove(3, ResourceSet(wh = 3), ResourceSet(or = 1))),
      Coproduct[PerfectInfoMoves](PortTradeMove(3, ResourceSet(wh = 3), ResourceSet(or = 1))),
      Coproduct[PerfectInfoMoves](BuildCityMove(3, Vertex(31))),
      Coproduct[PerfectInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(0, 3)),
      Coproduct[PerfectInfoMoves](PerfectInfoPlayKnightResult(PerfectInfoRobberMoveResult(0, 0, Some(PlayerSteal(3, WOOD))))),
      Coproduct[PerfectInfoMoves](PortTradeMove(0, ResourceSet(wo = 2), ResourceSet(wh = 1))),
      Coproduct[PerfectInfoMoves](BuildSettlementMove(0, Vertex(39))),
      Coproduct[PerfectInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(1, 10)),
      Coproduct[PerfectInfoMoves](PlayYearOfPlentyMove(1, ORE, WHEAT)),
      Coproduct[PerfectInfoMoves](PerfectInfoBuyDevelopmentCardMoveResult(1, KNIGHT)),
      Coproduct[PerfectInfoMoves](BuildRoadMove(1, Edge(Vertex(35), Vertex(49)))),
      Coproduct[PerfectInfoMoves](BuildRoadMove(1, Edge(Vertex(34), Vertex(35)))),
      Coproduct[PerfectInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(2, 11)),
      Coproduct[PerfectInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(3, 6)),
      Coproduct[PerfectInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PerfectInfoMoves](RollDiceMoveResult(0, 5)),
      Coproduct[PerfectInfoMoves](BuildRoadMove(0, Edge(Vertex(19), Vertex(20)))),
      Coproduct[PerfectInfoMoves](BuildRoadMove(0, Edge(Vertex(19), Vertex(42)))),
      Coproduct[PerfectInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PerfectInfoMoves](PerfectInfoPlayKnightResult[Resource](PerfectInfoRobberMoveResult(1, 7, Some(PlayerSteal(0, WHEAT)))))
    )

    val initPerfectInfoState: PerfectInfoState =
      ImmutableGame.initialize[PerfectInfoState].replaceAll(board :: Bank(bank) :: DevelopmentCardDeck(devDeck) :: robberLocation :: HNil)
    val initPublicInfoState: PublicInfoState       =
      ImmutableGame.initialize[PublicInfoState].replaceAll(board :: Bank(bank) :: DevelopmentCardDeckSize(devDeck.size) :: robberLocation :: HNil)

    val perfectResult = testMoveResults.foldLeft(initPerfectInfoState) { case (s, m) => BaseGame.PerfectInfoGame.game.applyMove(m, s) }

    val imperfectTestMoveResults = testMoveResults.map(implicitly[ToPublicInfo[PublicInfoMoves, PerfectInfoMoves]].apply) ::: List(
      Coproduct[PublicInfoMoves](PlayPointMove(0)),
      Coproduct[PublicInfoMoves](PlayPointMove(1)),
      Coproduct[PublicInfoMoves](PlayPointMove(2)),
      Coproduct[PublicInfoMoves](PlayPointMove(3))
    )

    val publicResult = imperfectTestMoveResults.foldLeft(initPublicInfoState) { case (s, m) => BaseGame.PublicInfoGame.game.applyMove(m, s) }
  }

  object imperfectInfoGame {

    private val devDeck = DevelopmentCardDeckSize(25)

    private val ports = {
      import Ports._
      List(SHEEP, MISC, BRICK, ORE, WHEAT, MISC, WOOD, MISC, MISC)
    }

    private val board = BaseBoard[Resource](
      List[Hex[Resource]](
        ResourceHex(ORE, 4),
        ResourceHex(SHEEP, 8),
        ResourceHex(ORE, 5),
        ResourceHex(BRICK, 2),
        ResourceHex(ORE, 6),
        ResourceHex(WHEAT, 3),
        ResourceHex(SHEEP, 8),
        ResourceHex(WOOD, 10),
        ResourceHex(BRICK, 9),
        ResourceHex(WOOD, 12),
        ResourceHex(WOOD, 11),
        Desert,
        ResourceHex(SHEEP, 3),
        ResourceHex(WOOD, 10),
        ResourceHex(BRICK, 9),
        ResourceHex(WHEAT, 4),
        ResourceHex(WHEAT, 5),
        ResourceHex(WHEAT, 6),
        ResourceHex(SHEEP, 11)
      ),
      ports
    )

    private val robberLocation = RobberLocation(11)

    val testMoveResults = List[PublicInfoMoves](
      Coproduct[PublicInfoMoves](InitialPlacementMove(Vertex(33), Edge(Vertex(4), Vertex(33)), 0)),
      Coproduct[PublicInfoMoves](InitialPlacementMove(Vertex(42), Edge(Vertex(42), Vertex(43)), 1)),
      Coproduct[PublicInfoMoves](InitialPlacementMove(Vertex(37), Edge(Vertex(12), Vertex(37)), 2)),
      Coproduct[PublicInfoMoves](InitialPlacementMove(Vertex(31), Edge(Vertex(30), Vertex(31)), 3)),
      Coproduct[PublicInfoMoves](InitialPlacementMove(Vertex(39), Edge(Vertex(14), Vertex(39)), 3)),
      Coproduct[PublicInfoMoves](InitialPlacementMove(Vertex(52), Edge(Vertex(52), Vertex(53)), 2)),
      Coproduct[PublicInfoMoves](InitialPlacementMove(Vertex(35), Edge(Vertex(35), Vertex(36)), 1)),
      Coproduct[PublicInfoMoves](InitialPlacementMove(Vertex(50), Edge(Vertex(49), Vertex(50)), 0)),
      // 0
      Coproduct[PublicInfoMoves](RollDiceMoveResult(0, 5)),
      Coproduct[PublicInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PublicInfoMoves](RollDiceMoveResult(1, 7)),
      Coproduct[PublicInfoMoves](RobberMoveResult[Resource](1, 1, Some(PlayerSteal(0, Some(ORE))))),
      Coproduct[PublicInfoMoves](BuildRoadMove(1, Edge(9, 36))),
      Coproduct[PublicInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PublicInfoMoves](RollDiceMoveResult(2, 4)),
      Coproduct[PublicInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PublicInfoMoves](RollDiceMoveResult(3, 3)),
      Coproduct[PublicInfoMoves](BuyDevelopmentCardMoveResult[DevelopmentCard](3, None)),
      Coproduct[PublicInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PublicInfoMoves](RollDiceMoveResult(0, 8)),
      Coproduct[PublicInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PublicInfoMoves](RollDiceMoveResult(1, 6)),
      Coproduct[PublicInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PublicInfoMoves](RollDiceMoveResult(2, 8)),
      Coproduct[PublicInfoMoves](BuyDevelopmentCardMoveResult[DevelopmentCard](2, None)),
      Coproduct[PublicInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PublicInfoMoves](RollDiceMoveResult(3, 2)),
      Coproduct[PublicInfoMoves](TradeMove(3, 1, ResourceSet(SHEEP), ResourceSet(BRICK))),
      Coproduct[PublicInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PublicInfoMoves](RollDiceMoveResult(0, 10)),
      Coproduct[PublicInfoMoves](BuildRoadMove(0, Edge(48, 49))),
      Coproduct[PublicInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PublicInfoMoves](RollDiceMoveResult(1, 10)),
      Coproduct[PublicInfoMoves](TradeMove(1, 3, ResourceSet(WOOD), ResourceSet(WHEAT, SHEEP))),
      Coproduct[PublicInfoMoves](BuildSettlementMove(1, Vertex(9))),
      Coproduct[PublicInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PublicInfoMoves](RollDiceMoveResult(2, 11)),
      Coproduct[PublicInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PublicInfoMoves](RollDiceMoveResult(3, 8)),
      Coproduct[PublicInfoMoves](PlayRoadBuilderMove(3, Edge(2, 31), Some(Edge(2, 3)))),
      Coproduct[PublicInfoMoves](BuildSettlementMove(3, Vertex(3))),
      Coproduct[PublicInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PublicInfoMoves](RollDiceMoveResult(0, 5)),
      Coproduct[PublicInfoMoves](BuyDevelopmentCardMoveResult[DevelopmentCard](0, None)),
      Coproduct[PublicInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PublicInfoMoves](RollDiceMoveResult(1, 7)),
      Coproduct[PublicInfoMoves](RobberMoveResult[Resource](1, 15, Some(PlayerSteal(3, Some(WHEAT))))),
      Coproduct[PublicInfoMoves](BuyDevelopmentCardMoveResult(1, Some(KNIGHT))),
      Coproduct[PublicInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PublicInfoMoves](RollDiceMoveResult(2, 7)),
      Coproduct[PublicInfoMoves](RobberMoveResult[Resource](2, 1, Some(PlayerSteal(3, None)))),
      Coproduct[PublicInfoMoves](PortTradeMove[Resource](2, ResourceSet(WHEAT, WHEAT, WHEAT, WHEAT), ResourceSet(ORE))),
      Coproduct[PublicInfoMoves](BuyDevelopmentCardMoveResult[DevelopmentCard](2, None)),
      Coproduct[PublicInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PublicInfoMoves](RollDiceMoveResult(3, 5)),
      Coproduct[PublicInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PublicInfoMoves](RollDiceMoveResult(0, 3)),
      Coproduct[PublicInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PublicInfoMoves](RollDiceMoveResult(1, 8)),
      Coproduct[PublicInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PublicInfoMoves](RollDiceMoveResult(2, 7)),
      Coproduct[PublicInfoMoves](RobberMoveResult[Resource](2, 15, Some(PlayerSteal(3, None)))),
      Coproduct[PublicInfoMoves](TradeMove[Resource](2, 1, ResourceSet(WHEAT), ResourceSet(WOOD))),
      Coproduct[PublicInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PublicInfoMoves](RollDiceMoveResult(3, 7)),
      Coproduct[PublicInfoMoves](RobberMoveResult[Resource](3, 14, Some(PlayerSteal(1, Some(WHEAT))))),
      Coproduct[PublicInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PublicInfoMoves](RollDiceMoveResult(0, 11)),
      Coproduct[PublicInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PublicInfoMoves](PlayKnightResult(RobberMoveResult[Resource](1, 1, Some(PlayerSteal(3, Some(WHEAT)))))),
      Coproduct[PublicInfoMoves](RollDiceMoveResult(1, 10)),
      Coproduct[PublicInfoMoves](TradeMove(1, 3, ResourceSet(WHEAT), ResourceSet(SHEEP))),
      Coproduct[PublicInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PublicInfoMoves](RollDiceMoveResult(2, 10)),
      Coproduct[PublicInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PublicInfoMoves](RollDiceMoveResult(3, 8)),
      Coproduct[PublicInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PublicInfoMoves](RollDiceMoveResult(0, 12)),
      Coproduct[PublicInfoMoves](BuyDevelopmentCardMoveResult[DevelopmentCard](0, None)),
      Coproduct[PublicInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PublicInfoMoves](RollDiceMoveResult(1, 7)),
      Coproduct[PublicInfoMoves](DiscardMove(1, ResourceSet(WOOD, WOOD, WHEAT, WHEAT))),
      Coproduct[PublicInfoMoves](RobberMoveResult[Resource](1, 0, Some(PlayerSteal(3, Some(SHEEP))))),
      Coproduct[PublicInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PublicInfoMoves](RollDiceMoveResult(2, 4)),
      Coproduct[PublicInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PublicInfoMoves](RollDiceMoveResult(3, 9)),
      Coproduct[PublicInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PublicInfoMoves](RollDiceMoveResult(0, 8)),
      Coproduct[PublicInfoMoves](BuildSettlementMove(0, Vertex(48))),
      Coproduct[PublicInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PublicInfoMoves](RollDiceMoveResult(1, 8)),
      Coproduct[PublicInfoMoves](BuildRoadMove(1, Edge(22, 43))),
      Coproduct[PublicInfoMoves](BuildSettlementMove(1, Vertex(43))),
      Coproduct[PublicInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PublicInfoMoves](RollDiceMoveResult(2, 7)),
      Coproduct[PublicInfoMoves](DiscardMove(3, ResourceSet(SHEEP, SHEEP, SHEEP, SHEEP))),
      Coproduct[PublicInfoMoves](RobberMoveResult[Resource](2, 15, Some(PlayerSteal(0, None)))),
      Coproduct[PublicInfoMoves](BuildRoadMove(2, Edge(47, 53))),
      Coproduct[PublicInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PublicInfoMoves](RollDiceMoveResult(3, 8)),
      Coproduct[PublicInfoMoves](PortTradeMove(3, ResourceSet(SHEEP, SHEEP, SHEEP, SHEEP), ResourceSet(ORE, ORE))),
      Coproduct[PublicInfoMoves](BuyDevelopmentCardMoveResult[DevelopmentCard](3, None)),
      Coproduct[PublicInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PublicInfoMoves](PlayKnightResult(RobberMoveResult[Resource](0, 0, Some(PlayerSteal(3, None))))),
      Coproduct[PublicInfoMoves](RollDiceMoveResult(0, 10)),
      Coproduct[PublicInfoMoves](TradeMove(3, 2, ResourceSet(SHEEP), ResourceSet(WHEAT))),
      Coproduct[PublicInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PublicInfoMoves](RollDiceMoveResult(1, 7)),
      Coproduct[PublicInfoMoves](RobberMoveResult[Resource](1, 1, Some(PlayerSteal(0, Some(WHEAT))))),
      Coproduct[PublicInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PublicInfoMoves](RollDiceMoveResult(2, 4)),
      Coproduct[PublicInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PublicInfoMoves](PlayKnightResult(RobberMoveResult[Resource](3, 4, Some(PlayerSteal(2, None))))),
      Coproduct[PublicInfoMoves](RollDiceMoveResult(3, 10)),
      Coproduct[PublicInfoMoves](BuyDevelopmentCardMoveResult[DevelopmentCard](3, None)),
      Coproduct[PublicInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PublicInfoMoves](RollDiceMoveResult(0, 4)),
      Coproduct[PublicInfoMoves](PortTradeMove(0, ResourceSet(WOOD, WOOD, WOOD, WOOD), ResourceSet(BRICK))),
      Coproduct[PublicInfoMoves](BuildRoadMove(0, Edge(4, 5))),
      Coproduct[PublicInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PublicInfoMoves](RollDiceMoveResult(1, 9)),
      Coproduct[PublicInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PublicInfoMoves](PlayKnightResult(RobberMoveResult[Resource](2, 1, Some(PlayerSteal(3, None))))),
      Coproduct[PublicInfoMoves](RollDiceMoveResult(2, 8)),
      Coproduct[PublicInfoMoves](TradeMove(2, 1, ResourceSet(SHEEP), ResourceSet(WOOD))),
      Coproduct[PublicInfoMoves](BuildSettlementMove(2, Vertex(47))),
      Coproduct[PublicInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PublicInfoMoves](RollDiceMoveResult(3, 7)),
      Coproduct[PublicInfoMoves](RobberMoveResult[Resource](3, 4, Some(PlayerSteal(1, Some(WHEAT))))),
      Coproduct[PublicInfoMoves](BuyDevelopmentCardMoveResult[DevelopmentCard](3, None)),
      Coproduct[PublicInfoMoves](PlayRoadBuilderMove(3, Edge(29, 30), Some(Edge(14, 15)))),
      Coproduct[PublicInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PublicInfoMoves](RollDiceMoveResult(0, 4)),
      Coproduct[PublicInfoMoves](BuildSettlementMove(0, Vertex(5))),
      Coproduct[PublicInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PublicInfoMoves](RollDiceMoveResult(1, 9)),
      Coproduct[PublicInfoMoves](PortTradeMove(1, ResourceSet(BRICK, BRICK, BRICK, BRICK), ResourceSet(ORE, ORE))),
      Coproduct[PublicInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PublicInfoMoves](RollDiceMoveResult(2, 11)),
      Coproduct[PublicInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PublicInfoMoves](RollDiceMoveResult(3, 4)),
      Coproduct[PublicInfoMoves](BuildCityMove(3, Vertex(31))),
      Coproduct[PublicInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PublicInfoMoves](RollDiceMoveResult(0, 7)),
      Coproduct[PublicInfoMoves](RobberMoveResult[Resource](0, 5, Some(PlayerSteal(3, None)))),
      Coproduct[PublicInfoMoves](PortTradeMove(0, ResourceSet(WHEAT, WHEAT, WHEAT), ResourceSet(ORE))),
      Coproduct[PublicInfoMoves](BuyDevelopmentCardMoveResult[DevelopmentCard](0, None)),
      Coproduct[PublicInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PublicInfoMoves](RollDiceMoveResult(1, 5)),
      Coproduct[PublicInfoMoves](PortTradeMove(1, ResourceSet(BRICK, BRICK), ResourceSet(ORE))),
      Coproduct[PublicInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PublicInfoMoves](RollDiceMoveResult(2, 6)),
      Coproduct[PublicInfoMoves](BuyDevelopmentCardMoveResult[DevelopmentCard](2, None)),
      Coproduct[PublicInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PublicInfoMoves](RollDiceMoveResult(3, 8)),
      Coproduct[PublicInfoMoves](PlayKnightResult(RobberMoveResult[Resource](3, 14, Some(PlayerSteal(1, Some(ORE)))))),
      Coproduct[PublicInfoMoves](PortTradeMove(3, ResourceSet(SHEEP, SHEEP), ResourceSet(WHEAT))),
      Coproduct[PublicInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PublicInfoMoves](PlayKnightResult(RobberMoveResult[Resource](0, 0, Some(PlayerSteal(3, None))))),
      Coproduct[PublicInfoMoves](RollDiceMoveResult(0, 6)),
      Coproduct[PublicInfoMoves](PortTradeMove(0, ResourceSet(SHEEP, SHEEP, SHEEP), ResourceSet(WHEAT))),
      Coproduct[PublicInfoMoves](BuyDevelopmentCardMoveResult[DevelopmentCard](0, None)),
      Coproduct[PublicInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PublicInfoMoves](RollDiceMoveResult(1, 8)),
      Coproduct[PublicInfoMoves](TradeMove(1, 2, ResourceSet(ORE), ResourceSet(WHEAT))),
      Coproduct[PublicInfoMoves](BuildCityMove(1, Vertex(42))),
      Coproduct[PublicInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PublicInfoMoves](RollDiceMoveResult(2, 8)),
      Coproduct[PublicInfoMoves](PlayKnightResult(RobberMoveResult[Resource](2, 8, Some(PlayerSteal(1, Some(SHEEP)))))),
      Coproduct[PublicInfoMoves](BuyDevelopmentCardMoveResult[DevelopmentCard](2, None)),
      Coproduct[PublicInfoMoves](BuyDevelopmentCardMoveResult[DevelopmentCard](2, None)),
      Coproduct[PublicInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PublicInfoMoves](RollDiceMoveResult(3, 9)),
      Coproduct[PublicInfoMoves](PortTradeMove(3, ResourceSet(SHEEP, SHEEP, SHEEP, SHEEP), ResourceSet(ORE, WHEAT))),
      Coproduct[PublicInfoMoves](BuyDevelopmentCardMoveResult[DevelopmentCard](3, None)),
      Coproduct[PublicInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PublicInfoMoves](RollDiceMoveResult(0, 10)),
      Coproduct[PublicInfoMoves](BuildRoadMove(0, Edge(32, 48))),
      Coproduct[PublicInfoMoves](BuildRoadMove(0, Edge(32, 33))),
      Coproduct[PublicInfoMoves](PlayYearOfPlentyMove(0, WHEAT, WHEAT)),
      Coproduct[PublicInfoMoves](BuyDevelopmentCardMoveResult[DevelopmentCard](0, None)),
      Coproduct[PublicInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PublicInfoMoves](RollDiceMoveResult(1, 7)),
      Coproduct[PublicInfoMoves](RobberMoveResult[Resource](1, 1, Some(PlayerSteal(0, Some(WHEAT))))),
      Coproduct[PublicInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PublicInfoMoves](RollDiceMoveResult(2, 7)),
      Coproduct[PublicInfoMoves](RobberMoveResult[Resource](2, 2, Some(PlayerSteal(0, None)))),
      Coproduct[PublicInfoMoves](PlayKnightResult(RobberMoveResult[Resource](2, 1, Some(PlayerSteal(3, None))))),
      Coproduct[PublicInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PublicInfoMoves](PlayKnightResult(RobberMoveResult[Resource](3, 4, Some(PlayerSteal(2, None))))),
      Coproduct[PublicInfoMoves](RollDiceMoveResult(3, 9)),
      Coproduct[PublicInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PublicInfoMoves](RollDiceMoveResult(0, 11)),
      Coproduct[PublicInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PublicInfoMoves](RollDiceMoveResult(1, 8)),
      Coproduct[PublicInfoMoves](PortTradeMove(1, ResourceSet(BRICK, BRICK), ResourceSet(SHEEP))),
      Coproduct[PublicInfoMoves](BuildRoadMove(1, Edge(43, 44))),
      Coproduct[PublicInfoMoves](BuildRoadMove(1, Edge(44, 45))),
      Coproduct[PublicInfoMoves](BuildSettlementMove(1, Vertex(45))),
      Coproduct[PublicInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PublicInfoMoves](RollDiceMoveResult(2, 10)),
      Coproduct[PublicInfoMoves](PlayMonopolyMoveResult(2, SHEEP, Map[Int, Int](0 -> 3, 3 -> 6))),
      Coproduct[PublicInfoMoves](PortTradeMove(2, ResourceSet(sh = 8), ResourceSet(ORE, ORE))),
      Coproduct[PublicInfoMoves](BuyDevelopmentCardMoveResult[DevelopmentCard](2, None)),
      Coproduct[PublicInfoMoves](BuyDevelopmentCardMoveResult[DevelopmentCard](2, None)),
      Coproduct[PublicInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PublicInfoMoves](RollDiceMoveResult(3, 2)),
      Coproduct[PublicInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PublicInfoMoves](RollDiceMoveResult(0, 11)),
      Coproduct[PublicInfoMoves](BuildRoadMove(0, Edge(5, 6))),
      Coproduct[PublicInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PublicInfoMoves](RollDiceMoveResult(1, 6)),
      Coproduct[PublicInfoMoves](BuildRoadMove(1, Edge(19, 42))),
      Coproduct[PublicInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PublicInfoMoves](RollDiceMoveResult(2, 12)),
      Coproduct[PublicInfoMoves](PlayYearOfPlentyMove(2, ORE, ORE)),
      Coproduct[PublicInfoMoves](BuyDevelopmentCardMoveResult[DevelopmentCard](2, None)),
      Coproduct[PublicInfoMoves](BuyDevelopmentCardMoveResult[DevelopmentCard](2, None)),
      Coproduct[PublicInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PublicInfoMoves](RollDiceMoveResult(3, 7)),
      Coproduct[PublicInfoMoves](RobberMoveResult[Resource](3, 17, Some(PlayerSteal(2, None)))),
      Coproduct[PublicInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PublicInfoMoves](RollDiceMoveResult(0, 7)),
      Coproduct[PublicInfoMoves](RobberMoveResult[Resource](0, 4, Some(PlayerSteal(0, None)))),
      Coproduct[PublicInfoMoves](BuildRoadMove(0, Edge(6, 7))),
      Coproduct[PublicInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PublicInfoMoves](RollDiceMoveResult(1, 6)),
      Coproduct[PublicInfoMoves](BuildRoadMove(1, Edge(18, 19))),
      Coproduct[PublicInfoMoves](PortTradeMove(1, ResourceSet(WOOD, WOOD), ResourceSet(SHEEP))),
      Coproduct[PublicInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PublicInfoMoves](PlayKnightResult(RobberMoveResult[Resource](2, 1, Some(PlayerSteal(0, None))))),
      Coproduct[PublicInfoMoves](RollDiceMoveResult(2, 11)),
      Coproduct[PublicInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PublicInfoMoves](RollDiceMoveResult(3, 7)),
      Coproduct[PublicInfoMoves](RobberMoveResult[Resource](3, 4, Some(PlayerSteal(2, None)))),
      Coproduct[PublicInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PublicInfoMoves](RollDiceMoveResult(0, 5)),
      Coproduct[PublicInfoMoves](PlayMonopolyMoveResult(0, WHEAT, Map[Int, Int](1 -> 4, 2 -> 3))),
      Coproduct[PublicInfoMoves](PortTradeMove(0, ResourceSet(SHEEP, SHEEP, SHEEP), ResourceSet(ORE))),
      Coproduct[PublicInfoMoves](BuildCityMove(0, Vertex(33))),
      Coproduct[PublicInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PublicInfoMoves](RollDiceMoveResult(1, 8)),
      Coproduct[PublicInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PublicInfoMoves](PlayKnightResult(RobberMoveResult[Resource](2, 2, Some(PlayerSteal(0, None))))),
      Coproduct[PublicInfoMoves](RollDiceMoveResult(2, 9)),
      Coproduct[PublicInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PublicInfoMoves](RollDiceMoveResult(3, 6)),
      Coproduct[PublicInfoMoves](PortTradeMove(3, ResourceSet(SHEEP, SHEEP, SHEEP, SHEEP), ResourceSet(WOOD, WHEAT))),
      Coproduct[PublicInfoMoves](BuildSettlementMove(3, Vertex(15))),
      Coproduct[PublicInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PublicInfoMoves](PlayKnightResult(RobberMoveResult[Resource](0, 4, Some(PlayerSteal(2, None))))),
      Coproduct[PublicInfoMoves](RollDiceMoveResult(0, 6)),
      Coproduct[PublicInfoMoves](PortTradeMove(0, ResourceSet(WHEAT, WHEAT, WHEAT), ResourceSet(ORE))),
      Coproduct[PublicInfoMoves](BuyDevelopmentCardMoveResult[DevelopmentCard](0, None)),
      Coproduct[PublicInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PublicInfoMoves](RollDiceMoveResult(1, 10)),
      Coproduct[PublicInfoMoves](BuildRoadMove(1, Edge(17, 18))),
      Coproduct[PublicInfoMoves](BuildSettlementMove(1, Vertex(17))),
      Coproduct[PublicInfoMoves](TradeMove(1, 0, ResourceSet(ORE, WHEAT), ResourceSet(BRICK))),
      Coproduct[PublicInfoMoves](BuildRoadMove(1, Edge(22, 23))),
      Coproduct[PublicInfoMoves](BuildRoadMove(1, Edge(23, 24))),
      Coproduct[PublicInfoMoves](BuildRoadMove(1, Edge(24, 45))),
      Coproduct[PublicInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PublicInfoMoves](RollDiceMoveResult(2, 10)),
      Coproduct[PublicInfoMoves](BuyDevelopmentCardMoveResult[DevelopmentCard](2, None)),
      Coproduct[PublicInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PublicInfoMoves](RollDiceMoveResult(3, 8)),
      Coproduct[PublicInfoMoves](PortTradeMove(3, ResourceSet(SHEEP, SHEEP, SHEEP, SHEEP), ResourceSet(WOOD, WHEAT))),
      Coproduct[PublicInfoMoves](BuildSettlementMove(3, Vertex(29))),
      Coproduct[PublicInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PublicInfoMoves](RollDiceMoveResult(0, 6)),
      Coproduct[PublicInfoMoves](BuyDevelopmentCardMoveResult[DevelopmentCard](0, None)),
      Coproduct[PublicInfoMoves](BuildSettlementMove(0, Vertex(7))),
      Coproduct[PublicInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PublicInfoMoves](RollDiceMoveResult(1, 6)),
      Coproduct[PublicInfoMoves](PortTradeMove(1, ResourceSet(WOOD, WOOD), ResourceSet(ORE))),
      Coproduct[PublicInfoMoves](BuyDevelopmentCardMoveResult[DevelopmentCard](1, Some(KNIGHT))),
      Coproduct[PublicInfoMoves](PortTradeMove(1, ResourceSet(WOOD, WOOD), ResourceSet(BRICK))),
      Coproduct[PublicInfoMoves](BuildRoadMove(1, Edge(17, 40))),
      Coproduct[PublicInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PublicInfoMoves](RollDiceMoveResult(2, 4)),
      Coproduct[PublicInfoMoves](PortTradeMove(2, ResourceSet(WHEAT, WHEAT, WHEAT, WHEAT), ResourceSet(ORE))),
      Coproduct[PublicInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PublicInfoMoves](RollDiceMoveResult(3, 7)),
      Coproduct[PublicInfoMoves](RobberMoveResult[Resource](3, 17, Some(PlayerSteal(1, Some(WHEAT))))),
      Coproduct[PublicInfoMoves](BuildCityMove(3, Vertex(39))),
      Coproduct[PublicInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PublicInfoMoves](RollDiceMoveResult(0, 11)),
      Coproduct[PublicInfoMoves](PortTradeMove(0, ResourceSet(WOOD, WOOD, WOOD), ResourceSet(BRICK))),
      Coproduct[PublicInfoMoves](BuildRoadMove(0, Edge(7, 34))),
      Coproduct[PublicInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PublicInfoMoves](PlayKnightResult(RobberMoveResult[Resource](1, 1, Some(PlayerSteal(0, Some(WHEAT)))))),
      Coproduct[PublicInfoMoves](RollDiceMoveResult(1, 10)),
      Coproduct[PublicInfoMoves](PortTradeMove(1, ResourceSet(WOOD, WOOD), ResourceSet(ORE))),
      Coproduct[PublicInfoMoves](EndTurnMove(1)),
      // 2
      Coproduct[PublicInfoMoves](RollDiceMoveResult(2, 4)),
      Coproduct[PublicInfoMoves](EndTurnMove(2)),
      // 3
      Coproduct[PublicInfoMoves](RollDiceMoveResult(3, 5)),
      Coproduct[PublicInfoMoves](BuildCityMove(3, Vertex(15))),
      Coproduct[PublicInfoMoves](EndTurnMove(3)),
      // 0
      Coproduct[PublicInfoMoves](PlayKnightResult(RobberMoveResult[Resource](0, 6, Some(PlayerSteal(1, Some(ORE)))))),
      Coproduct[PublicInfoMoves](RollDiceMoveResult(0, 6)),
      Coproduct[PublicInfoMoves](PortTradeMove(0, ResourceSet(WOOD, WOOD, WOOD), ResourceSet(WHEAT))),
      Coproduct[PublicInfoMoves](BuildCityMove(0, Vertex(50))),
      Coproduct[PublicInfoMoves](EndTurnMove(0)),
      // 1
      Coproduct[PublicInfoMoves](RollDiceMoveResult(1, 10)),
      Coproduct[PublicInfoMoves](PortTradeMove(1, ResourceSet(WOOD, WOOD, WOOD, WOOD), ResourceSet(ORE, ORE))),
      Coproduct[PublicInfoMoves](BuildCityMove(1, Vertex(9))),

      Coproduct[PublicInfoMoves](PlayPointMove(0)),
      Coproduct[PublicInfoMoves](PlayPointMove(2)),
      Coproduct[PublicInfoMoves](PlayPointMove(2)),
      Coproduct[PublicInfoMoves](PlayPointMove(2)),
      Coproduct[PublicInfoMoves](PlayPointMove(3))
    )

    val initPublicInfoState: PublicInfoState =
      ImmutableGame.initialize[PublicInfoState].replaceAll(board :: Bank(bank) :: DevelopmentCardDeckSize(25) :: robberLocation :: HNil)

    val publicResult = testMoveResults.foldLeft(initPublicInfoState) {
      case (s, m) => BaseGame.PublicInfoGame.game.applyMove(m, s)
    }
  }

}
