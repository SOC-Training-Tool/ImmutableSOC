package soc.base.actions.developmentcards

import game.Delta.DeltaGen
import game.{Delta, DeltaList, GameAction, GameMoveResult, GameState, MergeDelta, MergeState, TupleOps, CoproductOps, :+:, CNil, CoproductInject, tupleSelect}
import soc.core.DevTransactions.PlayCard
import soc.core.state.Turn

object PlayDevelopmentCardAction {

  def apply[Dev, Inv[_]](using dg: DeltaGen[Inv[Dev], PlayCard[Dev]]): GameAction[(Dev, Int), Turn *: EmptyTuple, Delta[Inv[Dev]] :+: CNil] =
    GameAction.fromState[(Dev, Int), Turn *: EmptyTuple] { case ((card, player), state) =>
      val turn = tupleSelect[Turn *: EmptyTuple, Turn](state).t
      DeltaList().add[Inv[Dev]](PlayCard(card, player, turn)).toList
    }

  extension [M <: GameMoveResult, S <: Tuple, D, A](action: A)(using ev: A <:< GameAction[M, S, D])
    def playDevelopmentCard[Dev, Inv[_]] = PlayApply[Dev, Inv, M, S, D](action)

  final case class PlayApply[Dev, Inv[_], M <: GameMoveResult, S <: Tuple, D](action: GameAction[M, S, D]) {
    // Scala 3 macro-based composition - uses andThen macro
    inline def apply[C](card: C)(using
      inject: CoproductInject[Dev, C],
      dg: DeltaGen[Inv[Dev], PlayCard[Dev]]
    ): GameAction[M, ?, ?] = {
      val playCardAction = PlayDevelopmentCardAction[Dev, Inv].compose[M](m => (inject.apply(card), m.move.player))
      playCardAction.andThen(action)
    }
  }
}
