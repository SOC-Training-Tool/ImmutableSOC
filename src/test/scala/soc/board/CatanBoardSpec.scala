package soc.board

import soc.core.Roll
import soc.inventory.{_}
import org.scalatest.{FunSpec, Matchers}
import soc.CatanFixtures._


class CatanBoardSpec extends FunSpec with Matchers {

  describe("canBuildSettlement") {

    describe("should return false when") {
      it("not valid vertex") {
          singleHexBoard.canBuildSettlement(Vertex(7), 0, initialPlacement = true) shouldBe false
      }

      it("there already exists a building on that vertex") {
        val board = singleHexBoard.buildSettlement(Vertex(0), 0)
        board.canBuildSettlement(Vertex(0), 0, initialPlacement = true) shouldBe false
      }

      it ("a neighboring vertex contains a building") {
        val board = singleHexBoard.buildSettlement(Vertex(1), 0)
        board.canBuildSettlement(Vertex(0), 0, initialPlacement = true) shouldBe false
      }

      describe ("not an initial placement") {

        it("and no road is adjacent to vertex") {
           singleHexBoard.canBuildSettlement(Vertex(0), 0, initialPlacement = false) shouldBe false
        }

        it("and only road is adjacent to vertex is with incorrect playerId") {
          val board = singleHexBoard.buildRoad(Edge(Vertex(0), Vertex(1)), 1)
          board.canBuildSettlement(Vertex(0), 0, initialPlacement = false) shouldBe false
        }
      }
    }

    describe ("should return true when it is a valid vertex, no neighboring vertex contains a building,") {

      it("and it is an initial settlement") {
        singleHexBoard.canBuildSettlement(Vertex(0), 0, initialPlacement = true) shouldBe true
      }

      it ("and it is not an initial settlement but there is an adjacent road with the correct playerId ") {
        val board = singleHexBoard.buildRoad(Edge(Vertex(0), Vertex(1)), 0)
        board.canBuildSettlement(Vertex(0), 0, initialPlacement = false) shouldBe true
      }

      it("even if there are settlements at least two vertices away") {
        val board = singleHexBoard
          .buildSettlement(Vertex(2), 0)
          .buildSettlement(Vertex(4), 1)
        board.canBuildSettlement(Vertex(0), 0, initialPlacement = true) shouldBe true
      }
    }
  }

  describe("buildSettlement") {

    it("should return a CatanBoard where the verticesBuildingMap contains a settlement owned by the correct player") {
      val board = singleHexBoard.buildSettlement(Vertex(0), 0)
      board.verticesBuildingMap should contain (Vertex(0) -> Settlement(0))
    }

  }

  describe("canBuildCity") {

    describe("should return false when") {

      it("there is not a settlement on the vertex") {
        singleHexBoard.canBuildCity(Vertex(0), 0) shouldBe false
      }

      it("there is a settlement on the vertex with a different owner") {
        val board = singleHexBoard.buildSettlement(Vertex(0), 1)
        board.canBuildCity(Vertex(0), 0) shouldBe false
      }

      it("there is already a city on the vertex") {
        val board = singleHexBoard.buildSettlement(Vertex(0), 0)
          .buildCity(Vertex(0), 0)
        board.canBuildCity(Vertex(0), 0) shouldBe false
      }

    }

    it ("should return true when there is a settlement with the same owner on the vertex") {
      val board = singleHexBoard.buildSettlement(Vertex(0), 0)
      board.canBuildCity(Vertex(0), 0) shouldBe true
    }

  }

  describe("buildCity") {
    it("should return a CatanBoard where the verticesBuildingMap contains a city owned by the correct player") {
      val board = singleHexBoard.buildSettlement(Vertex(0), 0).buildCity(Vertex(0), 0)
      board.verticesBuildingMap should contain (Vertex(0) -> City(0))
    }
  }

