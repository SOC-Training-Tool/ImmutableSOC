package soc.base.actions.developmentcards

import game._
import shapeless.ops.coproduct
import shapeless.{::, Coproduct, HNil}
import soc.base.actions.MoveRobberAndStealAction
import soc.base.state.{RobberLocation, Turn}
import soc.base.{PerfectInfoPlayKnightResult, PlayKnightResult}
import soc.inventory.Transactions.{ImperfectInfo, PerfectInfo}
import soc.inventory.{DevelopmentCardInventories, ResourceInventories}

case object Knight

object PlayKnightAction {

  def apply[Res, ResInv[_], Dev <: Coproduct, DevInv[_]]
  (implicit dev: DevelopmentCardInventories[Dev, DevInv],
   inject: coproduct.Inject[Dev, Knight.type],
   inv: ResourceInventories[Res, ImperfectInfo[Res], ResInv]
  ): GameAction[PlayKnightResult[Res], DevInv[Dev] :: Turn :: RobberLocation :: ResInv[Res] :: HNil] = {
    MoveRobberAndStealAction[Res, ResInv]
      .compose[PlayKnightResult[Res]](_.inner)
      .extend(PlayDevelopmentCardActionExtension[PlayKnightResult[Res], Dev, Knight.type, DevInv](_.inner.player, Knight)).apply()
  }

  def perfectInfo[Res, ResInv[_], Dev <: Coproduct, DevInv[_]]
  (implicit dev: DevelopmentCardInventories[Dev, DevInv],
   inject: coproduct.Inject[Dev, Knight.type],
   inv: ResourceInventories[Res, PerfectInfo[Res], ResInv]
  ): GameAction[PerfectInfoPlayKnightResult[Res], DevInv[Dev] :: Turn :: RobberLocation :: ResInv[Res] :: HNil] = {
    MoveRobberAndStealAction.perfectInfo[Res, ResInv]
      .compose[PerfectInfoPlayKnightResult[Res]](_.inner)
      .extend(PlayDevelopmentCardActionExtension[PerfectInfoPlayKnightResult[Res], Dev, Knight.type, DevInv](_.inner.player, Knight)).apply()
  }
}