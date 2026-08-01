package soc.rules
package validators

import soc.base.*
import soc.base.state.*
import soc.core.*
import soc.core.state.*
import soc.rules.*
import soc.rules.CachedBoard

object RobberValidator:

  case class RobberPlacement(hexId: Int, victims: Seq[Int])

  def validHexes(cached: CachedBoard[Resource], robberLocation: RobberLocation): Seq[Int] =
    cached.hexesWithNodes.map(_.node).filter(_ != robberLocation.robberHexId)

  def stealTargets(
    hexId: Int,
    player: Int,
    cached: CachedBoard[Resource],
    vertexState: VertexBuildingState[BaseVertexBuilding],
    inv: CachedBoard.ResourceView
  ): Seq[Int] =
    cached.hexToVertices
      .getOrElse(hexId, Nil)
      .flatMap(v => vertexState.map.get(v).filter(pb => pb.player != player))
      .map(_.player)
      .distinct
      .filter(p => inv.getTotal(p) > 0)

  def placements(
    player: Int,
    robberLocation: RobberLocation,
    cached: CachedBoard[Resource],
    vertexState: VertexBuildingState[BaseVertexBuilding],
    inv: CachedBoard.ResourceView
  ): Seq[RobberPlacement] =
    validHexes(cached, robberLocation).map { hex =>
      RobberPlacement(hex, stealTargets(hex, player, cached, vertexState, inv))
    }

  def robberMoves(
    player: Int,
    robberLocation: RobberLocation,
    cached: CachedBoard[Resource],
    vertexState: VertexBuildingState[BaseVertexBuilding],
    inv: CachedBoard.ResourceView
  ): Seq[RobberMoveResult[Resource]] =
    placements(player, robberLocation, cached, vertexState, inv).flatMap {
      case RobberPlacement(hex, victims) if victims.nonEmpty =>
        victims.map(v => RobberMoveResult[Resource](player, hex, Some(PlayerSteal[Option[Resource]](v, None))))
      case RobberPlacement(hex, _) =>
        Seq(RobberMoveResult[Resource](player, hex, None))
    }
