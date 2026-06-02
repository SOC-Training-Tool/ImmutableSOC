package soc.base.stats

import soc.core.ResourceSet
import soc.core.ResourceSet.Resources

/** Total resources gained and lost per player across all action types. */
case class ResourceLedger(
  gained: Map[Int, Resources],
  lost:   Map[Int, Resources]
):
  def netGain(player: Int): Resources =
    gained.getOrElse(player, ResourceSet.empty[Int])
      .subtract(lost.getOrElse(player, ResourceSet.empty[Int]))

object ResourceLedger:
  val empty: ResourceLedger = ResourceLedger(Map.empty, Map.empty)

  def update[D: GainDeltas: LoseDeltas](stats: ResourceLedger, deltas: List[D]): ResourceLedger =
    val gainTC = summon[GainDeltas[D]]
    val loseTC = summon[LoseDeltas[D]]
    deltas.foldLeft(stats): (s, d) =>
      val s1 = gainTC.gain(d).fold(s)((player, res) =>
        s.copy(gained = s.gained.updatedWith(player):
          case Some(existing) => Some(existing.add(res))
          case None           => Some(res)
        )
      )
      loseTC.lose(d).fold(s1)((player, res) =>
        s1.copy(lost = s1.lost.updatedWith(player):
          case Some(existing) => Some(existing.add(res))
          case None           => Some(res)
        )
      )
