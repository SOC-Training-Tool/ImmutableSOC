package soc.board

import soc.core.Roll
import soc.inventory._
import soc.inventory.resources.ResourceSet
import soc.inventory.resources.ResourceSet.Resources
import util.MapReverse

import scala.util.Random

sealed trait Hex {
  val getResourceAndNumber: Option[(Resource, Roll)]
  val getResource: Option[Resource]
  val getNumber: Option[Roll]
}
case class ResourceHex(resource: Resource, number: Roll) extends Hex {
  override val getResourceAndNumber: Option[(Resource, Roll)] = Some((resource, number))
  override val getResource: Option[Resource] = Some(resource)
  override val getNumber: Option[Roll] = Some(number)
}
case object Desert extends Hex {
  override val getResourceAndNumber: Option[(Resource, Roll)] = None
  override val getResource: Option[Resource] = None
  override val getNumber: Option[Roll] = None
}


case class BoardHex(
  node: Int,
  hex: Hex,
  vertices: List[Vertex]) {
}

case class Vertex(node: Int)
case class Edge(v1: Vertex, v2: Vertex) {

  def contains(v: Vertex): Boolean = v == v1 || v == v2

  override def canEqual(a: Any): Boolean = a.isInstanceOf[Edge]

  override def equals(that: Any): Boolean = that match {
    case e@Edge(ev1, ev2) =>
      e.canEqual(this) &&
        (ev1 == v1 && ev2 == v2) || (ev2 == v1 && ev1 == v2)
    case _ => false
  }

  override def hashCode: Int = (v1.node * v1.node) + (v2.node * v2.node)
}

