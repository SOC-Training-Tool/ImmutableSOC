package soc.base.actions

import game.InventorySet
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import soc.base.*
import soc.base.BaseGame.*
import soc.base.DevelopmentCards.*
import soc.base.state.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.ResourceSet.*
import soc.core.Resources.*
import soc.core.state.*

class LongestRoadIntegrationSpec extends AnyFunSpec with Matchers:

  private val board: BaseBoard[Resource] = BaseBoard(List(Desert), Nil)
  private val chain = List(Edge(0, 1), Edge(1, 2), Edge(2, 31), Edge(31, 30), Edge(30, 29))

  private val initialState = PerfectInfoState(
    robberLocation       = RobberLocation(0),
    privateInventories   = PrivateInventories(Map.empty),
    privateDevCardInv    = PrivateDevCardInv(Map.empty),
    developmentCardDeck  = DevelopmentCardDeck(Nil),
    bank                 = Bank(InventorySet.fromMap(Map(WOOD -> 19, BRICK -> 19, SHEEP -> 19, WHEAT -> 19, ORE -> 19))),
    turn                 = Turn(0),
    playerPoints         = PlayerPoints(Map.empty),
    largestArmyPlayer    = LargestArmyPlayer(None),
    playerArmyCount      = PlayerArmyCount(Map.empty),
    vertexBuildingState  = VertexBuildingState(Map.empty),
    socRoadLengths       = SOCRoadLengths(Map.empty),
    socLongestRoadPlayer = SOCLongestRoadPlayer(None),
    board                = board,
    edgeBuildingState    = EdgeBuildingState(Map.empty),
    moveCount            = MoveCount(0),
    setupPlacementOrder  = SetupPlacementOrder(Nil)
  )

  private def roads(edges: List[Edge]): EdgeBuildingState[BaseEdgeBuilding] =
    EdgeBuildingState(edges.map(_ -> PlayerBuilding(0, Road)).toMap)

  describe("longest-road replay integration") {
    it("awards two points when a fifth road is built") {
      val givenState = initialState.copy(
        privateInventories = PrivateInventories(Map(0 -> ResourceSet(wo = 1, br = 1))),
        edgeBuildingState = roads(chain.take(4)),
        socRoadLengths = SOCRoadLengths(Map(0 -> 4))
      )

      val move = BuildRoadMove(0, chain.last)
      val output = BuildRoadCoreAction()(move, BuildRoadInput(
        givenState.board,
        givenState.socRoadLengths,
        givenState.socLongestRoadPlayer,
        givenState.vertexBuildingState,
        givenState.edgeBuildingState
      ))
      val (_, afterBuild) = perfectInfoGame.applyMove(move, givenState)

      output.longestRoadPlayerChanges shouldBe List(SOCLongestRoadPlayer.Delta(SpecialPlayer.Set(0)))
      output.longestRoadPointChanges shouldBe List(PlayerPoints.Increment(0), PlayerPoints.Increment(0))
      afterBuild.socRoadLengths shouldBe SOCRoadLengths(Map(0 -> 5))
      afterBuild.socLongestRoadPlayer shouldBe SOCLongestRoadPlayer(Some(0))
      afterBuild.playerPoints shouldBe PlayerPoints(Map(0 -> 2))
    }

    it("removes the award when an opponent settlement splits the holder's road") {
      val givenState = initialState.copy(
        privateInventories = PrivateInventories(Map(1 -> ResourceSet(wo = 1, br = 1, sh = 1, wh = 1))),
        playerPoints = PlayerPoints(Map(0 -> 2)),
        edgeBuildingState = roads(chain),
        socRoadLengths = SOCRoadLengths(Map(0 -> 5)),
        socLongestRoadPlayer = SOCLongestRoadPlayer(Some(0))
      )

      val move = BuildSettlementMove(1, Vertex(2))
      val output = BuildSettlementCoreAction()(move, BuildSettlementInput(
        givenState.board,
        givenState.socRoadLengths,
        givenState.socLongestRoadPlayer,
        givenState.vertexBuildingState,
        givenState.edgeBuildingState
      ))
      val (_, afterBuild) = perfectInfoGame.applyMove(move, givenState)

      output.longestRoadPlayerChanges shouldBe List(SOCLongestRoadPlayer.Delta(SpecialPlayer.Remove))
      output.longestRoadPointChanges shouldBe List(PlayerPoints.Decrement(0), PlayerPoints.Decrement(0))
      afterBuild.socRoadLengths shouldBe SOCRoadLengths(Map(0 -> 3))
      afterBuild.socLongestRoadPlayer shouldBe SOCLongestRoadPlayer(None)
      afterBuild.playerPoints shouldBe PlayerPoints(Map(0 -> 0, 1 -> 1))
    }

    it("recalculates and awards longest road after road builder places two roads") {
      val givenState = initialState.copy(
        privateDevCardInv = PrivateDevCardInv(Map(0 -> Seq((ROAD_BUILDER, 0)))),
        turn = Turn(1),
        edgeBuildingState = roads(chain.take(3)),
        socRoadLengths = SOCRoadLengths(Map(0 -> 3))
      )

      val move = PlayRoadBuilderMove(0, chain(3), Some(chain(4)))
      val output = PlayRoadBuilderCoreAction()(move, PlayRoadBuilderInput(
        givenState.turn,
        givenState.board,
        givenState.socRoadLengths,
        givenState.socLongestRoadPlayer,
        givenState.vertexBuildingState,
        givenState.edgeBuildingState
      ))
      val (_, afterPlay) = perfectInfoGame.applyMove(move, givenState)

      output.longestRoadPlayerChanges shouldBe List(SOCLongestRoadPlayer.Delta(SpecialPlayer.Set(0)))
      output.longestRoadPointChanges shouldBe List(PlayerPoints.Increment(0), PlayerPoints.Increment(0))
      afterPlay.socRoadLengths shouldBe SOCRoadLengths(Map(0 -> 5))
      afterPlay.socLongestRoadPlayer shouldBe SOCLongestRoadPlayer(Some(0))
      afterPlay.playerPoints shouldBe PlayerPoints(Map(0 -> 2))
    }
  }
