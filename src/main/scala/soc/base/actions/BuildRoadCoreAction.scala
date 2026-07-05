package soc.base.actions

import game.{GameAction, NoInput}
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.Transactions.*
import soc.core.state.*
import soc.core.state.BoardBuildingState.*

case class BuildRoadCoreOutput(
  addedRoad:         EdgeBuildingState[BaseEdgeBuilding]#Delta,
  resourcesSpent:    Lose[Resource],
  resourcesReturned: Bank[Resource]#Delta
)

class BuildRoadCoreAction extends GameAction[BuildRoadMove, NoInput.type, BuildRoadCoreOutput]:
  def apply(move: BuildRoadMove, input: NoInput.type): BuildRoadCoreOutput =
    BuildRoadCoreOutput(
      addedRoad         = BoardBuildingState.add(move.edge, Road, move.player),
      resourcesSpent    = Lose(move.player, ROAD_COST),
      resourcesReturned = Bank.Add(ROAD_COST)
    )