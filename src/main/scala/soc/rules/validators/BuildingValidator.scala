package soc.rules
package validators

import soc.base.*
import soc.base.state.*
import soc.core.*
import soc.core.state.*
import soc.rules.*

object BuildingValidator:

  def roadMoves(
    player: Int,
    inv: ResourceView,
    edgeState: EdgeBuildingState[BaseEdgeBuilding],
    vertexState: VertexBuildingState[BaseVertexBuilding],
    board: BaseBoard[Resource]
  ): Seq[BuildRoadMove] =
    if !inv.hasEnough(player, ROAD_COST) then Nil
    else
      roadPlacementEdges(player, edgeState, vertexState, board)
        .filter(_ => roadCount(player, edgeState) < 15)
        .map(edge => BuildRoadMove(player, edge))

  def roadPlacementEdges(
    player: Int,
    edgeState: EdgeBuildingState[BaseEdgeBuilding],
    vertexState: VertexBuildingState[BaseVertexBuilding],
    board: BaseBoard[Resource]
  ): Seq[Edge] =
    board.edges.filter { edge =>
      edgeState.map.get(edge).isEmpty && connectedToNetwork(player, edge, edgeState, vertexState)
    }

  def settlementMoves(
    player: Int,
    inv: ResourceView,
    vertexState: VertexBuildingState[BaseVertexBuilding],
    edgeState: EdgeBuildingState[BaseEdgeBuilding],
    board: BaseBoard[Resource]
  ): Seq[BuildSettlementMove] =
    if !inv.hasEnough(player, SETTLEMENT_COST) then Nil
    else
      board.vertices.filter { vertex =>
        SetupValidator.distanceRuleOk(vertex, vertexState, board) &&
          hasAdjacentRoad(player, vertex, edgeState) &&
          settlementCityCount(player, vertexState) < 5
      }.map(vertex => BuildSettlementMove(player, vertex))

  def cityMoves(
    player: Int,
    inv: ResourceView,
    vertexState: VertexBuildingState[BaseVertexBuilding],
    board: BaseBoard[Resource]
  ): Seq[BuildCityMove] =
    if !inv.hasEnough(player, CITY_COST) then Nil
    else if cityCount(player, vertexState) >= 4 then Nil
    else
      vertexState.map.collect {
        case (v, pb) if pb.player == player && pb.building == Settlement =>
          BuildCityMove(player, v)
      }.toSeq

  private def connectedToNetwork(
    player: Int,
    edge: Edge,
    edgeState: EdgeBuildingState[BaseEdgeBuilding],
    vertexState: VertexBuildingState[BaseVertexBuilding]
  ): Boolean =
    def endpointConnected(v: Vertex): Boolean =
      vertexState.map.get(v) match
        case Some(pb) if pb.player != player => false
        case _ =>
          val ownsBuilding = vertexState.map.get(v).exists(_.player == player)
          val ownsAdjacentRoad = edgeState.map.exists { case (e, pb) =>
            e != edge && pb.player == player && e.contains(v)
          }
          ownsBuilding || ownsAdjacentRoad
    endpointConnected(edge.v1) || endpointConnected(edge.v2)

  def hasAdjacentRoad(player: Int, vertex: Vertex, edgeState: EdgeBuildingState[BaseEdgeBuilding]): Boolean =
    edgeState.map.exists { case (e, pb) => pb.player == player && e.contains(vertex) }

  def roadCount(player: Int, edgeState: EdgeBuildingState[BaseEdgeBuilding]): Int =
    edgeState.map.values.count(_.player == player)

  def settlementCityCount(player: Int, vertexState: VertexBuildingState[BaseVertexBuilding]): Int =
    vertexState.map.values.count(_.player == player)

  def cityCount(player: Int, vertexState: VertexBuildingState[BaseVertexBuilding]): Int =
    vertexState.map.values.count(pb => pb.player == player && pb.building == City)
