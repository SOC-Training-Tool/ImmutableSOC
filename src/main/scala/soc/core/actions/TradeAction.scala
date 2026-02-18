package soc.core.actions

import game.Delta.DeltaGen
import game.{Delta, DeltaList, GameAction, GameState, :+:, CNil}
import soc.core.TradeMove
import soc.core.Transactions.{Gain, Lose, PerfectInfo}

object TradeAction {

  def apply[II, INV[_]]()(using gen: DeltaGen[INV[II], PerfectInfo[II]]): GameAction[TradeMove[II], EmptyTuple, Delta[INV[II]] :+: CNil] =
    GameAction.apply[TradeMove[II]] { move =>
      DeltaList()
      .add[INV[II]](Lose(move.player, move.give))
      .add[INV[II]](Lose(move.partner, move.get))
      .add[INV[II]](Gain(move.player, move.get))
      .add[INV[II]](Gain(move.partner, move.give))
      .toList
  }

}
