package soc.core.actions

import game.Delta.DeltaGen
import game.{Delta, DeltaList, GameAction, GameState}
import shapeless.{:+:, CNil, HNil}
import soc.core.TradeMove
import soc.core.Transactions.{Gain, Lose, PerfectInfo}

object TradeAction {

  def apply[II, INV[_] <: GameState[INV[II]]]()(implicit gen: DeltaGen[INV[II], PerfectInfo[II]]): GameAction[TradeMove[II], HNil, Delta[INV[II]] :+: CNil] =
    GameAction.apply[TradeMove[II]] { move =>
      DeltaList()
      .add[INV[II]](Lose(move.player, move.give))
      .add[INV[II]](Lose(move.partner, move.get))
      .add[INV[II]](Gain(move.player, move.get))
      .add[INV[II]](Gain(move.partner, move.give))
      .toList
  }

}
