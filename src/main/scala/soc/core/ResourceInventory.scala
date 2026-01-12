package soc.core

import game.Delta.DeltaGen
import game.{GameState, InventorySet}
import shapeless.{:+:, CNil, HNil, Poly2}
import soc.core.ResourceSet.Resources
import soc.core.Transactions._

object Transactions {

  case class Gain[II](player: Int, set: InventorySet[II, Int])

  case class Lose[II](player: Int, set: InventorySet[II, Int])

  case class ImperfectInfoExchange[II](from: Int, to: Int, exchange: Option[II])

  type PerfectInfo[II] = Gain[II] :+: Lose[II] :+: CNil
  type ImperfectInfo[II] = ImperfectInfoExchange[II] :+: PerfectInfo[II]
}

//trait ResourceInventories[II, Transaction, T[_]] {
//  def players(t: T[II]): Seq[Int]
//
//  def numCards(t: T[II], player: Int): Int
//
//  def canSpend(t: T[II], player: Int, resSet: InventorySet[II, Int]): Boolean
//
//  def update(t: T[II], transactions: List[Transaction]): T[II]
//
//}

object ResourceInventories {

  case class PublicInventories[II](m: Map[Int, Int]) extends GameState[PublicInventories[II]] {
    override type Delta = Transactions.ImperfectInfo[II]

    override def apply(delta: ImperfectInfo[II]): PublicInventories[II] = PublicInventories[II](delta.foldLeft(m)(PublicTransactionPoly))
  }

  case class PrivateInventories[II](m:  Map[Int, InventorySet[II, Int]]) extends GameState[PrivateInventories[II]] {
    override type Delta = Transactions.PerfectInfo[II]

    override def apply(delta: PerfectInfo[II]): PrivateInventories[II] = PrivateInventories(delta.foldLeft(m)(PerfectInfoTransactionPoly))
  }

  case class ProbableInventories[II](m: List[(Int, Map[Int, InventorySet[II, Int]])]) extends GameState[ProbableInventories[II]] {
    type Delta = Transactions.ImperfectInfo[II]

    override def apply(delta: ImperfectInfo[II]): ProbableInventories[II] = super.apply(delta)
  }

  val deltaGen = DeltaGen[PublicInventories[Resources], Transactions.PerfectInfo[Resources]]

  //type PublicInventories[_] = Map[Int, Int]
  //type PrivateInventories[II] = Map[Int, InventorySet[II, Int]]
  //type ProbableInventories[II] = List[(Int, Map[Int, InventorySet[II, Int]])]

//  implicit class ResourceInventoriesOp[II, Transactions, T[_]](inv: T[II])(implicit ev: ResourceInventories[II, Transactions, T]) {
//
//    def numCards(player: Int): Int = ev.numCards(inv, player)
//
//    def canSpend(player: Int, resSet: InventorySet[II, Int]): Boolean = ev.canSpend(inv, player, resSet)
//
//    def update(transactions: List[Transactions]): T[II] = ev.update(inv, transactions)
//
//    def update(transactions: Transactions*): T[II] = update(transactions.toList)
//
//    def toPublic: PublicInventories[II] = ev.players(inv).map(p => p -> numCards(p)).toMap
//  }

  object PublicTransactionPoly extends Poly2 {
    implicit def gain[II]: Case.Aux[Map[Int, Int], Gain[II], Map[Int, Int]] =
      at[Map[Int, Int], Gain[II]] { case (m, g) => m.get(g.player).fold(m)(i => m + (g.player -> (i + g.set.getTotal))) }

    implicit def lose[II]: Case.Aux[Map[Int, Int], Lose[II], Map[Int, Int]] =
      at[Map[Int, Int], Lose[II]] { case (m, g) => m.get(g.player).fold(m)(i => m + (g.player -> (i - g.set.getTotal))) }

    implicit def exchange[II]: Case.Aux[Map[Int, Int], ImperfectInfoExchange[II], Map[Int, Int]] =
      at[Map[Int, Int], ImperfectInfoExchange[II]] { case (m, g) =>
        m.get(g.to).zip(m.get(g.from)).fold(m) { case (t, f) =>
          m + (g.to -> (t + 1)) + (g.from -> (f - 1))
        }
      }
  }

  object ProbableTransactionPoly extends Poly2 {

    type Inv[II] = List[(Int, Map[Int, InventorySet[II, Int]])]

