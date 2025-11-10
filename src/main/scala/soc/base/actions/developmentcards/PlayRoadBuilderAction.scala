package soc.base.actions.developmentcards

import game.GameAction
import shapeless.ops.coproduct
import shapeless.{:+:, ::, CNil, Coproduct, HNil}
import soc.base.{PlayRoadBuilderMove, RoadBuilder}
import soc.base.state.ops.BuildRoadStateOps
import soc.core.state.{EdgeBuildingState, Turn}
import soc.core.{DevelopmentCardInventories, Road}
import util.DependsOn
import util.opext.Embedder

class PlayRoadBuilderAction[EB <: Coproduct](implicit roadEmbedder: Embedder[EB, Road.type :+: CNil]) extends GameAction[PlayRoadBuilderMove, EdgeBuildingState[EB] :: HNil] {

  override def apply(move: PlayRoadBuilderMove, state: EdgeBuildingState[EB] :: HNil): EdgeBuildingState[EB] :: HNil = {
    implicit val dep = DependsOn.single[EdgeBuildingState[EB] :: HNil]
    val result       = state.addRoad(move.edge1, move.player)
    move.edge2.fold(result)(e => result.addRoad(e, move.player))
  }
}
