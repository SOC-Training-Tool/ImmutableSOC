package soc.core

import game.{ImmutableGame, InventorySet, StateComponent}
import game.ImmutableGame.StateInitializer
import shapeless.{:+:, CNil, Coproduct, Poly1}

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

  // ═══════════════════════════════════════════════════════════════
  // Bank: Holds the game's resource supply
  // ═══════════════════════════════════════════════════════════════

  object BankDeltas {
    case class Add[II](resources: InventorySet[II, Int])
    case class Subtract[II](resources: InventorySet[II, Int])

    type BankDelta[II] = Add[II] :+: Subtract[II] :+: CNil
  }

  import BankDeltas._

  case class Bank[II](b: InventorySet[II, Int]) extends StateComponent[Bank[II], BankDelta[II]] {

    override def applyDelta(delta: BankDelta[II]): Bank[II] = {
      object ApplyPoly extends Poly1 {
        implicit def add: Case.Aux[Add[II], Bank[II]] =
          at(d => Bank(b.add(d.resources)))

        implicit def subtract: Case.Aux[Subtract[II], Bank[II]] =
          at(d => Bank(b.subtract(d.resources)))
      }
      delta.fold(ApplyPoly)
    }
  }

  // ═══════════════════════════════════════════════════════════════
  // Turn: Tracks the current turn number
  // ═══════════════════════════════════════════════════════════════

  case class Turn(t: Int)

  object Turn {
    implicit val initTurnStat: StateInitializer[Turn] = new StateInitializer[Turn] {
      override def apply(): Turn = Turn(0)
    }
  }

  // ═══════════════════════════════════════════════════════════════
  // PlayerPoints: Tracks victory points per player
  // ═══════════════════════════════════════════════════════════════

  object PointsDeltas {
    case class AddPoints(player: Int, amount: Int)
    case class SubtractPoints(player: Int, amount: Int)

    type PointsDelta = AddPoints :+: SubtractPoints :+: CNil
  }

  import PointsDeltas._

  case class PlayerPoints(points: PlayerMap[Int]) extends StateComponent[PlayerPoints, PointsDelta] {

    override def applyDelta(delta: PointsDelta): PlayerPoints = {
      object ApplyPoly extends Poly1 {
        implicit val add: Case.Aux[AddPoints, PlayerPoints] = at { d =>
          PlayerPoints(points.updated(d.player, points.getOrElse(d.player, 0) + d.amount))
        }

        implicit val subtract: Case.Aux[SubtractPoints, PlayerPoints] = at { d =>
          PlayerPoints(points.updated(d.player, points.getOrElse(d.player, 0) - d.amount))
        }
      }
      delta.fold(ApplyPoly)
    }
  }

  object PlayerPoints {
    implicit def initPlayerPoints(implicit ids: PlayerIds): StateInitializer[PlayerPoints] = new StateInitializer[PlayerPoints] {
      override def apply(): PlayerPoints = PlayerPoints(ids.players.map(_ -> 0).toMap)
    }
  }

}
