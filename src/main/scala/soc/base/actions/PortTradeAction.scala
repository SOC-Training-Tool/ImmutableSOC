package soc.base.actions

import game.GameAction
import shapeless.{::, HNil}
import soc.base.PortTradeMove
import soc.core.ResourceInventories
import soc.core.Transactions.PerfectInfo
import soc.core.state.Bank
import soc.core.state.ops.BankInvOps
import util.DependsOn

class PortTradeAction [II, INV[_]](implicit inv: ResourceInventories[II, PerfectInfo[II], INV]) extends GameAction[PortTradeMove[II], Bank[II] :: INV[II] :: HNil] {

  override def apply(move: PortTradeMove[II], state: Bank[II] :: INV[II] :: HNil): Bank[II] :: INV[II] :: HNil = {
    implicit val dep = DependsOn.single[Bank[II] :: INV[II] :: HNil]
    state
      .payToBank(move.player, move.give)
      .getFromBank(move.player, move.get)
  }
}