package soc.base.actions.developmentcards

import game.Delta.DeltaGen
import game.{Delta, DeltaList, GameAction, InventorySet, :+:, CNil}
import soc.base.PlayYearOfPlentyMove
import soc.core.Transactions.{Gain, PerfectInfo}
import soc.core.state.Bank

object PlayYearOfPlentyAction {

  def apply[II, Inv[_]]()(using gen: DeltaGen[Inv[II], PerfectInfo[II]]): GameAction[PlayYearOfPlentyMove[II], EmptyTuple, Delta[Inv[II]] :+: Delta[Bank[II]] :+: CNil] = {
    GameAction.apply[PlayYearOfPlentyMove[II]] { move =>
      val inv = InventorySet.fromList(List(move.c1, move.c2))
      DeltaList()
        .add[Bank[II]](Bank.Take(inv))
        .add[Inv[II]](Gain(move.player, inv))
        .toList
    }
  }
}
