/*
package soc.base

import game.InventorySet
import soc.ToPublicInfo
import soc.base.BaseGame.*
import soc.base.DevelopmentCards.*
import soc.base.state.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.ResourceSet.*
import soc.core.Resources.*
import soc.core.state.*

object BaseGameFixtures:

  private val bank: Resources = InventorySet.fromMap(Map(WOOD -> 19, BRICK -> 19, SHEEP -> 19, WHEAT -> 19, ORE -> 19))

  object perfectInfoGame:

    private val devDeck: List[DevelopmentCard] = List(
      KNIGHT, POINT, KNIGHT, POINT, POINT, KNIGHT, KNIGHT, ROAD_BUILDER,
      POINT, KNIGHT, MONOPOLY, YEAR_OF_PLENTY, YEAR_OF_PLENTY, KNIGHT, KNIGHT,
      KNIGHT, ROAD_BUILDER, MONOPOLY, KNIGHT, KNIGHT, KNIGHT, POINT, KNIGHT,
      KNIGHT, KNIGHT
    )

    val ports: List[Port] =
      import Ports.*
      List(MISC, ORE, MISC, WHEAT, MISC, BRICK, WOOD, SHEEP, MISC)

    val board: BaseBoard[Resource] = BaseBoard(
      List[Hex[Resource]](
        ResourceHex(WHEAT, 6),  ResourceHex(ORE, 2),    ResourceHex(SHEEP, 5),
        ResourceHex(ORE, 8),    ResourceHex(WOOD, 4),   ResourceHex(BRICK, 11),
        ResourceHex(SHEEP, 12), ResourceHex(ORE, 9),    ResourceHex(SHEEP, 10),
        ResourceHex(BRICK, 8),  Desert,                 ResourceHex(WHEAT, 3),
        ResourceHex(SHEEP, 9),  ResourceHex(BRICK, 10), ResourceHex(WOOD, 3),
        ResourceHex(WOOD, 6),   ResourceHex(WHEAT, 5),  ResourceHex(WOOD, 4),
        ResourceHex(WHEAT, 11)
      ),
      ports
    )

    val robberLocation: RobberLocation = RobberLocation(10)

    val initPerfectInfoState: PerfectInfoState = PerfectInfoState(
      robberLocation       = robberLocation,
      privateInventories   = PrivateInventories(Map.empty),
      privateDevCardInv    = PrivateDevCardInv(Map.empty),
      developmentCardDeck  = DevelopmentCardDeck(devDeck),
      bank                 = Bank(bank),
      turn                 = Turn(0),
      playerPoints         = PlayerPoints(Map.empty),
      largestArmyPlayer    = LargestArmyPlayer(None),
      playerArmyCount      = PlayerArmyCount(Map.empty),
      vertexBuildingState  = VertexBuildingState(Map.empty),
      socRoadLengths       = SOCRoadLengths(Map.empty),
      socLongestRoadPlayer = SOCLongestRoadPlayer(None),
      board                = board,
      edgeBuildingState    = EdgeBuildingState(Map.empty),
      moveCount            = MoveCount(0)
    )

    val testMoveResults: List[PerfectInfoGame.MOVES] = List(
      InitialPlacementMove(Vertex(41), Edge(Vertex(40), Vertex(41)), 0),
      InitialPlacementMove(Vertex(34), Edge(Vertex(7), Vertex(34)), 1),
      InitialPlacementMove(Vertex(44), Edge(Vertex(44), Vertex(45)), 2),
      InitialPlacementMove(Vertex(36), Edge(Vertex(9), Vertex(36)), 3),
      InitialPlacementMove(Vertex(31), Edge(Vertex(2), Vertex(31)), 3),
      InitialPlacementMove(Vertex(47), Edge(Vertex(30), Vertex(47)), 2),
      InitialPlacementMove(Vertex(48), Edge(Vertex(48), Vertex(49)), 1),
      InitialPlacementMove(Vertex(22), Edge(Vertex(21), Vertex(22)), 0),
      // Turn 0
      RollDiceMoveResult(0, 5), EndTurnMove(0),
      // Turn 1
      RollDiceMoveResult(1, 6), EndTurnMove(1),
      // Turn 2
      RollDiceMoveResult(2, 4), EndTurnMove(2),
      // Turn 3
      RollDiceMoveResult(3, 4), PerfectInfoBuyDevelopmentCardMoveResult(3, KNIGHT), EndTurnMove(3),
      // Turn 0
      RollDiceMoveResult(0, 9), PerfectInfoBuyDevelopmentCardMoveResult(0, POINT),
      BuildRoadMove(0, Edge(Vertex(17), Vertex(40))), EndTurnMove(0),
      // Turn 1
      RollDiceMoveResult(1, 8), PerfectInfoBuyDevelopmentCardMoveResult(1, KNIGHT), EndTurnMove(1),
      // Turn 2
      RollDiceMoveResult(2, 9),
      PortTradeMove(2, ResourceSet(WOOD, WOOD, WOOD, WOOD), ResourceSet(ORE)),
      PerfectInfoBuyDevelopmentCardMoveResult(2, POINT),
      BuildRoadMove(2, Edge(Vertex(24), Vertex(45))), EndTurnMove(2),
      // Turn 3
      RollDiceMoveResult(3, 7),
      PerfectInfoRobberMoveResult(3, 9, Some(PlayerSteal(3, BRICK))),
      PerfectInfoBuyDevelopmentCardMoveResult(3, POINT),
      BuildRoadMove(3, Edge(Vertex(1), Vertex(2))), EndTurnMove(3),
      // Turn 0
      RollDiceMoveResult(0, 6), EndTurnMove(0),
      // Turn 1
      RollDiceMoveResult(1, 5),
      PortTradeMove(1, ResourceSet(SHEEP, SHEEP, SHEEP, SHEEP), ResourceSet(WOOD)),
      BuildRoadMove(1, Edge(Vertex(49), Vertex(50))), EndTurnMove(1),
      // Turn 2
      RollDiceMoveResult(2, 3), EndTurnMove(2),
      // Turn 3
      RollDiceMoveResult(3, 10),
      PerfectInfoPlayKnightResult(PerfectInfoRobberMoveResult(3, 13, Some(PlayerSteal(1, BRICK)))),
      BuildSettlementMove(3, Vertex(1)), EndTurnMove(3),
      // Turn 0
      RollDiceMoveResult(0, 10), PerfectInfoBuyDevelopmentCardMoveResult(0, KNIGHT), EndTurnMove(0),
      // Turn 1
      PerfectInfoPlayKnightResult(PerfectInfoRobberMoveResult(1, 0, Some(PlayerSteal(3, WOOD)))),
      RollDiceMoveResult(1, 8), EndTurnMove(1),
      // Turn 2
      RollDiceMoveResult(2, 9), EndTurnMove(2),
      // Turn 3
      RollDiceMoveResult(3, 7),
      PerfectInfoRobberMoveResult(3, 9, Some(PlayerSteal(0, SHEEP))), EndTurnMove(3),
      // Turn 0
      PerfectInfoPlayKnightResult(PerfectInfoRobberMoveResult(0, 3, Some(PlayerSteal(3, BRICK)))),
      RollDiceMoveResult(0, 5), EndTurnMove(0),
      // Turn 1
      RollDiceMoveResult(1, 8), EndTurnMove(1),
      // Turn 2
      RollDiceMoveResult(2, 10), EndTurnMove(2),
      // Turn 3
      RollDiceMoveResult(3, 11), EndTurnMove(3),
      // Turn 0
      RollDiceMoveResult(0, 10),
      BuildSettlementMove(0, Vertex(17)), EndTurnMove(0),
      // Turn 1
      RollDiceMoveResult(1, 11),
      BuildSettlementMove(1, Vertex(50)),
      PerfectInfoBuyDevelopmentCardMoveResult(1, KNIGHT), EndTurnMove(1),
      // Turn 2
      RollDiceMoveResult(2, 5),
      PortTradeMove(2, ResourceSet(WHEAT, WHEAT, WHEAT, WHEAT), ResourceSet(WOOD)),
      BuildSettlementMove(2, Vertex(24)),
      PortTradeMove(2, ResourceSet(SHEEP, SHEEP), ResourceSet(WOOD)),
      BuildRoadMove(2, Edge(Vertex(29), Vertex(30))), EndTurnMove(2),
      // Turn 3
      RollDiceMoveResult(3, 3), EndTurnMove(3),
      // Turn 0
      RollDiceMoveResult(0, 10),
      PerfectInfoBuyDevelopmentCardMoveResult(0, ROAD_BUILDER), EndTurnMove(0),
      // Turn 1
      PerfectInfoPlayKnightResult(PerfectInfoRobberMoveResult(1, 9, Some(PlayerSteal(2, WHEAT)))),
      RollDiceMoveResult(1, 9),
      BuildRoadMove(1, Edge(Vertex(7), Vertex(8))),
      PortTradeMove(1, ResourceSet(br = 4), ResourceSet(wo = 1)),
      BuildSettlementMove(1, Vertex(8)), EndTurnMove(1),
      // Turn 2
      RollDiceMoveResult(2, 5), EndTurnMove(2),
      // Turn 3
      RollDiceMoveResult(3, 8), EndTurnMove(3),
      // Turn 0
      RollDiceMoveResult(0, 5),
      BuildCityMove(0, Vertex(41)), EndTurnMove(0),
      // Turn 1
      RollDiceMoveResult(1, 5), EndTurnMove(1),
      // Turn 2
      RollDiceMoveResult(2, 8), EndTurnMove(2),
      // Turn 3
      RollDiceMoveResult(3, 11), EndTurnMove(3),
      // Turn 0
      RollDiceMoveResult(0, 9),
      BuildCityMove(0, Vertex(22)), EndTurnMove(0),
      // Turn 1
      RollDiceMoveResult(1, 5),
      BuildCityMove(1, Vertex(34)),
      PortTradeMove(1, ResourceSet(sh = 3), ResourceSet(wh = 1)),
      PerfectInfoBuyDevelopmentCardMoveResult(1, POINT), EndTurnMove(1),
      // Turn 2
      RollDiceMoveResult(2, 8), EndTurnMove(2),
      // Turn 3
      RollDiceMoveResult(3, 8),
      PortTradeMove(3, ResourceSet(sh = 3), ResourceSet(br = 1)),
      BuildRoadMove(3, Edge(Vertex(9), Vertex(10))), EndTurnMove(3),
      // Turn 0
      RollDiceMoveResult(0, 7),
      DiscardMove(1, ResourceSet(or = 3, br = 1)),
      PerfectInfoRobberMoveResult(0, 3, Some(PlayerSteal(1, ORE))),
      PerfectInfoBuyDevelopmentCardMoveResult(0, KNIGHT), EndTurnMove(0),
      // Turn 1
      RollDiceMoveResult(1, 5), EndTurnMove(1),
      // Turn 2
      RollDiceMoveResult(2, 8),
      PortTradeMove(2, ResourceSet(wh = 4), ResourceSet(wo = 1)),
      BuildSettlementMove(2, Vertex(29)), EndTurnMove(2),
      // Turn 3
      RollDiceMoveResult(3, 5),
      PortTradeMove(3, ResourceSet(or = 3), ResourceSet(wh = 1)),
      PerfectInfoBuyDevelopmentCardMoveResult(3, MONOPOLY), EndTurnMove(3),
      // Turn 0
      RollDiceMoveResult(0, 10),
      PlayRoadBuilderMove(0, Edge(Vertex(20), Vertex(21)), Some(Edge(Vertex(39), Vertex(40)))),
      PortTradeMove(0, ResourceSet(br = 2), ResourceSet(wo = 1)),
      BuildSettlementMove(0, Vertex(20)), EndTurnMove(0),
      // Turn 1
      RollDiceMoveResult(1, 5),
      PortTradeMove(1, ResourceSet(sh = 3), ResourceSet(wh = 1)),
      PerfectInfoBuyDevelopmentCardMoveResult(1, YEAR_OF_PLENTY),
      PortTradeMove(1, ResourceSet(br = 3), ResourceSet(wh = 1)),
      PerfectInfoBuyDevelopmentCardMoveResult(1, YEAR_OF_PLENTY), EndTurnMove(1),
      // Turn 2
      RollDiceMoveResult(2, 8), EndTurnMove(2),
      // Turn 3
      RollDiceMoveResult(3, 6),
      PlayMonopolyMoveResult(3, WHEAT, Map(0 -> 6, 2 -> 3)),
      PortTradeMove(3, ResourceSet(wh = 3), ResourceSet(or = 1)),
      PortTradeMove(3, ResourceSet(wh = 3), ResourceSet(or = 1)),
      PortTradeMove(3, ResourceSet(wh = 3), ResourceSet(or = 1)),
      BuildCityMove(3, Vertex(31)), EndTurnMove(3),
      // Turn 0
      RollDiceMoveResult(0, 3),
      PerfectInfoPlayKnightResult(PerfectInfoRobberMoveResult(0, 0, Some(PlayerSteal(3, WOOD)))),
      PortTradeMove(0, ResourceSet(wo = 2), ResourceSet(wh = 1)),
      BuildSettlementMove(0, Vertex(39)), EndTurnMove(0),
      // Turn 1
      RollDiceMoveResult(1, 10),
      PlayYearOfPlentyMove(1, ORE, WHEAT),
      PerfectInfoBuyDevelopmentCardMoveResult(1, KNIGHT),
      BuildRoadMove(1, Edge(Vertex(35), Vertex(49))),
      BuildRoadMove(1, Edge(Vertex(34), Vertex(35))), EndTurnMove(1),
      // Turn 2
      RollDiceMoveResult(2, 11), EndTurnMove(2),
      // Turn 3
      RollDiceMoveResult(3, 6), EndTurnMove(3),
      // Turn 0
      RollDiceMoveResult(0, 5),
      BuildRoadMove(0, Edge(Vertex(19), Vertex(20))),
      BuildRoadMove(0, Edge(Vertex(19), Vertex(42))), EndTurnMove(0),
      // Turn 1
      PerfectInfoPlayKnightResult(PerfectInfoRobberMoveResult(1, 7, Some(PlayerSteal(0, WHEAT))))
    )

    lazy val perfectResult: PerfectInfoState =
      testMoveResults.foldLeft(initPerfectInfoState) { case (s, m) =>
        PerfectInfoGame.game.applyMove(m, s)._2
      }

    val imperfectTestMoveResults: List[PublicInfoGame.MOVES] =
      testMoveResults.map(ToPublicInfo.apply) ++ List[PublicInfoGame.MOVES](
        PlayPointMove(0), PlayPointMove(1), PlayPointMove(2), PlayPointMove(3)
      )

  object imperfectInfoGame:

    private val devDeck = DevelopmentCardDeckSize(25)

    private val ports: List[Port] =
      import Ports.*
      List(SHEEP, MISC, BRICK, ORE, WHEAT, MISC, WOOD, MISC, MISC)

    private val board: BaseBoard[Resource] = BaseBoard(
      List[Hex[Resource]](
        ResourceHex(ORE, 4),   ResourceHex(SHEEP, 8),  ResourceHex(ORE, 5),
        ResourceHex(BRICK, 2), ResourceHex(ORE, 6),    ResourceHex(WHEAT, 3),
        ResourceHex(SHEEP, 8), ResourceHex(WOOD, 10),  ResourceHex(BRICK, 9),
        ResourceHex(WOOD, 12), ResourceHex(WOOD, 11),  Desert,
        ResourceHex(SHEEP, 3), ResourceHex(WOOD, 10),  ResourceHex(BRICK, 9),
        ResourceHex(WHEAT, 4), ResourceHex(WHEAT, 5),  ResourceHex(WHEAT, 6),
        ResourceHex(SHEEP, 11)
      ),
      ports
    )

    private val robberLocation: RobberLocation = RobberLocation(11)

    val initPublicInfoState: PublicInfoState = PublicInfoState(
      robberLocation          = robberLocation,
      publicInventories       = PublicInventories(Map.empty),
      publicDevCardInv        = PublicDevCardInv(Map.empty),
      developmentCardDeckSize = devDeck,
      bank                    = Bank(bank),
      turn                    = Turn(0),
      playerPoints            = PlayerPoints(Map.empty),
      largestArmyPlayer       = LargestArmyPlayer(None),
      playerArmyCount         = PlayerArmyCount(Map.empty),
      vertexBuildingState     = VertexBuildingState(Map.empty),
      socRoadLengths          = SOCRoadLengths(Map.empty),
      socLongestRoadPlayer    = SOCLongestRoadPlayer(None),
      board                   = board,
      edgeBuildingState       = EdgeBuildingState(Map.empty),
      moveCount               = MoveCount(0)
    )

    val testMoveResults: List[PublicInfoGame.MOVES] = List(
      InitialPlacementMove(Vertex(33), Edge(Vertex(4), Vertex(33)), 0),
      InitialPlacementMove(Vertex(42), Edge(Vertex(42), Vertex(43)), 1),
      InitialPlacementMove(Vertex(37), Edge(Vertex(12), Vertex(37)), 2),
      InitialPlacementMove(Vertex(31), Edge(Vertex(30), Vertex(31)), 3),
      InitialPlacementMove(Vertex(39), Edge(Vertex(14), Vertex(39)), 3),
      InitialPlacementMove(Vertex(52), Edge(Vertex(52), Vertex(53)), 2),
      InitialPlacementMove(Vertex(35), Edge(Vertex(35), Vertex(36)), 1),
      InitialPlacementMove(Vertex(50), Edge(Vertex(49), Vertex(50)), 0),
      // Turn 0
      RollDiceMoveResult(0, 5), EndTurnMove(0),
      // Turn 1
      RollDiceMoveResult(1, 7),
      RobberMoveResult[Resource](1, 1, Some(PlayerSteal(0, Some(ORE)))),
      BuildRoadMove(1, Edge(9, 36)), EndTurnMove(1),
      // Turn 2
      RollDiceMoveResult(2, 4), EndTurnMove(2),
      // Turn 3
      RollDiceMoveResult(3, 3),
      BuyDevelopmentCardMoveResult[DevelopmentCard](3, None), EndTurnMove(3),
      // Turn 0
      RollDiceMoveResult(0, 8), EndTurnMove(0),
      // Turn 1
      RollDiceMoveResult(1, 6), EndTurnMove(1),
      // Turn 2
      RollDiceMoveResult(2, 8),
      BuyDevelopmentCardMoveResult[DevelopmentCard](2, None), EndTurnMove(2),
      // Turn 3
      RollDiceMoveResult(3, 2),
      TradeMove(3, 1, ResourceSet(SHEEP), ResourceSet(BRICK)), EndTurnMove(3),
      // Turn 0
      RollDiceMoveResult(0, 10),
      BuildRoadMove(0, Edge(48, 49)), EndTurnMove(0),
      // Turn 1
      RollDiceMoveResult(1, 10),
      TradeMove(1, 3, ResourceSet(WOOD), ResourceSet(WHEAT, SHEEP)),
      BuildSettlementMove(1, Vertex(9)), EndTurnMove(1),
      // Turn 2
      RollDiceMoveResult(2, 11), EndTurnMove(2),
      // Turn 3
      RollDiceMoveResult(3, 8),
      PlayRoadBuilderMove(3, Edge(2, 31), Some(Edge(2, 3))),
      BuildSettlementMove(3, Vertex(3)), EndTurnMove(3),
      // Turn 0
      RollDiceMoveResult(0, 5),
      BuyDevelopmentCardMoveResult[DevelopmentCard](0, None), EndTurnMove(0),
      // Turn 1
      RollDiceMoveResult(1, 7),
      RobberMoveResult[Resource](1, 15, Some(PlayerSteal(3, Some(WHEAT)))),
      BuyDevelopmentCardMoveResult(1, Some(KNIGHT)), EndTurnMove(1),
      // Turn 2
      RollDiceMoveResult(2, 7),
      RobberMoveResult[Resource](2, 1, Some(PlayerSteal(3, None))),
      PortTradeMove[Resource](2, ResourceSet(WHEAT, WHEAT, WHEAT, WHEAT), ResourceSet(ORE)),
      BuyDevelopmentCardMoveResult[DevelopmentCard](2, None), EndTurnMove(2),
      // Turn 3
      RollDiceMoveResult(3, 5), EndTurnMove(3),
      // Turn 0
      RollDiceMoveResult(0, 3), EndTurnMove(0),
      // Turn 1
      RollDiceMoveResult(1, 8), EndTurnMove(1),
      // Turn 2
      RollDiceMoveResult(2, 7),
      RobberMoveResult[Resource](2, 15, Some(PlayerSteal(3, None))),
      TradeMove[Resource](2, 1, ResourceSet(WHEAT), ResourceSet(WOOD)), EndTurnMove(2),
      // Turn 3
      RollDiceMoveResult(3, 7),
      RobberMoveResult[Resource](3, 14, Some(PlayerSteal(1, Some(WHEAT)))), EndTurnMove(3),
      // Turn 0
      RollDiceMoveResult(0, 11), EndTurnMove(0),
      // Turn 1
      PlayKnightResult(RobberMoveResult[Resource](1, 1, Some(PlayerSteal(3, Some(WHEAT))))),
      RollDiceMoveResult(1, 10),
      TradeMove(1, 3, ResourceSet(WHEAT), ResourceSet(SHEEP)), EndTurnMove(1),
      // Turn 2
      RollDiceMoveResult(2, 10), EndTurnMove(2),
      // Turn 3
      RollDiceMoveResult(3, 8), EndTurnMove(3),
      // Turn 0
      RollDiceMoveResult(0, 12),
      BuyDevelopmentCardMoveResult[DevelopmentCard](0, None), EndTurnMove(0),
      // Turn 1
      RollDiceMoveResult(1, 7),
      DiscardMove(1, ResourceSet(WOOD, WOOD, WHEAT, WHEAT)),
      RobberMoveResult[Resource](1, 0, Some(PlayerSteal(3, Some(SHEEP)))), EndTurnMove(1),
      // Turn 2
      RollDiceMoveResult(2, 4), EndTurnMove(2),
      // Turn 3
      RollDiceMoveResult(3, 9), EndTurnMove(3),
      // Turn 0
      RollDiceMoveResult(0, 8),
      BuildSettlementMove(0, Vertex(48)), EndTurnMove(0),
      // Turn 1
      RollDiceMoveResult(1, 8),
      BuildRoadMove(1, Edge(22, 43)),
      BuildSettlementMove(1, Vertex(43)), EndTurnMove(1),
      // Turn 2
      RollDiceMoveResult(2, 7),
      DiscardMove(3, ResourceSet(SHEEP, SHEEP, SHEEP, SHEEP)),
      RobberMoveResult[Resource](2, 15, Some(PlayerSteal(0, None))),
      BuildRoadMove(2, Edge(47, 53)), EndTurnMove(2),
      // Turn 3
      RollDiceMoveResult(3, 8),
      PortTradeMove(3, ResourceSet(SHEEP, SHEEP, SHEEP, SHEEP), ResourceSet(ORE, ORE)),
      BuyDevelopmentCardMoveResult[DevelopmentCard](3, None), EndTurnMove(3)
    )

    lazy val publicResult: PublicInfoState =
      testMoveResults.foldLeft(initPublicInfoState) { case (s, m) =>
        PublicInfoGame.game.applyMove(m, s)._2
      }
*/
