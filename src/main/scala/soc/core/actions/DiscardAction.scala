package soc.core.actions

import game.Delta.DeltaGen
import game.{Delta, DeltaList, GameAction, GameState, :+:, CNil}
import soc.core.Transactions.PerfectInfo
import soc.core.state.Bank
import soc.core.{DiscardMove, Transactions}

object DiscardAction {

  def apply[II, INV[_]]()(using deltaGen: DeltaGen[INV[II], PerfectInfo[II]]): GameAction[DiscardMove[II], EmptyTuple, Delta[INV[II]] :+: Delta[Bank[II]] :+: CNil] =
    GameAction.apply[DiscardMove[II]] { move =>
      DeltaList()
        .add[Bank[II]](Bank.Add(move.set))
        .add[INV[II]](Transactions.Lose(move.player, move.set))
        .toList
    }

}
