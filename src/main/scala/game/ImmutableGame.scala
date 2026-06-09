package game

trait ImmutableGame[MOVES, STATE, DELTA]:
  def applyMove(move: MOVES, state: STATE): (List[DELTA], STATE)


object ImmutableGame {
  
  def apply[MOVES <: |, STATE <: Tuple](move: MOVES, state: STATE)(given )
  
}