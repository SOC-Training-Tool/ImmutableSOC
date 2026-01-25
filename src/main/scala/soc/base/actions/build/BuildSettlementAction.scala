package soc.base.actions.build

import game.Delta.DeltaGen
import game.{Delta, DeltaList, GameAction, GameState, InventorySet}
import shapeless.{:+:, CNil, Coproduct, HNil}
import shapeless.ops.coproduct
import soc.core.state.{Bank, BoardBuildingState, PlayerPoints, VertexBuildingState}
import soc.core.{BuildSettlementMove, Settlement, Transactions, Vertex}

object BuildSettlementAction {

  def apply[II, Inv[_], VB <: Coproduct](cost: InventorySet[II, Int])(implicit gen: DeltaGen[Inv[II], Transactions.PerfectInfo[II]], inject: coproduct.Inject[VB, Settlement.type]): GameAction[BuildSettlementMove, HNil, Delta[PlayerPoints] :+: Delta[Bank[II]] :+: Delta[Inv[II]] :+: Delta[VertexBuildingState[VB]] :+: CNil] =
    GameAction.apply[BuildSettlementMove] { move =>
      DeltaList()
        .add[VertexBuildingState[VB]](BoardBuildingState.add(move.vertex, Settlement, move.player))
        .add[Inv[II]](Transactions.Lose(move.player, cost))
        .add[Bank[II]](Bank.Add(cost))
        .add[PlayerPoints](PlayerPoints.Increment(move.player))
        .toList
    }

}
