package soc.base.actions

import game.Delta.DeltaGen
import game.{Delta, DeltaList, GameAction, GameState, InventorySet}
import shapeless.ops.coproduct
import shapeless.{:+:, ::, CNil, Coproduct, HNil, Poly1}
import soc.base.state.RobberLocation
import soc.core.SOCBoard.SOCBoardOps
import soc.core.Transactions.Gain
import soc.core.state.{Bank, VertexBuildingState}
import soc.core.{BoardHex, RollDiceMoveResult, SOCBoard, VertexBuildingValue}

object RollDiceAction {

  type ResForVertex[VB <: Coproduct] = coproduct.Folder.Aux[ResourcesForBuildingPoly.type, VB, Int]
  object ResourcesForBuildingPoly extends Poly1 {
    implicit def vertexBuilding[A](implicit vbValue: VertexBuildingValue[A]): Case.Aux[A, Int] = at(_ => vbValue.apply)
  }

  def apply[II, Inv[_], BOARD, VB <: Coproduct]()(implicit delta: DeltaGen[Inv[II], Gain[II]], b: SOCBoard[II, BOARD], vertexFolder: ResForVertex[VB]): GameAction[RollDiceMoveResult, BOARD :: RobberLocation :: Bank[II] :: VertexBuildingState[VB] :: HNil, Delta[Inv[II]] :+: Delta[Bank[II]] :+: CNil] = {

    GameAction.fromState[RollDiceMoveResult, BOARD :: RobberLocation :: Bank[II] :: VertexBuildingState[VB] :: HNil] { case (move, state) =>

      val robberHexId       = state.select[RobberLocation].robberHexId
      val vertexBuildingMap = state.select[VertexBuildingState[VB]].map
      val bank              = state.select[Bank[II]].b
      val board             = state.select[BOARD]

      def resourcesFromHex(hexes: Seq[BoardHex[II]]) = {
        val playerGains = for {
          node <- hexes
          resource <- node.hex.getResource.toSeq
          vertex <- node.vertices
          vb <- vertexBuildingMap.get(vertex).toSeq
          player = vb.player
          amt = vb.building.fold(ResourcesForBuildingPoly)
        } yield player -> InventorySet.fromMap(Map(resource -> amt))
        playerGains.foldLeft(Map.empty[Int, InventorySet[II, Int]]) { case (m, (player, res)) =>
          m + (player -> res)
        }
      }

      val (gainedHex, blockedHex) = board.numberHexes
        .get(move.result)
        .fold[(Seq[BoardHex[II]], Seq[BoardHex[II]])]((Nil, Nil))(_.partition(_.node != robberHexId))

      val gained = resourcesFromHex(gainedHex)
      //val blocked = resourcesFromHex(blockedHex)

      val totalResourcesCollected: InventorySet[II, Int]           = gained.values.foldLeft(InventorySet.empty[II, Int])(_.add(_))
      val actualResForPlayers    : Map[Int, InventorySet[II, Int]] = {
        val resTypes: Seq[II] = totalResourcesCollected.getTypes
        val overflowTypes     = resTypes.filter(item => !bank.contains(totalResourcesCollected.getAmount(item), item))
        gained.map[Int, InventorySet[II, Int]] { case (player, resourceSet) =>
          player -> overflowTypes.foldLeft(resourceSet) { case (set, res) => set.subtract(set.getAmount(res), res) }
        }
      }
      val trueTotalCollected     : InventorySet[II, Int]           = actualResForPlayers.values.foldLeft(InventorySet.empty[II, Int])(_.add(_))

      val gains = actualResForPlayers.map { case (player, inv) => Gain(player, inv) }.toList
      DeltaList()
        .add[Bank[II]](Bank.Take(trueTotalCollected))
        .add[Inv[II]](gains: _*)
        .toList
    }
  }
}