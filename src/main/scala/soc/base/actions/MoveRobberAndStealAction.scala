package soc.base.actions

import game.Delta.DeltaGen
import game._
import shapeless.{:+:, CNil, HNil}
import soc.base.state.RobberLocation
import soc.base.{PerfectInfoRobberMoveResult, RobberMoveResult}
import soc.core.Transactions

object MoveRobberAndStealAction {

  def public[II, Inv[_] <: GameState[Inv[II]]](implicit delta: DeltaGen[Inv[II], Transactions.ImperfectInfoExchange[II]]): GameAction[RobberMoveResult[II], HNil, Delta[Inv[II]] :+: Delta[RobberLocation] :+: CNil] =
    GameAction.apply[RobberMoveResult[II]] { move =>
      val steal = move.steal.map(s => Transactions.ImperfectInfoExchange[II](s.victim, move.player, s.resource))
      DeltaList()
        .add[RobberLocation](move.robberHexId)
        .add[Inv[II]](steal.toList:_*)
        .toList
    }

  def perfect[II, Inv[_] <: GameState[Inv[II]]](implicit delta: DeltaGen[Inv[II], Transactions.PerfectInfo[II]]): GameAction[PerfectInfoRobberMoveResult[II], HNil, Delta[Inv[II]] :+: Delta[RobberLocation] :+: CNil] =
    GameAction.apply[PerfectInfoRobberMoveResult[II]] { move =>
      val (gain, lose) = move.steal.map { s =>
        val inv = InventorySet.fromList(Seq(s.resource))
        (Transactions.Gain(move.player, inv), Transactions.Lose(s.victim, inv))
      }.unzip
      DeltaList()
        .add[RobberLocation](move.robberHexId)
        .add[Inv[II]](gain.toList:_*)
        .add[Inv[II]](lose.toList:_*)
        .toList
    }
}
