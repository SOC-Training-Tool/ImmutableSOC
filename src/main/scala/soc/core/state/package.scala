package soc.core

import game.{GameState, InventorySet}

package object state:

  case class MoveCount(count: Int) extends GameState[MoveCount]:
    import MoveCount.Delta
    override type Delta = MoveCount.Delta
    override def apply(delta: Delta): MoveCount = MoveCount(count + delta.n)

  object MoveCount:
    case class Delta(n: Int)

  case class SetupPlacementOrder(placements: List[(Int, Vertex)])
      extends GameState[SetupPlacementOrder]:
    type Delta = SetupPlacementOrder.Placement
    override def apply(delta: Delta): SetupPlacementOrder =
      SetupPlacementOrder(placements :+ (delta.player -> delta.vertex))

  object SetupPlacementOrder:
    case class Placement(player: Int, vertex: Vertex)

  case class PlayerBuilding[+BB](player: Int, building: BB)

  object BoardBuildingState:

    type BuildingMap[BB, T] = Map[T, PlayerBuilding[BB]]

    case class AddBuilding[+BB, T](location: T, player: Int, building: BB)
    case class RemoveBuilding[T](location: T)

    def add[L, B](location: L, building: B, player: Int): AddBuilding[B, L] =
      AddBuilding(location, player, building)

    type Delta[BB, T] = AddBuilding[BB, T] | RemoveBuilding[T]

  case class VertexBuildingState[BB](map: BoardBuildingState.BuildingMap[BB, Vertex])
      extends GameState[VertexBuildingState[BB]]:
    type Delta = BoardBuildingState.Delta[BB, Vertex]
    override def apply(delta: Delta): VertexBuildingState[BB] = delta match
      case BoardBuildingState.AddBuilding(location, player, building) =>
        VertexBuildingState(map + (location -> PlayerBuilding(player, building.asInstanceOf[BB])))
      case BoardBuildingState.RemoveBuilding(location) =>
        VertexBuildingState(map - location.asInstanceOf[Vertex])

  case class EdgeBuildingState[BB](map: BoardBuildingState.BuildingMap[BB, Edge])
      extends GameState[EdgeBuildingState[BB]]:
    type Delta = BoardBuildingState.Delta[BB, Edge]
    override def apply(delta: Delta): EdgeBuildingState[BB] = delta match
      case BoardBuildingState.AddBuilding(location, player, building) =>
        EdgeBuildingState(map + (location -> PlayerBuilding(player, building.asInstanceOf[BB])))
      case BoardBuildingState.RemoveBuilding(location) =>
        EdgeBuildingState(map - location.asInstanceOf[Edge])

  case class Bank[II](b: InventorySet[II, Int]) extends GameState[Bank[II]]:
    type Delta = Bank.Add[II] | Bank.Take[II]
    override def apply(delta: Delta): Bank[II] = delta match
      case Bank.Add(inv)  => Bank(b.add(inv))
      case Bank.Take(inv) => Bank(b.subtract(inv))

  object Bank:
    case class Add[II](inv: InventorySet[II, Int])
    case class Take[II](inv: InventorySet[II, Int])

  case class Turn(number: Int) extends GameState[Turn]:
    import Turn.Delta
    override type Delta = Turn.Delta
    override def apply(delta: Delta): Turn = Turn(number + delta.n)

  object Turn:
    case class Delta(n: Int)

  case class PlayerPoints(points: PlayerMap[Int]) extends GameState[PlayerPoints]:
    override type Delta = PlayerPoints.Increment | PlayerPoints.Decrement
    override def apply(delta: Delta): PlayerPoints = delta match
      case PlayerPoints.Increment(player) =>
        PlayerPoints(points + (player -> (points.getOrElse(player, 0) + 1)))
      case PlayerPoints.Decrement(player) =>
        PlayerPoints(points + (player -> points.get(player).fold(0)(_ - 1)))

  object PlayerPoints:
    case class Increment(player: Int)
    case class Decrement(player: Int)