    implicit def gain[II]: Case.Aux[Inv[II], Gain[II], Inv[II]] = at[Inv[II], Gain[II]] { case (hands, g) =>
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

    implicit def lose[II]: Case.Aux[Inv[II], Lose[II], Inv[II]] = at[Inv[II], Lose[II]] { case (hands, l) =>
      val player = l.player
      hands.filter { case (_, pr) => pr.get(player).fold(false)(_.contains(l.set)) }.map { case (mult, pr) => (mult, pr.map {
        case (`player`, resources) => player -> resources.subtract(l.set)
        case (p, rm) => p -> rm
      })
      }
    }

    implicit def exchange[II]: Case.Aux[Inv[II], ImperfectInfoExchange[II], Inv[II]] = at[Inv[II], ImperfectInfoExchange[II]] { case (hands, e) =>
      def transfer(inv: Inv[II], res: II): Inv[II] = {
        val set = InventorySet.fromList(List(res))
        val l = lose(inv :: Lose(e.from, set) :: HNil)
        gain(l :: Gain(e.to, set) :: HNil)
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

  object PerfectInfoTransactionPoly extends Poly2 {
    implicit def gain[II]: Case.Aux[Map[Int, InventorySet[II, Int]], Gain[II], Map[Int, InventorySet[II, Int]]] =
      at[Map[Int, InventorySet[II, Int]], Gain[II]] { case (m, g) => m.get(g.player).fold(m)(i => m + (g.player -> i.add(g.set))) }

    implicit def lose[II]: Case.Aux[Map[Int, InventorySet[II, Int]], Lose[II], Map[Int, InventorySet[II, Int]]] =
      at[Map[Int, InventorySet[II, Int]], Lose[II]] { case (m, g) => m.get(g.player).fold(m)(i => m + (g.player -> i.subtract(g.set))) }
  }

//  implicit def convert[II, Super <: Coproduct, Sub <: Coproduct, Inv[_]](implicit inv: ResourceInventories[II, Super, Inv], embedder: Embedder[Super, Sub]): ResourceInventories[II, Sub, Inv] = new ResourceInventories[II, Sub, Inv] {
//    override def players(t: Inv[II]): Seq[Int] = inv.players(t)
//
//    override def numCards(t: Inv[II], player: Int): Int = inv.numCards(t, player)
//
//    override def canSpend(t: Inv[II], player: Int, resSet: InventorySet[II, Int]): Boolean = inv.canSpend(t, player, resSet)
//
//    override def update(t: Inv[II], transactions: List[Sub]): Inv[II] = inv.update(t, transactions.map(embedder.embed))
//  }


//  implicit def publicResourceInventories[II]: ResourceInventories[II, ImperfectInfo[II], PublicInventories] = {
//    new ResourceInventories[II, ImperfectInfo[II], PublicInventories] {
//
//      override def numCards(t: PublicInventories[II], player: Int): Int =
//        t.getOrElse(player, 0)
//
//      override def canSpend(t: PublicInventories[II], player: Int, resSet: InventorySet[II, Int]): Boolean =
//        numCards(t, player) > resSet.getTotal
//
//      override def update(m: PublicInventories[II], transactions: List[ImperfectInfo[II]]): PublicInventories[II] = {
//        transactions.foldLeft(m) { case (m, transaction) =>
//          transaction.foldLeft(m)(PublicTransactionPoly)
//        }
//      }
//
//      override def players(t: PublicInventories[II]): Seq[Int] = t.keys.toSeq
//    }
//  }

//  implicit def probableResourceInventories[II]: ResourceInventories[II, ImperfectInfo[II], ProbableInventories] = new ResourceInventories[II, ImperfectInfo[II], ProbableInventories] {
//    override def players(t: ProbableInventories[II]): Seq[Int] = t.flatMap(_._2.keys).distinct
//
//    override def numCards(t: ProbableInventories[II], player: Int): Int = t.headOption.flatMap(_._2.get(player)).fold(0)(_.getTotal)
//
//    override def canSpend(t: ProbableInventories[II], player: Int, resSet: InventorySet[II, Int]): Boolean = t.exists(_._2.get(player).fold(false)(_.contains(resSet)))
//
//    override def update(t: ProbableInventories[II], transactions: List[ImperfectInfo[II]]): ProbableInventories[II] = {
//      transactions.foldLeft(t) { case (m, transaction) =>
//        transaction.foldLeft(m)(ProbableTransactionPoly)
//      }
//    }
//  }

//  implicit def privateResourceInventories[II]: ResourceInventories[II, PerfectInfo[II], PrivateInventories] = {
//    new ResourceInventories[II, PerfectInfo[II], PrivateInventories] {
//
//      private def playerInvSet(t: PrivateInventories[II], player: Int) = t.getOrElse(player, InventorySet.empty[II, Int])
//
//      override def numCards(t: PrivateInventories[II], player: Int): Int = playerInvSet(t, player).getTotal
//
//      override def canSpend(t: PrivateInventories[II], player: Int, resSet: InventorySet[II, Int]): Boolean = {
//        playerInvSet(t, player).contains(resSet)
//      }
//
//      override def update(m: PrivateInventories[II], transactions: List[PerfectInfo[II]]): PrivateInventories[II] = {
//        transactions.foldLeft(m) { case (m, transaction) =>
//          transaction.foldLeft(m)(PerfectInfoTransactionPoly)
//        }
//      }
//
//      override def players(t: PrivateInventories[II]): Seq[Int] = t.keys.toSeq
//    }
//  }
}