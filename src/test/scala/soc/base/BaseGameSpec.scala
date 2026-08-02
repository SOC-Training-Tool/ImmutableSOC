package soc.base

import game.InventorySet
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import soc.base.BaseGame.*
import soc.base.DevelopmentCards.*
import soc.base.actions.{EndTurnOutput, InitialPlacementCoreOutput, RollDiceOutput}
import soc.base.state.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.ResourceSet.*
import soc.core.Resources.*
import soc.core.Transactions.*
import soc.core.DevTransactions.*
import soc.core.state.*

class BaseGameSpec extends AnyFunSpec with Matchers:

  private val bank = InventorySet.fromMap(Map(
    WOOD -> 19, BRICK -> 19, SHEEP -> 19, WHEAT -> 19, ORE -> 19
  ))

  private val devDeck: List[DevelopmentCard] = List(
    KNIGHT, POINT, KNIGHT, POINT, POINT, KNIGHT, KNIGHT, ROAD_BUILDER,
    POINT, KNIGHT, MONOPOLY, YEAR_OF_PLENTY, YEAR_OF_PLENTY, KNIGHT, KNIGHT,
    KNIGHT, ROAD_BUILDER, MONOPOLY, KNIGHT, KNIGHT, KNIGHT, POINT, KNIGHT,
    KNIGHT, KNIGHT
  )

  private val portList: List[Port] =
    import Ports.*
    List(MISC, ORE, MISC, WHEAT, MISC, BRICK, WOOD, SHEEP, MISC)

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
    portList
  )

  private def initPerfect: PerfectInfoState = PerfectInfoState(
    robberLocation       = RobberLocation(10),
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
      board                = sampleBoard,
      edgeBuildingState    = EdgeBuildingState(Map.empty),
      moveCount            = MoveCount(0),
      setupPlacementOrder  = SetupPlacementOrder(Nil)
    )

  private def initPublic: PublicInfoState = PublicInfoState(
    robberLocation          = RobberLocation(10),
    publicInventories       = PublicInventories(Map.empty),
    publicDevCardInv        = PublicDevCardInv(Map.empty),
    developmentCardDeckSize = DevelopmentCardDeckSize(25),
    bank                    = Bank(bank),
    turn                    = Turn(0),
    playerPoints            = PlayerPoints(Map.empty),
    largestArmyPlayer       = LargestArmyPlayer(None),
    playerArmyCount         = PlayerArmyCount(Map.empty),
    vertexBuildingState     = VertexBuildingState(Map.empty),
    socRoadLengths          = SOCRoadLengths(Map.empty),
    socLongestRoadPlayer    = SOCLongestRoadPlayer(None),
      board                   = sampleBoard,
      edgeBuildingState       = EdgeBuildingState(Map.empty),
      moveCount               = MoveCount(0),
      setupPlacementOrder     = SetupPlacementOrder(Nil)
    )

  describe("PerfectInfoGame"):

    it("applies an initial placement move"):
      val move = InitialPlacementMove(Vertex(41), Edge(Vertex(40), Vertex(41)), 0)
      val (_, state) = perfectInfoGame.applyMove(move, initPerfect)
      state.playerPoints.points(0) shouldBe 1

    it("applies initial placements for 4 players"):
      val moves = List(
        InitialPlacementMove(Vertex(41), Edge(Vertex(40), Vertex(41)), 0),
        InitialPlacementMove(Vertex(34), Edge(Vertex(7), Vertex(34)), 1),
        InitialPlacementMove(Vertex(44), Edge(Vertex(44), Vertex(45)), 2),
        InitialPlacementMove(Vertex(36), Edge(Vertex(9), Vertex(36)), 3),
        InitialPlacementMove(Vertex(31), Edge(Vertex(2), Vertex(31)), 3),
        InitialPlacementMove(Vertex(47), Edge(Vertex(30), Vertex(47)), 2),
        InitialPlacementMove(Vertex(48), Edge(Vertex(48), Vertex(49)), 1),
        InitialPlacementMove(Vertex(22), Edge(Vertex(21), Vertex(22)), 0),
      )
      val state = moves.foldLeft(initPerfect) { case (s, m) =>
        perfectInfoGame.applyMove(m, s)._2
      }
      state.playerPoints.points(0) shouldBe 2
      state.playerPoints.points(3) shouldBe 2

    it("applies roll dice and end turn"):
      val (_, s1) = perfectInfoGame.applyMove(RollDiceMoveResult(0, 5), initPerfect)
      s1.turn.number shouldBe 0
      val (_, s2) = perfectInfoGame.applyMove(EndTurnMove(0), s1)
      s2.turn.number shouldBe 1

    it("applies a build settlement move"):
      val s0 = initPerfect.copy(
        privateInventories = PrivateInventories(Map(0 -> ResourceSet(wo = 2, br = 2, sh = 2, wh = 2)))
      )
      val (_, s) = perfectInfoGame.applyMove(BuildSettlementMove(0, Vertex(41)), s0)
      s.playerPoints.points(0) shouldBe 1
      s.vertexBuildingState.map should contain key Vertex(41)

    it("applies a build city move"):
      val s0 = initPerfect.copy(
        playerPoints = PlayerPoints(Map(0 -> 1)),
        privateInventories = PrivateInventories(Map(0 -> ResourceSet(or = 5, wh = 3))),
        vertexBuildingState = VertexBuildingState(Map(Vertex(41) -> PlayerBuilding(0, Settlement)))
      )
      val (_, s) = perfectInfoGame.applyMove(BuildCityMove(0, Vertex(41)), s0)
      s.vertexBuildingState.map(Vertex(41)).building shouldBe City
      s.playerPoints.points(0) shouldBe 2

    it("applies a build road move"):
      val s0 = initPerfect.copy(
        privateInventories = PrivateInventories(Map(0 -> ResourceSet(wo = 2, br = 2)))
      )
      val edge = Edge(Vertex(40), Vertex(41))
      val (_, s) = perfectInfoGame.applyMove(BuildRoadMove(0, edge), s0)
      s.edgeBuildingState.map should contain key edge

    it("applies a port trade move"):
      val s0 = initPerfect.copy(
        privateInventories = PrivateInventories(Map(0 -> ResourceSet(sh = 6)))
      )
      val (_, s) = perfectInfoGame.applyMove(
        PortTradeMove(0, ResourceSet(SHEEP, SHEEP, SHEEP, SHEEP), ResourceSet(WOOD)), s0
      )
      s.privateInventories.m(0).getAmount(WOOD) shouldBe 1
      s.bank.b.getAmount(SHEEP) shouldBe 23

    it("applies a perfect robber move with steal"):
      val s0 = initPerfect.copy(
        privateInventories = PrivateInventories(Map(0 -> ResourceSet(), 1 -> ResourceSet(wh = 3)))
      )
      val move = PerfectInfoRobberMoveResult[Resource](0, 5, Some(PlayerSteal(1, WHEAT)))
      val (_, s) = perfectInfoGame.applyMove(move, s0)
      s.robberLocation.robberHexId shouldBe 5
      s.privateInventories.m(0).getAmount(WHEAT) shouldBe 1
      s.privateInventories.m(1).getAmount(WHEAT) shouldBe 2

    it("applies a discard move"):
      val s0 = initPerfect.copy(
        privateInventories = PrivateInventories(Map(0 -> ResourceSet(wo = 3, br = 3)))
      )
      val (_, s) = perfectInfoGame.applyMove(DiscardMove(0, ResourceSet(WOOD, BRICK)), s0)
      s.privateInventories.m(0).getAmount(WOOD) shouldBe 2
      s.privateInventories.m(0).getAmount(BRICK) shouldBe 2

    it("applies a buy development card move"):
      val s0 = initPerfect.copy(
        privateInventories = PrivateInventories(Map(0 -> ResourceSet(or = 1, wh = 1, sh = 1)))
      )
      val move = PerfectInfoBuyDevelopmentCardMoveResult(0, KNIGHT)
      val (_, s) = perfectInfoGame.applyMove(move, s0)
      s.developmentCardDeck.cards.length shouldBe 24

    it("applies a play knight move"):
      val s0 = initPerfect.copy(
        privateInventories = PrivateInventories(Map(0 -> ResourceSet(), 1 -> ResourceSet(wh = 5))),
        privateDevCardInv = PrivateDevCardInv(Map(0 -> Seq((KNIGHT, 1))))
      )
      val inner = PerfectInfoRobberMoveResult(0, 7, Some(PlayerSteal(1, WHEAT)))
      val (_, s) = perfectInfoGame.applyMove(PerfectInfoPlayKnightResult(inner), s0)
      s.robberLocation.robberHexId shouldBe 7

    it("applies a play monopoly move"):
      val s0 = initPerfect.copy(
        privateInventories = PrivateInventories(Map(
          0 -> ResourceSet(), 1 -> ResourceSet(wh = 3), 2 -> ResourceSet(wh = 2)
        )),
        privateDevCardInv = PrivateDevCardInv(Map(0 -> Seq((MONOPOLY, 1))))
      )
      val (_, s) = perfectInfoGame.applyMove(
        PlayMonopolyMoveResult(0, WHEAT, Map(1 -> 3, 2 -> 2)), s0
      )
      s.privateInventories.m(0).getAmount(WHEAT) shouldBe 5

    it("applies a play road builder move"):
      val s0 = initPerfect.copy(
        privateDevCardInv = PrivateDevCardInv(Map(0 -> Seq((ROAD_BUILDER, 1))))
      )
      val edge1 = Edge(Vertex(40), Vertex(41))
      val edge2 = Edge(Vertex(17), Vertex(40))
      val (_, s) = perfectInfoGame.applyMove(
        PlayRoadBuilderMove(0, edge1, Some(edge2)), s0
      )
      s.edgeBuildingState.map should contain key edge1
      s.edgeBuildingState.map should contain key edge2

    it("applies a play year of plenty move"):
      val s0 = initPerfect.copy(
        privateDevCardInv = PrivateDevCardInv(Map(0 -> Seq((YEAR_OF_PLENTY, 1))))
      )
      val (_, s) = perfectInfoGame.applyMove(
        PlayYearOfPlentyMove(0, ORE, WHEAT), s0
      )
      s.privateInventories.m(0).getAmount(ORE) shouldBe 1
      s.privateInventories.m(0).getAmount(WHEAT) shouldBe 1

    it("replays a list of union-typed moves with applyMoveAny"):
      val moves: List[PerfectInfoMove] = List(
        InitialPlacementMove(Vertex(41), Edge(Vertex(40), Vertex(41)), 0),
        RollDiceMoveResult(0, 5),
        EndTurnMove(0)
      )
      val state = moves.foldLeft(initPerfect) { (s, m) =>
        perfectInfoGame.applyMoveAny(m, s)._2
      }
      state.playerPoints.points(0) shouldBe 1
      state.turn.number shouldBe 1

    it("applyMoveAny returns a value of the expected output union type"):
      val move: PerfectInfoMove = RollDiceMoveResult(0, 5)
      val (out, _) = perfectInfoGame.applyMoveAny(move, initPerfect)
      out match
        case _: RollDiceOutput => // expected
        case _                 => fail(s"unexpected output type: ${out.getClass.getSimpleName}")

    it("applyMoveAny returns the correct delta output for an end turn"):
      val move: PerfectInfoMove = EndTurnMove(0)
      val (out, state) = perfectInfoGame.applyMoveAny(move, initPerfect)
      out match
        case EndTurnOutput(delta) =>
          delta shouldBe Turn.Delta(1)
          state.turn.number shouldBe 1
        case _ => fail("expected EndTurnOutput")

    it("applyMoveAny returns the correct delta output for an initial placement"):
      val move: PerfectInfoMove = InitialPlacementMove(Vertex(41), Edge(Vertex(40), Vertex(41)), 0)
      val (out, state) = perfectInfoGame.applyMoveAny(move, initPerfect)
      out match
        case InitialPlacementCoreOutput(settlement, road, point, gains, bankLost, placement) =>
          settlement shouldBe BoardBuildingState.AddBuilding(Vertex(41), 0, Settlement)
          road shouldBe BoardBuildingState.AddBuilding(Edge(Vertex(40), Vertex(41)), 0, Road)
          point shouldBe PlayerPoints.Increment(0)
          gains shouldBe Nil
          bankLost shouldBe Nil
          placement shouldBe SetupPlacementOrder.Placement(0, Vertex(41))
          state.playerPoints.points(0) shouldBe 1
        case _ => fail("expected InitialPlacementCoreOutput")

  describe("PublicInfoGame"):

    it("applies an initial placement move"):
      val move = InitialPlacementMove(Vertex(41), Edge(Vertex(40), Vertex(41)), 0)
      val (_, s) = publicInfoGame.applyMove(move, initPublic)
      s.playerPoints.points(0) shouldBe 1

    it("applies roll dice and end turn"):
      val (_, s1) = publicInfoGame.applyMove(RollDiceMoveResult(0, 5), initPublic)
      val (_, s2) = publicInfoGame.applyMove(EndTurnMove(0), s1)
      s2.turn.number shouldBe 1

    it("applies a public robber move with steal"):
      val s0 = initPublic.copy(
        publicInventories = PublicInventories(Map(0 -> 0, 1 -> 5))
      )
      val move = RobberMoveResult[Resource](0, 5, Some(PlayerSteal(1, Some(WHEAT))))
      val (_, s) = publicInfoGame.applyMove(move, s0)
      s.robberLocation.robberHexId shouldBe 5

    it("applies a trade move"):
      val s0 = initPublic.copy(
        publicInventories = PublicInventories(Map(0 -> 3, 1 -> 3))
      )
      val (_, s) = publicInfoGame.applyMove(
        TradeMove(0, 1, ResourceSet(WOOD), ResourceSet(BRICK)), s0
      )
      s.publicInventories.m(0) shouldBe 3

    it("applies a build settlement move"):
      val s0 = initPublic.copy(
        publicInventories = PublicInventories(Map(0 -> 4))
      )
      val (_, s) = publicInfoGame.applyMove(BuildSettlementMove(0, Vertex(41)), s0)
      s.vertexBuildingState.map should contain key Vertex(41)
      s.playerPoints.points(0) shouldBe 1

    it("applies a buy dev card move with unknown card"):
      val s0 = initPublic.copy(
        publicInventories = PublicInventories(Map(0 -> 3))
      )
      val (_, s) = publicInfoGame.applyMove(
        BuyDevelopmentCardMoveResult[DevelopmentCard](0, None), s0
      )
      s.developmentCardDeckSize.size shouldBe 24

    it("applies a play knight move (public)"):
      val s0 = initPublic.copy(
        publicInventories = PublicInventories(Map(0 -> 0, 1 -> 5)),
        publicDevCardInv = PublicDevCardInv(Map(0 -> 1))
      )
      val inner = RobberMoveResult(0, 7, Some(PlayerSteal(1, Some(WHEAT))))
      val (_, s) = publicInfoGame.applyMove(PlayKnightResult(inner), s0)
      s.robberLocation.robberHexId shouldBe 7

    it("applies a play point move"):
      val s0 = initPublic.copy(
        publicDevCardInv = PublicDevCardInv(Map(0 -> 1))
      )
      val (_, s) = publicInfoGame.applyMove(PlayPointMove(0), s0)
      s.playerPoints.points(0) shouldBe 1

    it("applies a play monopoly move (public)"):
      val s0 = initPublic.copy(
        publicInventories = PublicInventories(Map(0 -> 2, 1 -> 5, 2 -> 3)),
        publicDevCardInv = PublicDevCardInv(Map(0 -> 1))
      )
      val (_, s) = publicInfoGame.applyMove(
        PlayMonopolyMoveResult(0, WHEAT, Map(1 -> 5, 2 -> 3)), s0
      )
      s.publicInventories.m(0) shouldBe 10
      s.publicInventories.m(1) shouldBe 0

    it("applies a discard move"):
      val s0 = initPublic.copy(
        publicInventories = PublicInventories(Map(0 -> 8))
      )
      val (_, s) = publicInfoGame.applyMove(
        DiscardMove(0, ResourceSet(WOOD, BRICK)), s0
      )
      s.publicInventories.m(0) shouldBe 6

    it("replays a list of union-typed moves with applyMoveAny"):
      val moves: List[PublicInfoMove] = List(
        InitialPlacementMove(Vertex(41), Edge(Vertex(40), Vertex(41)), 0),
        RollDiceMoveResult(0, 5),
        EndTurnMove(0)
      )
      val state = moves.foldLeft(initPublic) { (s, m) =>
        publicInfoGame.applyMoveAny(m, s)._2
      }
      state.playerPoints.points(0) shouldBe 1
      state.turn.number shouldBe 1

    it("applyMoveAny returns a value of the expected output union type"):
      val move: PublicInfoMove = RollDiceMoveResult(0, 5)
      val (out, _) = publicInfoGame.applyMoveAny(move, initPublic)
      out match
        case _: RollDiceOutput => // expected
        case _                 => fail(s"unexpected output type: ${out.getClass.getSimpleName}")

    it("applyMoveAny returns the correct delta output for an end turn"):
      val move: PublicInfoMove = EndTurnMove(0)
      val (out, state) = publicInfoGame.applyMoveAny(move, initPublic)
      out match
        case EndTurnOutput(delta) =>
          delta shouldBe Turn.Delta(1)
          state.turn.number shouldBe 1
        case _ => fail("expected EndTurnOutput")

    it("applyMoveAny returns the correct delta output for an initial placement"):
      val move: PublicInfoMove = InitialPlacementMove(Vertex(41), Edge(Vertex(40), Vertex(41)), 0)
      val (out, state) = publicInfoGame.applyMoveAny(move, initPublic)
      out match
        case InitialPlacementCoreOutput(settlement, road, point, gains, bankLost, placement) =>
          settlement shouldBe BoardBuildingState.AddBuilding(Vertex(41), 0, Settlement)
          road shouldBe BoardBuildingState.AddBuilding(Edge(Vertex(40), Vertex(41)), 0, Road)
          point shouldBe PlayerPoints.Increment(0)
          gains shouldBe Nil
          bankLost shouldBe Nil
          placement shouldBe SetupPlacementOrder.Placement(0, Vertex(41))
          state.playerPoints.points(0) shouldBe 1
        case _ => fail("expected InitialPlacementCoreOutput")
