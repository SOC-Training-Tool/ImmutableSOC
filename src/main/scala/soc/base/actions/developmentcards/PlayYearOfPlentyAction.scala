package soc.base.actions.developmentcards

import game.{GameAction, InventorySet}
import shapeless.ops.coproduct
import shapeless.{::, Coproduct, HNil}
import soc.base.{PlayYearOfPlentyMove, YearOfPlenty}
import soc.core.state.ops.BankInvOps
import soc.core.state.{Bank, Turn}
import soc.core.{DevelopmentCardInventories, ResourceInventories, Transactions}
import util.DependsOn

class PlayYearOfPlentyAction[Res, ResInv[_]](implicit res: ResourceInventories[Res, Transactions.PerfectInfo[Res], ResInv])
    extends GameAction[PlayYearOfPlentyMove[Res], Bank[Res] :: ResInv[Res] :: HNil] {

  override def apply(move: PlayYearOfPlentyMove[Res], state: Bank[Res] :: ResInv[Res] :: HNil): Bank[Res] :: ResInv[Res] :: HNil = {
    implicit val dep = DependsOn.single[Bank[Res] :: ResInv[Res] :: HNil]
    val set          = InventorySet.fromList(Seq(move.c1, move.c2))
    state.getFromBank(move.player, set)
  }
}