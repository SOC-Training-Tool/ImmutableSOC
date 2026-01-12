package soc.base.actions

import game.Delta.DeltaGen
import game.{Delta, DeltaList, GameAction, GameState}
import shapeless.{:+:, CNil, HNil}
import soc.base.PortTradeMove
import soc.core.Transactions
import soc.core.state.Bank

object PortTradeAction {

  def apply[II, Inv[_] <: GameState[Inv[II]]]()(implicit gen: DeltaGen[Inv[II], Transactions.PerfectInfo[II]]): GameAction[PortTradeMove[II], HNil, Delta[Bank[II]] :+: Delta[Inv[II]] :+: CNil] =
    GameAction.apply[PortTradeMove[II]] { move =>
      DeltaList()
        .add[Inv[II]](Transactions.Lose(move.player, move.give))
        .add[Bank[II]](Bank.Add(move.give))
        .add[Bank[II]](Bank.Take(move.get))
        .add[Inv[II]](Transactions.Gain(move.player, move.get))
        .toList
    }

}
