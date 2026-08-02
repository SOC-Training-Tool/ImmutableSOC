package soc.base.actions

import game.GameAction
import soc.base.DevelopmentCards.DevelopmentCard
import soc.base.*
import soc.core.*
import soc.core.DevTransactions.*
import soc.core.state.*

case class PlayPerfectPointOutput(
  cardPlayed: PlayCard[DevelopmentCard]
)

class PlayPerfectPointAction extends GameAction[PlayPointMove, TurnInput, PlayPerfectPointOutput]:
  def apply(move: PlayPointMove, input: TurnInput): PlayPerfectPointOutput =
    PlayPerfectPointOutput(
      cardPlayed = PlayCard(DevelopmentCards.POINT, move.player, input.turn.number)
    )
