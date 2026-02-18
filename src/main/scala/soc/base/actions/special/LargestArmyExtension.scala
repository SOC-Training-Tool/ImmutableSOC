package soc.base.actions.special

import game.{DeltaList, GameAction, tupleSelect}
import soc.base.state.{LargestArmyPlayer, PlayerArmyCount, SpecialCounts}

object LargestArmyExtension {

  def apply(minCount: Int = 3) = {
    GameAction.fromState[Int, (LargestArmyPlayer, PlayerArmyCount)] { case (player, state) =>
      val largestArmyPlayer = tupleSelect[(LargestArmyPlayer, PlayerArmyCount), LargestArmyPlayer](state).player
      val updatedArmyCount  = {
        val currArmySize = tupleSelect[(LargestArmyPlayer, PlayerArmyCount), PlayerArmyCount](state).m
        PlayerArmyCount(currArmySize + (player -> (currArmySize.getOrElse(player, 0) + 1)))
      }
      val updatedArmyPlayer = updatedSpecialPlayer(minCount, largestArmyPlayer, updatedArmyCount.m)
      DeltaList()
        .add[PlayerArmyCount](SpecialCounts.Increment(player))
        .addAll(specialPlayerDelta[LargestArmyPlayer](largestArmyPlayer, updatedArmyPlayer))
        .toList
    }
  }
}
