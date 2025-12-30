package soc.core

import game.{GameState, ImmutableGame}
import game.ImmutableGame.StateInitializer
import game.InventorySet
import shapeless.{:+:, CNil, Coproduct}

package object state {

  case class PlayerIds(players: Seq[Int]) extends GameState[PlayerIds] {
    override type Delta = Seq[Int] :+: CNil

    implicit val seqCase: folder.Case[Seq[Int]] = folder.at[Seq[Int]](newPlayers => PlayerIds(newPlayers))
  }

  case class PlayerBuilding[BB <: Coproduct](building: BB, player: Int) extends GameState[PlayerBuilding[BB]] {
    override type Delta = (BB, Int) :+: CNil

    implicit val tupleCase: folder.Case[(BB, Int)] = folder.at[(BB, Int)] { case (newBuilding, newPlayer) => PlayerBuilding(newBuilding, newPlayer) }
  }

  case class MoveCount(count: Int) extends GameState[MoveCount] {
    override type Delta = Int :+: CNil

    implicit val intCase: folder.Case[Int] = folder.at[Int](increment => MoveCount(count + increment))
  }

  type BoardBuildingState[BB <: Coproduct, T] = Map[T, PlayerBuilding[BB]]
  type VertexBuildingState[BB <: Coproduct]   = BoardBuildingState[BB, Vertex]
  type EdgeBuildingState[BB <: Coproduct]     = BoardBuildingState[BB, Edge]

  implicit def initBoardBuildingState[BB <: Coproduct, T]: StateInitializer[BoardBuildingState[BB, T]] = new StateInitializer[BoardBuildingState[BB, T]] {
    override def apply(): BoardBuildingState[BB, T] = Map.empty
  }

  case class Bank[II](b: InventorySet[II, Int]) extends GameState[Bank[II]] {
    override type Delta = InventorySet[II, Int] :+: CNil

    implicit val inventoryCase: folder.Case[InventorySet[II, Int]] = folder.at[InventorySet[II, Int]](newInventory => Bank(newInventory))
  }

  case class Turn(t: Int) extends GameState[Turn] {
    override type Delta = Int :+: CNil

    implicit val intCase: folder.Case[Int] = folder.at[Int](newTurn => Turn(newTurn))
  }

  object Turn {
    implicit val initTurnStat: StateInitializer[Turn] = new StateInitializer[Turn] {
      override def apply(): Turn = Turn(0)
    }
  }

  case class PlayerPoints(points: PlayerMap[Int]) extends GameState[PlayerPoints] {
    override type Delta = PlayerMap[Int] :+: CNil

    implicit val playerMapCase: folder.Case[PlayerMap[Int]] = folder.at[PlayerMap[Int]](newPoints => PlayerPoints(newPoints))
  }

  object PlayerPoints {
    implicit def initPlayerPoints(implicit ids: PlayerIds): StateInitializer[PlayerPoints] = new StateInitializer[PlayerPoints] {
      override def apply(): PlayerPoints = PlayerPoints(ids.players.map(_ -> 0).toMap)
    }
  }

}
