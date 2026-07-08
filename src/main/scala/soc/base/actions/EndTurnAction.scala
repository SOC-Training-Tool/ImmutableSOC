package soc.base.actions

import game.{GameAction, NoInput}
import soc.core.*
import soc.core.state.*

case class EndTurnOutput(turnIncrement: Turn#Delta)

class EndTurnAction extends GameAction[EndTurnMove, NoInput.type, EndTurnOutput]:
  def apply(move: EndTurnMove, input: NoInput.type): EndTurnOutput =
    EndTurnOutput(Turn.Delta(1))
