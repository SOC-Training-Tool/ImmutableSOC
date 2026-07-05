package soc.base.actions

import game.{GameAction, InventorySet}
import soc.base.state.*
import soc.base.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.ResourceSet.Resources
import soc.core.Transactions.*
import soc.core.state.*
import soc.core.SOCBoard.SOCBoardOps

case class RollDiceInput(
  robberLocation:      RobberLocation,
  board:               BaseBoard[Resource],
  vertexBuildingState: VertexBuildingState[BaseVertexBuilding],
  bank:                Bank[Resource]
)

case class RollDiceOutput(
  bankLost:    Option[Bank[Resource]#Delta],
  playerGains: List[Gain[Resource]]
)

class RollDiceAction extends GameAction[RollDiceMoveResult, RollDiceInput, RollDiceOutput]:
  def apply(move: RollDiceMoveResult, input: RollDiceInput): RollDiceOutput =
    val robberHex = input.robberLocation.robberHexId
    val (gainedHexes, _) = input.board.numberHexes
      .getOrElse(move.result, Nil)
      .partition(_.node != robberHex)
    val playerGains: Map[Int, Resources] = (for
      node     <- gainedHexes
      resource <- node.hex.getResource.toSeq
      vertex   <- node.vertices
      vb       <- input.vertexBuildingState.map.get(vertex).toSeq
      amt       = vertexBuildingResources(vb.building)
    yield vb.player -> InventorySet.fromMap(Map(resource -> amt)))
      .foldLeft(Map.empty[Int, Resources]) { case (acc, (p, inv)) =>
        acc + (p -> acc.get(p).fold(inv)(_.add(inv)))
      }
    val totalCollected = playerGains.values.foldLeft(InventorySet.empty[Resource, Int])(_.add(_))
    val overflowTypes  = totalCollected.getTypes.filter(r => !input.bank.b.contains(totalCollected.getAmount(r), r))
    val actual: Map[Int, Resources] = playerGains.map { case (p, inv) =>
      p -> overflowTypes.foldLeft(inv)((set, r) => set.subtract(set.getAmount(r), r))
    }
    val trueTotal = actual.values.foldLeft(InventorySet.empty[Resource, Int])(_.add(_))
    val gainList  = actual.toList.flatMap { case (p, inv) =>
      if inv.getTotal == 0 then Nil
      else List(Gain(p, inv))
    }
    val bankLost  = if trueTotal.getTotal > 0 then Some(Bank.Take(trueTotal)) else None
    RollDiceOutput(bankLost, gainList)
