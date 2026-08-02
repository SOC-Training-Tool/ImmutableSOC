package soc.base

import game.GameState
import soc.core.{BoardHex, Edge, Hex, Port, Resource, SOCBoard, Vertex}

case class BaseBoard[Res](hexes: List[Hex[Res]], ports: List[Port]) extends GameState[BaseBoard[Res]]:
  override type Delta = Nothing
  override def apply(delta: Nothing): BaseBoard[Res] = this

  lazy val hexesWithNodes: Seq[BoardHex[Res]] =
    val vertexMap = BaseBoard.baseVertexMap.view.mapValues(_.map(Vertex.apply)).toMap
    hexes.zipWithIndex.map { case (hex, node) =>
      BoardHex(node, hex, vertexMap.getOrElse(node, Nil))
    }

  lazy val vertices: Seq[Vertex] =
    hexesWithNodes.flatMap(_.vertices).distinct

  lazy val edges: Seq[Edge] =
    hexesWithNodes.flatMap { hex =>
      val vs = hex.vertices
      vs.zip(vs.tail ::: List(vs.head)).map { case (v1, v2) => Edge(v1, v2) }
    }.distinct

  lazy val edgesFromVertex: Map[Vertex, Seq[Edge]] =
    vertices.map { v =>
      v -> edges.filter(e => e.v1 == v || e.v2 == v)
    }.toMap

  lazy val neighbors: Map[Vertex, Seq[Vertex]] =
    vertices.map { v =>
      v -> edgesFromVertex(v).flatMap(e => Seq(e.v1, e.v2).filter(_ != v)).distinct
    }.toMap

  lazy val numberHexes: Map[Int, Seq[BoardHex[Res]]] =
    hexesWithNodes
      .flatMap(h => h.hex.getNumber.map(_ -> h))
      .groupBy(_._1)
      .view.mapValues(_.map(_._2))
      .toMap

  lazy val hexesForVertex: Map[Vertex, Seq[BoardHex[Res]]] =
    vertices.map { v =>
      v -> hexesWithNodes.filter(_.vertices.contains(v))
    }.toMap

  lazy val hexToVertices: Map[Int, Seq[Vertex]] =
    hexesWithNodes.map(h => h.node -> h.vertices).toMap

  lazy val portEdges: Map[Edge, Port] =
    ports.zip(BaseBoard.basePortEdges).map { case (port, (v1, v2)) =>
      Edge(Vertex(v1), Vertex(v2)) -> port
    }.toMap

object BaseBoard:

  val basePortEdges = Seq((0, 1), (3, 4), (7, 8), (10, 11), (13, 14), (17, 18), (20, 21), (23, 34), (27, 28))

  val baseVertexMap: Map[Int, List[Int]] = Map(
    0 -> List(0, 1, 2, 31, 30, 29),
    1 -> List(2, 3, 4, 33, 32, 31),
    2 -> List(4, 5, 6, 7, 34, 33),
    3 -> List(34, 7, 8, 9, 36, 35),
    4 -> List(36, 9, 10, 11, 12, 37),
    5 -> List(38, 37, 12, 13, 14, 39),
    6 -> List(40, 39, 14, 15, 16, 17),
    7 -> List(42, 41, 40, 17, 18, 19),
    8 -> List(22, 43, 42, 19, 20, 21),
    9 -> List(24, 45, 44, 43, 22, 23),
    10 -> List(26, 27, 46, 45, 24, 25),
    11 -> List(27, 28, 29, 30, 47, 46),
    12 -> List(30, 31, 32, 48, 53, 47),
    13 -> List(32, 33, 34, 35, 49, 48),
    14 -> List(49, 35, 36, 37, 38, 50),
    15 -> List(51, 50, 38, 39, 40, 41),
    16 -> List(44, 52, 51, 41, 42, 43),
    17 -> List(46, 47, 53, 52, 44, 45),
    18 -> List(53, 48, 49, 50, 51, 52)
  )

  given baseBoard: SOCBoard[Resource, BaseBoard[Resource]] = (t: BaseBoard[Resource]) =>
    t.hexesWithNodes
