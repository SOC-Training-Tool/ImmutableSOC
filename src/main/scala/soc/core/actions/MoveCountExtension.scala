package soc.core.actions

import game.{Delta, DeltaList, GameAction}
import shapeless.{:+:, ::, CNil, HNil}
import soc.core.state.MoveCount

object MoveCountExtension {

  def apply(): GameAction[Any, HNil, Delta[MoveCount] :+: CNil] =
    GameAction.apply[Any] { _ => DeltaList().add[MoveCount](1).toList }
}
