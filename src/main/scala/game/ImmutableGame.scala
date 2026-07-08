package game

import scala.reflect.ClassTag

trait ImmutableGame[MOVES, STATE]:
  type OutFor[M <: MOVES]
  def applyMove[M <: MOVES](move: M, state: STATE)(using ClassTag[M]): (OutFor[M], STATE)