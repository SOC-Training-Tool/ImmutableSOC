package soc.base.state

import game.InventorySet
import shapeless.ops.coproduct
import shapeless.{:+:, ::, CNil, Coproduct, HList, HNil, Poly1}
import soc.core
import soc.core.ResourceInventories.ResourceInventoriesOp
import soc.core.SOCBoard.SOCBoardOps
import soc.core.Transactions.{Gain, PerfectInfo}
import soc.core.state._
import soc.core.state.ops._
import soc.core._
import util.DependsOn
import util.MapOps.MapOps
import util.opext.Embedder

import scala.collection.immutable.Seq

object ops {

  implicit class BuildSettlementStateOps[VB <: Coproduct, STATE <: HList](state: STATE)(implicit
      dep: DependsOn[STATE, VertexBuildingState[VB] :: PlayerPoints :: HNil],
      settleEmbedder: Embedder[VB, Settlement.type :+: CNil]
  ) {

    implicit val pointsDep: DependsOn[STATE, PlayerPoints :: HNil] = dep.innerDependency[PlayerPoints :: HNil]

    val settlements: Map[Vertex, Int] = dep
      .get[VertexBuildingState[VB]](state)
      .map { case (v, PlayerBuilding(vb, p)) =>
        settleEmbedder.select[Settlement.type](vb).map(_ => (v, p))
      }
      .flatten
      .toMap

    def addSettlement(vertex: Vertex, player: Int): STATE = {
      val map = dep.get[VertexBuildingState[VB]](state)
      dep.update(map + (vertex -> PlayerBuilding(settleEmbedder.inject(Settlement), player)), state)
    }

    def subtractSettlement(vertex: Vertex): STATE = {
      val map = dep.get[VertexBuildingState[VB]](state)
      dep.update(map - vertex, state)
    }

    def settlementVerticesForPlayer(player: Int): List[Vertex] = settlements.filter(_._2 == player).keys.toList

    def numSettlementsForPlayer(player: Int): Int = settlementVerticesForPlayer(player).size

    def placeSettlement(vertex: Vertex, player: Int): STATE = {
      addSettlement(vertex, player).incrementPointForPlayer(player)
    }

    def removeSettlement(vertex: Vertex): STATE = {
      settlements.get(vertex).fold(state)(subtractSettlement(vertex).decrementPointForPlayer)
    }
  }

  implicit class BuildCityStateOps[VB <: Coproduct, STATE <: HList](state: STATE)(implicit
      dep: DependsOn[STATE, VertexBuildingState[VB] :: PlayerPoints :: HNil],
      cityEmbedder: Embedder[VB, City.type :+: Settlement.type :+: CNil]
  ) {

    implicit val pointsDep: DependsOn[STATE, PlayerPoints :: HNil]       = dep.innerDependency[PlayerPoints :: HNil]
    implicit val innerEmbed: Embedder[VB, core.Settlement.type :+: CNil] = cityEmbedder.innerEmbed[Settlement.type :+: CNil]

    val cities: Map[Vertex, Int] = dep
      .get[VertexBuildingState[VB]](state)
      .map { case (v, PlayerBuilding(vb, p)) =>
        cityEmbedder.select[City.type](vb).map(_ => (v, p))
      }
      .flatten
      .toMap

    def addCity(vertex: Vertex, player: Int): STATE = {
      val map = dep.get[VertexBuildingState[VB]](state)
      dep.update(map + (vertex -> PlayerBuilding(cityEmbedder.inject(City), player)), state)
    }

    def cityVerticesForPlayer(player: Int): List[Vertex] = cities.filter(_._2 == player).keys.toList

    def numCitiesForPlayer(player: Int): Int = cityVerticesForPlayer(player).size

    def buildCity(vertex: Vertex, player: Int): STATE = {
      state
        .removeSettlement(vertex)
        .addCity(vertex, player)
        .incrementPointForPlayer(player)
        .incrementPointForPlayer(player)
    }
  }

  implicit class BuildRoadStateOps[EB <: Coproduct, STATE <: HList](state: STATE)(implicit dep: DependsOn[STATE, EdgeBuildingState[EB] :: HNil], roadEmbedder: Embedder[EB, Road.type :+: CNil]) {

    val roads = dep
      .get[EdgeBuildingState[EB]](state)
      .map { case (v, PlayerBuilding(eb, p)) =>
        roadEmbedder.select[Road.type](eb).map(_ => (v, p))
      }
      .flatten
      .toMap

    def addRoad(edge: Edge, player: Int): STATE = {
      val map = dep.get[EdgeBuildingState[EB]](state)
      dep.update(map + (edge -> PlayerBuilding(roadEmbedder.inject(Road), player)), state)
    }

    def roadEdgesForPlayer(player: Int): List[Edge] = roads.filter(_._2 == player).keys.toList

    def numRoadsForPlayer(player: Int): Int = roadEdgesForPlayer(player).size
  }

  implicit class RobberStateOps[STATE <: HList](state: STATE)(implicit dep: DependsOn[STATE, RobberLocation :: HNil]) {
    val robberHexId: Int = dep.get[RobberLocation](state).robberHexId

    def updateRobberHexId(location: Int): STATE = dep.update(RobberLocation(location), state)
  }

  type ResForVertex[VB <: Coproduct] = coproduct.Folder.Aux[ResourcesForBuildingPoly.type, VB, Int]
  object ResourcesForBuildingPoly extends Poly1 {
    implicit def vertexBuilding[A](implicit vbValue: VertexBuildingValue[A]): Case.Aux[A, Int] = at(_ => vbValue.apply)
  }

  implicit class RoadLengthOps[STATE <: HList](state: STATE)(implicit dep: DependsOn[STATE, SOCLongestRoadPlayer :: SOCRoadLengths :: HNil]) {
    val longestRoadPlayer: Option[Int] = dep.get[SOCLongestRoadPlayer](state).player
    val roadLengths: Map[Int, Int]     = dep.get[SOCRoadLengths](state).m

    def updateLongestRoadPlayer(player: Option[Int]) = dep.update[SOCLongestRoadPlayer](SOCLongestRoadPlayer(player), state)

    def updateRoadLengths(lengths: Map[Int, Int]) = dep.update[SOCRoadLengths](SOCRoadLengths(lengths), state)
  }

  implicit class ArmySizeOps[STATE <: HList](state: STATE)(implicit dep: DependsOn[STATE, LargestArmyPlayer :: PlayerArmyCount :: HNil]) {
    val largestArmyPlayer: Option[Int] = dep.get[LargestArmyPlayer](state).player
    val armyCount: Map[Int, Int]       = dep.get[PlayerArmyCount](state).m

    def updateLargestArmyPlayer(player: Option[Int]) = dep.update[LargestArmyPlayer](LargestArmyPlayer(player), state)

    def updateArmyCount(count: Map[Int, Int]) = dep.update[PlayerArmyCount](PlayerArmyCount(count), state)
  }
}
