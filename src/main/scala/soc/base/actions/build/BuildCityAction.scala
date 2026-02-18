package soc.base.actions.build

import game.Delta.DeltaGen
import game.{DeltaList, GameAction, GameState, InventorySet, CoproductInject}
import soc.core.Transactions.PerfectInfo
import soc.core.state.{Bank, BoardBuildingState, PlayerPoints, VertexBuildingState}
import soc.core.{BuildCityMove, City, Transactions}

object BuildCityAction {

  def apply[II, Inv[_], VB](cost: InventorySet[II, Int])(using gen: DeltaGen[Inv[II], PerfectInfo[II]], inject: CoproductInject[VB, City.type]) =
    GameAction.apply[BuildCityMove] { move =>
      DeltaList()
        .add[VertexBuildingState[VB]](BoardBuildingState.RemoveBuilding(move.vertex))
        .add[Inv[II]](Transactions.Lose(move.player, cost))
        .add[Bank[II]](Bank.Add(cost))
        .add[PlayerPoints](PlayerPoints.Decrement(move.player))
        .add[VertexBuildingState[VB]](BoardBuildingState.add(move.vertex, City, move.player))
        .add[PlayerPoints](PlayerPoints.Increment(move.player), PlayerPoints.Increment(move.player))
        .toList
    }
}
