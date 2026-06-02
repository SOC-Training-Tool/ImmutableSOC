package soc.base.stats

import game.{Delta, InventorySet}
import soc.base.BaseBoard
import soc.base.BaseGame.{BaseVertexBuilding, PerfectInfoDelta, PublicInfoDelta}
import soc.base.state.RobberLocation
import soc.core.{City, Resource, ResourceSet, Settlement}
import soc.core.ResourceSet.Resources
import soc.core.SOCBoard.SOCBoardOps
import soc.core.Transactions.{Gain, Lose}
import soc.core.ResourceInventories.{PrivateInventories, PublicInventories}
import soc.core.state.VertexBuildingState

/** Extracts (player, resources) gain pairs from a single typed delta value.
 *  Returns None for deltas that aren't inventory gains. */
trait GainDeltas[D]:
  def gain(d: D): Option[(Int, Resources)]

object GainDeltas:
  given GainDeltas[PerfectInfoDelta] with
    def gain(d: PerfectInfoDelta): Option[(Int, Resources)] = d match
      case d: Delta[PrivateInventories[Resource]] => d.rawValue match
        case Gain(player, resources) => Some(player -> resources.asInstanceOf[Resources])
        case _                       => None
      case _ => None

  given GainDeltas[PublicInfoDelta] with
    def gain(d: PublicInfoDelta): Option[(Int, Resources)] = d match
      case d: Delta[PublicInventories[Resource]] => d.rawValue match
        case Gain(player, resources) => Some(player -> resources.asInstanceOf[Resources])
        case _                       => None
      case _ => None

/** Extracts (player, resources) lose pairs from a single typed delta value.
 *  Returns None for deltas that aren't inventory losses. */
trait LoseDeltas[D]:
  def lose(d: D): Option[(Int, Resources)]

object LoseDeltas:
  given LoseDeltas[PerfectInfoDelta] with
    def lose(d: PerfectInfoDelta): Option[(Int, Resources)] = d match
      case d: Delta[PrivateInventories[Resource]] => d.rawValue match
        case Lose(player, resources) => Some(player -> resources.asInstanceOf[Resources])
        case _                       => None
      case _ => None

  given LoseDeltas[PublicInfoDelta] with
    def lose(d: PublicInfoDelta): Option[(Int, Resources)] = d match
      case d: Delta[PublicInventories[Resource]] => d.rawValue match
        case Lose(player, resources) => Some(player -> resources.asInstanceOf[Resources])
        case _                       => None
      case _ => None

/** Resources actually collected from dice rolls plus probability-weighted expected production.
 *
 *  `collected`    — resources actually distributed by dice rolls.
 *  `expectedPips` — accumulated pips considering the robber's current hex.
 *  `basePips`     — accumulated pips ignoring the robber (theoretical maximum).
 *
 *  Pips are the probability numerator with denominator 36.  A settlement on a
 *  6-WOOD hex contributes 5 pips of WOOD per roll; a city contributes 10 pips.
 *  After N rolls, `expectedPips(player)(resource) / 36.0` is the total expected
 *  resources for that player from all recorded rolls.
 *
 *  Call `recordExpected` with the pre-roll game state once per dice roll to
 *  update both pip accumulators, then `update` on the resulting deltas for
 *  `collected`. */
case class DiceRollStats(
  collected:    Map[Int, Resources],
  expectedPips: Map[Int, Resources],
  basePips:     Map[Int, Resources]
):
  /** Resources actually collected by one player across all recorded rolls. */
  def byPlayer(player: Int): Resources =
    collected.getOrElse(player, ResourceSet.empty[Int])

  /** How much of one resource type each player collected. */
  def byResource(resource: Resource): Map[Int, Int] =
    collected.view.mapValues(_(resource)).toMap

  /** Accumulated pips (robber considered).  Divide by 36 for expected resources. */
  def expectedByPlayer(player: Int): Resources =
    expectedPips.getOrElse(player, ResourceSet.empty[Int])

  /** Accumulated pips ignoring robber.  Divide by 36 for theoretical expected. */
  def baseExpectedByPlayer(player: Int): Resources =
    basePips.getOrElse(player, ResourceSet.empty[Int])

  /** Pips the robber has cost this player: `basePips - expectedPips`.
   *  Divide by 36 for expected resource loss per roll due to robber blocking. */
  def robberImpact(player: Int): Resources =
    baseExpectedByPlayer(player).subtract(expectedByPlayer(player))

object DiceRollStats:
  val empty: DiceRollStats = DiceRollStats(Map.empty, Map.empty, Map.empty)

  /** Pip counts (probability numerator / 36) for each dice result that produces resources. */
  val numberToPips: Map[Int, Int] =
    Map(2 -> 1, 3 -> 2, 4 -> 3, 5 -> 4, 6 -> 5, 8 -> 5, 9 -> 4, 10 -> 3, 11 -> 2, 12 -> 1)

  private def buildingValue(b: BaseVertexBuilding): Int = b match
    case _: Settlement.type => 1
    case _: City.type       => 2

  /** Compute pip contributions from all buildings.
   *  `robberHex = Some(n)` excludes hex n; `None` counts all hexes. */
  def computePips(
    board:     BaseBoard[Resource],
    vbs:       VertexBuildingState[BaseVertexBuilding],
    robberHex: Option[Int]
  ): Map[Int, Resources] =
    (for
      (vertex, pb) <- vbs.map.toSeq
      boardHex     <- board.hexesForVertex.getOrElse(vertex, Nil)
      resource     <- boardHex.hex.getResource.toSeq
      number       <- boardHex.hex.getNumber.toSeq
      pips         <- numberToPips.get(number).toSeq
      if robberHex.forall(_ != boardHex.node)
      player        = pb.player
      pipAmt        = buildingValue(pb.building) * pips
    yield player -> InventorySet.fromMap(Map(resource -> pipAmt)))
      .foldLeft(Map.empty[Int, Resources]) { case (acc, (p, inv)) =>
        acc + (p -> acc.get(p).fold(inv)(_.add(inv)))
      }

  /** Accumulate one roll's expected pips into both `expectedPips` and `basePips`.
   *  Call this with the game state *before* the roll move is applied. */
  def recordExpected(
    stats:          DiceRollStats,
    board:          BaseBoard[Resource],
    vbs:            VertexBuildingState[BaseVertexBuilding],
    robberLocation: RobberLocation
  ): DiceRollStats =
    val withRobber = computePips(board, vbs, Some(robberLocation.robberHexId))
    val noRobber   = computePips(board, vbs, None)
    def accum(existing: Map[Int, Resources], additions: Map[Int, Resources]): Map[Int, Resources] =
      additions.foldLeft(existing) { case (acc, (p, inv)) =>
        acc.updatedWith(p):
          case Some(e) => Some(e.add(inv))
          case None    => Some(inv)
      }
    stats.copy(
      expectedPips = accum(stats.expectedPips, withRobber),
      basePips     = accum(stats.basePips, noRobber)
    )

  def update[D: GainDeltas](stats: DiceRollStats, deltas: List[D]): DiceRollStats =
    val extract = summon[GainDeltas[D]]
    deltas.foldLeft(stats): (s, d) =>
      extract.gain(d).fold(s)((player, resources) => s.record(player, resources))

  extension (s: DiceRollStats)
    private def record(player: Int, resources: Resources): DiceRollStats =
      s.copy(collected = s.collected.updatedWith(player):
        case Some(existing) => Some(existing.add(resources))
        case None           => Some(resources)
      )
