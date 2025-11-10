package soc.base.actions.developmentcards

import game._
import shapeless.{::, HNil}
import soc.base.state.{DevelopmentCardDeck, DevelopmentCardDeckSize}
import soc.base.{BuyDevelopmentCardMoveResult, PerfectInfoBuyDevelopmentCardMoveResult}
import soc.core.DevTransactions.{ImperfectInfoBuyCard, PerfectInfoBuyCard}
import soc.core.DevelopmentCardInventories.DevelopmentCardInventoriesOps
import soc.core.state.ops.{BankInvOps, TurnOps}
import soc.core.state.{Bank, Turn}
import soc.core.{BuyDevelopmentCard, DevelopmentCardInventories, ResourceInventories, Transactions}
import util.DependsOn

class BuyDevelopmentCardAction[Res, ResInv[_], Dev, DevInv[_]](cost: InventorySet[Res, Int])(implicit
    res: ResourceInventories[Res, Transactions.PerfectInfo[Res], ResInv],
    dev: DevelopmentCardInventories[Dev, DevInv],
    buyDev: BuyDevelopmentCard[ImperfectInfoBuyCard[Dev], DevInv[Dev]]
) extends GameAction[BuyDevelopmentCardMoveResult[Dev], DevInv[Dev] :: DevelopmentCardDeckSize :: ResInv[Res] :: Bank[Res] :: Turn :: HNil] {

  override def apply(
      move: BuyDevelopmentCardMoveResult[Dev],
      state: DevInv[Dev] :: DevelopmentCardDeckSize :: ResInv[Res] :: Bank[Res] :: Turn :: HNil
  ): DevInv[Dev] :: DevelopmentCardDeckSize :: ResInv[Res] :: Bank[Res] :: Turn :: HNil = {
    implicit val turnDep = DependsOn[STATE, Turn :: HNil]
    implicit val invDep  = DependsOn[STATE, Bank[Res] :: ResInv[Res] :: HNil]
    state
      .updateWith[DevInv[Dev], DevInv[Dev], STATE](_.buyCard[ImperfectInfoBuyCard[Dev]](move.player, state.turn, ImperfectInfoBuyCard[Dev](move.card)))
      .updateWith[DevelopmentCardDeckSize, DevelopmentCardDeckSize, STATE](d => d.copy(d.size - 1))
      .payToBank(move.player, cost)
  }
}

class PerfectInfoBuyDevelopmentCardAction[Res, ResInv[_], Dev, DevInv[_]](cost: InventorySet[Res, Int])(implicit
    res: ResourceInventories[Res, Transactions.PerfectInfo[Res], ResInv],
    dev: DevelopmentCardInventories[Dev, DevInv],
    buyDev: BuyDevelopmentCard[PerfectInfoBuyCard[Dev], DevInv[Dev]]
) extends GameAction[PerfectInfoBuyDevelopmentCardMoveResult[Dev], DevInv[Dev] :: DevelopmentCardDeck[Dev] :: ResInv[Res] :: Bank[Res] :: Turn :: HNil] {

  override def apply(
      move: PerfectInfoBuyDevelopmentCardMoveResult[Dev],
      state: DevInv[Dev] :: DevelopmentCardDeck[Dev] :: ResInv[Res] :: Bank[Res] :: Turn :: HNil
  ): DevInv[Dev] :: DevelopmentCardDeck[Dev] :: ResInv[Res] :: Bank[Res] :: Turn :: HNil = {
    val dep              = DependsOn.single[STATE]
    implicit val turnDep = dep.innerDependency[Turn :: HNil]
    implicit val invDep  = dep.innerDependency[Bank[Res] :: ResInv[Res] :: HNil]
    state
      .updateWith[DevInv[Dev], DevInv[Dev], STATE](_.buyCard(move.player, state.turn, PerfectInfoBuyCard[Dev](move.card)))
      // TODO: error handling to ensure that the Deck is not empty and
      //  that the move result is the same card as the top of the deck
      .updateWith[DevelopmentCardDeck[Dev], DevelopmentCardDeck[Dev], STATE](deck => deck.copy(cards = deck.cards.tail))
      .payToBank(move.player, cost)
  }
}
