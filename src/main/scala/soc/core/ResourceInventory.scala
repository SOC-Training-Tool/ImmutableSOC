package soc.core

import game.{GameState, InventorySet}

object Transactions:

  case class Gain[II](player: Int, set: InventorySet[II, Int])
  case class Lose[II](player: Int, set: InventorySet[II, Int])
  case class ImperfectInfoExchange[II](from: Int, to: Int, exchange: Option[II])

  type PerfectInfo[II]   = Gain[II] | Lose[II]
  type ImperfectInfo[II] = ImperfectInfoExchange[II] | Gain[II] | Lose[II]

object ResourceInventories:

  case class PublicInventories[II](m: Map[Int, Int]) extends GameState[PublicInventories[II]]:
    override type Delta = Transactions.ImperfectInfo[II]
    override def apply(delta: Transactions.ImperfectInfo[II]): PublicInventories[II] = delta match
      case Transactions.Gain(player, set) =>
        PublicInventories(m.updatedWith(player)(_.map(_ + set.getTotal)))
      case Transactions.Lose(player, set) =>
        PublicInventories(m.updatedWith(player)(_.map(_ - set.getTotal)))
      case Transactions.ImperfectInfoExchange(from, to, _) =>
        val updated = for
          t <- m.get(to)
          f <- m.get(from)
        yield m + (to -> (t + 1)) + (from -> (f - 1))
        PublicInventories(updated.getOrElse(m))

  case class PrivateInventories[II](m: Map[Int, InventorySet[II, Int]])
      extends GameState[PrivateInventories[II]]:
    override type Delta = Transactions.PerfectInfo[II]
    override def apply(delta: Transactions.PerfectInfo[II]): PrivateInventories[II] = delta match
      case Transactions.Gain(player, set) =>
        PrivateInventories(m.updatedWith(player)(_.map(_.add(set))))
      case Transactions.Lose(player, set) =>
        PrivateInventories(m.updatedWith(player)(_.map(_.subtract(set))))

  case class ProbableInventories[II](m: List[(Int, Map[Int, InventorySet[II, Int]])])
      extends GameState[ProbableInventories[II]]:
    type Delta = Transactions.ImperfectInfo[II]
    override def apply(delta: Transactions.ImperfectInfo[II]): ProbableInventories[II] = this
