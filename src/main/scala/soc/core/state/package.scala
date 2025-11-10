package soc.core

import game.ImmutableGame.StateInitializer
import game.InventorySet
import shapeless.Coproduct

package object state {

  case class PlayerIds(players: Seq[Int])

  case class PlayerBuilding[BB <: Coproduct](building: BB, player: Int)

  case class MoveCount(count: Int)

  type BoardBuildingState[BB <: Coproduct, T] = Map[T, PlayerBuilding[BB]]
  type VertexBuildingState[BB <: Coproduct]   = BoardBuildingState[BB, Vertex]
  type EdgeBuildingState[BB <: Coproduct]     = BoardBuildingState[BB, Edge]

  implicit def initBoardBuildingState[BB <: Coproduct, T]: StateInitializer[BoardBuildingState[BB, T]] = new StateInitializer[BoardBuildingState[BB, T]] {
    override def apply(): BoardBuildingState[BB, T] = Map.empty
  }

  case class Bank[II](b: InventorySet[II, Int])

  case class Turn(t: Int)

  object Turn {
    implicit val initTurnStat: StateInitializer[Turn] = new StateInitializer[Turn] {
      override def apply(): Turn = Turn(0)
    }
  }

  case class PlayerPoints(points: PlayerMap[Int])

  object PlayerPoints {
    implicit def initPlayerPoints(implicit ids: PlayerIds): StateInitializer[PlayerPoints] = new StateInitializer[PlayerPoints] {
      override def apply(): PlayerPoints = PlayerPoints(ids.players.map(_ -> 0).toMap)
    }
  }

}
