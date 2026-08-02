package soc.base.actions

import game.GameAction
import soc.base.DevelopmentCards.DevelopmentCard
import soc.base.*
import soc.base.state.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.Transactions.*
import soc.core.DevTransactions.*
import soc.core.state.*

case class PublicBuyDevCardOutput(
  resourcesSpent:    Lose[Resource],
  resourcesReturned: Bank[Resource]#Delta,
  cardBought:        PublicDevCardInv[DevelopmentCard]#Delta,
  deckShrunk:        DevelopmentCardDeckSize#Delta
)

class PublicBuyDevCardAction extends GameAction[BuyDevelopmentCardMoveResult[DevelopmentCard], TurnInput, PublicBuyDevCardOutput]:
  def apply(move: BuyDevelopmentCardMoveResult[DevelopmentCard], input: TurnInput): PublicBuyDevCardOutput =
    PublicBuyDevCardOutput(
      resourcesSpent    = Lose(move.player, DEV_CARD_COST),
      resourcesReturned = Bank.Add(DEV_CARD_COST),
      cardBought        = ImperfectInfoBuyCard(move.card, move.player, input.turn.number),
      deckShrunk        = DevelopmentCardDeck.Remove
    )
