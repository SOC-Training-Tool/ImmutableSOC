package soc.base.actions.special

import game.{GameAction, GameMoveResult}
import shapeless.{::, Coproduct, HList, HNil}
import soc.base.state.ops._
import soc.base.state.{LongestRoadOps, SOCLongestRoadPlayer, SOCRoadLengths}
import soc.core.{Edge, SOCBoard}
import soc.core.state.ops.PointsOps
import soc.core.state.{EdgeBuildingState, PlayerPoints, VertexBuildingState}

class LongestRoadExtension[Res, VB <: Coproduct, EB <: Coproduct, BOARD](min: Int = 5)(implicit socBoard: SOCBoard[Res, BOARD])
    extends GameAction[Seq[Edge], SOCRoadLengths :: SOCLongestRoadPlayer :: BOARD :: VertexBuildingState[VB] :: EdgeBuildingState[EB] :: PlayerPoints :: HNil] {

  override def apply(v1: Seq[Edge], state: STATE): STATE = {
    val board             = state.select[BOARD]
    val edgeBuildingMap   = state.select[EdgeBuildingState[EB]]
    val vertexBuildingMap = state.select[VertexBuildingState[VB]]
    val roadLengthOps     = new LongestRoadOps(board, edgeBuildingMap, vertexBuildingMap)
    val longestRoadPlayer = state.longestRoadPlayer

    val updatedRoadLengths       = roadLengthOps.calcLongestRoadLengths()
    val updatedLongestRoadPlayer = updatedSpecialPlayer(min, longestRoadPlayer, updatedRoadLengths.m)

    val result = state.updateRoadLengths(updatedRoadLengths.m)
    updateState[STATE](longestRoadPlayer, updatedLongestRoadPlayer, result)(_.incrementPointForPlayer(_), _.decrementPointForPlayer(_), _.updateLongestRoadPlayer(_))

  }
}

