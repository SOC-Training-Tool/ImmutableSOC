package soc.base.actions.developmentcards

import game.Delta.DeltaGen
import game.{Delta, DeltaList, GameAction, InventorySet, :+:, CNil, tupleSelect}
import soc.base.state.{DevelopmentCardDeck, DevelopmentCardDeckSize}
import soc.base.{BuyDevelopmentCardMoveResult, PerfectInfoBuyDevelopmentCardMoveResult}
import soc.core.DevTransactions.{ImperfectInfoBuyCard, PerfectInfoBuyCard}
import soc.core.Transactions
import soc.core.state.{Bank, Turn}

object BuyDevelopmentCardAction {

  def public[II, Inv[_], Dev, DevInv[_]](cost: InventorySet[II, Int])(using
    invGen: DeltaGen[Inv[II], Transactions.PerfectInfo[II]],
    devGen: DeltaGen[DevInv[Dev], ImperfectInfoBuyCard[Dev]]
  ): GameAction[BuyDevelopmentCardMoveResult[Dev], Turn *: EmptyTuple, Delta[DevelopmentCardDeckSize] :+: Delta[DevInv[Dev]] :+: Delta[Bank[II]] :+: Delta[Inv[II]] :+: CNil] =
    GameAction.fromState[BuyDevelopmentCardMoveResult[Dev], Turn *: EmptyTuple] { case (move, state) =>
      val turn = tupleSelect[Turn *: EmptyTuple, Turn](state).t
      DeltaList()
        .add[Inv[II]](Transactions.Gain(move.player, cost))
        .add[Bank[II]](Bank.Add(cost))
        .add[DevInv[Dev]](ImperfectInfoBuyCard(move.card, move.player, turn))
        .add[DevelopmentCardDeckSize](DevelopmentCardDeck.Remove)
        .toList
    }

  def perfect[II, Inv[_], Dev, DevInv[_]](cost: InventorySet[II, Int])(using
    invGen: DeltaGen[Inv[II], Transactions.PerfectInfo[II]],
    devGen: DeltaGen[DevInv[Dev], PerfectInfoBuyCard[Dev]]
  ): GameAction[PerfectInfoBuyDevelopmentCardMoveResult[Dev], Turn *: EmptyTuple, Delta[DevelopmentCardDeck[Dev]] :+: Delta[DevInv[Dev]] :+: Delta[Bank[II]] :+: Delta[Inv[II]] :+: CNil] =
    GameAction.fromState[PerfectInfoBuyDevelopmentCardMoveResult[Dev], Turn *: EmptyTuple] { case (move, state) =>
      val turn = tupleSelect[Turn *: EmptyTuple, Turn](state).t
      DeltaList()
        .add[Inv[II]](Transactions.Gain(move.player, cost))
        .add[Bank[II]](Bank.Add(cost))
        // TODO: error handling to ensure that the Deck is not empty and
        //  that the move result is the same card as the top of the deck
        .add[DevInv[Dev]](PerfectInfoBuyCard(move.card, move.player, turn))
        .add[DevelopmentCardDeck[Dev]](DevelopmentCardDeck.Remove)
        .toList
    }

}
