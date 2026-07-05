package soc.base

import game.{Delta, GameState, InventorySet}
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
  ): List[SP | Delta[PlayerPoints]] =
    (current, updated) match
      case (None, None)                     => Nil
      case (None, Some(p))                  =>
        wrap(SpecialPlayer.Set(p)) ::
        Delta[PlayerPoints](PlayerPoints.Increment(p)) ::
        Delta[PlayerPoints](PlayerPoints.Increment(p)) :: Nil
      case (Some(o), None)                  =>
        wrap(SpecialPlayer.Remove) ::
        Delta[PlayerPoints](PlayerPoints.Decrement(o)) ::
        Delta[PlayerPoints](PlayerPoints.Decrement(o)) :: Nil
      case (Some(o), Some(n)) if o == n     => Nil
      case (Some(o), Some(n))               =>
        wrap(SpecialPlayer.Remove) ::
        Delta[PlayerPoints](PlayerPoints.Decrement(o)) ::
        Delta[PlayerPoints](PlayerPoints.Decrement(o)) ::
        wrap(SpecialPlayer.Set(n)) ::
        Delta[PlayerPoints](PlayerPoints.Increment(n)) ::
        Delta[PlayerPoints](PlayerPoints.Increment(n)) :: Nil

  case class TurnInput(turn: Turn)
