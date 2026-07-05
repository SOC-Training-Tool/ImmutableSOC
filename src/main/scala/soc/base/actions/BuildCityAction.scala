package soc.base.actions

import game.{GameAction, NoInput}
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.Transactions.*
import soc.core.state.*
import soc.core.state.BoardBuildingState.*

case class BuildCityOutput(
  vertexBuildingChanges: List[VertexBuildingState[BaseVertexBuilding]#Delta],
  resourcesSpent:        Lose[Resource],
  resourcesReturned:     Bank[Resource]#Delta,
  pointChanges:          List[PlayerPoints#Delta]
)

class BuildCityAction extends GameAction[BuildCityMove, NoInput.type, BuildCityOutput]:
  def apply(move: BuildCityMove, input: NoInput.type): BuildCityOutput =
    BuildCityOutput(
      vertexBuildingChanges = List(
        BoardBuildingState.RemoveBuilding(move.vertex),
        BoardBuildingState.add(move.vertex, City, move.player)
      ),
      resourcesSpent    = Lose(move.player, CITY_COST),
      resourcesReturned = Bank.Add(CITY_COST),
      pointChanges = List(
        PlayerPoints.Decrement(move.player),
        PlayerPoints.Increment(move.player),
        PlayerPoints.Increment(move.player)
      )
    )