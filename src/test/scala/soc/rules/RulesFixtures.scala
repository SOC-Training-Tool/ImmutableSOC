package soc.rules

import game.InventorySet
import soc.base.BaseGame.*
import soc.base.BaseGameFixtures
import soc.base.state.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.ResourceSet.*
import soc.core.Resources.*
import soc.core.state.*

object RulesFixtures:

  val players: Set[Int] = Set(0, 1, 2, 3)

  val bank: Resources =
    InventorySet.fromMap(Map(WOOD -> 19, BRICK -> 19, SHEEP -> 19, WHEAT -> 19, ORE -> 19))

  val setupMoves: List[PerfectInfoMove] = List(
    InitialPlacementMove(Vertex(41), Edge(Vertex(40), Vertex(41)), 0),
    InitialPlacementMove(Vertex(34), Edge(Vertex(7), Vertex(34)), 1),
    InitialPlacementMove(Vertex(44), Edge(Vertex(44), Vertex(45)), 2),
    InitialPlacementMove(Vertex(36), Edge(Vertex(9), Vertex(36)), 3),
    InitialPlacementMove(Vertex(31), Edge(Vertex(2), Vertex(31)), 3),
    InitialPlacementMove(Vertex(47), Edge(Vertex(30), Vertex(47)), 2),
    InitialPlacementMove(Vertex(48), Edge(Vertex(48), Vertex(49)), 1),
    InitialPlacementMove(Vertex(22), Edge(Vertex(21), Vertex(22)), 0)
  )

  def initPerfect: PerfectInfoState = PerfectInfoState(
    robberLocation       = RobberLocation(10),
    privateInventories   = PrivateInventories(Map.empty),
    privateDevCardInv    = PrivateDevCardInv(Map.empty),
    developmentCardDeck  = DevelopmentCardDeck(Nil),
    bank                 = Bank(bank),
    turn                 = Turn(0),
    playerPoints         = PlayerPoints(players.map(_ -> 0).toMap),
    largestArmyPlayer    = LargestArmyPlayer(None),
    playerArmyCount      = PlayerArmyCount(Map.empty),
    vertexBuildingState  = VertexBuildingState(Map.empty),
    socRoadLengths       = SOCRoadLengths(Map.empty),
    socLongestRoadPlayer = SOCLongestRoadPlayer(None),
    board                = BaseGameFixtures.perfectInfoFixture.board,
    edgeBuildingState    = EdgeBuildingState(Map.empty),
    moveCount            = MoveCount(0),
    setupPlacementOrder  = SetupPlacementOrder(Nil)
  )

  def initPublic: PublicInfoState = PublicInfoState(
    robberLocation          = RobberLocation(10),
    publicInventories       = PublicInventories(Map.empty),
    publicDevCardInv        = PublicDevCardInv(Map.empty),
    developmentCardDeckSize = DevelopmentCardDeckSize(25),
    bank                    = Bank(bank),
    turn                    = Turn(0),
    playerPoints            = PlayerPoints(players.map(_ -> 0).toMap),
    largestArmyPlayer       = LargestArmyPlayer(None),
    playerArmyCount         = PlayerArmyCount(Map.empty),
    vertexBuildingState     = VertexBuildingState(Map.empty),
    socRoadLengths          = SOCRoadLengths(Map.empty),
    socLongestRoadPlayer    = SOCLongestRoadPlayer(None),
    board                   = BaseGameFixtures.perfectInfoFixture.board,
    edgeBuildingState       = EdgeBuildingState(Map.empty),
    moveCount               = MoveCount(0),
    setupPlacementOrder     = SetupPlacementOrder(Nil)
  )

  def afterPerfect(moves: PerfectInfoMove*): PerfectInfoState =
    moves.foldLeft(initPerfect) { case (s, m) => perfectInfoGame.applyMoveAny(m, s)._2 }

  def afterPublic(moves: PublicInfoMove*): PublicInfoState =
    moves.foldLeft(initPublic) { case (s, m) => publicInfoGame.applyMoveAny(m, s)._2 }

  def afterSetupPerfect: PerfectInfoState = afterPerfect(setupMoves*)
