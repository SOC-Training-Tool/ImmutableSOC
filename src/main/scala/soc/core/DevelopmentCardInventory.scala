package soc.core

import soc.core.DevTransactions.{ImperfectInfoBuyCard, PerfectInfoBuyCard}

trait BuyDevelopmentCard[T, Inv] {
  def apply(t: Inv, player: Int, turn: Int, transaction: T): Inv
}

object DevTransactions {

  case class PerfectInfoBuyCard[Card](card: Card)

  case class ImperfectInfoBuyCard[Card](card: Option[Card])
}

trait DevelopmentCardInventories[Card, T[_]] {

  def numCards(t: T[Card], player: Int): Int

  def playCard(t: T[Card], player: Int, card: Card): T[Card]

  def buyCard[Transaction](t: T[Card], player: Int, turn: Int, transaction: Transaction)(implicit devTransactions: BuyDevelopmentCard[Transaction, T[Card]]): T[Card] = {
    devTransactions.apply(t, player, turn, transaction)
  }
}

case class PublicDevCardInv(m: Map[Int, Int])

object DevelopmentCardInventories {

  type PublicDevelopmentCards[_]     = PublicDevCardInv
  type PrivateDevelopmentCards[Card] = Map[Int, Seq[(Card, Int)]]

  implicit class DevelopmentCardInventoriesOps[Card, T[_]](inv: T[Card])(implicit ev: DevelopmentCardInventories[Card, T]) {

    def numCards(player: Int): Int = ev.numCards(inv, player)

    def playCard(player: Int, card: Card): T[Card] = ev.playCard(inv, player, card)

    def buyCard[Transaction](player: Int, turn: Int, transaction: Transaction)(implicit devTransactions: BuyDevelopmentCard[Transaction, T[Card]]) = {
      ev.buyCard(inv, player, turn, transaction)
    }
  }

  implicit def publicBuyDevCard[Card]: BuyDevelopmentCard[ImperfectInfoBuyCard[Card], PublicDevelopmentCards[Card]] = {
    (t: PublicDevelopmentCards[Card], player: Int, _: Int, _: ImperfectInfoBuyCard[Card]) =>
      {
        PublicDevCardInv(t.m + (player -> (t.m.getOrElse(player, 0) + 1)))
      }
  }

  implicit def publicDevelopmentCardInventories[Card]: DevelopmentCardInventories[Card, PublicDevelopmentCards] = {
    new DevelopmentCardInventories[Card, PublicDevelopmentCards] {
      override def numCards(t: PublicDevelopmentCards[Card], player: Int): Int = t.m.getOrElse(player, 0)

      override def playCard(t: PublicDevelopmentCards[Card], player: Int, card: Card): PublicDevelopmentCards[Card] =
        PublicDevCardInv(t.m + (player -> (numCards(t, player) - 1)))
    }
  }

  implicit def privateBuyDevCard[Card]: BuyDevelopmentCard[PerfectInfoBuyCard[Card], PrivateDevelopmentCards[Card]] = {
    (t: PrivateDevelopmentCards[Card], player: Int, turn: Int, buy: PerfectInfoBuyCard[Card]) =>
      {
        t + (player -> t.get(player).fold[Seq[(Card, Int)]](Nil)(_ :+ (buy.card, turn)))
      }
  }

  implicit def perfectInfoDevelopmentCardInventories[Card]: DevelopmentCardInventories[Card, PrivateDevelopmentCards] =
    new DevelopmentCardInventories[Card, PrivateDevelopmentCards] {
      override def numCards(t: PrivateDevelopmentCards[Card], player: Int): Int =
        t.get(player).fold(0)(_.size)

      override def playCard(m: PrivateDevelopmentCards[Card], player: Int, card: Card): PrivateDevelopmentCards[Card] = {
        m + (player -> m
          .get(player)
          .map {
            _.sortWith { case ((c1, t1), (c2, t2)) =>
              if (c1 == c2) t1 < t2 else c1 == card
            }.drop(1)
          }
          .getOrElse(Nil))
      }
    }
}
