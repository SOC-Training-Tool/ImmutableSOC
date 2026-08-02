package soc.rules
package validators

import soc.base.*
import soc.base.state.*
import soc.core.*
import soc.core.state.*
import soc.rules.*

object RobberValidator:

  case class RobberPlacement(hexId: Int, victims: Seq[Int])

  def validHexes(board: BaseBoard[Resource], robberLocation: RobberLocation): Seq[Int] =
    board.hexesWithNodes.map(_.node).filter(_ != robberLocation.robberHexId)

  def stealTargets(
    hexId: Int,
    player: Int,
    board: BaseBoard[Resource],
    vertexState: VertexBuildingState[BaseVertexBuilding],
    inv: ResourceView
  ): Seq[Int] =
    board.hexToVertices
      .getOrElse(hexId, Nil)
      .flatMap(v => vertexState.map.get(v).filter(pb => pb.player != player))
      .map(_.player)
      .distinct
      .filter(p => inv.getTotal(p) > 0)

  def placements(
    player: Int,
    robberLocation: RobberLocation,
    board: BaseBoard[Resource],
    vertexState: VertexBuildingState[BaseVertexBuilding],
    inv: ResourceView
  ): Seq[RobberPlacement] =
    validHexes(board, robberLocation).map { hex =>
      RobberPlacement(hex, stealTargets(hex, player, board, vertexState, inv))
    }

  def robberMoves(
    player: Int,
    robberLocation: RobberLocation,
    board: BaseBoard[Resource],
    vertexState: VertexBuildingState[BaseVertexBuilding],
    inv: ResourceView
  ): Seq[RobberMoveResult[Resource]] =
    placements(player, robberLocation, board, vertexState, inv).flatMap {
      case RobberPlacement(hex, victims) if victims.nonEmpty =>
        victims.map(v => RobberMoveResult[Resource](player, hex, Some(PlayerSteal[Option[Resource]](v, None))))
      case RobberPlacement(hex, _) =>
        Seq(RobberMoveResult[Resource](player, hex, None))
    }
