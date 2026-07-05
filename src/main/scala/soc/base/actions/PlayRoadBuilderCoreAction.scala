package soc.base.actions

import game.GameAction
import soc.base.DevelopmentCards.DevelopmentCard
import soc.base.*
import soc.core.*
import soc.core.DevTransactions.*
import soc.core.state.*
import soc.core.state.BoardBuildingState.*

case class PlayRoadBuilderCoreOutput(
  addedRoads: List[EdgeBuildingState[BaseEdgeBuilding]#Delta],
  cardPlayed: PlayCard[DevelopmentCard]
)

class PlayRoadBuilderCoreAction extends GameAction[PlayRoadBuilderMove, TurnInput, PlayRoadBuilderCoreOutput]:
  def apply(move: PlayRoadBuilderMove, input: TurnInput): PlayRoadBuilderCoreOutput =
    val roads   = List(Some(move.edge1), move.edge2).flatten
    val edgeDlt = roads.map(r => BoardBuildingState.add(r, Road, move.player))
    PlayRoadBuilderCoreOutput(
      addedRoads = edgeDlt,
      cardPlayed = PlayCard(DevelopmentCards.ROAD_BUILDER, move.player, input.turn.t)
    )
