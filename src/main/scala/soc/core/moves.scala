package soc.core

import game.{:+:, CNil, GameMove, GameMoveResult, InventorySet, PerfectInfoMoveResult, PerfectInformationGameMove}

case class EndTurnMove(player: Int) extends PerfectInformationGameMove[EndTurnMove]

case class DiscardMove[II](player: Int, set: InventorySet[II, Int]) extends PerfectInformationGameMove[DiscardMove[II]]

case class TradeMove[II](player: Int, partner: Int, give: InventorySet[II, Int], get: InventorySet[II, Int]) extends PerfectInformationGameMove[TradeMove[II]]

case class RollDiceMove(player: Int) extends GameMove

case class RollDiceMoveResult(player: Int, result: Int) extends PerfectInfoMoveResult {
  override type A = RollDiceMove

  override def move: A = RollDiceMove(player)

  override type PublicInfoMoveResult = RollDiceMoveResult

  override def getPerspectiveResults(playerIds: Seq[Int]): Map[Int, RollDiceMoveResult] = playerIds.map(_ -> this).toMap
}

case class BuildCityMove(player: Int, vertex: Vertex) extends PerfectInformationGameMove[BuildCityMove]

case class BuildRoadMove(player: Int, edge: Edge) extends PerfectInformationGameMove[BuildRoadMove]

case class BuildSettlementMove(player: Int, vertex: Vertex) extends PerfectInformationGameMove[BuildSettlementMove]

case class InitialPlacementMove(vertex: Vertex, edge: Edge, player: Int) extends PerfectInformationGameMove[InitialPlacementMove]

object moves {

  type CORE_MOVES[II] = EndTurnMove :+: DiscardMove[II] :+: TradeMove[II] :+: RollDiceMove :+: BuildCityMove :+: BuildSettlementMove :+: BuildRoadMove :+: InitialPlacementMove :+: CNil

}
