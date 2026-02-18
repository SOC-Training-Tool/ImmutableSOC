package soc.base.actions.developmentcards

import game.{Delta, DeltaList, GameAction, :+:, CNil, CoproductInject}
import soc.base.PlayRoadBuilderMove
import soc.core.Road
import soc.core.state.{BoardBuildingState, EdgeBuildingState}

object PlayRoadBuilderAction {

  def apply[EB]()(using inject: CoproductInject[EB, Road.type]): GameAction[PlayRoadBuilderMove, EmptyTuple, Delta[EdgeBuildingState[EB]] :+: CNil] =
    GameAction.apply[PlayRoadBuilderMove] { move =>
      val roads = List(Some(move.edge1), move.edge2).flatten
      DeltaList()
        .add[EdgeBuildingState[EB]](roads.map(r => BoardBuildingState.add(r, Road, move.player))*)
        .toList
    }
}
