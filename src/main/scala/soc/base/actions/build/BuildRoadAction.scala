package soc.base.actions.build

import game.{GameAction, InventorySet}
import shapeless.{:+:, ::, CNil, Coproduct, HNil}
import soc.base.state.ops._
import soc.core.Transactions.PerfectInfo
import soc.core.state.ops.BankInvOps
import soc.core.state.{Bank, EdgeBuildingState}
import soc.core.{BuildRoadMove, ResourceInventories, Road}
import util.DependsOn
import util.opext.Embedder

class BuildRoadAction[Res, Inv[_], EB <: Coproduct](cost: InventorySet[Res, Int])(implicit
    inv: ResourceInventories[Res, PerfectInfo[Res], Inv],
    roadEmbedder: Embedder[EB, Road.type :+: CNil]
) extends GameAction[BuildRoadMove, EdgeBuildingState[EB] :: Bank[Res] :: Inv[Res] :: HNil] {

  override def apply(move: BuildRoadMove, state: EdgeBuildingState[EB] :: Bank[Res] :: Inv[Res] :: HNil): EdgeBuildingState[EB] :: Bank[Res] :: Inv[Res] :: HNil = {
    val dep              = DependsOn.single[EdgeBuildingState[EB] :: Bank[Res] :: Inv[Res] :: HNil]
    implicit val roadDep = dep.innerDependency[EdgeBuildingState[EB] :: HNil]
    implicit val invDep  = dep.innerDependency[Bank[Res] :: Inv[Res] :: HNil]
    state.addRoad(move.edge, move.player).payToBank(move.player, cost)
  }
}