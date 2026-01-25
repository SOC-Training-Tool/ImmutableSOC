package soc.base.actions.developmentcards

import game.{Delta, DeltaList, GameAction}
import shapeless.ops.coproduct
import shapeless.{:+:, CNil, Coproduct, HNil}
import soc.base.{PerfectInfoBuyDevelopmentCardMoveResult, PlayPointMove, Point}
import soc.core.state.PlayerPoints

object PlayPointAction {

  val public: GameAction[PlayPointMove, HNil, Delta[PlayerPoints] :+: CNil] = GameAction.apply[PlayPointMove] { move =>
    DeltaList()
      .add[PlayerPoints](PlayerPoints.Increment(move.player))
      .toList
  }

  def onPerfectBuy[Dev <: Coproduct](implicit selector: coproduct.Selector[Dev, Point.type]): GameAction[PerfectInfoBuyDevelopmentCardMoveResult[Dev], HNil, Delta[PlayerPoints] :+: CNil] =
    GameAction.apply[PerfectInfoBuyDevelopmentCardMoveResult[Dev]] { move =>
      move.card.select[Point.type].fold(DeltaList().add[PlayerPoints]()) { _ =>
        DeltaList().add[PlayerPoints](PlayerPoints.Increment(move.player))
      }.toList
    }
}