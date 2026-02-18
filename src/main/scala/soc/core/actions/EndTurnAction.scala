package soc.core.actions

import game.{Delta, DeltaList, GameAction, :+:, CNil}
import soc.core.EndTurnMove
import soc.core.state.Turn

object EndTurnAction {

  val action: GameAction[EndTurnMove, EmptyTuple, Delta[Turn] :+: CNil] = GameAction.apply[EndTurnMove] { _ =>
    DeltaList().add[Turn](1).toList
  }

}
