package soc.core

import game.GameState
import shapeless.{:+:, CNil, Poly2}
import soc.core.DevTransactions.{ImperfectInfoBuyCard, PerfectInfoBuyCard, PlayCard}
import soc.core.DevelopmentCardInventories.{PrivateDevInvPoly, PublicDevInvPoly}

trait BuyDevelopmentCard[T, Inv] {
  def apply(t: Inv, player: Int, turn: Int, transaction: T): Inv
}

object DevTransactions {

  case class PerfectInfoBuyCard[Card](card: Card, player: Int, turn: Int)

  case class ImperfectInfoBuyCard[Card](card: Option[Card], player: Int, turn: Int)

  case class PlayCard[Card](card: Card, player: Int, turn: Int)
}

//trait DevelopmentCardInventories[Card, T[_]] {
//
//  def numCards(t: T[Card], player: Int): Int
//
//  def playCard(t: T[Card], player: Int, card: Card): T[Card]
//
//  def buyCard[Transaction](t: T[Card], player: Int, turn: Int, transaction: Transaction)(implicit devTransactions: BuyDevelopmentCard[Transaction, T[Card]]): T[Card] = {
//    devTransactions.apply(t, player, turn, transaction)
//  }
//}

case class PublicDevCardInv[Card](m: Map[Int, Int]) extends GameState[PublicDevCardInv[Card]] {
  override type Delta = ImperfectInfoBuyCard[Card] :+: PlayCard[Card] :+: CNil

  override def apply(delta: Delta): PublicDevCardInv[Card] = PublicDevCardInv[Card](delta.foldLeft(m)(PublicDevInvPoly))
}

case class PrivateDevCardInv[Card](m: Map[Int, Seq[(Card, Int)]]) extends GameState[PrivateDevCardInv[Card]] {
  override type Delta = PerfectInfoBuyCard[Card] :+: PlayCard[Card] :+: CNil

  override def apply(delta: Delta): PrivateDevCardInv[Card] = PrivateDevCardInv[Card](delta.foldLeft(m)(PrivateDevInvPoly))
}


object DevelopmentCardInventories {

  //type PublicDevelopmentCards[_]     = PublicDevCardInv
  //type PrivateDevelopmentCards[Card] = Map[Int, Seq[(Card, Int)]]

  //  implicit class DevelopmentCardInventoriesOps[Card, T[_]](inv: T[Card])(implicit ev: DevelopmentCardInventories[Card, T]) {
  //
  //    def numCards(player: Int): Int = ev.numCards(inv, player)
  //
  //    def playCard(player: Int, card: Card): T[Card] = ev.playCard(inv, player, card)
  //
  //    def buyCard[Transaction](player: Int, turn: Int, transaction: Transaction)(implicit devTransactions: BuyDevelopmentCard[Transaction, T[Card]]) = {
  //      ev.buyCard(inv, player, turn, transaction)
  //    }
  //  }

  object PublicDevInvPoly extends Poly2 {

    implicit def buy[Card]: Case.Aux[Map[Int, Int], ImperfectInfoBuyCard[Card], Map[Int, Int]] =
      at[Map[Int, Int], ImperfectInfoBuyCard[Card]] { case (m, b) => m + (b.player -> (m.getOrElse(b.player, 0) + 1)) }

    implicit def play[Card]: Case.Aux[Map[Int, Int], PlayCard[Card], Map[Int, Int]] =
      at[Map[Int, Int], PlayCard[Card]] { case (m, p) => m + (p.player -> m.get(p.player).fold(0)(_ - 1)) }

  }

  object PrivateDevInvPoly extends Poly2 {

    type Inv[Card] = Map[Int, Seq[(Card, Int)]]

    implicit def buy[Card]: Case.Aux[Inv[Card], PerfectInfoBuyCard[Card], Inv[Card]] =
      at[Inv[Card], PerfectInfoBuyCard[Card]] { case (m, b) => m + (b.player -> m.get(b.player).fold[Seq[(Card, Int)]](Nil)(_ :+ (b.card, b.turn))) }

    implicit def play[Card]: Case.Aux[Inv[Card], PlayCard[Card], Inv[Card]] =
      at[Inv[Card], PlayCard[Card]] { case (m, p) => m + (p.player -> m
        .get(p.player)
        .map {
          _.sortWith { case ((c1, t1), (c2, t2)) =>
            if (c1 == c2) t1 < t2 else c1 == p.card
          }.drop(1)
        }
        .getOrElse(Nil))
      }
  }
}
