package soc.base.actions

import game.GameAction
import soc.base.DevelopmentCards.DevelopmentCard
import soc.base.*
import soc.base.state.*
import soc.core.*
import soc.core.DevTransactions.*
import soc.core.state.*
import soc.core.state.BoardBuildingState.*

case class PlayRoadBuilderInput(
  turn: Turn,
  board: BaseBoard[Resource],
  socRoadLengths: SOCRoadLengths,
  socLongestRoadPlayer: SOCLongestRoadPlayer,
  vertexBuildingState: VertexBuildingState[BaseVertexBuilding],
  edgeBuildingState: EdgeBuildingState[BaseEdgeBuilding]
) extends LongestRoadState

case class PlayRoadBuilderCoreOutput(
  addedRoads:                List[EdgeBuildingState[BaseEdgeBuilding]#Delta],
  roadLengthChanges:         List[SOCRoadLengths#Delta],
  longestRoadPlayerChanges:  List[SOCLongestRoadPlayer.Delta],
  longestRoadPointChanges:   List[PlayerPoints#Delta],
  cardPlayed:                PlayCard[DevelopmentCard]
)

class PlayRoadBuilderCoreAction extends GameAction[PlayRoadBuilderMove, PlayRoadBuilderInput, PlayRoadBuilderCoreOutput]:
  def apply(move: PlayRoadBuilderMove, input: PlayRoadBuilderInput): PlayRoadBuilderCoreOutput =
    val roads   = List(Some(move.edge1), move.edge2).flatten
    val edgeDlt = roads.map(r => BoardBuildingState.add(r, Road, move.player))
    val updatedEdges: EdgeBuildingState[BaseEdgeBuilding] =
      EdgeBuildingState(input.edgeBuildingState.map ++ roads.map(_ -> PlayerBuilding(move.player, Road)))
    val changes = longestRoadChanges(input, input.vertexBuildingState, updatedEdges)
    PlayRoadBuilderCoreOutput(
      addedRoads               = edgeDlt,
      roadLengthChanges        = changes.roadLengthChanges,
      longestRoadPlayerChanges = changes.longestRoadPlayerChanges,
      longestRoadPointChanges  = changes.pointChanges,
      cardPlayed               = PlayCard(DevelopmentCards.ROAD_BUILDER, move.player, input.turn.t)
    )

  def apply(move: PlayRoadBuilderMove, input: TurnInput): PlayRoadBuilderCoreOutput =
    val roads = List(Some(move.edge1), move.edge2).flatten
    PlayRoadBuilderCoreOutput(
      roads.map(edge => BoardBuildingState.add(edge, Road, move.player)),
      Nil,
      Nil,
      Nil,
      PlayCard(DevelopmentCards.ROAD_BUILDER, move.player, input.turn.t)
    )
