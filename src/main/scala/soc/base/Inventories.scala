package soc.base

import game.{Delta, GameState}
import soc.core.{DevTransactions, PrivateDevCardInv, PublicDevCardInv, Resource}
import soc.core.ResourceInventories.{PrivateInventories, PublicInventories}
import soc.core.ResourceSet.Resources
import soc.core.Transactions.{Gain, Lose}
import soc.base.DevelopmentCards.DevelopmentCard

// ── Resource inventory ──────────────────────────────────────────────────────

trait ResourceInventory[Inv <: GameState[Inv]]:
  def gain(player: Int, resources: Resources): Delta[Inv]
  def lose(player: Int, resources: Resources): Delta[Inv]

/** Marker: PrivateInventory is a subtype of ResourceInventory.
 *  Anything requiring ResourceInventory[Inv] accepts PrivateInventory[Inv] too. */
trait PrivateResourceInventory[Inv <: GameState[Inv]] extends ResourceInventory[Inv]

given ResourceInventory[PublicInventories[Resource]] with
  def gain(p: Int, r: Resources) = Delta[PublicInventories[Resource]](Gain(p, r))
  def lose(p: Int, r: Resources) = Delta[PublicInventories[Resource]](Lose(p, r))

given PrivateResourceInventory[PrivateInventories[Resource]] with
  def gain(p: Int, r: Resources) = Delta[PrivateInventories[Resource]](Gain(p, r))
  def lose(p: Int, r: Resources) = Delta[PrivateInventories[Resource]](Lose(p, r))

// ── Dev card inventory ──────────────────────────────────────────────────────

trait DevCardInventory[Inv <: GameState[Inv]]:
  def playCard(card: DevelopmentCard, player: Int, turn: Int): Delta[Inv]

trait PrivateDevCardInventory[Inv <: GameState[Inv]] extends DevCardInventory[Inv]

given DevCardInventory[PublicDevCardInv[DevelopmentCard]] with
  def playCard(card: DevelopmentCard, player: Int, turn: Int) =
    Delta[PublicDevCardInv[DevelopmentCard]](DevTransactions.PlayCard(card, player, turn))

given PrivateDevCardInventory[PrivateDevCardInv[DevelopmentCard]] with
  def playCard(card: DevelopmentCard, player: Int, turn: Int) =
    Delta[PrivateDevCardInv[DevelopmentCard]](DevTransactions.PlayCard(card, player, turn))