case class CatanBoard private(
  hexesWithNodes: Seq[BoardHex],
  vertices: Seq[Vertex],
  edges: Seq[Edge],
  edgesFromVertex: Map[Vertex, Seq[Edge]],
  neighboringVertices: Map[Vertex, Seq[Vertex]],
  adjacentHexes: Map[Vertex, Seq[BoardHex]],
  portMap: Map[Edge, Port],
  robberHex: Int,
  verticesBuildingMap: Map[Vertex, VertexBuilding] = Map.empty,
  edgesBuildingMap: Map[Edge, EdgeBuilding] = Map.empty,
  roadLengths: Map[Int, Int] = Map.empty
) {

  def canBuildSettlement(vertex: Vertex, playerId: Int, initialPlacement: Boolean = false): Boolean = {
    vertices.contains(vertex) &&
      !verticesBuildingMap.contains(vertex) &&
      neighboringVertices(vertex).forall { v => !verticesBuildingMap.contains(v) } &&
      (if (!initialPlacement) {
        edgesFromVertex(vertex).exists { edge =>
          edgesBuildingMap.get(edge).fold(false)(_.playerId == playerId)
        }
      } else true)
  }

  def buildSettlement(vertex: Vertex, playerId: Int): CatanBoard = {
    val settlement = Settlement(playerId)
    val vertexMap = verticesBuildingMap + (vertex -> settlement)
    copy(verticesBuildingMap = vertexMap)
  }

  def canBuildCity(vertex: Vertex, playerId: Int): Boolean = {
    verticesBuildingMap.contains(vertex) &&
      verticesBuildingMap(vertex).isInstanceOf[Settlement] && {
      val settlement = verticesBuildingMap(vertex)
      settlement.playerId == playerId
    }
  }

  def buildCity(vertex: Vertex, playerId: Int): CatanBoard = {
    val city = City(playerId)
    val vertexMap = (verticesBuildingMap - vertex) + (vertex -> city)
    copy(verticesBuildingMap = vertexMap)
  }

  def canBuildRoad(edge: Edge, playerId: Int): Boolean = {
    def canBuildRoadOffVertex(v: Vertex): Boolean = {
      if (verticesBuildingMap.get(v).fold(false)(_.playerId == playerId)) true
      else if (verticesBuildingMap.get(v).fold(false)(_.playerId != playerId)) false
      else {
        edgesFromVertex(v).filterNot(_ == edge).exists { e =>
          edgesBuildingMap.contains(e) && edgesBuildingMap(e).playerId == playerId
        }
      }
    }

    edges.contains(edge) && !edgesBuildingMap.contains(edge) && (canBuildRoadOffVertex(edge.v1) || canBuildRoadOffVertex(edge.v2))
  }

  def buildRoad(edge: Edge, playerId: Int): CatanBoard = {
    val road = Road(playerId)
    val edgeMap = edgesBuildingMap + (edge -> road)
    val roadLen = Seq(roadLengths.getOrElse(playerId, 0), calcLongestRoadLength(playerId, edge)).max
    copy(
      edgesBuildingMap = edgeMap,
      roadLengths = roadLengths + (playerId -> roadLen)
    )
  }

  def getSettlementVerticesForPlayer(id: Int): Seq[Vertex] = verticesBuildingMap.toSeq.filter {
    case (v, Settlement(`id`)) => true
    case _ => false
  }.map(_._1)
  def getNumSettlementsForPlayer(id: Int): Int = getSettlementVerticesForPlayer(id).length

  def getNumCityVerticesForPlayer(id: Int): Seq[Vertex] = verticesBuildingMap.toSeq.filter {
    case (v, City(`id`)) => true
    case _ => false
  }.map(_._1)
  def getNumCitiesForPlayer(id: Int): Int = getNumCityVerticesForPlayer(id).length

  def getRoadEdgesForPlayer(id: Int): Seq[Edge] = edgesBuildingMap.toSeq.filter {
    case (e, Road(`id`)) => true
    case _ => false
  }.map(_._1)
  def getNumRoadsForPlayer(id: Int): Int = getRoadEdgesForPlayer(id).length

  def getPortsForPlayer(id: Int): Set[Port] = getSettlementVerticesForPlayer(id).flatMap(getPort).toSet

  def getPort(vertex: Vertex): Option[Port] = {
    portMap.find { case (edge, _) => edge.contains(vertex) }.map(_._2)
  }

  def getPossibleSettlements(playerId: Int, initial: Boolean): Seq[Vertex] = {
    vertices.filter(canBuildSettlement(_, playerId, initial))
  }

  def getPossibleCities(playerId: Int): Seq[Vertex] = {
    vertices.filter(canBuildCity(_, playerId))
  }

  def getPossibleRoads(playerId: Int): Seq[Edge] = {
    edges.filter(canBuildRoad(_, playerId))
  }

  def playersOnHex(node: Int): Seq[Int] = {
    hexesWithNodes.find(_.node == node).get.vertices.flatMap { v =>
      verticesBuildingMap.get(v).map(_.playerId)
    }.distinct
  }

  def longestRoadLength(playerId: Int): Int = roadLengths.getOrElse(playerId, 0)

  def getResourcesGainedOnRoll(roll: Int): Map[Int, Resources] = {
    hexesWithNodes.filter { boardHex =>
      boardHex.hex.getNumber.fold(false)(_.number == roll) && boardHex.node != robberHex
    }.flatMap { node =>
      node.vertices.flatMap { vertex =>
        verticesBuildingMap.get(vertex) match {
          case Some(Settlement(playerId)) => Seq(playerId -> node.hex.getResource.get)
          case Some(City(playerId)) => Seq(playerId -> node.hex.getResource.get, playerId -> node.hex.getResource.get)
          case _ => Nil
        }
      }
    }.groupBy(_._1).view.mapValues(_.map(_._2).foldLeft(ResourceSet.empty[Int])(_.add(1, _)))
  }.toMap

  def calcLongestRoadLength(playerId: Int): Int = {
    val roads = edgesBuildingMap.toSeq.flatMap {
      case (edge, building) if building.playerId == playerId => Seq(edge)
      case _ => Nil
    }
    calcLongestRoadLength(playerId, roads: _*)
  }


  def calcLongestRoadLength(playerId: Int, roads: Edge*): Int = {
    roads.map(r => calcLongestRoadLengthRecur(playerId, List((r.v1, r.v2)), List(r))).max
  }

  def updateRoadLengths: CatanBoard = {
    val players = edgesBuildingMap.view.values.map(_.playerId).toSeq.distinct
    copy(roadLengths = players.map(p => p -> calcLongestRoadLength(p)).toMap)
  }

  private def calcLongestRoadLengthRecur(playerId: Int, stack: List[(Vertex, Vertex)], visited: List[Edge] = Nil): Int = {
    if (stack.isEmpty) visited.length
    else {
      val (v1, v2): (Vertex, Vertex) = stack.head
      if (v1 == v2) {
        return Math.max(visited.length, calcLongestRoadLengthRecur(playerId, stack.tail, visited))
      }

      val fromV1 = if (verticesBuildingMap.get(v1).fold(true)(_.playerId == playerId)) {
        neighboringVertices(v1).filterNot(v => visited.contains(Edge(v1, v))).filter(v => edgesBuildingMap.get(Edge(v1, v)).fold(false)(_.playerId == playerId)).toList
      } else Nil
      val fromV2 = if (verticesBuildingMap.get(v2).fold(true)(_.playerId == playerId)) {
        neighboringVertices(v2).filterNot(v => visited.contains(Edge(v2, v))).filter(v => edgesBuildingMap.get(Edge(v2, v)).fold(false)(_.playerId == playerId)).toList
      } else Nil

      (fromV1, fromV2) match {
        // see road scenario1
        case (Nil, Nil) =>
          Math.max(visited.length, calcLongestRoadLengthRecur(playerId, stack.tail, visited))

        // see road scenario2
        case (Nil, r :: Nil) =>
          calcLongestRoadLengthRecur(playerId, (v1, r) :: stack.tail, Edge(v2, r) :: visited)

        case (Nil, r1 :: r2 :: Nil) =>
          Seq(
            calcLongestRoadLengthRecur(playerId, (v1, r1) :: stack.tail, Edge(v2, r1) :: visited),
            calcLongestRoadLengthRecur(playerId, (v1, r2) :: stack.tail, Edge(v2, r2) :: visited)
          ).max

        case (l :: Nil, Nil) =>
          calcLongestRoadLengthRecur(playerId, (l, v2) :: stack.tail, Edge(l, v1) :: visited)

        case (l :: Nil, r :: Nil) =>
          calcLongestRoadLengthRecur(playerId, (l, r) :: stack.tail, Edge(r, v2) :: Edge(l, v1) :: visited)

        case (l :: Nil, r1 :: r2 :: Nil) =>
          Seq(
            calcLongestRoadLengthRecur(playerId, (l, r1) :: stack.tail, Edge(r1, v2) :: Edge(l, v1) :: visited),
            calcLongestRoadLengthRecur(playerId, (l, r2) :: stack.tail, Edge(r2, v2) :: Edge(l, v1) :: visited)
          ).max
        // see road scenario4

        case (l1 :: l2 :: Nil, Nil) =>
          Seq(
            calcLongestRoadLengthRecur(playerId, (l1, v2) :: stack.tail, Edge(l1, v1) :: visited),
            calcLongestRoadLengthRecur(playerId, (l2, v2) :: stack.tail, Edge(l2, v1) :: visited)
          ).max

        case (l1 :: l2 :: Nil, r :: Nil) =>
          Seq(
            calcLongestRoadLengthRecur(playerId, (l1, r) :: stack.tail, Edge(l1, v1) :: Edge(r, v2) :: visited),
            calcLongestRoadLengthRecur(playerId, (l2, r) :: stack.tail, Edge(l2, v1) :: Edge(r, v2) :: visited)
          ).max

        case (l1 :: l2 :: Nil, r1 :: r2 :: Nil) =>
          Seq(
            calcLongestRoadLengthRecur(playerId, (l1, r1) :: stack.tail, Edge(l1, v1) :: Edge(r1, v2) :: visited),
            calcLongestRoadLengthRecur(playerId, (l2, r2) :: stack.tail, Edge(l2, v1) :: Edge(r2, v2) :: visited),
            calcLongestRoadLengthRecur(playerId, (l1, r2) :: stack.tail, Edge(l1, v1) :: Edge(r2, v2) :: visited),
            calcLongestRoadLengthRecur(playerId, (l2, r1) :: stack.tail, Edge(l2, v1) :: Edge(r1, v2) :: visited)
          ).max
        case _ =>
          println(v1, v2)
          println(fromV1, fromV2)
          println(neighboringVertices(v1), neighboringVertices(v2))
          throw new Exception("")
      }
    }
  }
}

