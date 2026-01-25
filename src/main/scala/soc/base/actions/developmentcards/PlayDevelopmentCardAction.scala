package soc.base.actions.developmentcards

import game.Delta.DeltaGen
import game.{Delta, DeltaList, GameAction, GameMoveResult, GameState, MergeDelta, MergeState}
import shapeless.ops.coproduct
import shapeless.{:+:, ::, CNil, Coproduct, HNil}
import soc.core.DevTransactions.PlayCard
import soc.core.state.Turn

object PlayDevelopmentCardAction {

  def apply[Dev <: Coproduct, Inv[_]](implicit dg: DeltaGen[Inv[Dev], PlayCard[Dev]]): GameAction[(Dev, Int), Turn :: HNil, Delta[Inv[Dev]] :+: CNil] =
    GameAction.fromState[(Dev, Int), Turn :: HNil] { case ((card, player), state) =>
      val turn = state.select[Turn].t
      DeltaList().add[Inv[Dev]](PlayCard(card, player, turn)).toList
    }

  implicit class PlayDevelopmentCardActionOps[M <: GameMoveResult, S, D, A](action: A)(implicit ev: A <:< GameAction[M, S, D]) {
    def playDevelopmentCard[Dev <: Coproduct, Inv[_]] = PlayApply[Dev, Inv, M, S, D](action)
  }

  final case class PlayApply[Dev <: Coproduct, Inv[_], M <: GameMoveResult, S, D](action: GameAction[M, S, D]) {
    def apply[C, OutS, OutD](card: C)(implicit inject: coproduct.Inject[Dev, C], dg: DeltaGen[Inv[Dev], PlayCard[Dev]], ms: MergeState.Aux[Turn :: HNil, S, OutS], md: MergeDelta.Aux[Delta[Inv[Dev]] :+: CNil, D, OutD]): GameAction[M, OutS, OutD] = {
      PlayDevelopmentCardAction[Dev, Inv].compose[M](m => (inject.apply(card), m.move.player))
        .andThen(action)
    }
  }
}

