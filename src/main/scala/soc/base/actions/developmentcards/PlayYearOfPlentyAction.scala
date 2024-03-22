package soc.base.actions.developmentcards

import game.{GameAction, InventorySet}
import shapeless.ops.coproduct
import shapeless.{::, Coproduct, HNil}
import soc.base.state.Bank
import soc.base.state.ops.BankInvOps
import soc.base.{PlayYearOfPlentyMove, state}
import soc.inventory.{DevelopmentCardInventories, ResourceInventories, Transactions}
import util.DependsOn

case object YearOfPlenty

object PlayYearOfPlentyAction {

  def apply[Res, ResInv[_], Dev <: Coproduct, DevInv[_]]
  (implicit res: ResourceInventories[Res, Transactions.PerfectInfo[Res], ResInv],
   dev: DevelopmentCardInventories[Dev, DevInv],
   inject: coproduct.Inject[Dev, YearOfPlenty.type]
  ): GameAction[PlayYearOfPlentyMove[Res], DevInv[Dev] :: state.Turn :: Bank[Res] :: ResInv[Res] :: HNil] = {
    GameAction[PlayYearOfPlentyMove[Res], Bank[Res] :: ResInv[Res] :: HNil] { case (move, state) =>
      implicit val dep = DependsOn.single[Bank[Res] :: ResInv[Res] :: HNil]
      val set = InventorySet.fromList(Seq(move.c1, move.c2))
      state.getFromBank(move.player, set)
    }.extend(PlayDevelopmentCardActionExtension[PlayYearOfPlentyMove[Res], Dev, YearOfPlenty.type, DevInv](_.player, YearOfPlenty)).apply()
  }
}
