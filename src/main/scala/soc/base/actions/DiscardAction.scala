package soc.base.actions

import game.{GameAction, NoInput}
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.Transactions.*
import soc.core.state.*

case class DiscardOutput(
  playerLost: Lose[Resource],
  bankGained: Bank[Resource]#Delta
)

class DiscardAction extends GameAction[DiscardMove[Resource], NoInput.type, DiscardOutput]:
  def apply(move: DiscardMove[Resource], input: NoInput.type): DiscardOutput =
    DiscardOutput(
      playerLost = Lose(move.player, move.set),
      bankGained = Bank.Add(move.set)
    )
