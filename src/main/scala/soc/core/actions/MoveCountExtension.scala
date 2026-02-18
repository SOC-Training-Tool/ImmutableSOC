package soc.core.actions

import game.{Delta, DeltaList, GameAction, :+:, CNil}
import soc.core.state.MoveCount

object MoveCountExtension {

  def apply(): GameAction[Any, EmptyTuple, Delta[MoveCount] :+: CNil] =
    GameAction.apply[Any] { _ => DeltaList().add[MoveCount](1).toList }
}
