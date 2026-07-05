package soc.base.actions

import game.GameAction
import soc.base.DevelopmentCards.DevelopmentCard
import soc.base.*
import soc.core.*
import soc.core.DevTransactions.*
import soc.core.state.*

case class PlayPointOutput(
  pointGained: PlayerPoints#Delta,
  cardPlayed:  PlayCard[DevelopmentCard]
)

class PlayPointAction extends GameAction[PlayPointMove, TurnInput, PlayPointOutput]:
  def apply(move: PlayPointMove, input: TurnInput): PlayPointOutput =
    PlayPointOutput(
      pointGained = PlayerPoints.Increment(move.player),
      cardPlayed  = PlayCard(DevelopmentCards.POINT, move.player, input.turn.t)
    )
