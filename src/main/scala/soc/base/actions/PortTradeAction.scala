package soc.base.actions

import game.{GameAction, NoInput}
import soc.base.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.Transactions.*
import soc.core.state.*

case class PortTradeOutput(
  resourceChanges: List[Gain[Resource] | Lose[Resource]],
  bankChanges:     List[Bank[Resource]#Delta]
)

class PortTradeAction extends GameAction[PortTradeMove[Resource], NoInput.type, PortTradeOutput]:
  def apply(move: PortTradeMove[Resource], input: NoInput.type): PortTradeOutput =
    PortTradeOutput(
      resourceChanges = List(
        Lose(move.player, move.give),
        Gain(move.player, move.get)
      ),
      bankChanges = List(
        Bank.Add(move.give),
        Bank.Take(move.get)
      )
    )
