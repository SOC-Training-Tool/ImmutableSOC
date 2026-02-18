package soc.core

import game.{:+:, CNil, Inl, Inr, CoproductInject, GameState, InventorySet}
import game.ImmutableGame.StateInitializer

package object state {

  case class PlayerIds(players: Seq[Int]) extends GameState[PlayerIds] {
    override type Delta = Nothing
    override def apply(delta: Nothing): PlayerIds = this
  }

  case class MoveCount(count: Int) extends GameState[MoveCount] {
    override type Delta = Int
    override def apply(delta: Int): MoveCount = MoveCount(count + delta)
  }

  case class PlayerBuilding[BB](player: Int, building: BB)
  object BoardBuildingState {

    type BuildingMap[BB, T] = Map[T, PlayerBuilding[BB]]

    case class AddBuilding[BB, T](location: T, player: Int, building: BB)
    case class RemoveBuilding[T](location: T)

    def add[L, B <: BoardBuilding[L], BB](location: L, building: B, player: Int)(using inject: CoproductInject[BB, B]) = {
      AddBuilding[BB, L](location, player, inject(building))
    }

    type Delta[BB, T] = AddBuilding[BB, T] :+: RemoveBuilding[T] :+: CNil

    given initBoardBuildingState[BB, T]: StateInitializer[BuildingMap[BB, T]] with
      def apply(): BuildingMap[BB, T] = Map.empty
  }

  case class VertexBuildingState[BB](map: BoardBuildingState.BuildingMap[BB, Vertex]) extends GameState[VertexBuildingState[BB]] {
    type Delta = BoardBuildingState.Delta[BB, Vertex]
    override def apply(delta: Delta): VertexBuildingState[BB] = VertexBuildingState(delta match {
      case Inl(add)          => map + (add.location -> PlayerBuilding(add.player, add.building))
      case Inr(Inl(rm))      => map - rm.location
      case Inr(Inr(cnil))    => cnil.impossible
    })
  }

  case class EdgeBuildingState[BB](map: BoardBuildingState.BuildingMap[BB, Edge]) extends GameState[EdgeBuildingState[BB]] {
    type Delta = BoardBuildingState.Delta[BB, Edge]
    override def apply(delta: Delta): EdgeBuildingState[BB] = EdgeBuildingState(delta match {
      case Inl(add)          => map + (add.location -> PlayerBuilding(add.player, add.building))
      case Inr(Inl(rm))      => map - rm.location
      case Inr(Inr(cnil))    => cnil.impossible
    })
  }

  case class Bank[II](b: InventorySet[II, Int]) extends GameState[Bank[II]] {
    override type Delta = Bank.Add[II] :+: Bank.Take[II] :+: CNil
    override def apply(delta: Delta): Bank[II] = Bank(delta match {
      case Inl(add)       => b.add(add.inv)
      case Inr(Inl(take)) => b.subtract(take.inv)
      case Inr(Inr(cnil)) => cnil.impossible
    })
  }

  object Bank {

    case class Add[II](inv: InventorySet[II, Int])
    case class Take[II](inv: InventorySet[II, Int])
  }

  case class Turn(t: Int) extends GameState[Turn] {
    override type Delta = Int

    override def apply(delta: Int): Turn = Turn(t + delta)
  }

  object Turn {
    given initTurnStat: StateInitializer[Turn] with
      def apply(): Turn = Turn(0)
  }

  case class PlayerPoints(points: PlayerMap[Int]) extends GameState[PlayerPoints] {
    override type Delta = PlayerPoints.Increment :+: PlayerPoints.Decrement :+: CNil
    override def apply(delta: Delta): PlayerPoints = PlayerPoints(delta match {
      case Inl(inc) => points + (inc.player -> (points.getOrElse(inc.player, 0) + 1))
      case Inr(Inl(dec)) => points + (dec.player -> points.get(dec.player).fold(0)(_ - 1))
      case Inr(Inr(cnil)) => cnil.impossible
    })
  }

  object PlayerPoints {

    case class Increment(player: Int)
    case class Decrement(player: Int)

    given initPlayerPoints(using ids: PlayerIds): StateInitializer[PlayerPoints] with
      def apply(): PlayerPoints = PlayerPoints(ids.players.map(_ -> 0).toMap)
  }
}