object CatanBoard {

  def apply(
    vertexMap: Map[Int, List[Vertex]],
    portMap: Map[Edge, Port],
    hexes: List[Hex]
  ): CatanBoard = {
    val hexesWithNodes: Seq[BoardHex] = hexes.zipWithIndex.map { case (hex: Hex, node: Int) =>
      BoardHex(node, hex, vertexMap(node))
    }
    CatanBoard(hexesWithNodes, portMap)
  }

  def apply(
    hexes: Seq[BoardHex],
    portMap: Map[Edge, Port],
    robber: Int,
    vertexBuildings: Map[Vertex, VertexBuilding],
    edgeBuildings: Map[Edge, EdgeBuilding]
  ): CatanBoard = {
    CatanBoard(hexes, portMap).copy(
      verticesBuildingMap = vertexBuildings,
      edgesBuildingMap = edgeBuildings,
      robberHex = robber
    ).updateRoadLengths
  }

  def apply(
    hexesWithNodes: Seq[BoardHex],
    portMap: Map[Edge, Port]
  ): CatanBoard = {

    val vertices: Seq[Vertex] = hexesWithNodes.flatMap(_.vertices).distinct
    val edges: Seq[Edge] = hexesWithNodes.flatMap { hex =>
      val vertices = hex.vertices
      vertices.zip(vertices.tail ::: List(vertices.head)).map { case (v1, v2) => Edge(v1, v2) }
    }.distinct

    val edgesFromVertex: Map[Vertex, Seq[Edge]] = vertices.map { vertex =>
      vertex -> edges.flatMap {
        case Edge(`vertex`, v) => Seq(Edge(`vertex`, v))
        case Edge(v, `vertex`) => Seq(Edge(v, `vertex`))
        case _ => Nil
      }
    }.toMap

    val neighboringVertices: Map[Vertex, Seq[Vertex]] = vertices.map { vertex =>
      vertex -> edgesFromVertex(vertex).flatMap {
        case Edge(`vertex`, v) => Seq(v)
        case Edge(v, `vertex`) => Seq(v)
        case _ => Nil
      }
    }.toMap.view.mapValues(_.distinct).toMap

    val adjacentHexes: Map[Vertex, Seq[BoardHex]] = vertices.map { vertex =>
      vertex -> hexesWithNodes.filter(_.vertices.contains(vertex))
    }.toMap

    val robber = hexesWithNodes.find(_.hex.getNumber.isEmpty).fold(0)(_.node)
    CatanBoard(hexesWithNodes, vertices, edges, edgesFromVertex, neighboringVertices, adjacentHexes, portMap, robber)
  }

  def checkValid(board: CatanBoard): Boolean = {
    board.adjacentHexes.forall { case (_, hexes) =>
      hexes.groupBy(_.hex.getNumber).forall {
        case (Some(Roll(6)), neighbors) if neighbors.length >= 2 => false
        case (Some(Roll(8)), neighbors) if neighbors.length >= 2 => false
        case _ => true
      }
    }
  }
}

trait BoardConfiguration

trait BoardGenerator[T <: BoardConfiguration] {
  def apply(config: T): CatanBoard

  def randomBoard(implicit rand: Random): T
}

trait BoardMapping[T <: BoardConfiguration] {
  val hexMapping: Map[Int, String]
  val vertexMapping: Map[Int, String]
  lazy val reverseHexMapping = MapReverse.reverseMap(hexMapping)
  lazy val reverseVertexMapping = MapReverse.reverseMap(vertexMapping)

}