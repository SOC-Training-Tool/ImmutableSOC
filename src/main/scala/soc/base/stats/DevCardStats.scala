package soc.base.stats

import game.Delta
import soc.base.{DevelopmentCard, DevelopmentCards}
import soc.base.BaseGame.PerfectInfoDelta
import soc.core.{DevTransactions, PrivateDevCardInv}

/** Cards each player bought and played (PerfectInfo only — PublicInfo lacks card identity for buys). */
case class DevCardStats(
  bought: Map[Int, Seq[DevelopmentCard]],
  played: Map[Int, Seq[DevelopmentCard]]
):
  def boughtByPlayer(player: Int): Seq[DevelopmentCard] = bought.getOrElse(player, Nil)
  def playedByPlayer(player: Int): Seq[DevelopmentCard] = played.getOrElse(player, Nil)

object DevCardStats:
  val empty: DevCardStats = DevCardStats(Map.empty, Map.empty)

  def update(stats: DevCardStats, deltas: List[PerfectInfoDelta]): DevCardStats =
    deltas.foldLeft(stats): (s, d) =>
      d match
        case d: Delta[PrivateDevCardInv[DevelopmentCard]] => d.rawValue match
          case DevTransactions.PerfectInfoBuyCard(card, player, _) =>
            s.copy(bought = s.bought.updatedWith(player)(v => Some(v.getOrElse(Nil) :+ card.asInstanceOf[DevelopmentCard])))
          case DevTransactions.PlayCard(card, player, _) =>
            s.copy(played = s.played.updatedWith(player)(v => Some(v.getOrElse(Nil) :+ card.asInstanceOf[DevelopmentCard])))
          case _ => s
        case _ => s
