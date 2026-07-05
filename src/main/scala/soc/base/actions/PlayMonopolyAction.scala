package soc.base.actions

import game.{GameAction, InventorySet}
import soc.base.DevelopmentCards.DevelopmentCard
import soc.base.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.Transactions.*
import soc.core.DevTransactions.*
import soc.core.ResourceSet.*

case class PlayMonopolyOutput(
  cardsLost:   List[Lose[Resource]],
  cardsGained: Gain[Resource],
  cardPlayed:  PlayCard[DevelopmentCard]
)

class PlayMonopolyAction extends GameAction[PlayMonopolyMoveResult[Resource], TurnInput, PlayMonopolyOutput]:
  def apply(move: PlayMonopolyMoveResult[Resource], input: TurnInput): PlayMonopolyOutput =
    val loseDeltas = move.cardsLost.toList.map { case (p, cards) =>
      Lose(p, InventorySet.fromMap(Map(move.res -> cards)))
    }
    val totalLost = InventorySet.fromMap(Map(move.res -> move.cardsLost.values.sum))
    PlayMonopolyOutput(
      cardsLost   = loseDeltas,
      cardsGained = Gain(move.player, totalLost),
      cardPlayed  = PlayCard(DevelopmentCards.MONOPOLY, move.player, input.turn.t)
    )
