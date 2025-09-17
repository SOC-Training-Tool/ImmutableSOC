package soc.base.actions

import game.{GameAction, InventorySet}
import shapeless.{:+:, ::, CNil, Coproduct, HNil}
import soc.base.state.ops.{BuildRoadStateOps, BuildSettlementStateOps}
import soc.core.SOCBoard.SOCBoardOps
import soc.core.Transactions.PerfectInfo
import soc.core.state.ops.{BankInvOps, PointsOps, TurnOps}
import soc.core.state.{Bank, EdgeBuildingState, MoveCount, PlayerPoints, Turn, VertexBuildingState}
import soc.core.{InitialPlacementMove, ResourceInventories, Road, SOCBoard, Settlement}
import util.DependsOn
import util.opext.Embedder

class InitialPlacementAction[Res, INV[_], VB <: Coproduct, EB <: Coproduct, BOARD](implicit
    inv: ResourceInventories[Res, PerfectInfo[Res], INV],
    settle: Embedder[VB, Settlement.type :+: CNil],
    road: Embedder[EB, Road.type :+: CNil],
    socBoard: SOCBoard[Res, BOARD]
) extends GameAction[InitialPlacementMove, VertexBuildingState[VB] :: EdgeBuildingState[EB] :: BOARD :: PlayerPoints :: Bank[Res] :: INV[Res] :: MoveCount :: HNil] {
  override def apply(move: InitialPlacementMove, state: STATE): STATE = {
    val dep                = DependsOn.single[STATE]
    implicit val settleDep = dep.innerDependency[VertexBuildingState[VB] :: PlayerPoints :: HNil]
    implicit val roadDep   = dep.innerDependency[EdgeBuildingState[EB] :: HNil]
    implicit val invDep    = dep.innerDependency[Bank[Res] :: INV[Res] :: HNil]
    val moveCount          = state.select[MoveCount].count
    val result             = state
      .placeSettlement(move.vertex, move.player)
      .addRoad(move.edge, move.player)
    if (moveCount >= state.playerPoints.keys.size) {
      val board     = result.select[BOARD]
      val resources = board.hexesForVertex
        .get(move.vertex)
        .fold[Seq[Res]](Nil)(_.flatMap(_.hex.getResource))
      result.getFromBank(move.player, InventorySet.fromList(resources))
    } else result
  }
}
