package soc.base.actions.developmentcards

import game.{Delta, DeltaList, GameAction, :+:, CNil, CoproductSelector}
import game.select
import soc.base.{PerfectInfoBuyDevelopmentCardMoveResult, PlayPointMove, Point}
import soc.core.state.PlayerPoints

object PlayPointAction {

  val public: GameAction[PlayPointMove, EmptyTuple, Delta[PlayerPoints] :+: CNil] = GameAction.apply[PlayPointMove] { move =>
    DeltaList()
      .add[PlayerPoints](PlayerPoints.Increment(move.player))
      .toList
  }

  def onPerfectBuy[Dev](using selector: CoproductSelector[Dev, Point.type]): GameAction[PerfectInfoBuyDevelopmentCardMoveResult[Dev], EmptyTuple, Delta[PlayerPoints] :+: CNil] =
    GameAction.apply[PerfectInfoBuyDevelopmentCardMoveResult[Dev]] { move =>
      move.card.select[Point.type].fold(DeltaList().add[PlayerPoints]()) { _ =>
        DeltaList().add[PlayerPoints](PlayerPoints.Increment(move.player))
      }.toList
    }
}
