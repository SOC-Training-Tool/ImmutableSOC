package soc.base.actions.special

import game.{DeltaList, GameAction}
import shapeless.{::, HNil}
import soc.base.state.{LargestArmyPlayer, PlayerArmyCount, SpecialCounts}

object LargestArmyExtension {

  def apply(minCount: Int = 3) = {
    GameAction.fromState[Int, LargestArmyPlayer :: PlayerArmyCount :: HNil] { case (player, state) =>
      val largestArmyPlayer = state.select[LargestArmyPlayer].player
      val updatedArmyCount  = {
        val currArmySize = state.select[PlayerArmyCount].m
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