package soc.base

import game.GameState
import soc.core.PlayerMap

package object state:

  object SpecialPlayer:
    case class Set(player: Int)
    case object Remove
    type Delta = Set | Remove.type

  object SpecialCounts:
    case class Increment(player: Int)
    case class Set(player: Int, length: Int)
    type Delta = Increment | Set

  case class RobberLocation(robberHexId: Int) extends GameState[RobberLocation]:
    import RobberLocation.Delta
    type Delta = RobberLocation.Delta
    override def apply(delta: Delta): RobberLocation = RobberLocation(delta.hexId)

  object RobberLocation:
    case class Delta(hexId: Int)

  case class DevelopmentCardDeckSize(size: Int) extends GameState[DevelopmentCardDeckSize]:
    override type Delta = DevelopmentCardDeck.Remove.type
    override def apply(delta: Delta): DevelopmentCardDeckSize = DevelopmentCardDeckSize(size - 1)

  case class DevelopmentCardDeck[Dev](cards: List[Dev]) extends GameState[DevelopmentCardDeck[Dev]]:
    override type Delta = DevelopmentCardDeck.Remove.type
    override def apply(delta: Delta): DevelopmentCardDeck[Dev] = DevelopmentCardDeck(cards.tail)

  object DevelopmentCardDeck:
    case object Remove

  case class SOCLongestRoadPlayer(player: Option[Int]) extends GameState[SOCLongestRoadPlayer]:
    override type Delta = SpecialPlayer.Delta
    override def apply(delta: Delta): SOCLongestRoadPlayer = delta match
      case SpecialPlayer.Set(p) => SOCLongestRoadPlayer(Some(p))
      case SpecialPlayer.Remove => SOCLongestRoadPlayer(None)

  case class SOCRoadLengths(m: PlayerMap[Int]) extends GameState[SOCRoadLengths]:
    override type Delta = SpecialCounts.Delta
    override def apply(delta: Delta): SOCRoadLengths = delta match
      case SpecialCounts.Increment(player)     =>
        SOCRoadLengths(m + (player -> (m.getOrElse(player, 0) + 1)))
      case SpecialCounts.Set(player, length)   =>
        SOCRoadLengths(m + (player -> length))

  case class LargestArmyPlayer(player: Option[Int]) extends GameState[LargestArmyPlayer]:
    type Delta = SpecialPlayer.Delta
    override def apply(delta: Delta): LargestArmyPlayer = delta match
      case SpecialPlayer.Set(p) => LargestArmyPlayer(Some(p))
      case SpecialPlayer.Remove => LargestArmyPlayer(None)

  case class PlayerArmyCount(m: PlayerMap[Int]) extends GameState[PlayerArmyCount]:
    type Delta = SpecialCounts.Increment
    override def apply(delta: Delta): PlayerArmyCount =
      PlayerArmyCount(m + (delta.player -> (m.getOrElse(delta.player, 0) + 1)))
