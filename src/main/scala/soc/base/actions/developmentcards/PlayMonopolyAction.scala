package soc.base.actions.developmentcards

import game.Delta.DeltaGen
import game.{Delta, DeltaList, GameAction, InventorySet, :+:, CNil}
import soc.base.PlayMonopolyMoveResult
import soc.core.Transactions
import soc.core.Transactions.{Gain, Lose}

object PlayMonopolyAction {

  def apply[II, Inv[_]]()(using gen: DeltaGen[Inv[II], Transactions.PerfectInfo[II]]): GameAction[PlayMonopolyMoveResult[II], EmptyTuple, Delta[Inv[II]] :+: CNil] =
    GameAction.apply[PlayMonopolyMoveResult[II]] { move =>
      val lose         = move.cardsLost.map { case (p, cards) =>
        val set = InventorySet.fromMap(Map(move.res -> cards))
        Lose(p, set)
      }.toList
      val totalLost    = InventorySet.fromMap(Map(move.res -> move.cardsLost.values.sum))
      DeltaList()
        .add[Inv[II]](lose*)
        .add[Inv[II]](Gain(move.player, totalLost))
        .toList
    }
}
