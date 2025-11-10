package soc.core.state

import game.InventorySet
import shapeless.{::, Coproduct, HList, HNil}
import soc.core.ResourceInventories
import soc.core.ResourceInventories.ResourceInventoriesOp
import soc.core.Transactions.{Gain, Lose, PerfectInfo}
import util.DependsOn

object ops {

  implicit class TurnOps[STATE <: HList](state: STATE)(implicit dep: DependsOn[STATE, Turn :: HNil]) {
    val turn: Int = dep.get[Turn](state).t

    def incrementTurn: STATE = dep.update(Turn(turn + 1), state)
  }

  implicit class PointsOps[STATE <: HList](state: STATE)(implicit dep: DependsOn[STATE, PlayerPoints :: HNil]) {
    val playerPoints: Map[Int, Int] = dep.get[PlayerPoints](state).points

    def incrementPointForPlayer(player: Int): STATE = {
      val currValue = playerPoints.getOrElse(player, 0)
      dep.update(PlayerPoints(playerPoints + (player -> (currValue + 1))), state)
    }

    def decrementPointForPlayer(player: Int): STATE = {
      val currValue = playerPoints.getOrElse(player, 0)
      dep.update(PlayerPoints(playerPoints + (player -> (currValue - 1))), state)
    }

  }

  implicit class BankOps[Res, STATE <: HList](state: STATE)(implicit dep: DependsOn[STATE, Bank[Res] :: HNil]) {
    val bank: InventorySet[Res, Int] = dep.get[Bank[Res]](state).b

    def addToBank(set: InventorySet[Res, Int]): STATE = dep.update(Bank(bank.add(set)), state)

    def subtractFromBank(set: InventorySet[Res, Int]): STATE = dep.update(Bank(bank.subtract(set)), state)
  }

  implicit class BankInvOps[Res, Inv[_], STATE <: HList](state: STATE)(implicit dep: DependsOn[STATE, Bank[Res] :: Inv[Res] :: HNil], inv: ResourceInventories[Res, PerfectInfo[Res], Inv]) {

    implicit val bankDep = dep.innerDependency[Bank[Res] :: HNil]

    def payToBank(player: Int, set: InventorySet[Res, Int]): STATE = {
      dep
        .updateWith[Inv[Res]](state)(_.update(Coproduct[PerfectInfo[Res]](Lose(player, set))))
        .addToBank(set)
    }

    def getFromBank(player: Int, set: InventorySet[Res, Int]): STATE = {
      dep
        .updateWith[Inv[Res]](state)(_.update(Coproduct[PerfectInfo[Res]](Gain(player, set))))
        .subtractFromBank(set)
    }
  }

}
