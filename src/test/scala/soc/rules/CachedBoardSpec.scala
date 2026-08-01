package soc.rules

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import soc.base.BaseGame.*
import soc.base.BaseGameFixtures
import soc.core.*
import soc.rules.RulesFixtures.*

class CachedBoardSpec extends AnyFunSpec with Matchers:

  private val cached = new CachedBoard[Resource](BaseGameFixtures.perfectInfoFixture.board)

  describe("precomputed geometry"):

    it("has the standard board dimensions"):
      cached.hexesWithNodes.length shouldBe 19
      cached.vertices.length shouldBe 54
      cached.edges.length shouldBe 72

    it("maps every vertex to its incident edges"):
      cached.edgesFromVertex.keys.size shouldBe 54
      cached.edgesFromVertex.values.forall(_.nonEmpty) shouldBe true

    it("computes vertex neighbors from incident edges"):
      val neighborsOf41 = cached.neighbors(Vertex(41))
      neighborsOf41.toSet shouldBe Set(Vertex(40), Vertex(42), Vertex(51))
      cached.edgesFromVertex(Vertex(41)).length shouldBe 3

    it("maps every vertex to the hexes surrounding it"):
      cached.hexesForVertex(Vertex(41)).map(_.node).toSet shouldBe Set(7, 15, 16)

    it("maps dice rolls to hexes"):
      cached.numberHexes.keySet should contain allOf (2, 3, 4, 5, 6, 8, 9, 10, 11, 12)
      cached.numberHexes(6).map(_.node).toSet shouldBe Set(0, 15)
      cached.numberHexes(8).map(_.node).toSet shouldBe Set(3, 9)

    it("maps each hex node to its six vertices"):
      cached.hexToVertices.size shouldBe 19
      cached.hexToVertices.values.forall(_.length == 6) shouldBe true

    it("maps the nine port edges to their port types"):
      cached.portEdges.size shouldBe 9
      cached.portEdges(Edge(Vertex(0), Vertex(1))) shouldBe Misc
      cached.portEdges(Edge(Vertex(7), Vertex(8))) shouldBe Misc
      cached.portEdges(Edge(Vertex(3), Vertex(4))) shouldBe Ore

  describe("cost constants"):

    it("defines the standard build costs"):
      CachedBoard.ROAD_COST.getTotal shouldBe 2
      CachedBoard.SETTLEMENT_COST.getTotal shouldBe 4
      CachedBoard.CITY_COST.getTotal shouldBe 5
      CachedBoard.DEV_CARD_COST.getTotal shouldBe 3