  describe("canBuildRoad") {

    val edge = Edge(Vertex(0), Vertex(1))

    describe ("should return false when") {
      it("not a valid edge") {
        singleHexBoard.canBuildRoad(Edge(Vertex(-1), Vertex(0)), 0) shouldBe false
        singleHexBoard.canBuildRoad(Edge(Vertex(0), Vertex(2)), 0) shouldBe false
      }

      it("there is already a road on that edge") {

        val board = singleHexBoard.buildRoad(edge, 0)
        board.canBuildRoad(edge, 0) shouldBe false
      }

      it("there is building on one of its vertices with a different owner") {
        val board = singleHexBoard.buildSettlement(Vertex(0), 1)
        board.canBuildRoad(edge, 0) shouldBe false
      }

      it("there are no building on its vertices and no neighboring roads") {
        singleHexBoard.canBuildRoad(edge, 0) shouldBe false
      }

      it("there are no building on its vertices and neighboring roads with different owners") {
        val neighborEdge = Edge(Vertex(1), Vertex(2))
        val board = singleHexBoard.buildRoad(neighborEdge, 1)
        board.canBuildRoad(edge, 0)
      }
    }

    describe ("should return true when") {

      it("there is a settlement with same owner on its vertex") {
        val board = singleHexBoard.buildSettlement(Vertex(0), 0)
        board.canBuildRoad(edge, 0) shouldBe true
      }

      it("there is a neighboring road with the same owner") {
        val board = singleHexBoard.buildRoad(Edge(Vertex(1), Vertex(2)), 0)
        board.canBuildRoad(edge, 0) shouldBe true
      }
    }
  }

  describe("buildRoad") {

    it("should return a CatanBoard where the edgesBuildingMap contains a city owned by the correct player") {
      val board = singleHexBoard.buildSettlement(Vertex(0), 0).buildRoad(Edge(Vertex(0), Vertex(1)), 0)
      board.edgesBuildingMap should contain (Edge(Vertex(0), Vertex(1)) -> Road(0))
    }

  }

  describe("getPossibleSettlements") {

    describe("when initial") {
      it("should return all vertices if soc.board is empty") {
        singleHexBoard.getPossibleSettlements(0, initial = true) should contain allElementsOf(singleHexBoard.vertices)
      }

      it("should return vertices at least 2 edges away if there is already a settlement on the soc.board") {
        val occupiedVertices = List(5, 0, 1).map(Vertex)
        val board = singleHexBoard.buildSettlement(Vertex(0), 1)
        board.getPossibleSettlements(0, initial = true) should contain allElementsOf(board.vertices.filterNot(occupiedVertices.contains))
      }
    }

    describe ("when not initial") {

      it ("should return empty list if there are no valid settlement spots touching roads") {
        val board = singleHexBoard.buildSettlement(Vertex(0), 0).buildRoad(Edge(Vertex(0), Vertex(1)), 0)
        board.getPossibleSettlements(0, false) shouldBe empty
      }

      it ("should return a list of valid vertices if there are valid settlement spots touching roads") {
        val board = singleHexBoard.buildSettlement(Vertex(0), 0)
          .buildRoad(Edge(Vertex(0), Vertex(1)), 0)
          .buildRoad(Edge(Vertex(1), Vertex(2)), 0)
          .buildRoad(Edge(Vertex(2), Vertex(3)), 0)
          .buildRoad(Edge(Vertex(3), Vertex(4)), 0)
        board.getPossibleSettlements(0, false) should contain only (Vertex(2), Vertex(3), Vertex(4))
      }
    }
  }

  describe("getPossibleCities") {

    it ("should return empty list if there are no settlements owned by player") {
      singleHexBoard.getPossibleCities(0) shouldBe empty
    }

    it ("should return list of valid vertices if there are settlements on the vertices") {
      val board = singleHexBoard.buildSettlement(Vertex(0), 0)
        .buildSettlement(Vertex(2), 0)
      board.getPossibleCities(0) should contain only (Vertex(0), Vertex(2))
    }
  }

  describe("getPossibleRoads") {

    it ("should return empty list when there are no settlements or roads on the board") {
      singleHexBoard.getPossibleRoads(0) shouldBe empty
    }

    it ("should return edges coming off settlement if those edges are not occupied") {
      val board = singleHexBoard.buildSettlement(Vertex(0), 0)
      board.getPossibleRoads(0) should contain only (Edge(Vertex(0), Vertex(1)), Edge(Vertex(5), Vertex(0)))
    }

    it ("should return edges coming off road if those edges are not occupied") {
      val board = singleHexBoard.buildRoad(Edge(Vertex(0), Vertex(1)), 0)
      board.getPossibleRoads(0) should contain only (Edge(Vertex(1), Vertex(2)), Edge(Vertex(5), Vertex(0)))
    }
  }

  describe("playersOnHex") {

  }

  describe("getResourcesGainedOnRoll") {

  }

