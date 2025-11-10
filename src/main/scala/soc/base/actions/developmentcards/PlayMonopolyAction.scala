package soc.base.actions.developmentcards

import game.{GameAction, InventorySet}
import shapeless.ops.coproduct
import shapeless.{::, Coproduct, HNil}
import soc.base
import soc.base.{DevelopmentCards, PlayMonopolyMoveResult}
import soc.core.ResourceInventories.ResourceInventoriesOp
import soc.core.Transactions.{Gain, Lose}
import soc.core.{DevelopmentCardInventories, ResourceInventories, Transactions}
import util.DependsOn

class PlayMonopolyAction[Res, ResInv[_]](implicit res: ResourceInventories[Res, Transactions.PerfectInfo[Res], ResInv]) extends GameAction[PlayMonopolyMoveResult[Res], ResInv[Res] :: HNil] {

  override def apply(move: PlayMonopolyMoveResult[Res], state: STATE): STATE = {
    implicit val dep = DependsOn.single[ResInv[Res] :: HNil]
    val lose         = move.cardsLost.map { case (p, cards) =>
      val set = InventorySet.fromMap(Map(move.res -> cards))
      Coproduct[Transactions.PerfectInfo[Res]](Lose(p, set))
    }.toList
    val totalLost    = InventorySet.fromMap(Map(move.res -> move.cardsLost.values.sum))
    dep.updateWith[ResInv[Res]](state)(_.update(lose :+ Coproduct[Transactions.PerfectInfo[Res]](Gain(move.player, totalLost))))
  }
}
