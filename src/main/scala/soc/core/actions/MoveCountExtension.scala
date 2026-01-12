//package soc.core.actions
//
//import game.GameAction
//import shapeless.{::, HNil}
//import soc.core.state.MoveCount
//
//case object MoveCountExtension extends GameAction[Any, MoveCount :: HNil] {
//
//  override def apply(v1: Any, state: MoveCount :: HNil): MoveCount :: HNil = {
//    val count = state.select[MoveCount].count
//    state.updatedElem(MoveCount(count + 1))
//  }
//
//}