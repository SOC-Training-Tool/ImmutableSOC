package soc.base.actions

import game.GameAction
import shapeless.ops.coproduct
import shapeless.{::, Coproduct, HNil}
import soc.base.state.RobberLocation
import soc.base.state.ops._
import soc.core.Transactions.PerfectInfo
import soc.core.state.{Bank, VertexBuildingState}
import soc.core.{ResourceInventories, RollDiceMoveResult, SOCBoard}
import util.DependsOn

class RollDiceAction[II, VB <: Coproduct, BOARD, INV[_]](implicit
    inv: ResourceInventories[II, PerfectInfo[II], INV],
    socBoard: SOCBoard[II, BOARD],
    vertexFolder: coproduct.Folder.Aux[ResourcesForBuildingPoly.type, VB, Int]
) extends GameAction[RollDiceMoveResult, RobberLocation :: VertexBuildingState[VB] :: BOARD :: Bank[II] :: INV[II] :: HNil] {

  override def apply(
      move: RollDiceMoveResult,
      state: RobberLocation :: VertexBuildingState[VB] :: BOARD :: Bank[II] :: INV[II] :: HNil
  ): RobberLocation :: VertexBuildingState[VB] :: BOARD :: Bank[II] :: INV[II] :: HNil = {
    implicit val dep = DependsOn.single[RobberLocation :: VertexBuildingState[VB] :: BOARD :: Bank[II] :: INV[II] :: HNil]
    state.distributeResources(state.getResourcesGainedOnRoll(move.result).gained)
  }
}
