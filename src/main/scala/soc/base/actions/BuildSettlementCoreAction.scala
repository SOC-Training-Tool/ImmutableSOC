package soc.base.actions

import game.{GameAction, NoInput}
import soc.base.*
import soc.base.state.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.Transactions.*
import soc.core.state.*
import soc.core.state.BoardBuildingState.*

case class BuildSettlementInput(
  board: BaseBoard[Resource],
  socRoadLengths: SOCRoadLengths,
  socLongestRoadPlayer: SOCLongestRoadPlayer,
  vertexBuildingState: VertexBuildingState[BaseVertexBuilding],
  edgeBuildingState: EdgeBuildingState[BaseEdgeBuilding]
) extends LongestRoadState

case class BuildSettlementCoreOutput(
  addedSettlement:           VertexBuildingState[BaseVertexBuilding]#Delta,
  roadLengthChanges:         List[SOCRoadLengths#Delta],
  longestRoadPlayerChanges:  List[SOCLongestRoadPlayer.Delta],
  longestRoadPointChanges:   List[PlayerPoints#Delta],
  resourcesSpent:            Lose[Resource],
  resourcesReturned:         Bank[Resource]#Delta,
  pointGained:               PlayerPoints#Delta
)

class BuildSettlementCoreAction extends GameAction[BuildSettlementMove, BuildSettlementInput, BuildSettlementCoreOutput]:
  def apply(move: BuildSettlementMove, input: BuildSettlementInput): BuildSettlementCoreOutput =
    val updatedVertices: VertexBuildingState[BaseVertexBuilding] =
      VertexBuildingState(input.vertexBuildingState.map + (move.vertex -> PlayerBuilding(move.player, Settlement)))
    val changes = longestRoadChanges(input, updatedVertices, input.edgeBuildingState)
    BuildSettlementCoreOutput(
      addedSettlement          = BoardBuildingState.add(move.vertex, Settlement, move.player),
      roadLengthChanges        = changes.roadLengthChanges,
      longestRoadPlayerChanges = changes.longestRoadPlayerChanges,
      longestRoadPointChanges  = changes.pointChanges,
      resourcesSpent           = Lose(move.player, SETTLEMENT_COST),
      resourcesReturned        = Bank.Add(SETTLEMENT_COST),
      pointGained              = PlayerPoints.Increment(move.player)
    )

  def apply(move: BuildSettlementMove, input: NoInput.type): BuildSettlementCoreOutput =
    BuildSettlementCoreOutput(
      BoardBuildingState.add(move.vertex, Settlement, move.player),
      Nil,
      Nil,
      Nil,
      Lose(move.player, SETTLEMENT_COST),
      Bank.Add(SETTLEMENT_COST),
      PlayerPoints.Increment(move.player)
    )
