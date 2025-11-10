package soc.base.actions.special

import game.{GameAction, GameMoveResult}
import shapeless.{::, HNil}
import soc.base.PlayKnightMove
import soc.base.state.ops._
import soc.base.state.{LargestArmyPlayer, PlayerArmyCount}
import soc.core.state.PlayerPoints
import soc.core.state.ops.PointsOps
import util.DependsOn

class LargestArmyExtension[R, M <: GameMoveResult.Aux[PlayKnightMove[R]]](minCount: Int) extends GameAction[M, LargestArmyPlayer :: PlayerArmyCount :: PlayerPoints :: HNil] {

  override def apply(move: M, state: STATE): STATE = {
    implicit val pointsDep = DependsOn[STATE, PlayerPoints :: HNil]
    implicit val armyDep   = DependsOn[STATE, LargestArmyPlayer :: PlayerArmyCount :: HNil]
    val player             = move.move.player
    val largestArmyPlayer  = state.largestArmyPlayer
    val updatedArmyCount = {
      val currArmySize = state.armyCount
      PlayerArmyCount(currArmySize + (player -> (currArmySize.getOrElse(player, 0) + 1)))
    }
    val updatedArmyPlayer  = updatedSpecialPlayer(minCount, largestArmyPlayer, updatedArmyCount.m)
    val result             = state.updateArmyCount(updatedArmyCount.m)
    updateState[STATE](largestArmyPlayer, updatedArmyPlayer, result)(_.incrementPointForPlayer(_), _.decrementPointForPlayer(_), _.updateLargestArmyPlayer(_))
  }
}