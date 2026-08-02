package soc.base.actions

import game.GameAction
import soc.base.DevelopmentCards.{DevelopmentCard, POINT}
import soc.base.*
import soc.base.state.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.Transactions.*
import soc.core.DevTransactions.*
import soc.core.state.*

case class PerfectBuyDevCardOutput(
  resourcesSpent:    Lose[Resource],
  resourcesReturned: Bank[Resource]#Delta,
  cardBought:        PrivateDevCardInv[DevelopmentCard]#Delta,
  deckShrunk:        DevelopmentCardDeck[DevelopmentCard]#Delta,
  bonusPoint:        Option[PlayerPoints#Delta]
)

class PerfectBuyDevCardAction extends GameAction[PerfectInfoBuyDevelopmentCardMoveResult[DevelopmentCard], TurnInput, PerfectBuyDevCardOutput]:
  def apply(move: PerfectInfoBuyDevelopmentCardMoveResult[DevelopmentCard], input: TurnInput): PerfectBuyDevCardOutput =
    PerfectBuyDevCardOutput(
      resourcesSpent    = Lose(move.player, DEV_CARD_COST),
      resourcesReturned = Bank.Add(DEV_CARD_COST),
      cardBought        = PerfectInfoBuyCard(move.card, move.player, input.turn.number),
      deckShrunk        = DevelopmentCardDeck.Remove,
      bonusPoint        = if move.card == POINT then Some(PlayerPoints.Increment(move.player)) else None
    )
