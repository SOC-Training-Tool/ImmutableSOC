package soc.core.actions

import game.GameAction
import shapeless.{::, Coproduct, HNil}
import soc.core.ResourceInventories.ResourceInventoriesOp
import soc.core.Transactions.{Gain, Lose, PerfectInfo}
import soc.core.{ResourceInventories, TradeMove}
import util.DependsOn

class TradeAction[Res, Inv[_]](implicit inv: ResourceInventories[Res, PerfectInfo[Res], Inv]) extends GameAction[TradeMove[Res], Inv[Res] :: HNil] {

  override def apply(move: TradeMove[Res], state: Inv[Res] :: HNil): Inv[Res] :: HNil = {
    val dep = DependsOn.single[Inv[Res] :: HNil]
    dep.updateWith[Inv[Res]](state)(
      _.update(
        Coproduct[PerfectInfo[Res]](Lose(move.player, move.give)),
        Coproduct[PerfectInfo[Res]](Lose(move.partner, move.get)),
        Coproduct[PerfectInfo[Res]](Gain(move.player, move.get)),
        Coproduct[PerfectInfo[Res]](Gain(move.partner, move.give))
      )
    )
  }
}
