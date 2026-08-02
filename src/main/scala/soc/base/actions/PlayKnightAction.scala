package soc.base.actions

import game.{GameAction, InventorySet, NoInput}
import soc.base.*
import soc.base.DevelopmentCards.*
import soc.base.state.*
import soc.core.*
import soc.core.state.*
import soc.core.ResourceInventories.*
import soc.core.Transactions.*
import soc.core.DevTransactions.*

case class KnightInput(
  turn: Turn,
  playerArmyCount: PlayerArmyCount,
  largestArmyPlayer: LargestArmyPlayer
)

case class PlayPerfectKnightOutput(
  cardPlayed:              PlayCard[DevelopmentCard],
  newRobberLocation:       RobberLocation#Delta,
  steals:                  List[Gain[Resource] | Lose[Resource]],
  playerArmyCountChange:   Option[SpecialCounts.Increment],
  largestArmyPlayerChange: List[SpecialPlayer.Delta],
  largestArmyPointChanges: List[PlayerPoints#Delta]
)

class PlayPerfectKnightAction extends GameAction[PerfectInfoPlayKnightResult[Resource], KnightInput, PlayPerfectKnightOutput]:
  def apply(move: PerfectInfoPlayKnightResult[Resource], input: KnightInput): PlayPerfectKnightOutput =
    val stealDeltas: List[Gain[Resource] | Lose[Resource]] = move.inner.steal.toList.flatMap { steal =>
      val inv = InventorySet.fromList(Seq(steal.resource))
      List(Gain(move.inner.player, inv), Lose(steal.victim, inv))
    }
    val specialChanges = largestArmySpecialPlayerDeltas(
      input.largestArmyPlayer.player,
      input.playerArmyCount,
      move.inner.player
    )
    PlayPerfectKnightOutput(
      cardPlayed              = PlayCard(DevelopmentCards.KNIGHT, move.inner.player, input.turn.number),
      newRobberLocation       = RobberLocation.Delta(move.inner.robberHexId),
      steals                  = stealDeltas,
      playerArmyCountChange   = Some(SpecialCounts.Increment(move.inner.player)),
      largestArmyPlayerChange = specialChanges.collect { case d: SpecialPlayer.Delta => d },
      largestArmyPointChanges = specialChanges.collect {
        case i: PlayerPoints.Increment => i
        case d: PlayerPoints.Decrement => d
      }
    )

case class PlayPublicKnightOutput(
  cardPlayed:              PlayCard[DevelopmentCard],
  newRobberLocation:       RobberLocation#Delta,
  steal:                   Option[PublicInventories[Resource]#Delta],
  playerArmyCountChange:   Option[SpecialCounts.Increment],
  largestArmyPlayerChange: List[SpecialPlayer.Delta],
  largestArmyPointChanges: List[PlayerPoints#Delta]
)

class PlayPublicKnightAction extends GameAction[PlayKnightResult[Resource], KnightInput, PlayPublicKnightOutput]:
  def apply(move: PlayKnightResult[Resource], input: KnightInput): PlayPublicKnightOutput =
    val stealDelta = move.inner.steal.map { steal =>
      ImperfectInfoExchange[Resource](steal.victim, move.inner.player, steal.resource)
    }
    val specialChanges = largestArmySpecialPlayerDeltas(
      input.largestArmyPlayer.player,
      input.playerArmyCount,
      move.inner.player
    )
    PlayPublicKnightOutput(
      cardPlayed              = PlayCard(DevelopmentCards.KNIGHT, move.inner.player, input.turn.number),
      newRobberLocation       = RobberLocation.Delta(move.inner.robberHexId),
      steal                   = stealDelta,
      playerArmyCountChange   = Some(SpecialCounts.Increment(move.inner.player)),
      largestArmyPlayerChange = specialChanges.collect { case d: SpecialPlayer.Delta => d },
      largestArmyPointChanges = specialChanges.collect {
        case i: PlayerPoints.Increment => i
        case d: PlayerPoints.Decrement => d
      }
    )
