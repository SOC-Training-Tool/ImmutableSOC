package soc.base.actions

import game.Delta.DeltaGen
import game.{Delta, DeltaList, GameAction, GameState, InventorySet}
import shapeless.ops.coproduct
import shapeless.{:+:, ::, CNil, Coproduct, HNil}
import soc.core.SOCBoard.SOCBoardOps
import soc.core.Transactions.PerfectInfo
import soc.core.state._
import soc.core._

object InitialPlacementAction {

  def apply[II, Inv[_], BOARD, EB <: Coproduct, VB <: Coproduct]()(
    implicit
    gen: DeltaGen[Inv[II], PerfectInfo[II]],
    vbInject: coproduct.Inject[VB, Settlement.type],
    ebInject: coproduct.Inject[EB, Road.type],
    socBoard: SOCBoard[II, BOARD]
  ): GameAction[InitialPlacementMove, BOARD :: MoveCount :: PlayerPoints :: HNil, Delta[Bank[II]] :+: Delta[Inv[II]] :+: Delta[EdgeBuildingState[EB]] :+: Delta[PlayerPoints] :+: Delta[VertexBuildingState[VB]] :+: CNil] =
    GameAction.fromState[InitialPlacementMove, BOARD :: MoveCount :: PlayerPoints :: HNil] { case (move, state) =>
      val board      = state.select[BOARD]
      val moveCount  = state.select[MoveCount].count
      val numPlayers = state.select[PlayerPoints].points.keys.size

      val (gain, take) = Option.apply().filter(_ => moveCount >= numPlayers).map { _ =>
        val resources = board.hexesForVertex
          .get(move.vertex)
          .fold[Seq[II]](Nil)(_.flatMap(_.hex.getResource))
        val inv       = InventorySet.fromList(resources)
        (Transactions.Gain(move.player, inv), Bank.Take(inv))
      }.unzip

      DeltaList()
        .add[VertexBuildingState[VB]](BoardBuildingState.add(move.vertex, Settlement, move.player))
        .add[PlayerPoints](PlayerPoints.Increment(move.player))
        .add[EdgeBuildingState[EB]](BoardBuildingState.add(move.edge, Road, move.player))
        .add[Inv[II]](gain.toList: _*)
        .add[Bank[II]](take.toList: _*)
        .toList
    }
}