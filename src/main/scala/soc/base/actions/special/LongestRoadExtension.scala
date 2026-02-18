package soc.base.actions.special

import game.{Delta, DeltaList, GameAction, :+:, CNil, tupleSelect}
import soc.base.state.{LongestRoadOps, SOCLongestRoadPlayer, SOCRoadLengths, SpecialCounts}
import soc.core.state.{EdgeBuildingState, PlayerPoints, VertexBuildingState}
import soc.core.{Edge, SOCBoard}

object LongestRoadExtension {

  def apply[Res, VB, EB, BOARD](min: Int = 5)(using socBoard: SOCBoard[Res, BOARD]): GameAction[Seq[Edge], (SOCRoadLengths, SOCLongestRoadPlayer, BOARD, VertexBuildingState[VB], EdgeBuildingState[EB]), ?] =
    GameAction.fromState[Seq[Edge], (SOCRoadLengths, SOCLongestRoadPlayer, BOARD, VertexBuildingState[VB], EdgeBuildingState[EB])] { case (_, state) =>
      val board             = tupleSelect[(SOCRoadLengths, SOCLongestRoadPlayer, BOARD, VertexBuildingState[VB], EdgeBuildingState[EB]), BOARD](state)
      val edgeBuildingMap   = tupleSelect[(SOCRoadLengths, SOCLongestRoadPlayer, BOARD, VertexBuildingState[VB], EdgeBuildingState[EB]), EdgeBuildingState[EB]](state)
      val vertexBuildingMap = tupleSelect[(SOCRoadLengths, SOCLongestRoadPlayer, BOARD, VertexBuildingState[VB], EdgeBuildingState[EB]), VertexBuildingState[VB]](state)
      val roadLengthOps     = new LongestRoadOps(board, edgeBuildingMap, vertexBuildingMap)
      val longestRoadPlayer = tupleSelect[(SOCRoadLengths, SOCLongestRoadPlayer, BOARD, VertexBuildingState[VB], EdgeBuildingState[EB]), SOCLongestRoadPlayer](state).player

      val updatedRoadLengths       = roadLengthOps.calcLongestRoadLengths()
      val updatedLongestRoadPlayer = updatedSpecialPlayer(min, longestRoadPlayer, updatedRoadLengths.m)

      val setRoadLengths = updatedRoadLengths.m.map { case (k, v) => SpecialCounts.Set(k, v) }.toList

      DeltaList.empty[Delta[SOCRoadLengths] :+: CNil]
        .add[SOCRoadLengths](setRoadLengths*)
        .addAll(specialPlayerDelta[SOCLongestRoadPlayer](longestRoadPlayer, updatedLongestRoadPlayer))
        .toList
    }

}
