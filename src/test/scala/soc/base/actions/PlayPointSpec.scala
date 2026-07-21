package soc.base.actions

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import game.InventorySet
import soc.base.BaseBoard
import soc.base.BaseGame.*
import soc.base.PlayPointMove
import soc.base.DevelopmentCards.POINT
import soc.base.state.*
import soc.core.*
import soc.core.DevTransactions.*
import soc.core.ResourceInventories.*
import soc.core.ResourceSet.*
import soc.core.Resources.*
import soc.core.Transactions.*
import soc.core.state.*

class PlayPointSpec extends AnyFunSpec with Matchers:

  private val bank = Bank(InventorySet.fromMap(Map(
    WOOD -> 19, BRICK -> 19, SHEEP -> 19, WHEAT -> 19, ORE -> 19
  )))

  private def initPerfect: PerfectInfoState = PerfectInfoState(
    robberLocation       = RobberLocation(10),
    privateInventories   = PrivateInventories(Map.empty),
    privateDevCardInv    = PrivateDevCardInv(Map.empty),
    developmentCardDeck  = DevelopmentCardDeck(List(POINT)),
    bank                 = bank,
    turn                 = Turn(1),
    playerPoints         = PlayerPoints(Map.empty),
    largestArmyPlayer    = LargestArmyPlayer(None),
    playerArmyCount      = PlayerArmyCount(Map.empty),
    vertexBuildingState  = VertexBuildingState(Map.empty),
    socRoadLengths       = SOCRoadLengths(Map.empty),
    socLongestRoadPlayer = SOCLongestRoadPlayer(None),
    board                = BaseBoard(List.empty, List.empty),
    edgeBuildingState    = EdgeBuildingState(Map.empty),
    moveCount            = MoveCount(0),
    setupPlacementOrder  = SetupPlacementOrder(Nil)
  )

  private def initPublic: PublicInfoState = PublicInfoState(
    robberLocation          = RobberLocation(10),
    publicInventories       = PublicInventories(Map.empty),
    publicDevCardInv        = PublicDevCardInv(Map.empty),
    developmentCardDeckSize = DevelopmentCardDeckSize(1),
    bank                    = bank,
    turn                    = Turn(1),
    playerPoints            = PlayerPoints(Map.empty),
    largestArmyPlayer       = LargestArmyPlayer(None),
    playerArmyCount         = PlayerArmyCount(Map.empty),
    vertexBuildingState     = VertexBuildingState(Map.empty),
    socRoadLengths          = SOCRoadLengths(Map.empty),
    socLongestRoadPlayer    = SOCLongestRoadPlayer(None),
    board                   = BaseBoard(List.empty, List.empty),
    edgeBuildingState       = EdgeBuildingState(Map.empty),
    moveCount               = MoveCount(0),
    setupPlacementOrder     = SetupPlacementOrder(Nil)
  )

  describe("PlayPointAction"):

    it("perfect info records the card played without adding a second point"):
      val s0 = initPerfect.copy(
        privateDevCardInv = PrivateDevCardInv(Map(0 -> Seq((POINT, 1)))),
        playerPoints = PlayerPoints(Map(0 -> 1))
      )
      val (_, s) = perfectInfoGame.applyMove(PlayPointMove(0), s0)
      s.playerPoints.points(0) shouldBe 1
      s.privateDevCardInv.m(0).map(_._1) should not contain POINT

    it("public info awards the point when the card is revealed"):
      val s0 = initPublic.copy(
        publicDevCardInv = PublicDevCardInv(Map(0 -> 1))
      )
      val (_, s) = publicInfoGame.applyMove(PlayPointMove(0), s0)
      s.playerPoints.points(0) shouldBe 1
      s.publicDevCardInv.m(0) shouldBe 0

    it("records the card as played in both modes"):
      val perfectState = initPerfect.copy(
        privateDevCardInv = PrivateDevCardInv(Map(0 -> Seq((POINT, 1))))
      )
      val publicState = initPublic.copy(
        publicDevCardInv = PublicDevCardInv(Map(0 -> 1))
      )

      val (_, perfectAfter) = perfectInfoGame.applyMove(PlayPointMove(0), perfectState)
      val (_, publicAfter) = publicInfoGame.applyMove(PlayPointMove(0), publicState)

      perfectAfter.privateDevCardInv.m(0).map(_._1) should not contain POINT
      publicAfter.publicDevCardInv.m(0) shouldBe 0
