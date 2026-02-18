error id: file://<WORKSPACE>/src/main/scala/soc/base/actions/RollDiceAction.scala:game/Delta.DeltaGen#
file://<WORKSPACE>/src/main/scala/soc/base/actions/RollDiceAction.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 1107
uri: file://<WORKSPACE>/src/main/scala/soc/base/actions/RollDiceAction.scala
text:
```scala
package soc.base.actions

import game.Delta.DeltaGen
import game.{Delta, DeltaList, GameAction, GameState, InventorySet, :+:, CNil, Inl, Inr, tupleSelect}
import soc.base.state.RobberLocation
import soc.core.SOCBoard.{hexesForVertex, numberHexes}
import soc.core.Transactions.Gain
import soc.core.state.{Bank, VertexBuildingState}
import soc.core.{BoardHex, RollDiceMoveResult, SOCBoard, VertexBuildingValue}

object RollDiceAction {

  trait VertexBuildingFolder[VB] {
    def fold(vb: VB): Int
  }

  object VertexBuildingFolder {
    given cnilFolder: VertexBuildingFolder[CNil] = new VertexBuildingFolder[CNil] {
      def fold(vb: CNil): Int = vb.impossible
    }

    given consFolder[H, T](using VertexBuildingValue[H], VertexBuildingFolder[T]): VertexBuildingFolder[H :+: T] = new VertexBuildingFolder[H :+: T] {
      def fold(vb: H :+: T): Int = vb match
        case Inl(h) => summon[VertexBuildingValue[H]].apply
        case Inr(t) => summon[VertexBuildingFolder[T]].fold(t)
    }
  }

  type ResForVertex[VB] = VertexBuildingFolder[VB]

  def apply[II, Inv[_], BOARD, VB]()(using delta: DeltaG@@en[Inv[II], Gain[II]], b: SOCBoard[II, BOARD], vertexFolder: ResForVertex[VB]): GameAction[RollDiceMoveResult, (BOARD, RobberLocation, Bank[II], VertexBuildingState[VB]), Delta[Inv[II]] :+: Delta[Bank[II]] :+: CNil] = {

    GameAction.fromState[RollDiceMoveResult, (BOARD, RobberLocation, Bank[II], VertexBuildingState[VB])] { case (move, state) =>

      val robberHexId       = tupleSelect[(BOARD, RobberLocation, Bank[II], VertexBuildingState[VB]), RobberLocation](state).robberHexId
      val vertexBuildingMap = tupleSelect[(BOARD, RobberLocation, Bank[II], VertexBuildingState[VB]), VertexBuildingState[VB]](state).map
      val bank              = tupleSelect[(BOARD, RobberLocation, Bank[II], VertexBuildingState[VB]), Bank[II]](state).b
      val board             = tupleSelect[(BOARD, RobberLocation, Bank[II], VertexBuildingState[VB]), BOARD](state)

      def resourcesFromHex(hexes: Seq[BoardHex[II]]) = {
        val playerGains = for {
          node <- hexes
          resource <- node.hex.getResource.toSeq
          vertex <- node.vertices
          vb <- vertexBuildingMap.get(vertex).toSeq
          player = vb.player
          amt = vertexFolder.fold(vb.building)
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
        .add[Inv[II]](gains*)
        .toList
    }
  }
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: 