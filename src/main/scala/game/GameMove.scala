package game

import game.GameMoveResult.Aux
import shapeless.Poly1

trait GameMove {
  type R <: GameMoveResult

  def player: Int
}

trait GameMoveResult {
  type A <: GameMove

  def move: A
}

trait PerfectInfoMoveResult extends GameMoveResult {
  type PublicInfoMoveResult <: Aux[A]
  def getPerspectiveResults(playerIds: Seq[Int]): Map[Int, PublicInfoMoveResult]

}

object GameMoveResult {
  type Aux[M <: GameMove]               = GameMoveResult { type A = M }
  type PAux[M <: GameMove, R <: Aux[M]] = PerfectInfoMoveResult { type ImperfectInfoMoveResult = R }
}

trait PerfectInformationGameMove[A0 <: GameMove with PerfectInformationGameMove[A0]] extends GameMove with PerfectInfoMoveResult {
  self: A0 =>
  override type A                       = A0
  override type PublicInfoMoveResult = A0

  override def move: A                                                                       = self
  override def getPerspectiveResults(playerIds: Seq[Int]): Map[Int, PublicInfoMoveResult] = playerIds.map(id => id -> self).toMap
}

object FromResultPoly extends Poly1 {
  implicit def result[M <: GameMove, R <: GameMoveResult.Aux[M]]: Case.Aux[R, M] = at(_.move)
}


