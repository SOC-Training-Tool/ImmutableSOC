package soc.base.actions

import game.{GameAction, NoInput}
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.Transactions.*

case class TradeOutput(
  resourceChanges: List[Gain[Resource] | Lose[Resource]]
)

class TradeAction extends GameAction[TradeMove[Resource], NoInput.type, TradeOutput]:
  def apply(move: TradeMove[Resource], input: NoInput.type): TradeOutput =
    TradeOutput(
      resourceChanges = List(
        Lose(move.player, move.give),
        Lose(move.partner, move.get),
        Gain(move.player, move.get),
        Gain(move.partner, move.give)
      )
    )
