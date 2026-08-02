package soc.base.actions

import game.{GameAction, InventorySet}
import soc.base.DevelopmentCards.DevelopmentCard
import soc.base.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.Transactions.*
import soc.core.DevTransactions.*
import soc.core.state.*

case class PlayYearOfPlentyOutput(
  bankLost:     Bank[Resource]#Delta,
  playerGained: Gain[Resource],
  cardPlayed:   PlayCard[DevelopmentCard]
)

class PlayYearOfPlentyAction extends GameAction[PlayYearOfPlentyMove[Resource], TurnInput, PlayYearOfPlentyOutput]:
  def apply(move: PlayYearOfPlentyMove[Resource], input: TurnInput): PlayYearOfPlentyOutput =
    val inv = InventorySet.fromList(List(move.c1, move.c2))
    PlayYearOfPlentyOutput(
      bankLost     = Bank.Take(inv),
      playerGained = Gain(move.player, inv),
      cardPlayed   = PlayCard(DevelopmentCards.YEAR_OF_PLENTY, move.player, input.turn.number)
    )
