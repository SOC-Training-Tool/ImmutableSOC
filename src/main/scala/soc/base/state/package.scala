package soc.base

import game.ImmutableGame.StateInitializer
import game.{InventorySet, StateComponent}
import shapeless.{:+:, CNil, Coproduct, Poly1}
import soc.core.state.PlayerIds
import soc.core.{Edge, PlayerMap, Vertex}

package object state {

  // ═══════════════════════════════════════════════════════════════
  // RobberLocation: Tracks where the robber is on the board
  // ═══════════════════════════════════════════════════════════════

  object RobberDeltas {
    case class MoveTo(hexId: Int)

    type RobberDelta = MoveTo :+: CNil
  }

  import RobberDeltas._

  case class RobberLocation(robberHexId: Int) extends StateComponent[RobberLocation, RobberDelta] {

    override def applyDelta(delta: RobberDelta): RobberLocation = {
      object ApplyPoly extends Poly1 {
        implicit val moveTo: Case.Aux[MoveTo, RobberLocation] =
          at(d => RobberLocation(d.hexId))
      }
      delta.fold(ApplyPoly)
    }
  }

  // ═══════════════════════════════════════════════════════════════
  // DevelopmentCardDeckSize: Tracks remaining cards in deck (public info)
  // ═══════════════════════════════════════════════════════════════

  case class DevelopmentCardDeckSize(size: Int)

  // ═══════════════════════════════════════════════════════════════
  // DevelopmentCardDeck: The actual deck of development cards
  // ═══════════════════════════════════════════════════════════════

  object DeckDeltas {
    case class DrawCard[Dev](card: Dev)
    case class AddCard[Dev](card: Dev)

    type DeckDelta[Dev] = DrawCard[Dev] :+: AddCard[Dev] :+: CNil
  }

  import DeckDeltas._

  case class DevelopmentCardDeck[Dev](cards: List[Dev]) extends StateComponent[DevelopmentCardDeck[Dev], DeckDelta[Dev]] {

    override def applyDelta(delta: DeckDelta[Dev]): DevelopmentCardDeck[Dev] = {
      object ApplyPoly extends Poly1 {
        implicit def draw: Case.Aux[DrawCard[Dev], DevelopmentCardDeck[Dev]] =
          at(d => DevelopmentCardDeck(cards.filterNot(_ == d.card)))

        implicit def add: Case.Aux[AddCard[Dev], DevelopmentCardDeck[Dev]] =
          at(d => DevelopmentCardDeck(d.card :: cards))
      }
      delta.fold(ApplyPoly)
    }
  }

  // ═══════════════════════════════════════════════════════════════
  // SOCLongestRoadPlayer: Tracks who has longest road
  // ═══════════════════════════════════════════════════════════════

  object LongestRoadDeltas {
    case class SetPlayer(player: Option[Int])

    type LongestRoadDelta = SetPlayer :+: CNil
  }

  import LongestRoadDeltas._

  case class SOCLongestRoadPlayer(player: Option[Int]) extends StateComponent[SOCLongestRoadPlayer, LongestRoadDelta] {

    override def applyDelta(delta: LongestRoadDelta): SOCLongestRoadPlayer = {
      object ApplyPoly extends Poly1 {
        implicit val setPlayer: Case.Aux[SetPlayer, SOCLongestRoadPlayer] =
          at(d => SOCLongestRoadPlayer(d.player))
      }
      delta.fold(ApplyPoly)
    }
  }

  object SOCLongestRoadPlayer {
    implicit val initLongestRoadPlayer: StateInitializer[SOCLongestRoadPlayer] = new StateInitializer[SOCLongestRoadPlayer] {
      override def apply(): SOCLongestRoadPlayer = SOCLongestRoadPlayer(None)
    }
  }

  // ═══════════════════════════════════════════════════════════════
  // SOCRoadLengths: Tracks road length per player
  // ═══════════════════════════════════════════════════════════════

  object RoadLengthDeltas {
    case class SetLength(player: Int, length: Int)

    type RoadLengthDelta = SetLength :+: CNil
  }

  import RoadLengthDeltas._

  case class SOCRoadLengths(m: PlayerMap[Int]) extends StateComponent[SOCRoadLengths, RoadLengthDelta] {

    override def applyDelta(delta: RoadLengthDelta): SOCRoadLengths = {
      object ApplyPoly extends Poly1 {
        implicit val setLength: Case.Aux[SetLength, SOCRoadLengths] =
          at(d => SOCRoadLengths(m.updated(d.player, d.length)))
      }
      delta.fold(ApplyPoly)
    }
  }

  object SOCRoadLengths {
    implicit def initRoadCount(implicit playerIds: PlayerIds): StateInitializer[SOCRoadLengths] = new StateInitializer[SOCRoadLengths] {
      override def apply(): SOCRoadLengths = SOCRoadLengths(playerIds.players.map(_ -> 0).toMap)
    }
  }

  // ═══════════════════════════════════════════════════════════════
  // LargestArmyPlayer: Tracks who has largest army
  // ═══════════════════════════════════════════════════════════════

  object LargestArmyDeltas {
    case class SetPlayer(player: Option[Int])

    type LargestArmyDelta = SetPlayer :+: CNil
  }

  import LargestArmyDeltas._

  case class LargestArmyPlayer(player: Option[Int]) extends StateComponent[LargestArmyPlayer, LargestArmyDelta] {

    override def applyDelta(delta: LargestArmyDelta): LargestArmyPlayer = {
      object ApplyPoly extends Poly1 {
        implicit val setPlayer: Case.Aux[SetPlayer, LargestArmyPlayer] =
          at(d => LargestArmyPlayer(d.player))
      }
      delta.fold(ApplyPoly)
    }
  }

  object LargestArmyPlayer {
    implicit val initLargestArmyPlayer: StateInitializer[LargestArmyPlayer] = new StateInitializer[LargestArmyPlayer] {
      override def apply(): LargestArmyPlayer = LargestArmyPlayer(None)
    }
  }

  // ═══════════════════════════════════════════════════════════════
  // PlayerArmyCount: Tracks knight count per player
  // ═══════════════════════════════════════════════════════════════

  object ArmyCountDeltas {
    case class Increment(player: Int)
    case class SetCount(player: Int, count: Int)

    type ArmyCountDelta = Increment :+: SetCount :+: CNil
  }

  import ArmyCountDeltas._

  case class PlayerArmyCount(m: PlayerMap[Int]) extends StateComponent[PlayerArmyCount, ArmyCountDelta] {

    override def applyDelta(delta: ArmyCountDelta): PlayerArmyCount = {
      object ApplyPoly extends Poly1 {
        implicit val increment: Case.Aux[Increment, PlayerArmyCount] =
          at(d => PlayerArmyCount(m.updated(d.player, m.getOrElse(d.player, 0) + 1)))

        implicit val setCount: Case.Aux[SetCount, PlayerArmyCount] =
          at(d => PlayerArmyCount(m.updated(d.player, d.count)))
      }
      delta.fold(ApplyPoly)
    }
  }

  object PlayerArmyCount {
    implicit def initArmyCount(implicit playerIds: PlayerIds): StateInitializer[PlayerArmyCount] = new StateInitializer[PlayerArmyCount] {
      override def apply(): PlayerArmyCount = PlayerArmyCount(playerIds.players.map(_ -> 0).toMap)
    }
  }
}
