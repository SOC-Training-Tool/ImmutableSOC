package soc.base.actions

import game._
import shapeless.{::, Coproduct, HNil}
import soc.base.state.RobberLocation
import soc.base.state.ops.RobberStateOps
import soc.base.{PerfectInfoRobberMoveResult, RobberMoveResult}
import soc.core.ResourceInventories
import ResourceInventories.ResourceInventoriesOp
import soc.core.Transactions._
import util.DependsOn

class MoveRobberAndStealAction[II, INV[_]](implicit inv: ResourceInventories[II, ImperfectInfo[II], INV]) extends GameAction[RobberMoveResult[II], RobberLocation :: INV[II] :: HNil] {
  override def apply(move: RobberMoveResult[II], state: RobberLocation :: INV[II] :: HNil): RobberLocation :: INV[II] :: HNil = {
    val dep                = DependsOn.single[RobberLocation :: INV[II] :: HNil]
    implicit val robberDep = dep.innerDependency[RobberLocation :: HNil]
    val result             = state.updateRobberHexId(move.robberHexId)
    move.steal.fold(result) { stl =>
      dep.updateWith[INV[II]](result)(_.update(Coproduct[ImperfectInfo[II]](ImperfectInfoExchange(stl.victim, move.player, stl.resource))))
    }
  }
}

class PerfectInfoMoveRobberAndStealAction[II, INV[_]](implicit inv: ResourceInventories[II, PerfectInfo[II], INV])
    extends GameAction[PerfectInfoRobberMoveResult[II], RobberLocation :: INV[II] :: HNil] {

  override def apply(move: PerfectInfoRobberMoveResult[II], state: RobberLocation :: INV[II] :: HNil): RobberLocation :: INV[II] :: HNil = {
    val dep                = DependsOn.single[RobberLocation :: INV[II] :: HNil]
    implicit val robberDep = dep.innerDependency[RobberLocation :: HNil]
    val result             = state.updateRobberHexId(move.robberHexId)
    move.steal.fold(result) { stl =>
      val set = InventorySet.fromList(Seq(stl.resource))
      dep.updateWith[INV[II]](result)(_.update(Coproduct[PerfectInfo[II]](Gain(move.player, set)), Coproduct[PerfectInfo[II]](Lose(stl.victim, set))))
    }
  }
}
