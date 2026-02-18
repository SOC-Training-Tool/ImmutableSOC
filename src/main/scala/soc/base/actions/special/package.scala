package soc.base.actions

import game.Delta.DeltaGen
import game.{Delta, DeltaList, :+:, CNil}
import soc.base.state.SpecialPlayer
import soc.core.state.PlayerPoints

package object special {

  def updatedSpecialPlayer(minCount: Int, currentSpecialPlayer: Option[Int], updatedPlayerCounts: Map[Int, Int]): Option[Int] = {
    updatedPlayerCounts.toSeq
      .groupBy(_._2)
      .view
      .mapValues(_.map(_._1).toList)
      .maxByOption(_._1)
      .flatMap {
        case (length, _) if length < minCount => None
        case (_, p :: Nil)                    => Some(p)
        case (_, players)                     => currentSpecialPlayer.filter(players.contains)
      }
  }

  def specialPlayerDelta[SP](currentSpecialPlayer: Option[Int], updatedSpecialPlayer: Option[Int])(using pGen: DeltaGen[SP, SpecialPlayer.Delta]): DeltaList[Delta[PlayerPoints] :+: Delta[SP] :+: CNil] = {
    (currentSpecialPlayer, updatedSpecialPlayer) match {
      case (None, None)       =>
        DeltaList()
          .add[SP]()
          .add[PlayerPoints]()
      case (None, Some(p))    =>
        DeltaList()
          .add[SP](SpecialPlayer.Set(p))
          .add[PlayerPoints](PlayerPoints.Increment(p), PlayerPoints.Increment(p))
      case (Some(p), None)    =>
        DeltaList()
          .add[SP](SpecialPlayer.Set(p))
          .add[PlayerPoints](PlayerPoints.Increment(p), PlayerPoints.Increment(p))
      case (Some(o), Some(n)) if o == n =>
        DeltaList()
          .add[SP]()
          .add[PlayerPoints]()
      case (Some(o), Some(n)) =>
        DeltaList()
          .add[SP](SpecialPlayer.Remove)
          .add[PlayerPoints](PlayerPoints.Decrement(o), PlayerPoints.Decrement(o))
          .add[SP](SpecialPlayer.Set(n))
          .add[PlayerPoints](PlayerPoints.Increment(n), PlayerPoints.Increment(n))
    }

  }
}
