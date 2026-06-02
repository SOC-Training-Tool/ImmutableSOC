package game

trait GameMove:
  def player: Int

trait GameMoveResult:
  type A <: GameMove
  def move: A

trait PerfectInfoMoveResult extends GameMoveResult:
  type PublicInfoMoveResult <: GameMoveResult
  def getPerspectiveResults(playerIds: Seq[Int]): Map[Int, PublicInfoMoveResult]

trait PerfectInformationGameMove[A0 <: GameMove & PerfectInformationGameMove[A0]]
    extends GameMove with PerfectInfoMoveResult:
  self: A0 =>
  override type A                    = A0
  override type PublicInfoMoveResult = A0
  override def move: A               = self
  override def getPerspectiveResults(playerIds: Seq[Int]): Map[Int, A0] =
    playerIds.map(id => id -> self).toMap
