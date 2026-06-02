package soc

import game.PerfectInfoMoveResult
import soc.base.BaseGame.*

/** Converts a perfect-info move to its public-info counterpart.
 *  Moves that are already public-info pass through.
 *  Perfect-info-specific moves (e.g. PerfectInfoRobberMoveResult) produce
 *  their public-info equivalents using getPerspectiveResults(-1).
 */
object ToPublicInfo:

  def apply(move: PerfectInfoGame.MOVES): PublicInfoGame.MOVES = move match
    // Perfect-info moves with a public-info counterpart
    case m: PerfectInfoMoveResult =>
      m.getPerspectiveResults(Seq(-1)).head._2.asInstanceOf[PublicInfoGame.MOVES]

    // Shared moves pass through directly
    case m => m.asInstanceOf[PublicInfoGame.MOVES]
