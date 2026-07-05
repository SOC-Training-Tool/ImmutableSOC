package soc.base.actions

import game.{GameAction, NoInput}
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.Transactions.*
import soc.core.state.*
import soc.core.state.BoardBuildingState.*

case class BuildSettlementCoreOutput(
  addedSettlement:   VertexBuildingState[BaseVertexBuilding]#Delta,
  resourcesSpent:    Lose[Resource],
  resourcesReturned: Bank[Resource]#Delta,
  pointGained:       PlayerPoints#Delta
)

class BuildSettlementCoreAction extends GameAction[BuildSettlementMove, NoInput.type, BuildSettlementCoreOutput]:
  def apply(move: BuildSettlementMove, input: NoInput.type): BuildSettlementCoreOutput =
    BuildSettlementCoreOutput(
      addedSettlement   = BoardBuildingState.add(move.vertex, Settlement, move.player),
      resourcesSpent    = Lose(move.player, SETTLEMENT_COST),
      resourcesReturned = Bank.Add(SETTLEMENT_COST),
      pointGained       = PlayerPoints.Increment(move.player)
    )