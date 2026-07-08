package soc.base.actions

import game.{GameAction, InventorySet}
import soc.base.*
import soc.base.DevelopmentCards.*
import soc.base.state.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.Transactions.*
import soc.core.DevTransactions.*

case class PlayPerfectKnightOutput(
  cardPlayed:        PlayCard[DevelopmentCard],
  newRobberLocation: RobberLocation#Delta,
  steals:            List[Gain[Resource] | Lose[Resource]]
)

class PlayPerfectKnightAction extends GameAction[PerfectInfoPlayKnightResult[Resource], TurnInput, PlayPerfectKnightOutput]:
  def apply(move: PerfectInfoPlayKnightResult[Resource], input: TurnInput): PlayPerfectKnightOutput =
    val stealDeltas: List[Gain[Resource] | Lose[Resource]] = move.inner.steal.toList.flatMap { steal =>
      val inv = InventorySet.fromList(Seq(steal.resource))
      List(Gain(move.inner.player, inv), Lose(steal.victim, inv))
    }
    PlayPerfectKnightOutput(
      cardPlayed        = PlayCard(DevelopmentCards.KNIGHT, move.inner.player, input.turn.t),
      newRobberLocation = RobberLocation.Delta(move.inner.robberHexId),
      steals            = stealDeltas
    )

case class PlayPublicKnightOutput(
  cardPlayed:        PlayCard[DevelopmentCard],
  newRobberLocation: RobberLocation#Delta,
  steal:             Option[PublicInventories[Resource]#Delta]
)

class PlayPublicKnightAction extends GameAction[PlayKnightResult[Resource], TurnInput, PlayPublicKnightOutput]:
  def apply(move: PlayKnightResult[Resource], input: TurnInput): PlayPublicKnightOutput =
    val stealDelta = move.inner.steal.map { steal =>
      ImperfectInfoExchange[Resource](steal.victim, move.inner.player, steal.resource)
    }
    PlayPublicKnightOutput(
      cardPlayed        = PlayCard(DevelopmentCards.KNIGHT, move.inner.player, input.turn.t),
      newRobberLocation = RobberLocation.Delta(move.inner.robberHexId),
      steal             = stealDelta
    )