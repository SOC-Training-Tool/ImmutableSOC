package soc.core

import game.{:+:, CNil, Inl, Inr, GameState}
import soc.core.DevTransactions.{ImperfectInfoBuyCard, PerfectInfoBuyCard, PlayCard}

trait BuyDevelopmentCard[T, Inv] {
  def apply(t: Inv, player: Int, turn: Int, transaction: T): Inv
}

object DevTransactions {

  case class PerfectInfoBuyCard[Card](card: Card, player: Int, turn: Int)

  case class ImperfectInfoBuyCard[Card](card: Option[Card], player: Int, turn: Int)

  case class PlayCard[Card](card: Card, player: Int, turn: Int)
}

case class PublicDevCardInv[Card](m: Map[Int, Int]) extends GameState[PublicDevCardInv[Card]] {
  override type Delta = ImperfectInfoBuyCard[Card] :+: PlayCard[Card] :+: CNil

  override def apply(delta: Delta): PublicDevCardInv[Card] = PublicDevCardInv[Card](delta match {
    case Inl(b) => m + (b.player -> (m.getOrElse(b.player, 0) + 1))
    case Inr(Inl(p)) => m + (p.player -> m.get(p.player).fold(0)(_ - 1))
    case Inr(Inr(cnil)) => cnil.impossible
  })
}

case class PrivateDevCardInv[Card](m: Map[Int, Seq[(Card, Int)]]) extends GameState[PrivateDevCardInv[Card]] {
  override type Delta = PerfectInfoBuyCard[Card] :+: PlayCard[Card] :+: CNil

  override def apply(delta: Delta): PrivateDevCardInv[Card] = PrivateDevCardInv[Card](delta match {
    case Inl(b) => m + (b.player -> m.get(b.player).fold[Seq[(Card, Int)]](Nil)(_ :+ (b.card, b.turn)))
    case Inr(Inl(p)) => m + (p.player -> m
      .get(p.player)
      .map {
        _.sortWith { case ((c1, t1), (c2, t2)) =>
          if (c1 == c2) t1 < t2 else c1 == p.card
        }.drop(1)
      }
      .getOrElse(Nil))
    case Inr(Inr(cnil)) => cnil.impossible
  })
}

object DevelopmentCardInventories
