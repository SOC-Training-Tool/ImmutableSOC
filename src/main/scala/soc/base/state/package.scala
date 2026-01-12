package soc.base

import game.GameState
import game.ImmutableGame.StateInitializer
import shapeless.{:+:, CNil, Poly1}
import soc.core.PlayerMap
import soc.core.state.PlayerIds

package object state {

  case class RobberLocation(robberHexId: Int) extends GameState[RobberLocation] {
    type Delta = Int

    override def apply(delta: Int): RobberLocation = RobberLocation(delta)
  }

  case class DevelopmentCardDeckSize(size: Int) extends GameState[DevelopmentCardDeckSize] {
    override type Delta = DevelopmentCardDeck.Remove.type

    override def apply(delta: Delta): DevelopmentCardDeckSize = DevelopmentCardDeckSize(size - 1)
  }

  case class DevelopmentCardDeck[Dev](cards: List[Dev]) extends GameState[DevelopmentCardDeck[Dev]] {
    override type Delta = DevelopmentCardDeck.Remove.type

    override def apply(delta: Delta): DevelopmentCardDeck[Dev] = DevelopmentCardDeck(cards.tail)
  }

  object DevelopmentCardDeck {
    case object Remove
  }

  object SpecialPlayer {

    case class Set(player: Int)
    case object Remove

    type Delta = Set :+: Remove.type :+: CNil

    object ApplyDelta extends Poly1 {
      implicit val set   : Case.Aux[Set, Option[Int]]         = at[Set](s => Option(s.player))
      implicit val remove: Case.Aux[Remove.type, Option[Int]] = at[Remove.type](_ => None)
    }
  }

  object SpecialCounts {

    case class Increment(player: Int)
    case class Set(player: Int, length: Int)

    type Delta = Increment :+: Set :+: CNil

    object ApplyDelta extends Poly1 {
      implicit val increment: Case.Aux[(Increment, PlayerMap[Int]), Map[Int, Int]] =
        at[(Increment, PlayerMap[Int])] { case (incr, map) => map + (incr.player -> map.getOrElse(incr.player, 0)) }
      implicit val set      : Case.Aux[(Set, PlayerMap[Int]), Map[Int, Int]]       =
        at[(Set, PlayerMap[Int])] { case (set, map) => map + (set.player -> set.length) }
    }

  }

  case class SOCLongestRoadPlayer(player: Option[Int]) extends GameState[SOCLongestRoadPlayer] {
    override type Delta = SpecialPlayer.Delta

    override def apply(delta: Delta): SOCLongestRoadPlayer = SOCLongestRoadPlayer(delta.fold(SpecialPlayer.ApplyDelta))
  }

  object SOCLongestRoadPlayer {
    implicit val initLongestRoadPlayer: StateInitializer[SOCLongestRoadPlayer] = new StateInitializer[SOCLongestRoadPlayer] {
      override def apply(): SOCLongestRoadPlayer = SOCLongestRoadPlayer(None)
    }
  }

  case class SOCRoadLengths(m: PlayerMap[Int]) extends GameState[SOCRoadLengths] {
    override type Delta = SpecialCounts.Delta

    override def apply(delta: Delta): SOCRoadLengths = SOCRoadLengths(delta.zipConst(m).fold(SpecialCounts.ApplyDelta))
  }

  object SOCRoadLengths {
    implicit def initRoadCount(implicit playerIds: PlayerIds): StateInitializer[SOCRoadLengths] = new StateInitializer[SOCRoadLengths] {
      override def apply(): SOCRoadLengths = SOCRoadLengths(playerIds.players.map(_ -> 0).toMap)
    }
  }

  case class LargestArmyPlayer(player: Option[Int]) extends GameState[LargestArmyPlayer] {
    type Delta = SpecialPlayer.Set

    override def apply(delta: SpecialPlayer.Set): LargestArmyPlayer = LargestArmyPlayer(Some(delta.player))
  }

  object LargestArmyPlayer {
    implicit val initLargestArmyPlayer: StateInitializer[LargestArmyPlayer] = new StateInitializer[LargestArmyPlayer] {
      override def apply(): LargestArmyPlayer = LargestArmyPlayer(None)
    }
  }

  case class PlayerArmyCount(m: PlayerMap[Int]) extends GameState[PlayerArmyCount] {
    type Delta = SpecialCounts.Increment

    override def apply(delta: Delta): PlayerArmyCount = PlayerArmyCount(SpecialCounts.ApplyDelta.increment.apply((delta, m)))
  }

  object PlayerArmyCount {
    implicit def initArmyCount(implicit playerIds: PlayerIds): StateInitializer[PlayerArmyCount] = new StateInitializer[PlayerArmyCount] {
      override def apply(): PlayerArmyCount = PlayerArmyCount(playerIds.players.map(_ -> 0).toMap)
    }
  }
}
