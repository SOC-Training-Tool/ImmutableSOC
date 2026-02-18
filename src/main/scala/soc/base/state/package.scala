package soc.base

import game.{:+:, CNil, Inl, Inr, GameState}
import game.ImmutableGame.StateInitializer
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

    def applyDelta(delta: Delta): Option[Int] = delta match {
      case Inl(s)          => Option(s.player)
      case Inr(Inl(_))     => None
      case Inr(Inr(cnil))  => cnil.impossible
    }
  }

  object SpecialCounts {

    case class Increment(player: Int)
    case class Set(player: Int, length: Int)

    type Delta = Increment :+: Set :+: CNil

    def applyDelta(delta: Delta, map: PlayerMap[Int]): Map[Int, Int] = delta match {
      case Inl(incr)       => map + (incr.player -> map.getOrElse(incr.player, 0))
      case Inr(Inl(set))   => map + (set.player -> set.length)
      case Inr(Inr(cnil))  => cnil.impossible
    }

    def applyIncrement(incr: Increment, map: PlayerMap[Int]): Map[Int, Int] =
      map + (incr.player -> map.getOrElse(incr.player, 0))
  }

  case class SOCLongestRoadPlayer(player: Option[Int]) extends GameState[SOCLongestRoadPlayer] {
    override type Delta = SpecialPlayer.Delta

    override def apply(delta: Delta): SOCLongestRoadPlayer = SOCLongestRoadPlayer(SpecialPlayer.applyDelta(delta))
  }

  object SOCLongestRoadPlayer {
    given initLongestRoadPlayer: StateInitializer[SOCLongestRoadPlayer] with
      override def apply(): SOCLongestRoadPlayer = SOCLongestRoadPlayer(None)
  }

  case class SOCRoadLengths(m: PlayerMap[Int]) extends GameState[SOCRoadLengths] {
    override type Delta = SpecialCounts.Delta

    override def apply(delta: Delta): SOCRoadLengths = SOCRoadLengths(SpecialCounts.applyDelta(delta, m))
  }

  object SOCRoadLengths {
    given initRoadCount(using playerIds: PlayerIds): StateInitializer[SOCRoadLengths] with
      override def apply(): SOCRoadLengths = SOCRoadLengths(playerIds.players.map(_ -> 0).toMap)
  }

  case class LargestArmyPlayer(player: Option[Int]) extends GameState[LargestArmyPlayer] {
    type Delta = SpecialPlayer.Delta

    override def apply(delta: Delta): LargestArmyPlayer = LargestArmyPlayer(SpecialPlayer.applyDelta(delta))
  }

  object LargestArmyPlayer {
    given initLargestArmyPlayer: StateInitializer[LargestArmyPlayer] with
      override def apply(): LargestArmyPlayer = LargestArmyPlayer(None)
  }

  case class PlayerArmyCount(m: PlayerMap[Int]) extends GameState[PlayerArmyCount] {
    type Delta = SpecialCounts.Increment

    override def apply(delta: Delta): PlayerArmyCount = PlayerArmyCount(SpecialCounts.applyIncrement(delta, m))
  }

  object PlayerArmyCount {
    given initArmyCount(using playerIds: PlayerIds): StateInitializer[PlayerArmyCount] with
      override def apply(): PlayerArmyCount = PlayerArmyCount(playerIds.players.map(_ -> 0).toMap)
  }
}
