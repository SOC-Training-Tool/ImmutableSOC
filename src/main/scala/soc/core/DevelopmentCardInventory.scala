package soc.core

import game.GameState

object DevTransactions:
  case class PerfectInfoBuyCard[Card](card: Card, player: Int, turn: Int)
  case class ImperfectInfoBuyCard[Card](card: Option[Card], player: Int, turn: Int)
  case class PlayCard[Card](card: Card, player: Int, turn: Int)

case class PublicDevCardInv[Card](m: Map[Int, Int]) extends GameState[PublicDevCardInv[Card]]:
  override type Delta = DevTransactions.ImperfectInfoBuyCard[Card] | DevTransactions.PlayCard[Card]
  override def apply(delta: Delta): PublicDevCardInv[Card] = delta match
    case DevTransactions.ImperfectInfoBuyCard(_, player, _) =>
      PublicDevCardInv(m + (player -> (m.getOrElse(player, 0) + 1)))
    case DevTransactions.PlayCard(_, player, _) =>
      PublicDevCardInv(m + (player -> m.get(player).fold(0)(_ - 1)))

case class PrivateDevCardInv[Card](m: Map[Int, Seq[(Card, Int)]]) extends GameState[PrivateDevCardInv[Card]]:
  override type Delta = DevTransactions.PerfectInfoBuyCard[Card] | DevTransactions.PlayCard[Card]
  override def apply(delta: Delta): PrivateDevCardInv[Card] = delta match
    case DevTransactions.PerfectInfoBuyCard(card, player, turn) =>
      PrivateDevCardInv(m + (player -> m.get(player).fold[Seq[(Card, Int)]](Nil)(_ :+ (card, turn))))
    case DevTransactions.PlayCard(card, player, _) =>
      PrivateDevCardInv(m + (player -> m
        .get(player)
        .map {
          _.sortWith { case ((c1, t1), (c2, t2)) =>
            if (c1 == c2) t1 < t2 else c1 == card
          }.drop(1)
        }
        .getOrElse(Nil)))
