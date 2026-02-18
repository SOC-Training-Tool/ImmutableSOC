package soc.core

import game.Delta.DeltaGen
import game.{:+:, CNil, Inl, Inr, GameState, InventorySet}
import soc.core.ResourceSet.Resources
import soc.core.Transactions._

object Transactions {

  case class Gain[II](player: Int, set: InventorySet[II, Int])

  case class Lose[II](player: Int, set: InventorySet[II, Int])

  case class ImperfectInfoExchange[II](from: Int, to: Int, exchange: Option[II])

  type PerfectInfo[II] = Gain[II] :+: Lose[II] :+: CNil
  type ImperfectInfo[II] = ImperfectInfoExchange[II] :+: PerfectInfo[II]
}

object ResourceInventories {

  case class PublicInventories[II](m: Map[Int, Int]) extends GameState[PublicInventories[II]] {
    override type Delta = Transactions.ImperfectInfo[II]

    override def apply(delta: ImperfectInfo[II]): PublicInventories[II] = PublicInventories[II](delta match {
      case Inl(g) => m.get(g.to).zip(m.get(g.from)).fold(m) { case (t, f) =>
        m + (g.to -> (t + 1)) + (g.from -> (f - 1))
      }
      case Inr(Inl(g)) => m.get(g.player).fold(m)(i => m + (g.player -> (i + g.set.getTotal)))
      case Inr(Inr(Inl(g))) => m.get(g.player).fold(m)(i => m + (g.player -> (i - g.set.getTotal)))
      case Inr(Inr(Inr(cnil))) => cnil.impossible
    })
  }

  case class PrivateInventories[II](m: Map[Int, InventorySet[II, Int]]) extends GameState[PrivateInventories[II]] {
    override type Delta = Transactions.PerfectInfo[II]

    override def apply(delta: PerfectInfo[II]): PrivateInventories[II] = PrivateInventories(delta match {
      case Inl(g) => m.get(g.player).fold(m)(i => m + (g.player -> i.add(g.set)))
      case Inr(Inl(g)) => m.get(g.player).fold(m)(i => m + (g.player -> i.subtract(g.set)))
      case Inr(Inr(cnil)) => cnil.impossible
    })
  }

  case class ProbableInventories[II](m: List[(Int, Map[Int, InventorySet[II, Int]])]) extends GameState[ProbableInventories[II]] {
    type Delta = Transactions.ImperfectInfo[II]

    override def apply(delta: ImperfectInfo[II]): ProbableInventories[II] = this
  }

  val deltaGen = DeltaGen[PublicInventories[Resources], Transactions.PerfectInfo[Resources]]

  object ProbableTransactionPoly {

    type Inv[II] = List[(Int, Map[Int, InventorySet[II, Int]])]

    def applyGain[II](hands: Inv[II], g: Gain[II]): Inv[II] = {
      val player = g.player
      hands.headOption.fold(List((1, Map(player -> InventorySet.empty[II, Int])))) {
        case (_, hand) if !hand.contains(player) => hands.map { case (mult, playerHands) => (mult, playerHands + (player -> InventorySet.empty[II, Int])) }
        case _ => hands
      }.map { case (mult, hand) => (mult, hand.map {
        case (`player`, resources) => player -> (resources.add(g.set))
        case (p, rm) => p -> rm
      })
      }
    }

    def applyLose[II](hands: Inv[II], l: Lose[II]): Inv[II] = {
      val player = l.player
      hands.filter { case (_, pr) => pr.get(player).fold(false)(_.contains(l.set)) }.map { case (mult, pr) => (mult, pr.map {
        case (`player`, resources) => player -> resources.subtract(l.set)
        case (p, rm) => p -> rm
      })
      }
    }

    def applyExchange[II](hands: Inv[II], e: ImperfectInfoExchange[II]): Inv[II] = {
      def transfer(inv: Inv[II], res: II): Inv[II] = {
        val set = InventorySet.fromList(List(res))
        val l = applyLose(inv, Lose(e.from, set))
        applyGain(l, Gain(e.to, set))
      }

      e.exchange.fold {
        hands.flatMap { case (_, handSet) =>
          handSet.get(e.from).fold(hands) { resSet =>
            resSet.getTypes.flatMap { res =>
              val amount = resSet.getAmount(res)
              transfer(hands, res)
                .map { case (m, pr) => (m * amount, pr) }
            }.toList
          }
        }.groupBy { case (_, f) => f }.toList.map { case (playerResources, playerResourcesWithMult) =>
          (playerResourcesWithMult.map(_._1).sum, playerResources)
        }
      }(res => transfer(hands, res))
    }
  }
}
