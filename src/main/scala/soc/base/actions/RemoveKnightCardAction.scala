package soc.base.actions

import game.GameAction
import soc.base.DevelopmentCards.*
import soc.base.{Knight}
import soc.core.*
import soc.core.DevTransactions.*

case class RemoveKnightCardOutput(
  cardPlayed: PlayCard[DevelopmentCard]
)

class RemoveKnightCardAction extends GameAction[Int, TurnInput, RemoveKnightCardOutput]:
  def apply(player: Int, input: TurnInput): RemoveKnightCardOutput =
    RemoveKnightCardOutput(
      cardPlayed = PlayCard(Knight, player, input.turn.number)
    )
