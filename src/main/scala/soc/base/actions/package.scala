package soc.base

import game.{GameState, InventorySet}
import soc.base.BaseGame.{BaseEdgeBuilding, BaseVertexBuilding}
import soc.base.state.*
import soc.core.*
import soc.core.ResourceSet.*
import soc.core.Resources.*
import soc.core.state.*

package object actions:

  type BaseVertexBuilding = City.type | Settlement.type
  type BaseEdgeBuilding   = Road.type

  type LongestRoadDelta   = (SOCRoadLengths, SOCLongestRoadPlayer, PlayerPoints)
  type LargestArmyDelta   = (PlayerArmyCount, LargestArmyPlayer, PlayerPoints)

  type LongestRoadSlice = (
    BaseBoard[Resource],
    SOCRoadLengths,
    SOCLongestRoadPlayer,
    VertexBuildingState[BaseVertexBuilding],
    EdgeBuildingState[BaseEdgeBuilding]
  )

  trait LongestRoadState:
    def board: BaseBoard[Resource]
    def socRoadLengths: SOCRoadLengths
    def socLongestRoadPlayer: SOCLongestRoadPlayer
    def vertexBuildingState: VertexBuildingState[BaseVertexBuilding]
    def edgeBuildingState: EdgeBuildingState[BaseEdgeBuilding]

  case class LongestRoadChanges(
    roadLengthChanges: List[SOCRoadLengths#Delta],
    longestRoadPlayerChanges: List[SOCLongestRoadPlayer.Delta],
    pointChanges: List[PlayerPoints#Delta]
  )

  val SETTLEMENT_COST: Resources = ResourceSet(WOOD, BRICK, WHEAT, SHEEP)
  val CITY_COST: Resources       = ResourceSet(ORE, ORE, ORE, WHEAT, WHEAT)
  val ROAD_COST: Resources       = ResourceSet(WOOD, BRICK)
  val DEV_CARD_COST: Resources   = ResourceSet(ORE, WHEAT, SHEEP)

  def vertexBuildingResources(b: BaseVertexBuilding): Int = b match
    case _: Settlement.type => 1
    case _: City.type       => 2

  def specialPlayerDeltas[SP](
    wrap: SpecialPlayer.Delta => SP,
    current: Option[Int],
    updated: Option[Int]
  ): List[SP | PlayerPoints#Delta] =
    (current, updated) match
      case (None, None)                     => Nil
      case (None, Some(p))                  =>
        wrap(SpecialPlayer.Set(p)) ::
        PlayerPoints.Increment(p) ::
        PlayerPoints.Increment(p) :: Nil
      case (Some(o), None)                  =>
        wrap(SpecialPlayer.Remove) ::
        PlayerPoints.Decrement(o) ::
        PlayerPoints.Decrement(o) :: Nil
      case (Some(o), Some(n)) if o == n     => Nil
      case (Some(o), Some(n))               =>
        wrap(SpecialPlayer.Remove) ::
        PlayerPoints.Decrement(o) ::
        PlayerPoints.Decrement(o) ::
        wrap(SpecialPlayer.Set(n)) ::
        PlayerPoints.Increment(n) ::
        PlayerPoints.Increment(n) :: Nil

  def longestRoadChanges(
    state: LongestRoadState,
    vertexBuildings: VertexBuildingState[BaseVertexBuilding],
    edgeBuildings: EdgeBuildingState[BaseEdgeBuilding]
  ): LongestRoadChanges =
    val updatedLengths = LongestRoadOps(state.board, edgeBuildings, vertexBuildings).calcLongestRoadLengths()
    val players = state.socRoadLengths.m.keySet ++ updatedLengths.m.keySet
    val roadLengthChanges = players.toList.sorted.flatMap { player =>
      val updatedLength = updatedLengths.m.getOrElse(player, 0)
      if state.socRoadLengths.m.getOrElse(player, 0) == updatedLength then Nil
      else List(SpecialCounts.Set(player, updatedLength))
    }
    val maximumLength = updatedLengths.m.values.maxOption.getOrElse(0)
    val longestPlayers = updatedLengths.m.collect { case (player, `maximumLength`) if maximumLength >= 5 => player }.toSet
    val updatedHolder = state.socLongestRoadPlayer.player.filter(longestPlayers.contains).orElse {
      if longestPlayers.size == 1 then longestPlayers.headOption else None
    }
    val specialChanges = specialPlayerDeltas[SOCLongestRoadPlayer.Delta](
      SOCLongestRoadPlayer.Delta.apply,
      state.socLongestRoadPlayer.player,
      updatedHolder
    )
    val longestRoadPlayerChanges: List[SOCLongestRoadPlayer.Delta] = specialChanges.collect {
      case delta: SOCLongestRoadPlayer.Delta => delta
    }
    val pointChanges: List[PlayerPoints#Delta] = specialChanges.collect {
      case increment: PlayerPoints.Increment => increment
      case decrement: PlayerPoints.Decrement => decrement
    }
    LongestRoadChanges(roadLengthChanges, longestRoadPlayerChanges, pointChanges)

  def largestArmySpecialPlayerDeltas(
    currentHolder: Option[Int],
    currentCounts: PlayerArmyCount,
    player: Int
  ): List[SpecialPlayer.Delta | PlayerPoints#Delta] =
    val updatedCount       = currentCounts.m.getOrElse(player, 0) + 1
    val currentLeaderCount = currentHolder.flatMap(currentCounts.m.get)

    if updatedCount < 3 then Nil
    else
      currentHolder match
        case None =>
          specialPlayerDeltas[SpecialPlayer.Delta](identity, None, Some(player))
        case Some(holder) if holder == player => Nil
        case Some(holder) if updatedCount > currentLeaderCount.getOrElse(0) =>
          specialPlayerDeltas[SpecialPlayer.Delta](identity, Some(holder), Some(player))
        case _ => Nil

  case class TurnInput(turn: Turn)
