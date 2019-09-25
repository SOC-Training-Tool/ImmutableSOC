package soc.inventory

import Inventory.{NoInfo, PerfectInfo, ProbableInfo}
import soc.inventory.developmentCard._
import soc.inventory.developmentCard.DevelopmentCardSet.PlayedInventory
import soc.inventory.resources._
import soc.inventory.resources.CatanResourceSet.Resources

trait Inventory[T <: Inventory[T]] { self: T =>

  type UpdateRes
  type UpdateDev

  val position: Int
  val playedDevCards: PlayedInventory
  val numUnplayedDevCards: Int
  val numCards: Int
  def canBuild(resSet: Resources): Boolean
  def endTurn: T
  def updateResources(update: UpdateRes): T
  def updateDevelopmentCard(update: UpdateDev): T
}

case object Inventory {
  type PerfectInfo = PerfectInfoInventory
  type ProbableInfo = ProbableInfoInventory
  type NoInfo = NoInfoInventory
}


case class PerfectInfoInventory(
  position: Int,
  resourceSet: Resources = CatanResourceSet.empty,
  playedDevCards: PlayedInventory = DevelopmentCardSet(),
  canPlayDevCards: PlayedInventory = DevelopmentCardSet(),
  cannotPlayDevCards: PlayedInventory = DevelopmentCardSet()
) extends Inventory[PerfectInfo] {

  type UpdateRes = List[SOCTransactions]
  type UpdateDev = DevCardTransaction

  override def updateResources(transactions: List[SOCTransactions]): PerfectInfoInventory = {
    val res = transactions.foldLeft(resourceSet){
      case (newSet, Gain(`position`, set)) => newSet.add(set)
      case (newSet, Lose(`position`, set)) => newSet.subtract(set)
      case (newSet, Steal(`position`, _, Some(set))) => newSet.add(set)
      case (newSet, Steal(_, `position`, Some(set))) => newSet.subtract(set)
      case (newSet, _) => newSet
    }
    copy(resourceSet = res)
  }

  override def updateDevelopmentCard(transaction: DevCardTransaction): PerfectInfoInventory = {
    transaction match {
      case BuyDevelopmentCard(`position`, Some(card)) =>
        copy (cannotPlayDevCards = cannotPlayDevCards.add(1, card))
      case PlayDevelopmentCard(`position`, card) =>
        copy (
          canPlayDevCards = canPlayDevCards.subtract(1, card),
          playedDevCards = playedDevCards.add(1, card)
        )
      case _ => copy()
    }

  }

  override def endTurn: PerfectInfoInventory = {
    val newCanPlayDevCards = canPlayDevCards.add(cannotPlayDevCards)
    copy (
      canPlayDevCards = newCanPlayDevCards,
      cannotPlayDevCards = DevelopmentCardSet.empty
    )
  }

  def buyDevelopmentCard(dCard: Option[DevelopmentCard]): PerfectInfoInventory = copy (
    cannotPlayDevCards = dCard.fold(cannotPlayDevCards)(cannotPlayDevCards.add(1, _))
  )

  override val numUnplayedDevCards: Int = cannotPlayDevCards.getTotal + canPlayDevCards.getTotal
  override val numCards: Int = resourceSet.getTotal

  override def canBuild(resSet: Resources): Boolean = resourceSet.contains(resSet)
}

case class NoInfoInventory(
  position: Int,
  playedDevCards: PlayedInventory = DevelopmentCardSet(),
  numCards: Int = 0,
  numUnplayedDevCards: Int = 0) extends Inventory[NoInfo] {

  type UpdateRes = List[SOCTransactions]
  type UpdateDev = DevCardTransaction

  override def updateResources(transactions: List[SOCTransactions]): NoInfoInventory = {
    val numCrds = transactions.foldLeft(numCards) {
      case (num, Gain(`position`, set)) => num + set.getTotal
      case (num, Lose(`position`, set)) => num - set.getTotal
      case (num, Steal(`position`, _, _)) => num + 1
      case (num, Steal(_, `position`, _)) => num - 1
      case (num, _) => num
    }
    copy(numCards = numCrds)
  }

  override def updateDevelopmentCard(transaction: DevCardTransaction): NoInfoInventory = {
    transaction match {
      case BuyDevelopmentCard(`position`, Some(card)) =>
        copy(numUnplayedDevCards = numUnplayedDevCards + 1)
      case PlayDevelopmentCard(`position`, card) =>
        copy(
          numUnplayedDevCards = numUnplayedDevCards - 1,
          playedDevCards = playedDevCards.add(1, card)
        )
      case _ => copy()
    }

  }

  override def endTurn: NoInfoInventory = copy()

  override def canBuild(resSet: Resources): Boolean = true
}

case class ProbableInfoInventory(
  position: Int,
  playedDevCards: PlayedInventory = DevelopmentCardSet.empty,
  probableResourceSet: ProbableResourceSet = ProbableResourceSet.empty,
  knownUnplayedDevCards: PlayedInventory=  DevelopmentCardSet.empty,
  probableDevCards: DevelopmentCardSet[Double] = DevelopmentCardSet.empty
) extends Inventory[ProbableInfo]  {

  type UpdateRes = ProbableResourceSet
  type UpdateDev = (PlayedInventory, PlayedInventory, DevelopmentCardSet[Double])

  override val numUnplayedDevCards: Int = probableDevCards.getTotal.toInt + knownUnplayedDevCards.getTotal
  override val numCards: Int = probableResourceSet.getTotal

  override def canBuild(resSet: Resources): Boolean = probableResourceSet.mightContain(resSet)

  override def updateResources(probableSet: ProbableResourceSet): ProbableInfoInventory = copy(probableResourceSet = probableSet)

  override def endTurn: ProbableInfoInventory = copy()

  override def updateDevelopmentCard(update: (PlayedInventory, PlayedInventory, DevelopmentCardSet[Double])): ProbableInfoInventory = {
    val (played, known, probable) = update
    copy(
      playedDevCards = played,
      knownUnplayedDevCards = known,
      probableDevCards = probable
    )
  }
}