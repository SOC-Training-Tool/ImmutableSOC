package soc.base.stats

import game.Delta
import soc.base.BaseGame.{BaseEdgeBuilding, BaseVertexBuilding, PerfectInfoDelta, PublicInfoDelta}
import soc.core.{City, Road, Settlement}
import soc.core.state.{BoardBuildingState, EdgeBuildingState, VertexBuildingState}

/** How many settlements, cities, and roads each player has built over the game. */
case class BuildStats(
  settlements: Map[Int, Int],
  cities:      Map[Int, Int],
  roads:       Map[Int, Int]
):
  def settlementCount(player: Int): Int = settlements.getOrElse(player, 0)
  def cityCount(player: Int):       Int = cities.getOrElse(player, 0)
  def roadCount(player: Int):       Int = roads.getOrElse(player, 0)

/** Extracts (settlement player, city player, road player) from a single delta. */
trait BuildDeltas[D]:
  def extract(d: D): (Option[Int], Option[Int], Option[Int])

object BuildDeltas:
  private val vtxClass  = classOf[VertexBuildingState[?]]
  private val edgeClass = classOf[EdgeBuildingState[?]]

  private def fromRaw(d: Delta[?]): (Option[Int], Option[Int], Option[Int]) =
    if d.ct.runtimeClass == vtxClass then
      d.rawValue match
        case BoardBuildingState.AddBuilding(_, player, Settlement) => (Some(player), None, None)
        case BoardBuildingState.AddBuilding(_, player, City)       => (None, Some(player), None)
        case _                                                     => (None, None, None)
    else if d.ct.runtimeClass == edgeClass then
      d.rawValue match
        case BoardBuildingState.AddBuilding(_, player, Road) => (None, None, Some(player))
        case _                                               => (None, None, None)
    else
      (None, None, None)

  given BuildDeltas[PerfectInfoDelta] with
    def extract(d: PerfectInfoDelta): (Option[Int], Option[Int], Option[Int]) = d match
      case d: Delta[?] => fromRaw(d)
      case _           => (None, None, None)

  given BuildDeltas[PublicInfoDelta] with
    def extract(d: PublicInfoDelta): (Option[Int], Option[Int], Option[Int]) = d match
      case d: Delta[?] => fromRaw(d)
      case _           => (None, None, None)

object BuildStats:
  val empty: BuildStats = BuildStats(Map.empty, Map.empty, Map.empty)

  def update[D: BuildDeltas](stats: BuildStats, deltas: List[D]): BuildStats =
    val extract = summon[BuildDeltas[D]]
    deltas.foldLeft(stats): (s, d) =>
      val (settleOpt, cityOpt, roadOpt) = extract.extract(d)
      val s1 = settleOpt.fold(s)(p => s.copy(settlements = s.settlements.updatedWith(p)(v => Some(v.getOrElse(0) + 1))))
      val s2 = cityOpt.fold(s1)(p => s1.copy(cities = s1.cities.updatedWith(p)(v => Some(v.getOrElse(0) + 1))))
      roadOpt.fold(s2)(p => s2.copy(roads = s2.roads.updatedWith(p)(v => Some(v.getOrElse(0) + 1))))
