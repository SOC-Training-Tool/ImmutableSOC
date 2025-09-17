package soc.core.actions

import game.GameAction
import shapeless.{::, HNil}
import soc.core.EndTurnMove
import soc.core.state.Turn
import soc.core.state.ops.TurnOps

case object EndTurnAction extends GameAction[EndTurnMove, Turn :: HNil] {

  override def apply(v1: EndTurnMove, state: Turn :: HNil): Turn :: HNil =
    state.incrementTurn

}
