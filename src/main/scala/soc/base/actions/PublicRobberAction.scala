package soc.base.actions

import game.{GameAction, NoInput}
import soc.base.*
import soc.base.state.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.Transactions.*

case class PublicRobberOutput(
  newRobberLocation: RobberLocation#Delta,
  steal:             Option[PublicInventories[Resource]#Delta]
)

class PublicRobberAction extends GameAction[RobberMoveResult[Resource], NoInput.type, PublicRobberOutput]:
  def apply(move: RobberMoveResult[Resource], input: NoInput.type): PublicRobberOutput =
    val stealDelta = move.steal.map { steal =>
      ImperfectInfoExchange[Resource](steal.victim, move.player, steal.resource)
    }
    PublicRobberOutput(RobberLocation.Delta(move.robberHexId), stealDelta)
