package soc.base.actions

import game.{GameAction, NoInput}
import soc.base.*
import soc.base.state.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.Transactions.*
import soc.core.state.*
import soc.core.state.BoardBuildingState.*

case class BuildRoadInput(
  board: BaseBoard[Resource],
  socRoadLengths: SOCRoadLengths,
  socLongestRoadPlayer: SOCLongestRoadPlayer,
  vertexBuildingState: VertexBuildingState[BaseVertexBuilding],
  edgeBuildingState: EdgeBuildingState[BaseEdgeBuilding]
) extends LongestRoadState

case class BuildRoadCoreOutput(
  addedRoad:                 EdgeBuildingState[BaseEdgeBuilding]#Delta,
  roadLengthChanges:         List[SOCRoadLengths#Delta],
  longestRoadPlayerChanges:  List[SOCLongestRoadPlayer.Delta],
  longestRoadPointChanges:   List[PlayerPoints#Delta],
  resourcesSpent:            Lose[Resource],
  resourcesReturned:         Bank[Resource]#Delta
)

class BuildRoadCoreAction extends GameAction[BuildRoadMove, BuildRoadInput, BuildRoadCoreOutput]:
  def apply(move: BuildRoadMove, input: BuildRoadInput): BuildRoadCoreOutput =
    val updatedEdges: EdgeBuildingState[BaseEdgeBuilding] =
      EdgeBuildingState(input.edgeBuildingState.map + (move.edge -> PlayerBuilding(move.player, Road)))
    val changes = longestRoadChanges(input, input.vertexBuildingState, updatedEdges)
    BuildRoadCoreOutput(
      addedRoad                = BoardBuildingState.add(move.edge, Road, move.player),
      roadLengthChanges        = changes.roadLengthChanges,
      longestRoadPlayerChanges = changes.longestRoadPlayerChanges,
      longestRoadPointChanges  = changes.pointChanges,
      resourcesSpent           = Lose(move.player, ROAD_COST),
      resourcesReturned        = Bank.Add(ROAD_COST)
    )

  def apply(move: BuildRoadMove, input: NoInput.type): BuildRoadCoreOutput =
    BuildRoadCoreOutput(
      BoardBuildingState.add(move.edge, Road, move.player),
      Nil,
      Nil,
      Nil,
      Lose(move.player, ROAD_COST),
      Bank.Add(ROAD_COST)
    )
