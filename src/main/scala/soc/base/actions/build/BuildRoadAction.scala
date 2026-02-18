package soc.base.actions.build

import game.Delta.DeltaGen
import game.{Delta, DeltaList, GameAction, GameState, InventorySet, :+:, CNil, CoproductInject}
import soc.core.Transactions.PerfectInfo
import soc.core.state.{Bank, BoardBuildingState, EdgeBuildingState}
import soc.core.{BuildRoadMove, Road, Transactions}

object BuildRoadAction {

  def apply[II, Inv[_], EB](cost: InventorySet[II, Int])(using gen: DeltaGen[Inv[II], PerfectInfo[II]], inject: CoproductInject[EB, Road.type]): GameAction[BuildRoadMove, EmptyTuple, Delta[EdgeBuildingState[EB]] :+: Delta[Bank[II]] :+: Delta[Inv[II]] :+: CNil] =
    GameAction.apply[BuildRoadMove] { move =>
      DeltaList()
        .add[Inv[II]](Transactions.Lose(move.player, cost))
        .add[Bank[II]](Bank.Add(cost))
        .add[EdgeBuildingState[EB]](BoardBuildingState.add(move.edge, Road, move.player))
        .toList
    }
}
