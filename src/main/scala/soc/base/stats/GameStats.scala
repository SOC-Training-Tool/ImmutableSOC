package soc.base.stats

import scala.annotation.targetName
import soc.base.BaseBoard
import soc.base.BaseGame.{BaseVertexBuilding, PerfectInfoDelta, PublicInfoDelta}
import soc.core.Resource
import soc.base.state.RobberLocation
import soc.core.state.VertexBuildingState

/** Composite accumulator for all game analytics. */
case class GameStats(
  diceRolls: DiceRollStats,
  builds:    BuildStats,
  devCards:  DevCardStats,
  resources: ResourceLedger
)

object GameStats:
  val empty: GameStats = GameStats(
    DiceRollStats.empty,
    BuildStats.empty,
    DevCardStats.empty,
    ResourceLedger.empty
  )

  @targetName("updatePerfectInfo")
  def update(stats: GameStats, deltas: List[PerfectInfoDelta]): GameStats =
    GameStats(
      DiceRollStats.update[PerfectInfoDelta](stats.diceRolls, deltas),
      BuildStats.update[PerfectInfoDelta](stats.builds, deltas),
      DevCardStats.update(stats.devCards, deltas),
      ResourceLedger.update[PerfectInfoDelta](stats.resources, deltas)
    )

  /** Accumulate one roll's probability-weighted production into `stats.diceRolls`.
   *  Call this with the game state *before* the roll move is applied.
   *  The specific roll result does not matter — pip expectations are constant
   *  across all rolls for a given building + robber configuration. */
  def recordRoll(
    stats:          GameStats,
    board:          BaseBoard[Resource],
    vbs:            VertexBuildingState[BaseVertexBuilding],
    robberLocation: RobberLocation
  ): GameStats =
    stats.copy(diceRolls = DiceRollStats.recordExpected(stats.diceRolls, board, vbs, robberLocation))

  @targetName("updatePublicInfo")
  def update(stats: GameStats, deltas: List[PublicInfoDelta]): GameStats =
    GameStats(
      DiceRollStats.update[PublicInfoDelta](stats.diceRolls, deltas),
      BuildStats.update[PublicInfoDelta](stats.builds, deltas),
      stats.devCards,  // PublicInfo lacks card identity for buys
      ResourceLedger.update[PublicInfoDelta](stats.resources, deltas)
    )
