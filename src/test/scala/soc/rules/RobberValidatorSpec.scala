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
import soc.rules.validators.RobberValidator

class RobberValidatorSpec extends AnyFunSpec with Matchers:

  private val board = BaseGameFixtures.perfectInfoFixture.board

  private def stateWith(vertex: Map[Vertex, PlayerBuilding[BaseVertexBuilding]],
                        inventory: Map[Int, Resources]): PerfectInfoState =
    afterSetupPerfect.copy(
      vertexBuildingState = VertexBuildingState(vertex),
      privateInventories = PrivateInventories(inventory)
    )

  describe("validHexes"):

    it("excludes the current robber hex"):
      val hexes = RobberValidator.validHexes(board, RobberLocation(10))
      hexes.length shouldBe 18
      hexes should not contain (10)

  describe("stealTargets"):

    it("enumerates only opponents with cards on the hex"):
      val state = stateWith(
        vertex = Map(
          Vertex(38) -> PlayerBuilding(1, Settlement),
          Vertex(37) -> PlayerBuilding(2, Settlement)
        ),
        inventory = Map(1 -> ResourceSet(wo = 1), 2 -> ResourceSet.empty)
      )
      val inv = new PerfectInfoResourceView(state)
      val targets = RobberValidator.stealTargets(5, 0, board, state.vertexBuildingState, inv)
      targets shouldBe Seq(1)

    it("returns no targets when every victim has zero cards"):
      val state = stateWith(
        vertex = Map(Vertex(38) -> PlayerBuilding(1, Settlement)),
        inventory = Map(1 -> ResourceSet.empty)
      )
      val inv = new PerfectInfoResourceView(state)
      RobberValidator.stealTargets(5, 0, board, state.vertexBuildingState, inv) shouldBe empty

    it("returns no targets when no buildings sit on the hex"):
      val state = stateWith(Map.empty, Map.empty)
      val inv = new PerfectInfoResourceView(state)
      RobberValidator.stealTargets(11, 0, board, state.vertexBuildingState, inv) shouldBe empty

  describe("placements and robberMoves"):

    it("yields a no-steal move for a hex with no victims"):
      val state = stateWith(Map.empty, Map.empty)
      val inv = new PerfectInfoResourceView(state)
      val placements = RobberValidator.placements(0, RobberLocation(10), board, state.vertexBuildingState, inv)
      placements.find(_.hexId == 11).map(_.victims) shouldBe Some(Seq.empty)

    it("yields one move per victim with the exact stolen resource in perfect info"):
      val state = stateWith(
        vertex = Map(Vertex(38) -> PlayerBuilding(1, Settlement)),
        inventory = Map(1 -> ResourceSet(wo = 2))
      )
      val inv = new PerfectInfoResourceView(state)
      val placements = RobberValidator.placements(0, RobberLocation(10), board, state.vertexBuildingState, inv)
      placements.find(_.hexId == 5).map(_.victims) shouldBe Some(Seq(1))

    it("public robber moves hide the stolen resource"):
      val state = stateWith(
        vertex = Map(Vertex(38) -> PlayerBuilding(1, Settlement)),
        inventory = Map(1 -> ResourceSet(wo = 2))
      )
      val inv = new PerfectInfoResourceView(state)
      val moves = RobberValidator.robberMoves(0, RobberLocation(10), board, state.vertexBuildingState, inv)
      val onHex5 = moves.filter(_.robberHexId == 5)
      onHex5.map(_.steal.map(_.victim)) shouldBe Seq(Some(1))
      onHex5.forall(_.steal.forall(_.resource.isEmpty)) shouldBe true
