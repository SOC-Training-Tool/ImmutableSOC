package soc.rules

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import soc.base.BaseGame.*
import soc.base.BaseGameFixtures
import soc.base.state.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.ResourceSet.*
import soc.core.Resources.*
import soc.core.state.*
import soc.rules.RulesFixtures.*
import soc.rules.validators.BuildingValidator

class BuildingValidatorSpec extends AnyFunSpec with Matchers:

  private val cached = new CachedBoard[Resource](BaseGameFixtures.perfectInfoFixture.board)

  private def stateWith(vertex: Map[Vertex, PlayerBuilding[BaseVertexBuilding]] = Map.empty,
                        edge: Map[Edge, PlayerBuilding[BaseEdgeBuilding]] = Map.empty,
                        inventory: Map[Int, Resources] = Map.empty): PerfectInfoState =
    afterSetupPerfect.copy(
      vertexBuildingState = VertexBuildingState(vertex),
      edgeBuildingState = EdgeBuildingState(edge),
      privateInventories = PrivateInventories(inventory)
    )

  describe("roadMoves"):

    it("requires wood and brick"):
      val state = stateWith(
        vertex = Map(Vertex(41) -> PlayerBuilding(0, Settlement)),
        edge = Map(Edge(Vertex(40), Vertex(41)) -> PlayerBuilding(0, Road)),
        inventory = Map(0 -> ResourceSet(wo = 1))
      )
      val inv = new PerfectInfoResourceView(state)
      BuildingValidator.roadMoves(0, inv, state.edgeBuildingState, state.vertexBuildingState, cached) shouldBe empty

    it("returns edges connected to the player's network"):
      val state = stateWith(
        vertex = Map(Vertex(41) -> PlayerBuilding(0, Settlement)),
        edge = Map(Edge(Vertex(40), Vertex(41)) -> PlayerBuilding(0, Road)),
        inventory = Map(0 -> ResourceSet(wo = 1, br = 1))
      )
      val inv = new PerfectInfoResourceView(state)
      val moves = BuildingValidator.roadMoves(0, inv, state.edgeBuildingState, state.vertexBuildingState, cached)
      moves.map(_.edge).toSet shouldBe Set(
        Edge(Vertex(40), Vertex(39)),
        Edge(Vertex(17), Vertex(40)),
        Edge(Vertex(41), Vertex(42)),
        Edge(Vertex(41), Vertex(51))
      )

    it("rejects edges not connected to the player's network"):
      val state = stateWith(
        vertex = Map(Vertex(41) -> PlayerBuilding(0, Settlement)),
        inventory = Map(0 -> ResourceSet(wo = 1, br = 1))
      )
      val inv = new PerfectInfoResourceView(state)
      val moves = BuildingValidator.roadMoves(0, inv, state.edgeBuildingState, state.vertexBuildingState, cached)
      moves.map(_.edge).toSet should not contain (Edge(Vertex(0), Vertex(1)))
      moves.map(_.edge).toSet should not contain (Edge(Vertex(30), Vertex(47)))

    it("rejects occupied edges"):
      val state = stateWith(
        vertex = Map(Vertex(41) -> PlayerBuilding(0, Settlement)),
        edge = Map(
          Edge(Vertex(40), Vertex(41)) -> PlayerBuilding(0, Road),
          Edge(Vertex(41), Vertex(42)) -> PlayerBuilding(1, Road)
        ),
        inventory = Map(0 -> ResourceSet(wo = 1, br = 1))
      )
      val inv = new PerfectInfoResourceView(state)
      val moves = BuildingValidator.roadMoves(0, inv, state.edgeBuildingState, state.vertexBuildingState, cached)
      moves.map(_.edge) should not contain (Edge(Vertex(41), Vertex(42)))

    it("respects the 15-road piece limit"):
      val manyRoads = (0 until 15).map(i => Edge(Vertex(i * 2), Vertex(i * 2 + 1)) -> PlayerBuilding(0, Road)).toMap
      val state = stateWith(
        vertex = Map(Vertex(41) -> PlayerBuilding(0, Settlement)),
        edge = manyRoads ++ Map(Edge(Vertex(40), Vertex(41)) -> PlayerBuilding(0, Road)),
        inventory = Map(0 -> ResourceSet(wo = 1, br = 1))
      )
      val inv = new PerfectInfoResourceView(state)
      BuildingValidator.roadMoves(0, inv, state.edgeBuildingState, state.vertexBuildingState, cached) shouldBe empty

  describe("settlementMoves"):

    it("requires a road touching the vertex"):
      val state = stateWith(
        vertex = Map(Vertex(41) -> PlayerBuilding(0, Settlement)),
        inventory = Map(0 -> ResourceSet(wo = 1, br = 1, wh = 1, sh = 1))
      )
      val inv = new PerfectInfoResourceView(state)
      val moves = BuildingValidator.settlementMoves(0, inv, state.vertexBuildingState, state.edgeBuildingState, cached)
      moves.map(_.vertex) should not contain (Vertex(40))

    it("enforces the distance rule and the 5-piece settlement limit"):
      val manySettlements = (0 until 5).map(i => Vertex(40 + i) -> PlayerBuilding(0, Settlement)).toMap
      val state = stateWith(
        vertex = manySettlements,
        inventory = Map(0 -> ResourceSet(wo = 1, br = 1, wh = 1, sh = 1))
      )
      val inv = new PerfectInfoResourceView(state)
      val moves = BuildingValidator.settlementMoves(0, inv, state.vertexBuildingState, state.edgeBuildingState, cached)
      moves shouldBe empty

    it("returns legal settlement vertices when affordable and connected"):
      val state = stateWith(
        vertex = Map(Vertex(41) -> PlayerBuilding(0, Settlement)),
        edge = Map(
          Edge(Vertex(40), Vertex(41)) -> PlayerBuilding(0, Road),
          Edge(Vertex(41), Vertex(42)) -> PlayerBuilding(0, Road),
          Edge(Vertex(42), Vertex(43)) -> PlayerBuilding(0, Road)
        ),
        inventory = Map(0 -> ResourceSet(wo = 1, br = 1, wh = 1, sh = 1))
      )
      val inv = new PerfectInfoResourceView(state)
      val moves = BuildingValidator.settlementMoves(0, inv, state.vertexBuildingState, state.edgeBuildingState, cached)
      moves.map(_.vertex).toSet should contain (Vertex(43))
      moves.map(_.vertex) should not contain (Vertex(42))

  describe("cityMoves"):

    it("requires owning a settlement at the vertex"):
      val state = stateWith(
        vertex = Map(Vertex(41) -> PlayerBuilding(1, Settlement)),
        inventory = Map(0 -> ResourceSet(or = 3, wh = 2))
      )
      val inv = new PerfectInfoResourceView(state)
      BuildingValidator.cityMoves(0, inv, state.vertexBuildingState, cached) shouldBe empty

    it("returns city upgrades for owned settlements when affordable"):
      val state = stateWith(
        vertex = Map(Vertex(41) -> PlayerBuilding(0, Settlement)),
        inventory = Map(0 -> ResourceSet(or = 3, wh = 2))
      )
      val inv = new PerfectInfoResourceView(state)
      val moves = BuildingValidator.cityMoves(0, inv, state.vertexBuildingState, cached)
      moves.map(_.vertex) shouldBe Seq(Vertex(41))

    it("respects the 4-city piece limit"):
      val fourCities = (0 until 4).map(i => Vertex(40 + i) -> PlayerBuilding(0, City)).toMap
      val state = stateWith(
        vertex = fourCities + (Vertex(44) -> PlayerBuilding(0, Settlement)),
        inventory = Map(0 -> ResourceSet(or = 3, wh = 2))
      )
      val inv = new PerfectInfoResourceView(state)
      BuildingValidator.cityMoves(0, inv, state.vertexBuildingState, cached) shouldBe empty

    it("requires ore and wheat"):
      val state = stateWith(
        vertex = Map(Vertex(41) -> PlayerBuilding(0, Settlement)),
        inventory = Map(0 -> ResourceSet(or = 3))
      )
      val inv = new PerfectInfoResourceView(state)
      BuildingValidator.cityMoves(0, inv, state.vertexBuildingState, cached) shouldBe empty
