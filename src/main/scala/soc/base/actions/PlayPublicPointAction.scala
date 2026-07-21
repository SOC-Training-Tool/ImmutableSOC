package soc.base.actions

import game.GameAction
import soc.base.DevelopmentCards.DevelopmentCard
import soc.base.*
import soc.base.state.*
import soc.core.*
import soc.core.DevTransactions.*
import soc.core.state.*

case class PlayPublicPointOutput(
  pointGained: PlayerPoints#Delta,
  cardPlayed:  PlayCard[DevelopmentCard]
)

class PlayPublicPointAction extends GameAction[PlayPointMove, TurnInput, PlayPublicPointOutput]:
  def apply(move: PlayPointMove, input: TurnInput): PlayPublicPointOutput =
    PlayPublicPointOutput(
      pointGained = PlayerPoints.Increment(move.player),
      cardPlayed  = PlayCard(DevelopmentCards.POINT, move.player, input.turn.t)
    )
