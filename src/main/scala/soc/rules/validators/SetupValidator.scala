package soc.rules
package validators

import soc.base.*
import soc.base.state.*
import soc.core.*
import soc.core.state.*
import soc.rules.*

object SetupValidator:

  def legalMoves(
    player: Int,
    numPlayers: Int,
    setupPlacementOrder: SetupPlacementOrder,
    vertexBuildingState: VertexBuildingState[BaseVertexBuilding],
    edgeBuildingState: EdgeBuildingState[BaseEdgeBuilding],
    board: BaseBoard[Resource]
  ): Seq[InitialPlacementMove] =
    val placedPlayers = setupPlacementOrder.placements.map(_._1)
    if player != PhaseMachine.setupActivePlayer(placedPlayers, numPlayers) then Nil
    else
      board.vertices.flatMap { vertex =>
        if !distanceRuleOk(vertex, vertexBuildingState, board) then Nil
        else
          board.edgesFromVertex.getOrElse(vertex, Nil).flatMap { edge =>
            if edgeBuildingState.map.contains(edge) then Nil
            else Seq(InitialPlacementMove(vertex, edge, player))
          }
      }

  def emptyVertex(vertex: Vertex, vertexBuildingState: VertexBuildingState[BaseVertexBuilding]): Boolean =
    !vertexBuildingState.map.contains(vertex)

  def distanceRuleOk(
    vertex: Vertex,
    vertexBuildingState: VertexBuildingState[BaseVertexBuilding],
    board: BaseBoard[Resource]
  ): Boolean =
    emptyVertex(vertex, vertexBuildingState) &&
      board.neighbors.getOrElse(vertex, Nil).forall(v => !vertexBuildingState.map.contains(v))
