package soc.base.actions

import game.{GameAction, InventorySet}
import soc.base.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.SOCBoard.SOCBoardOps
import soc.core.Transactions.*
import soc.core.state.*
import soc.core.state.BoardBuildingState.*

case class InitialPlacementInput(
  playerPoints:        PlayerPoints,
  setupPlacementOrder: SetupPlacementOrder,
  board:               BaseBoard[Resource],
  vertexBuildingState: VertexBuildingState[BaseVertexBuilding],
  edgeBuildingState:   EdgeBuildingState[BaseEdgeBuilding]
)

case class InitialPlacementCoreOutput(
  addedSettlement:     VertexBuildingState[BaseVertexBuilding]#Delta,
  addedRoad:           EdgeBuildingState[BaseEdgeBuilding]#Delta,
  pointGained:         PlayerPoints#Delta,
  resourceGains:       List[Gain[Resource]],
  bankLost:            List[Bank[Resource]#Delta],
  setupPlacementOrder: SetupPlacementOrder.Placement
)

class InitialPlacementCoreAction extends GameAction[InitialPlacementMove, InitialPlacementInput, InitialPlacementCoreOutput]:
  def apply(move: InitialPlacementMove, input: InitialPlacementInput): InitialPlacementCoreOutput =
    val placementCount = input.setupPlacementOrder.placements.length
    val numPlayers = input.playerPoints.points.keys.size match
      case 0 => placementCount + 1
      case n => n
    val isSecondRound = placementCount >= numPlayers
    val (resourceGains, bankLost) = if isSecondRound then
      val resources = input.board.hexesForVertex
        .getOrElse(move.vertex, Nil)
        .flatMap(_.hex.getResource)
      val inv = InventorySet.fromList(resources)
      if inv.getTotal == 0 then (Nil, Nil)
      else (List(Gain(move.player, inv)), List(Bank.Take(inv)))
    else (Nil, Nil)
    InitialPlacementCoreOutput(
      addedSettlement     = BoardBuildingState.add(move.vertex, Settlement, move.player),
      addedRoad           = BoardBuildingState.add(move.edge, Road, move.player),
      pointGained         = PlayerPoints.Increment(move.player),
      resourceGains       = resourceGains,
      bankLost            = bankLost,
      setupPlacementOrder = SetupPlacementOrder.Placement(move.player, move.vertex)
    )
