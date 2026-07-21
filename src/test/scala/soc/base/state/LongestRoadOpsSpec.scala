package soc.base.state

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import soc.core.*
import soc.core.state.*

class LongestRoadOpsSpec extends AnyFunSpec with Matchers:

  private case class TestBoard(edges: List[Edge])

  private given SOCBoard[Unit, TestBoard] with
    def hexesWithNodes(board: TestBoard): Seq[BoardHex[Unit]] =
      board.edges.zipWithIndex.map { case (edge, node) =>
        BoardHex(node, Desert, List(edge.v1, edge.v2))
      }

  private def roadState(edges: List[Edge]): EdgeBuildingState[Road.type] =
    EdgeBuildingState(edges.map(_ -> PlayerBuilding(0, Road)).toMap)

  private def longestRoad(
      board: TestBoard,
      roads: List[Edge],
      buildings: VertexBuildingState[Settlement.type] = VertexBuildingState(Map.empty)
  ): Int =
    LongestRoadOps[Unit, TestBoard, Settlement.type, Road.type](board, roadState(roads), buildings)
      .calcLongestRoadLength(0)

  describe("LongestRoadOps") {
    it("calculates five connected roads as length five") {
      val chain = List(Edge(0, 1), Edge(1, 2), Edge(2, 3), Edge(3, 4), Edge(4, 5))

      longestRoad(TestBoard(chain), chain) shouldBe 5
    }

    it("uses the two longest branches of a fork without reusing a road") {
      val fork = List(
        Edge(0, 1), Edge(1, 2), Edge(2, 3),
        Edge(0, 4), Edge(4, 5),
        Edge(0, 6)
      )

      longestRoad(TestBoard(fork), fork) shouldBe 5
    }

    it("stops a road at an opponent settlement") {
      val chain = List(Edge(0, 1), Edge(1, 2), Edge(2, 3), Edge(3, 4), Edge(4, 5))
      val opponentSettlement = VertexBuildingState(Map(Vertex(2) -> PlayerBuilding(1, Settlement)))

      longestRoad(TestBoard(chain), chain, opponentSettlement) shouldBe 3
    }
  }
