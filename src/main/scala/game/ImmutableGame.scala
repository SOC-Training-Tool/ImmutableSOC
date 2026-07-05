package game

trait ImmutableGame[MOVES, STATE, DELTA]:
  def applyMove(move: MOVES, state: STATE): (List[DELTA], STATE)