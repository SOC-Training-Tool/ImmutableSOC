package soc.base.actions.developmentcards

import game.Delta.DeltaGen
import game.{Delta, DeltaList, GameAction, GameState, InventorySet}
import shapeless.ops.coproduct
import shapeless.{:+:, ::, CNil, Coproduct, HNil}
import soc.base.{PlayYearOfPlentyMove, YearOfPlenty}
import soc.core.Transactions.{Gain, PerfectInfo}
import soc.core.state.ops.BankInvOps
import soc.core.state.{Bank, Turn}
import soc.core.{DevelopmentCardInventories, ResourceInventories, Transactions}
import util.DependsOn

object PlayYearOfPlentyAction {

  def apply[II, Inv[_] <: GameState[Inv[II]]](implicit gen: DeltaGen[Inv[II], PerfectInfo[II]]): GameAction[PlayYearOfPlentyMove[II], HNil, Delta[Inv[II]] :+: Delta[Bank[II]] :+: CNil] = {
    GameAction.apply[PlayYearOfPlentyMove[II]] { move =>
      val inv = InventorySet.fromList(List(move.c1, move.c2))
      DeltaList()
        .add[Bank[II]](Bank.Take(inv))
        .add[Inv[II]](Gain(move.player, inv))
        .toList
    }
  }
}