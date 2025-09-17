package soc.base.actions

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

  def updateState[S](currentSpecialPlayer: Option[Int], updatedSpecialPlayer: Option[Int], state: S)(incrementPoint: (S, Int) => S, decrementPoint: (S, Int) => S, update: (S, Option[Int]) => S) = {
    val s = (currentSpecialPlayer, updatedSpecialPlayer) match {
      case (None, None)       => state
      case (None, Some(p))    =>
        incrementPoint(incrementPoint(state, p), p)
      case (Some(p), None)    =>
        decrementPoint(decrementPoint(state, p), p)
      case (Some(o), Some(n)) if o == n =>
        state
      case (Some(o), Some(n)) =>
        decrementPoint(decrementPoint(incrementPoint(incrementPoint(state, n), n), o), o)
    }
    update(s, updatedSpecialPlayer)
  }
}
