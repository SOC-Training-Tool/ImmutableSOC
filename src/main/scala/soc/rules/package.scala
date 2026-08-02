package soc

import game.InventorySet
import soc.base.*
import soc.base.BaseGame.*
import soc.base.DevelopmentCards.POINT
import soc.base.state.*
import soc.core.*
import soc.core.ResourceSet.*
import soc.core.Resources.*
import soc.core.state.*

package object rules:

  type BaseVertexBuilding = City.type | Settlement.type
  type BaseEdgeBuilding   = Road.type

  trait ResourceView:
    def getTotal(player: Int): Int
    def hasEnough(player: Int, resources: InventorySet[Resource, Int]): Boolean
    def resourceAmount(player: Int, resource: Resource): Int

  trait DevCardView:
    def hasUnexpiredCard(player: Int, card: DevelopmentCard, currentTurn: Int): Boolean
    def deckNonEmpty: Boolean

  val ROAD_COST: Resources       = ResourceSet(WOOD, BRICK)
  val SETTLEMENT_COST: Resources = ResourceSet(WOOD, BRICK, WHEAT, SHEEP)
  val CITY_COST: Resources       = ResourceSet(ORE, ORE, ORE, WHEAT, WHEAT)
  val DEV_CARD_COST: Resources   = ResourceSet(ORE, WHEAT, SHEEP)

  object PhaseMachine:

    enum TurnPhase:
      case Setup
      case PreRoll
      case MainPlay(devCardPlayed: Boolean)
      case DiscardPhase(pendingPlayers: Set[Int])
      case RobberPhase(player: Int)
      case GameOver

    def diceRolled(turnMoves: Seq[Any]): Boolean =
      turnMoves.exists(_.isInstanceOf[RollDiceMoveResult])

    def hasSevenRolled(turnMoves: Seq[Any]): Boolean =
      turnMoves.exists {
        case r: RollDiceMoveResult => r.result == 7
        case _                     => false
      }

    def robberMoved(turnMoves: Seq[Any]): Boolean =
      turnMoves.exists {
        case _: PerfectInfoRobberMoveResult[_] => true
        case _: RobberMoveResult[_]            => true
        case _: PerfectInfoPlayKnightResult[_] => true
        case _: PlayKnightResult[_]            => true
        case _                                 => false
      }

    def hasPlayedDevCardThisTurn(turnMoves: Seq[Any]): Boolean =
      turnMoves.exists {
        case _: PerfectInfoPlayKnightResult[_] => true
        case _: PlayKnightResult[_]            => true
        case _: PlayMonopolyMoveResult[_]      => true
        case _: PlayRoadBuilderMove            => true
        case _: PlayYearOfPlentyMove[_]        => true
        case _: PlayPointMove                  => true
        case _                                 => false
      }

    def numPlayers(state: PerfectInfoState): Int =
      numPlayersFrom(state.playerPoints.points.keySet, state.setupPlacementOrder.placements.map(_._1))
    def numPlayers(state: PublicInfoState): Int =
      numPlayersFrom(state.playerPoints.points.keySet, state.setupPlacementOrder.placements.map(_._1))

    private def numPlayersFrom(pointsPlayers: Set[Int], placedPlayers: Seq[Int]): Int =
      val known = math.max(pointsPlayers.size, placedPlayers.distinct.size)
      if known == 0 then 1 else known

    def inSetup(state: PerfectInfoState): Boolean = inSetupFrom(state.setupPlacementOrder.placements.length, numPlayers(state))
    def inSetup(state: PublicInfoState): Boolean  = inSetupFrom(state.setupPlacementOrder.placements.length, numPlayers(state))

    private def inSetupFrom(placements: Int, players: Int): Boolean =
      placements == 0 || placements < players * 2

    def activePlayer(state: PerfectInfoState): Int =
      activePlayerFrom(state.setupPlacementOrder.placements.map(_._1), state.turn.number, numPlayers(state))
    def activePlayer(state: PublicInfoState): Int =
      activePlayerFrom(state.setupPlacementOrder.placements.map(_._1), state.turn.number, numPlayers(state))

    private def activePlayerFrom(placedPlayers: Seq[Int], turn: Int, players: Int): Int =
      if inSetupFrom(placedPlayers.length, players) then setupActivePlayer(placedPlayers, players)
      else turn % players

    def setupActivePlayer(placedPlayers: Seq[Int], players: Int): Int =
      if placedPlayers.isEmpty then 0
      else if placedPlayers.length < players then placedPlayers.length
      else 2 * players - 1 - placedPlayers.length

    def playersWithTooManyCards(state: PerfectInfoState): Set[Int] =
      state.privateInventories.m.collect { case (p, inv) if inv.getTotal > 7 => p }.toSet
    def playersWithTooManyCards(state: PublicInfoState): Set[Int] =
      state.publicInventories.m.collect { case (p, total) if total > 7 => p }.toSet

    def stillNeedDiscards(state: PerfectInfoState): Boolean = playersWithTooManyCards(state).nonEmpty
    def stillNeedDiscards(state: PublicInfoState): Boolean  = playersWithTooManyCards(state).nonEmpty

    def totalVPs(state: PerfectInfoState): Map[Int, Int] =
      val points = state.playerPoints.points
      val hidden = state.privateDevCardInv.m.map { case (p, cards) => p -> cards.count(_._1 == POINT) }
      val players = points.keySet ++ hidden.keySet
      players.map(p => p -> (points.getOrElse(p, 0) + hidden.getOrElse(p, 0))).toMap

    def isTerminal(state: PerfectInfoState): Boolean = totalVPs(state).values.exists(_ >= 10)
    def isTerminal(state: PublicInfoState): Boolean  = state.playerPoints.points.values.exists(_ >= 10)

    def winners(state: PerfectInfoState): Option[Set[Int]] =
      val ws = totalVPs(state).collect { case (p, v) if v >= 10 => p }.toSet
      if ws.nonEmpty then Some(ws) else None
    def winners(state: PublicInfoState): Option[Set[Int]] =
      val ws = state.playerPoints.points.collect { case (p, v) if v >= 10 => p }.toSet
      if ws.nonEmpty then Some(ws) else None

    def phase(state: PerfectInfoState, turnMoves: Seq[Any]): TurnPhase =
      if isTerminal(state) then TurnPhase.GameOver
      else if inSetup(state) then TurnPhase.Setup
      else if !diceRolled(turnMoves) then TurnPhase.PreRoll
      else if hasSevenRolled(turnMoves) && stillNeedDiscards(state) then TurnPhase.DiscardPhase(playersWithTooManyCards(state))
      else if hasSevenRolled(turnMoves) && !robberMoved(turnMoves) then TurnPhase.RobberPhase(activePlayer(state))
      else TurnPhase.MainPlay(hasPlayedDevCardThisTurn(turnMoves))

    def phase(state: PublicInfoState, turnMoves: Seq[Any]): TurnPhase =
      if isTerminal(state) then TurnPhase.GameOver
      else if inSetup(state) then TurnPhase.Setup
      else if !diceRolled(turnMoves) then TurnPhase.PreRoll
      else if hasSevenRolled(turnMoves) && stillNeedDiscards(state) then TurnPhase.DiscardPhase(playersWithTooManyCards(state))
      else if hasSevenRolled(turnMoves) && !robberMoved(turnMoves) then TurnPhase.RobberPhase(activePlayer(state))
      else TurnPhase.MainPlay(hasPlayedDevCardThisTurn(turnMoves))

  object PhaseGate:

    def isSetup(phase: PhaseMachine.TurnPhase): Boolean          = phase == PhaseMachine.TurnPhase.Setup
    def isPreRoll(phase: PhaseMachine.TurnPhase): Boolean         = phase == PhaseMachine.TurnPhase.PreRoll
    def isMainPlay(phase: PhaseMachine.TurnPhase): Boolean        = phase.isInstanceOf[PhaseMachine.TurnPhase.MainPlay]
    def isDiscardPhase(phase: PhaseMachine.TurnPhase): Boolean    = phase.isInstanceOf[PhaseMachine.TurnPhase.DiscardPhase]
    def isRobberPhase(phase: PhaseMachine.TurnPhase): Boolean     = phase.isInstanceOf[PhaseMachine.TurnPhase.RobberPhase]
    def isGameOver(phase: PhaseMachine.TurnPhase): Boolean        = phase == PhaseMachine.TurnPhase.GameOver
