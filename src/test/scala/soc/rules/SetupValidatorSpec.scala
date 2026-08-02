package soc.rules

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import soc.base.BaseGame.*
import soc.base.BaseGameFixtures
import soc.base.state.*
import soc.core.*
import soc.core.state.*
import soc.rules.RulesFixtures.*
import soc.rules.validators.SetupValidator

class SetupValidatorSpec extends AnyFunSpec with Matchers:

  private val board = BaseGameFixtures.perfectInfoFixture.board

  describe("slot order"):

    it("only the first player has moves on an empty board"):
      val state = initPerfect
      val p0 = SetupValidator.legalMoves(0, 4, state.setupPlacementOrder, state.vertexBuildingState, state.edgeBuildingState, board)
      val p1 = SetupValidator.legalMoves(1, 4, state.setupPlacementOrder, state.vertexBuildingState, state.edgeBuildingState, board)
      p0 should not be empty
      p1 shouldBe empty

    it("advances to the next player after a placement"):
      val state = initPerfect.copy(
        vertexBuildingState = VertexBuildingState(Map(Vertex(41) -> PlayerBuilding(0, Settlement))),
        edgeBuildingState = EdgeBuildingState(Map(Edge(Vertex(40), Vertex(41)) -> PlayerBuilding(0, Road))),
        setupPlacementOrder = SetupPlacementOrder(List((0, Vertex(41))))
      )
      val p0 = SetupValidator.legalMoves(0, 4, state.setupPlacementOrder, state.vertexBuildingState, state.edgeBuildingState, board)
      val p1 = SetupValidator.legalMoves(1, 4, state.setupPlacementOrder, state.vertexBuildingState, state.edgeBuildingState, board)
      p0 shouldBe empty
      p1 should not be empty

    it("reverses order for the second round"):
      val state = afterSetupPerfect
      val round2p3 = SetupValidator.legalMoves(3, 4, state.setupPlacementOrder, state.vertexBuildingState, state.edgeBuildingState, board)
      round2p3 shouldBe empty
      val placements = List((0, Vertex(41)), (1, Vertex(34)), (2, Vertex(44)), (3, Vertex(36)))
      PhaseMachine.setupActivePlayer(placements.map(_._1), 4) shouldBe 3

  describe("legality checks"):

    it("requires the road edge to be incident to the settlement vertex"):
      val moves = SetupValidator.legalMoves(0, 4, initPerfect.setupPlacementOrder, initPerfect.vertexBuildingState, initPerfect.edgeBuildingState, board)
      moves.forall(m => board.edgesFromVertex(m.vertex).contains(m.edge)) shouldBe true

    it("requires the road edge to be empty"):
      val state = initPerfect.copy(
        edgeBuildingState = EdgeBuildingState(Map(Edge(Vertex(40), Vertex(41)) -> PlayerBuilding(0, Road)))
      )
      val moves = SetupValidator.legalMoves(0, 4, state.setupPlacementOrder, state.vertexBuildingState, state.edgeBuildingState, board)
      moves.exists(m => m.edge == Edge(Vertex(40), Vertex(41))) shouldBe false

    it("enforces the distance rule against adjacent settlements"):
      val state = initPerfect.copy(
        vertexBuildingState = VertexBuildingState(Map(Vertex(41) -> PlayerBuilding(0, Settlement))),
        edgeBuildingState = EdgeBuildingState(Map(Edge(Vertex(40), Vertex(41)) -> PlayerBuilding(0, Road))),
        setupPlacementOrder = SetupPlacementOrder(List((0, Vertex(41))))
      )
      val moves = SetupValidator.legalMoves(1, 4, state.setupPlacementOrder, state.vertexBuildingState, state.edgeBuildingState, board)
      val forbidden = Set(Vertex(41), Vertex(40), Vertex(42), Vertex(51))
      moves.map(_.vertex) should not contain (Vertex(41))
      moves.map(_.vertex).toSet.intersect(forbidden) shouldBe empty

    it("allows a vertex two edges away from the first settlement"):
      val state = initPerfect.copy(
        vertexBuildingState = VertexBuildingState(Map(Vertex(41) -> PlayerBuilding(0, Settlement))),
        edgeBuildingState = EdgeBuildingState(Map(Edge(Vertex(40), Vertex(41)) -> PlayerBuilding(0, Road))),
        setupPlacementOrder = SetupPlacementOrder(List((0, Vertex(41))))
      )
      val moves = SetupValidator.legalMoves(1, 4, state.setupPlacementOrder, state.vertexBuildingState, state.edgeBuildingState, board)
      moves.map(_.vertex).toSet should contain (Vertex(17))
