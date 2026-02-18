package soc.base.actions

import game.Delta.DeltaGen
import game.{Delta, DeltaList, GameAction, GameState, InventorySet, :+:, CNil, CoproductInject, tupleSelect}
import soc.core.SOCBoard.hexesForVertex
import soc.core.Transactions.PerfectInfo
import soc.core.state._
import soc.core._

object InitialPlacementAction {

  def apply[II, Inv[_], BOARD, EB, VB]()(using
    gen: DeltaGen[Inv[II], PerfectInfo[II]],
    vbInject: CoproductInject[VB, Settlement.type],
    ebInject: CoproductInject[EB, Road.type],
    socBoard: SOCBoard[II, BOARD]
  ): GameAction[InitialPlacementMove, (BOARD, MoveCount, PlayerPoints), Delta[Bank[II]] :+: Delta[Inv[II]] :+: Delta[EdgeBuildingState[EB]] :+: Delta[PlayerPoints] :+: Delta[VertexBuildingState[VB]] :+: CNil] =
    GameAction.fromState[InitialPlacementMove, (BOARD, MoveCount, PlayerPoints)] { case (move, state) =>
      val board      = tupleSelect[(BOARD, MoveCount, PlayerPoints), BOARD](state)
      val moveCount  = tupleSelect[(BOARD, MoveCount, PlayerPoints), MoveCount](state).count
      val numPlayers = tupleSelect[(BOARD, MoveCount, PlayerPoints), PlayerPoints](state).points.keys.size

      val (gain, take) = Option.when(moveCount >= numPlayers) {
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
        .add[Inv[II]](gain.toList*)
        .add[Bank[II]](take.toList*)
        .toList
    }
}
