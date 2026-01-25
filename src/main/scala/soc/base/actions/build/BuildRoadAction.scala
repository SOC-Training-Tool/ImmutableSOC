package soc.base.actions.build

import game.Delta.DeltaGen
import game.{Delta, DeltaList, GameAction, GameState, InventorySet}
import shapeless.{:+:, CNil, Coproduct, HNil}
import shapeless.ops.coproduct
import soc.core.Transactions.PerfectInfo
import soc.core.state.{Bank, BoardBuildingState, EdgeBuildingState}
import soc.core.{BuildRoadMove, Road, Transactions}

object BuildRoadAction {

  def apply[II, Inv[_], EB <: Coproduct](cost: InventorySet[II, Int])(implicit gen: DeltaGen[Inv[II], PerfectInfo[II]], inject: coproduct.Inject[EB, Road.type]): GameAction[BuildRoadMove, HNil, Delta[EdgeBuildingState[EB]] :+: Delta[Bank[II]] :+: Delta[Inv[II]] :+: CNil] =
    GameAction.apply[BuildRoadMove] { move =>
      DeltaList()
        .add[Inv[II]](Transactions.Lose(move.player, cost))
        .add[Bank[II]](Bank.Add(cost))
        .add[EdgeBuildingState[EB]](BoardBuildingState.add(move.edge, Road, move.player))
        .toList
    }
}