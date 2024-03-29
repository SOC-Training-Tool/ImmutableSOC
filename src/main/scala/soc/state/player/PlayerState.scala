package soc.state.player

import soc.board._
import soc.core.GameRules
import soc.inventory._
import soc.inventory.developmentCard.{DevelopmentCardSpecificationSet}
import soc.inventory.resources.ResourceSet
import soc.inventory.resources.ResourceSet.ResourceSet

case class PlayerState[T <: Inventory[T]](
  position: Int,
  inventory: T,
  armyPoints: Int = 0,
  roadPoints: Int = 0,
  ports: Set[Port] = Set.empty,
  settlements: List[Vertex] = Nil,
  cities: List[Vertex] = Nil,
  roads: List[Edge] = Nil,
  dots: ResourceSet[Int] = ResourceSet.empty,
  roadLength: Int = 0) {

  val settlementPoints = settlements.length
  val cityPoints = 2 * cities.length
  val boardPoints: Int = settlementPoints + cityPoints
  val dCardPoints = inventory.pointCountIfKnown.getOrElse(0)
  val points = boardPoints + armyPoints + roadPoints + dCardPoints

  def canBuildSettlement(implicit gameRules: GameRules) = settlements.length < gameRules.numSettlements

  def buildSettlement(board: CatanBoard, vertex: Vertex): PlayerState[T] = copy(
    settlements = this.settlements ::: List(vertex),
    ports = board.getPort(vertex).fold(this.ports)(this.ports + _),
    dots = board.adjacentHexes(vertex).flatMap { node =>
      node.hex.getResourceAndNumber.map {
        case (resource, roll) => roll.dots -> resource
      }
    }.foldLeft(this.dots) { case (set, (amt, res)) => set.add(amt, res) }
  )

  def canBuildCity(implicit gameRules: GameRules) = cities.length < gameRules.numCities

  def buildCity(board: CatanBoard, vertex: Vertex): PlayerState[T] = copy(
    settlements = this.settlements.filterNot(_ == vertex),
    cities = this.cities ::: List(vertex),
    dots = board.adjacentHexes(vertex).flatMap { node =>
      node.hex.getResourceAndNumber.map {
        case (resource, roll) => roll.dots -> resource
      }
    }.foldLeft(this.dots) { case (set, (amt, res)) => set.add(amt, res) }
  )

  def canBuildRoad(implicit gameRules: GameRules) = roads.length < gameRules.numRoads

  def buildRoad(board: CatanBoard, edge: Edge): PlayerState[T] = copy(
    roads = this.roads ::: List(edge),
    roadLength = board.buildRoad(edge, position).roadLengths.get(position).getOrElse(0)
  )

  val playedDevCards: DevelopmentCardSpecificationSet = inventory.playedDevCards

  val numUnplayedDevCards: Int = inventory.numUnplayedDevCards

  val numCards: Int = inventory.numCards

  val hasLongestRoad = roadPoints > 0

  val hasLargestArmy = armyPoints > 0

  def gainLongestRoad = copy(roadPoints = 2)

  def loseLongestRoad = copy(roadPoints = 0)

  def gainLargestArmy = copy(armyPoints = 2)

  def loseLargestArmy = copy(armyPoints = 0)

  def updateResources(transactions: inventory.UpdateRes): PlayerState[T] = {
    copy(
      inventory = inventory.updateResources(position, transactions)
    )
  }

  def updateDevelopmentCard(turn: Int, card: inventory.UpdateDev): PlayerState[T]= copy (
    inventory = inventory.updateDevelopmentCard(turn, position, card)
  )

  def canBuyDevelopmentCard = true

  def endTurn: PlayerState[T] = copy(inventory = inventory.endTurn)

  lazy val toPublicInfo = copy(inventory = inventory.toPublicInfo)

}


//  def getStateArray: List[Double] = {
//    position.toDouble ::
//      numCards.toDouble ::
//      dots.getAmount(Brick).toDouble ::
//      dots.getAmount(Wood).toDouble ::
//      dots.getAmount(Sheep).toDouble ::
//      dots.getAmount(Wheat).toDouble ::
//      dots.getAmount(Ore).toDouble ::
//      points.toDouble ::
//      boardPoints.toDouble ::
//      armyPoints.toDouble ::
//      roadPoints.toDouble ::
//      dCardPoints.toDouble ::
//      playedDevCards.getTotal.toDouble ::
//      playedDevCards.getAmount(Knight).toDouble ::
//      playedDevCards.getAmount(Monopoly).toDouble ::
//      playedDevCards.getAmount(RoadBuilder).toDouble ::
//      playedDevCards.getAmount(YearOfPlenty).toDouble ::
//      playedDevCards.getAmount(CatanPoint).toDouble ::
//      numUnplayeDevCards.toDouble ::
//      (if (ports.contains(Brick)) 1.0 else 0.0) ::
//      (if (ports.contains(Ore)) 1.0 else 0.0) ::
//      (if (ports.contains(Sheep)) 1.0 else 0.0) ::
//      (if (ports.contains(Wheat)) 1.0 else 0.0) ::
//      (if (ports.contains(Wood)) 1.0 else 0.0) ::
//      (if (ports.contains(Misc)) 1.0 else 0.0) ::
//      (settlements.map(_.node.toDouble) ::: (1 to (5 - settlements.size)).map(_ => 0.0).toList) :::
//      (cities.map(_.node.toDouble) ::: (1 to (4 - cities.size)).map(_ => 0.0).toList) //:::
//    // (roads.map(_.toDouble) ::: (1 to (15 - roads.size)).map(_ => 0.0).toList)
//  }

