package soc.base

import game.GameState
import soc.core.{PrivateDevCardInv, PublicDevCardInv, Resource}
import soc.core.ResourceInventories.{PrivateInventories, PublicInventories}
import soc.core.Transactions.{Gain, Lose}
import soc.core.DevTransactions.PlayCard
import soc.base.DevelopmentCards.DevelopmentCard

trait ResourceInventory[Inv <: GameState[Inv]]:
  def applyGain(inv: Inv, gain: Gain[Resource]): Inv
  def applyLose(inv: Inv, lose: Lose[Resource]): Inv

given ResourceInventory[PublicInventories[Resource]] with
  def applyGain(inv: PublicInventories[Resource], gain: Gain[Resource]) = inv(gain)
  def applyLose(inv: PublicInventories[Resource], lose: Lose[Resource]) = inv(lose)

given ResourceInventory[PrivateInventories[Resource]] with
  def applyGain(inv: PrivateInventories[Resource], gain: Gain[Resource]) = inv(gain)
  def applyLose(inv: PrivateInventories[Resource], lose: Lose[Resource]) = inv(lose)

trait DevCardInventory[Inv <: GameState[Inv]]:
  def applyPlayCard(inv: Inv, pc: PlayCard[DevelopmentCard]): Inv

given DevCardInventory[PublicDevCardInv[DevelopmentCard]] with
  def applyPlayCard(inv: PublicDevCardInv[DevelopmentCard], pc: PlayCard[DevelopmentCard]) = inv(pc)

given DevCardInventory[PrivateDevCardInv[DevelopmentCard]] with
  def applyPlayCard(inv: PrivateDevCardInv[DevelopmentCard], pc: PlayCard[DevelopmentCard]) = inv(pc)