  describe("longestRoadLength") {


    val vertexMap = Map(
      0 -> List(0, 1, 2, 3, 4, 5),
      1 -> List(2, 6, 7, 8, 9, 3),
      2 -> List(10, 11, 12, 13, 7, 6)
    ).view.mapValues(_.map(Vertex)).toMap

    val hexes = List(ResourceHex(Wood, Roll(6)), ResourceHex(Wood, Roll(6)), ResourceHex(Wood, Roll(6)))

    val roadBoard = CatanBoard(vertexMap, Map.empty, hexes)

    describe("without settlements") {

      /*
          -
       */
      it("road scenario1") {
        roadBoard.buildRoad(Edge(Vertex(2), Vertex(6)), 0).longestRoadLength(0) shouldBe 1
      }

      /*
          \_
       */
      it("road scenario2") {
        roadBoard
          .buildRoad(Edge(Vertex(2), Vertex(6)), 0)
          .buildRoad(Edge(Vertex(1), Vertex(2)), 0)
          .longestRoadLength(0) shouldBe 2
      }

      /*
          _/
       */
      it("road scenario3") {
        roadBoard
          .buildRoad(Edge(Vertex(2), Vertex(6)), 0)
          .buildRoad(Edge(Vertex(6), Vertex(10)), 0)
          .longestRoadLength(0) shouldBe 2
      }

      /*
          \_
          /
       */
      it("road scenario4") {
        roadBoard
          .buildRoad(Edge(Vertex(2), Vertex(6)), 0)
          .buildRoad(Edge(Vertex(1), Vertex(2)), 0)
          .buildRoad(Edge(Vertex(3), Vertex(2)), 0)
          .longestRoadLength(0) shouldBe 2
      }

      /*
          _/
           \
       */
      it("road scenario5") {
        roadBoard
          .buildRoad(Edge(Vertex(2), Vertex(6)), 0)
          .buildRoad(Edge(Vertex(10), Vertex(6)), 0)
          .buildRoad(Edge(Vertex(7), Vertex(6)), 0)
          .longestRoadLength(0) shouldBe 2
      }

      /*
          \_/
            \
       */
      it("road scenario6") {
        roadBoard
          .buildRoad(Edge(Vertex(2), Vertex(6)), 0)
          .buildRoad(Edge(Vertex(1), Vertex(2)), 0)
          .buildRoad(Edge(Vertex(10), Vertex(6)), 0)
          .buildRoad(Edge(Vertex(7), Vertex(6)), 0)
          .longestRoadLength(0) shouldBe 3
      }

      /*
           _/
          / \
       */
      it("road scenario7") {
        roadBoard
          .buildRoad(Edge(Vertex(2), Vertex(6)), 0)
          .buildRoad(Edge(Vertex(3), Vertex(2)), 0)
          .buildRoad(Edge(Vertex(10), Vertex(6)), 0)
          .buildRoad(Edge(Vertex(7), Vertex(6)), 0)
          .longestRoadLength(0) shouldBe 3
      }

      /*
          \_/
          /
       */
      it("road scenario8") {
        roadBoard
          .buildRoad(Edge(Vertex(2), Vertex(6)), 0)
          .buildRoad(Edge(Vertex(1), Vertex(2)), 0)
          .buildRoad(Edge(Vertex(3), Vertex(2)), 0)
          .buildRoad(Edge(Vertex(10), Vertex(6)), 0)
          .longestRoadLength(0) shouldBe 3
      }

      /*
          \_
          / \
       */
      it("road scenario9") {
        roadBoard
          .buildRoad(Edge(Vertex(2), Vertex(6)), 0)
          .buildRoad(Edge(Vertex(1), Vertex(2)), 0)
          .buildRoad(Edge(Vertex(3), Vertex(2)), 0)
          .buildRoad(Edge(Vertex(7), Vertex(6)), 0)
          .longestRoadLength(0) shouldBe 3
      }

      /*
          \_/
          / \
       */
      it("road scenario10") {
        roadBoard
          .buildRoad(Edge(Vertex(2), Vertex(6)), 0)
          .buildRoad(Edge(Vertex(1), Vertex(2)), 0)
          .buildRoad(Edge(Vertex(3), Vertex(2)), 0)
          .buildRoad(Edge(Vertex(7), Vertex(6)), 0)
          .buildRoad(Edge(Vertex(10), Vertex(6)), 0)
          .longestRoadLength(0) shouldBe 3
      }
    }
  }


}
