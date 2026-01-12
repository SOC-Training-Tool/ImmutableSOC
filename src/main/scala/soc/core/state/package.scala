package soc.core

import game.ImmutableGame.StateInitializer
import game.{GameState, InventorySet}
import shapeless.ops.coproduct
import shapeless.{:+:, CNil, Coproduct, Poly1}

package object state {

  case class PlayerIds(players: Seq[Int]) extends GameState[PlayerIds] {
    override type Delta = Nothing
    override def apply(delta: Nothing): PlayerIds = this
  }

  case class MoveCount(count: Int) extends GameState[MoveCount] {
    override type Delta = Int
    override def apply(delta: Int): MoveCount = MoveCount(count + delta)
  }

  case class PlayerBuilding[BB <: Coproduct](player: Int, building: BB)
  object BoardBuildingState {

    type BuildingMap[BB <: Coproduct, T] = Map[T, PlayerBuilding[BB]]

    case class AddBuilding[BB <: Coproduct, T](location: T, player: Int, building: BB)
    case class RemoveBuilding[T](location: T)

    def add[L, B <: BoardBuilding[L], BB <: Coproduct](location: L, building: B, player: Int)(implicit inject: coproduct.Inject[BB, B]) = {
      AddBuilding[BB, L](location, player, inject.apply(building))
    }

    type Delta[BB <: Coproduct, T] = AddBuilding[BB, T] :+: RemoveBuilding[T] :+: CNil

    implicit def initBoardBuildingState[BB <: Coproduct, T]: StateInitializer[BuildingMap[BB, T]] = new StateInitializer[BuildingMap[BB, T]] {
      override def apply(): BuildingMap[BB, T] = Map.empty
    }

    object ApplyBuildingStatePoly extends Poly1 {

      implicit def onAdd[BB <: Coproduct, T]: Case.Aux[(AddBuilding[BB, T], BuildingMap[BB, T]), BuildingMap[BB, T]] =
        at[(AddBuilding[BB, T], BuildingMap[BB, T])] { case (add, map) =>
          map + (add.location -> PlayerBuilding(add.player, add.building))
        }

      implicit def onRemove[BB <: Coproduct, T]: Case.Aux[(RemoveBuilding[T], BuildingMap[BB, T]), BuildingMap[BB, T]] =
        at[(RemoveBuilding[T], BuildingMap[BB, T])] { case (rm, map) => map - rm.location }
    }
  }

  case class VertexBuildingState[BB <: Coproduct](map: BoardBuildingState.BuildingMap[BB, Vertex]) extends GameState[VertexBuildingState[BB]] {
    type Delta = BoardBuildingState.Delta[BB, Vertex]
    override def apply(delta: Delta): VertexBuildingState[BB] = VertexBuildingState(delta.zipConst(map).fold(BoardBuildingState.ApplyBuildingStatePoly))
  }

  case class EdgeBuildingState[BB <: Coproduct](map: BoardBuildingState.BuildingMap[BB, Edge]) extends GameState[EdgeBuildingState[BB]] {
    type Delta = BoardBuildingState.Delta[BB, Edge]
    override def apply(delta: Delta): EdgeBuildingState[BB] = EdgeBuildingState(delta.zipConst(map).fold(BoardBuildingState.ApplyBuildingStatePoly))
  }

  case class Bank[II](b: InventorySet[II, Int]) extends GameState[Bank[II]] {
    override type Delta = Bank.Add[II] :+: Bank.Take[II] :+: CNil
    override def apply(delta: Delta): Bank[II] = Bank(delta.zipConst(b).fold(Bank.ApplyBankStatePoly))
  }

  object Bank {

    case class Add[II](inv: InventorySet[II, Int])
    case class Take[II](inv: InventorySet[II, Int])

    object ApplyBankStatePoly extends Poly1 {

      implicit def onAdd[II]: Case.Aux[(Add[II], InventorySet[II, Int]), InventorySet[II, Int]] =
        at[(Add[II], InventorySet[II, Int])] { case (add, bank) => bank.add(add.inv) }
      implicit def onTake[II]: Case.Aux[(Take[II], InventorySet[II, Int]), InventorySet[II, Int]] =
        at[(Take[II], InventorySet[II, Int])] { case (add, bank) => bank.subtract(add.inv) }
    }
  }

  case class Turn(t: Int) extends GameState[Turn] {
    override type Delta = Int

    override def apply(delta: Int): Turn = Turn(t + delta)
  }

  object Turn {
    implicit val initTurnStat: StateInitializer[Turn] = new StateInitializer[Turn] {
      override def apply(): Turn = Turn(0)
    }
  }

  case class PlayerPoints(points: PlayerMap[Int]) extends GameState[PlayerPoints] {
    override type Delta = PlayerPoints.Increment :+: PlayerPoints.Decrement :+: CNil
    override def apply(delta: Delta): PlayerPoints = PlayerPoints(delta.zipConst(points).fold(PlayerPoints.PlayerPointsDeltaApplyPoly))
  }

  object PlayerPoints {

    case class Increment(player: Int)
    case class Decrement(player: Int)

    object PlayerPointsDeltaApplyPoly extends Poly1 {
      implicit val onIncrement: Case.Aux[(Increment, PlayerMap[Int]), Map[Int, Int]]                            =
        at[(Increment, PlayerMap[Int])] { case (inc, map) => map + (inc.player -> (map.getOrElse(inc.player, 0) + 1)) }
      implicit val onDecrement: Case.Aux[(Decrement, PlayerMap[Int]), Map[Int, Int]] =
        at[(Decrement, PlayerMap[Int])] { case (inc, map) => map + (inc.player -> map.get(inc.player).fold(0)(_ - 1)) }
    }

    implicit def initPlayerPoints(implicit ids: PlayerIds): StateInitializer[PlayerPoints] = new StateInitializer[PlayerPoints] {
      override def apply(): PlayerPoints = PlayerPoints(ids.players.map(_ -> 0).toMap)
    }
  }
}
