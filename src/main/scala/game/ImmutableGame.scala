package game

import scala.reflect.ClassTag

trait ImmutableGame[MOVES, STATE]:
  type OutFor[M <: MOVES]
  type AllOutputs
  def applyMove[M <: MOVES](move: M, state: STATE)(using ClassTag[M]): (OutFor[M], STATE)
  def applyMoveAny(move: MOVES, state: STATE): (AllOutputs, STATE)