package soc.base

import game.GameState

final case class IntGameState(value: Int) extends GameState[IntGameState]:
  override type Delta = IntGameState.Delta
  override def apply(delta: Delta): IntGameState = IntGameState(value + delta.by)

object IntGameState:
  final case class Delta(by: Int)

final case class StringGameState(text: String) extends GameState[StringGameState]:
  override type Delta = StringGameState.Delta
  override def apply(delta: Delta): StringGameState = StringGameState(delta.text)

object StringGameState:
  final case class Delta(text: String)

final case class UnionGameState(intValue: Int, stringValue: String) extends GameState[UnionGameState]:
  override type Delta = Int | String
  override def apply(delta: Delta): UnionGameState =
    delta match
      case i: Int    => copy(intValue = intValue + i)
      case s: String => copy(stringValue = stringValue + s)

final case class StatePair(int: IntGameState, str: StringGameState)
final case class StatePairDeltas(int: IntGameState.Delta, str: StringGameState.Delta)
