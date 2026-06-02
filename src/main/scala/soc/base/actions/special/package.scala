package soc.base.actions

package object special:

  /** Given current player counts for a special role (longest road / largest army),
   *  returns the player who should hold it, or None if no one qualifies. */
  def updatedSpecialPlayer(minCount: Int, currentSpecialPlayer: Option[Int], updatedPlayerCounts: Map[Int, Int]): Option[Int] =
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
