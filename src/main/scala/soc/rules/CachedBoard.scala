package soc.rules

import game.InventorySet
import soc.base.{BaseBoard, DevelopmentCard}
import soc.core.*
import soc.core.SOCBoard.SOCBoardOps
import soc.core.ResourceSet.*
import soc.core.Resources.*

object CachedBoard:

  trait ResourceView:
    def getTotal(player: Int): Int
    def hasEnough(player: Int, resources: InventorySet[Resource, Int]): Boolean
    def resourceAmount(player: Int, resource: Resource): Int

  trait DevCardView:
    def hasUnexpiredCard(player: Int, card: DevelopmentCard, currentTurn: Int): Boolean
    def deckNonEmpty: Boolean

  val ROAD_COST: Resources       = ResourceSet(WOOD, BRICK)
  val SETTLEMENT_COST: Resources = ResourceSet(WOOD, BRICK, WHEAT, SHEEP)
  val CITY_COST: Resources       = ResourceSet(ORE, ORE, ORE, WHEAT, WHEAT)
  val DEV_CARD_COST: Resources   = ResourceSet(ORE, WHEAT, SHEEP)

class CachedBoard[Res](board: BaseBoard[Res])(using SOCBoard[Res, BaseBoard[Res]]):
  import SOCBoard.SOCBoardOps

  lazy val hexesWithNodes: Seq[BoardHex[Res]]        = board.hexesWithNodes
  lazy val vertices: Seq[Vertex]                     = board.vertices
  lazy val edges: Seq[Edge]                          = board.edges
  lazy val neighbors: Map[Vertex, Seq[Vertex]]       = board.neighboringVertices
  lazy val edgesFromVertex: Map[Vertex, Seq[Edge]]   = board.edgesFromVertex
  lazy val hexesForVertex: Map[Vertex, Seq[BoardHex[Res]]] = board.hexesForVertex
  lazy val numberHexes: Map[Int, Seq[BoardHex[Res]]] = board.numberHexes
  lazy val hexToVertices: Map[Int, Seq[Vertex]]      = hexesWithNodes.map(h => h.node -> h.vertices).toMap
  lazy val portEdges: Map[Edge, Port] =
    board.ports.zip(BaseBoard.basePortEdges).map { case (port, (v1, v2)) =>
      Edge(Vertex(v1), Vertex(v2)) -> port
    }.toMap
