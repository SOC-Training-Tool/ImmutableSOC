package soc.base.actions.developmentcards

import game.{Delta, DeltaList, GameAction}
import shapeless.ops.coproduct
import shapeless.{:+:, CNil, Coproduct, HNil}
import soc.base.PlayRoadBuilderMove
import soc.core.Road
import soc.core.state.{BoardBuildingState, EdgeBuildingState}

object PlayRoadBuilderAction {

  def apply[EB <: Coproduct]()(implicit inject: coproduct.Inject[EB, Road.type]): GameAction[PlayRoadBuilderMove, HNil, Delta[EdgeBuildingState[EB]] :+: CNil] =
    GameAction.apply[PlayRoadBuilderMove] { move =>
      val roads = List(Some(move.edge1), move.edge2).flatten
      DeltaList()
        .add[EdgeBuildingState[EB]](roads.map(r => BoardBuildingState.add(r, Road, move.player)): _*)
        .toList
    }
}