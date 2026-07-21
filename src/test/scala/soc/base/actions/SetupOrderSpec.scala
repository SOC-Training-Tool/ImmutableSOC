package soc.base.actions

import game.InventorySet
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import soc.base.BaseBoard
import soc.base.BaseGame.*
import soc.base.DevelopmentCards.*
import soc.base.state.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.ResourceSet.*
import soc.core.Resources.*
import soc.core.Transactions.*
import soc.core.state.*

class SetupOrderSpec extends AnyFunSpec with Matchers:

  private val bank = InventorySet.fromMap(Map(
    WOOD -> 19, BRICK -> 19, SHEEP -> 19, WHEAT -> 19, ORE -> 19
  ))

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

  private def initPerfect(players: Set[Int]): PerfectInfoState = PerfectInfoState(
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
    board                = sampleBoard,
    edgeBuildingState    = EdgeBuildingState(Map.empty),
    moveCount            = MoveCount(0),
    setupPlacementOrder  = SetupPlacementOrder(Nil)
  )

  private def initPublic(players: Set[Int]): PublicInfoState = PublicInfoState(
    robberLocation          = RobberLocation(10),
    publicInventories       = PublicInventories(Map.empty),
    publicDevCardInv        = PublicDevCardInv(Map.empty),
    developmentCardDeckSize = DevelopmentCardDeckSize(0),
    bank                    = Bank(bank),
    turn                    = Turn(0),
    playerPoints            = PlayerPoints(players.map(_ -> 0).toMap),
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

  describe("SetupPlacementOrder"):

    it("4-player perfect-info setup: first 4 placements grant no resources; next 4 grant resources"):
      val players = Set(0, 1, 2, 3)
      val state0  = initPerfect(players)
      val round1Moves = List(
        InitialPlacementMove(Vertex(41), Edge(Vertex(40), Vertex(41)), 0),
        InitialPlacementMove(Vertex(34), Edge(Vertex(7), Vertex(34)), 1),
        InitialPlacementMove(Vertex(44), Edge(Vertex(44), Vertex(45)), 2),
        InitialPlacementMove(Vertex(36), Edge(Vertex(9), Vertex(36)), 3)
      )
      val round2Moves = List(
        InitialPlacementMove(Vertex(31), Edge(Vertex(2), Vertex(31)), 3),
        InitialPlacementMove(Vertex(47), Edge(Vertex(30), Vertex(47)), 2),
        InitialPlacementMove(Vertex(48), Edge(Vertex(48), Vertex(49)), 1),
        InitialPlacementMove(Vertex(22), Edge(Vertex(21), Vertex(22)), 0)
      )

      val (afterRound1, round1Outputs) = round1Moves.foldLeft((state0, List.empty[InitialPlacementCoreOutput])) {
        case ((s, outs), m) =>
          val (out, next) = perfectInfoGame.applyMove(m, s)
          val placementOut = out match
            case o: InitialPlacementCoreOutput => o
            case _                             => fail("expected InitialPlacementCoreOutput")
          (next, outs :+ placementOut)
      }
      round1Outputs.forall(_.resourceGains.isEmpty) shouldBe true
      round1Outputs.forall(_.bankLost.isEmpty) shouldBe true
      afterRound1.privateInventories.m.values.forall(_.getTotal == 0) shouldBe true
      afterRound1.bank.b.getTotal shouldBe bank.getTotal

      val (afterRound2, round2Outputs) = round2Moves.foldLeft((afterRound1, List.empty[InitialPlacementCoreOutput])) {
        case ((s, outs), m) =>
          val (out, next) = perfectInfoGame.applyMove(m, s)
          val placementOut = out match
            case o: InitialPlacementCoreOutput => o
            case _                             => fail("expected InitialPlacementCoreOutput")
          (next, outs :+ placementOut)
      }
      round2Outputs.exists(_.resourceGains.nonEmpty) shouldBe true
      round2Outputs.exists(_.bankLost.nonEmpty) shouldBe true
      afterRound2.bank.b.getTotal shouldBe <(bank.getTotal)

    it("4-player public-info setup: first 4 placements grant no resources; next 4 grant resources"):
      val players = Set(0, 1, 2, 3)
      val state0  = initPublic(players)
      val round1Moves = List(
        InitialPlacementMove(Vertex(41), Edge(Vertex(40), Vertex(41)), 0),
        InitialPlacementMove(Vertex(34), Edge(Vertex(7), Vertex(34)), 1),
        InitialPlacementMove(Vertex(44), Edge(Vertex(44), Vertex(45)), 2),
        InitialPlacementMove(Vertex(36), Edge(Vertex(9), Vertex(36)), 3)
      )
      val round2Moves = List(
        InitialPlacementMove(Vertex(31), Edge(Vertex(2), Vertex(31)), 3),
        InitialPlacementMove(Vertex(47), Edge(Vertex(30), Vertex(47)), 2),
        InitialPlacementMove(Vertex(48), Edge(Vertex(48), Vertex(49)), 1),
        InitialPlacementMove(Vertex(22), Edge(Vertex(21), Vertex(22)), 0)
      )

      val afterRound1 = round1Moves.foldLeft(state0) { (s, m) =>
        publicInfoGame.applyMove(m, s)._2
      }
      afterRound1.publicInventories.m.values.forall(_ == 0) shouldBe true
      afterRound1.bank.b.getTotal shouldBe bank.getTotal

      val afterRound2 = round2Moves.foldLeft(afterRound1) { (s, m) =>
        publicInfoGame.applyMove(m, s)._2
      }
      afterRound2.publicInventories.m.values.exists(_ > 0) shouldBe true
      afterRound2.bank.b.getTotal shouldBe <(bank.getTotal)

    it("interleaving a RollDiceMove between setup rounds does not break round-2 resource grants"):
      val players = Set(0, 1, 2, 3)
      val state0  = initPerfect(players)
      val round1Moves = List(
        InitialPlacementMove(Vertex(41), Edge(Vertex(40), Vertex(41)), 0),
        InitialPlacementMove(Vertex(34), Edge(Vertex(7), Vertex(34)), 1),
        InitialPlacementMove(Vertex(44), Edge(Vertex(44), Vertex(45)), 2),
        InitialPlacementMove(Vertex(36), Edge(Vertex(9), Vertex(36)), 3)
      )
      val round2FirstMove = InitialPlacementMove(Vertex(31), Edge(Vertex(2), Vertex(31)), 3)

      val afterRound1 = round1Moves.foldLeft(state0) { (s, m) =>
        perfectInfoGame.applyMove(m, s)._2
      }
      val afterRollDice = perfectInfoGame.applyMove(RollDiceMoveResult(0, 5), afterRound1)._2

      val (out, afterFifth) = perfectInfoGame.applyMove(round2FirstMove, afterRollDice)
      val placementOut = out match
        case o: InitialPlacementCoreOutput => o
        case _                             => fail("expected InitialPlacementCoreOutput")
      placementOut.resourceGains should not be empty
      placementOut.bankLost should not be empty
      afterFifth.privateInventories.m.get(3).exists(_.getTotal > 0) shouldBe true

    it("mirrors BaseGameSpec initial placement point tracking for 4 players"):
      val moves = List(
        InitialPlacementMove(Vertex(41), Edge(Vertex(40), Vertex(41)), 0),
        InitialPlacementMove(Vertex(34), Edge(Vertex(7), Vertex(34)), 1),
        InitialPlacementMove(Vertex(44), Edge(Vertex(44), Vertex(45)), 2),
        InitialPlacementMove(Vertex(36), Edge(Vertex(9), Vertex(36)), 3),
        InitialPlacementMove(Vertex(31), Edge(Vertex(2), Vertex(31)), 3),
        InitialPlacementMove(Vertex(47), Edge(Vertex(30), Vertex(47)), 2),
        InitialPlacementMove(Vertex(48), Edge(Vertex(48), Vertex(49)), 1),
        InitialPlacementMove(Vertex(22), Edge(Vertex(21), Vertex(22)), 0)
      )
      val state = moves.foldLeft(initPerfect(Set(0, 1, 2, 3))) { (s, m) =>
        perfectInfoGame.applyMove(m, s)._2
      }
      state.playerPoints.points(0) shouldBe 2
      state.playerPoints.points(3) shouldBe 2
      state.setupPlacementOrder.placements.length shouldBe 8
      state.setupPlacementOrder.placements.last shouldBe (0, Vertex(22))
