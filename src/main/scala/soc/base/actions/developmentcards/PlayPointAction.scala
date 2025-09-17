package soc.base.actions.developmentcards

import game.GameAction
import shapeless.ops.coproduct
import shapeless.{::, Coproduct, HNil}
import soc.base.{PerfectInfoBuyDevelopmentCardMoveResult, PlayPointMove, Point}
import soc.core.state.PlayerPoints
import soc.core.state.ops.PointsOps

object PlayPointAction {

  def apply(): GameAction[PlayPointMove, PlayerPoints :: HNil] = {
    GameAction.apply[PlayPointMove, PlayerPoints :: HNil]((move, state) => state.incrementPointForPlayer(move.player))
  }

  def extension[Dev <: Coproduct](implicit selector: coproduct.Selector[Dev, Point.type]): GameAction[PerfectInfoBuyDevelopmentCardMoveResult[Dev], PlayerPoints :: HNil] = {
    GameAction.apply[PerfectInfoBuyDevelopmentCardMoveResult[Dev], PlayerPoints :: HNil]((move, state) =>
      move.card.select[Point.type].fold(state)(_ => state.incrementPointForPlayer(move.player))
    )
  }
}