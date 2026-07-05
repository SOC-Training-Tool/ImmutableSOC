package soc.base.actions

import game.{GameAction, InventorySet, NoInput}
import soc.base.*
import soc.base.state.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.Transactions.*

case class PerfectRobberOutput(
  newRobberLocation: RobberLocation#Delta,
  steals:            List[Gain[Resource] | Lose[Resource]]
)

class PerfectRobberAction extends GameAction[PerfectInfoRobberMoveResult[Resource], NoInput.type, PerfectRobberOutput]:
  def apply(move: PerfectInfoRobberMoveResult[Resource], input: NoInput.type): PerfectRobberOutput =
    val stealDeltas: List[Gain[Resource] | Lose[Resource]] = move.steal.toList.flatMap { steal =>
      val inv = InventorySet.fromList(Seq(steal.resource))
      List(Gain(move.player, inv), Lose(steal.victim, inv))
    }
    PerfectRobberOutput(move.robberHexId, stealDeltas)
