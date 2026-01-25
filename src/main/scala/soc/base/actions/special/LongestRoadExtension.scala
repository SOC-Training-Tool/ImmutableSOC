package soc.base.actions.special

import game.{Delta, DeltaList, GameAction}
import shapeless.{:+:, ::, CNil, Coproduct, HNil}
import soc.base.state.{LongestRoadOps, SOCLongestRoadPlayer, SOCRoadLengths, SpecialCounts}
import soc.core.state.{EdgeBuildingState, PlayerPoints, VertexBuildingState}
import soc.core.{Edge, SOCBoard}

object LongestRoadExtension {

  def apply[Res, VB <: Coproduct, EB <: Coproduct, BOARD](min: Int = 5)(implicit socBoard: SOCBoard[Res, BOARD])
  : GameAction[Seq[Edge], SOCRoadLengths :: SOCLongestRoadPlayer :: BOARD :: VertexBuildingState[VB] :: EdgeBuildingState[EB] :: HNil, Delta[SOCLongestRoadPlayer] :+: Delta[PlayerPoints] :+: Delta[SOCRoadLengths] :+: CNil] =
    GameAction.fromState[Seq[Edge], SOCRoadLengths :: SOCLongestRoadPlayer :: BOARD :: VertexBuildingState[VB] :: EdgeBuildingState[EB] :: HNil] { case (_, state) =>
      val board             = state.select[BOARD]
      val edgeBuildingMap   = state.select[EdgeBuildingState[EB]]
      val vertexBuildingMap = state.select[VertexBuildingState[VB]]
      val roadLengthOps     = new LongestRoadOps(board, edgeBuildingMap, vertexBuildingMap)
      val longestRoadPlayer = state.select[SOCLongestRoadPlayer].player

      val updatedRoadLengths       = roadLengthOps.calcLongestRoadLengths()
      val updatedLongestRoadPlayer = updatedSpecialPlayer(min, longestRoadPlayer, updatedRoadLengths.m)

      val setRoadLengths = updatedRoadLengths.m.map { case (k, v) => SpecialCounts.Set(k, v) }.toList

      DeltaList()
        .add[SOCRoadLengths](setRoadLengths: _*)
        .addAll(specialPlayerDelta[SOCLongestRoadPlayer](longestRoadPlayer, updatedLongestRoadPlayer))
        .toList
    }

}