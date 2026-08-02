package soc.rules

import game.InventorySet
import soc.base.*
import soc.base.BaseGame.*
import soc.base.state.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.state.*
import soc.rules.validators.*
import soc.rules.validators.RobberValidator.RobberPlacement
import soc.rules.validators.TradeValidator.{PortTradeRanges, TradeRanges}

trait LegalMoveGenerator[-STATE, MOVE]:
  def legalMoves(state: STATE, player: Int, turnMoves: Seq[MOVE]): Seq[MOVE]
  def isLegal(state: STATE, player: Int, turnMoves: Seq[MOVE], move: MOVE): Boolean
  def isTerminal(state: STATE): Boolean
  def winners(state: STATE): Option[Set[Int]]

class PerfectInfoResourceView(state: PerfectInfoState) extends ResourceView:
  def getTotal(player: Int): Int = state.privateInventories.m.get(player).fold(0)(_.getTotal)
  def hasEnough(player: Int, resources: InventorySet[Resource, Int]): Boolean =
    state.privateInventories.m.get(player).fold(false)(_.contains(resources))
  def resourceAmount(player: Int, resource: Resource): Int =
    state.privateInventories.m.get(player).fold(0)(_.getAmount(resource))

class PublicInfoResourceView(state: PublicInfoState) extends ResourceView:
  def getTotal(player: Int): Int = state.publicInventories.m.getOrElse(player, 0)
  def hasEnough(player: Int, resources: InventorySet[Resource, Int]): Boolean =
    state.publicInventories.m.get(player).fold(false)(_ >= resources.getTotal)
  def resourceAmount(player: Int, resource: Resource): Int =
    state.publicInventories.m.get(player).fold(0)(identity)

class PerfectInfoDevCardView(state: PerfectInfoState) extends DevCardView:
  def hasUnexpiredCard(player: Int, card: DevelopmentCard, currentTurn: Int): Boolean =
    state.privateDevCardInv.m.get(player).fold(false)(_.exists { case (c, turn) => c == card && turn != currentTurn })
  def deckNonEmpty: Boolean = state.developmentCardDeck.cards.nonEmpty

class PublicInfoDevCardView(state: PublicInfoState) extends DevCardView:
  def hasUnexpiredCard(player: Int, card: DevelopmentCard, currentTurn: Int): Boolean =
    state.publicDevCardInv.m.get(player).fold(false)(_ > 0)
  def deckNonEmpty: Boolean = state.developmentCardDeckSize.size > 0

object PerfectInfoLegalMoves extends LegalMoveGenerator[PerfectInfoState, PerfectInfoMove]:

  def legalMoves(state: PerfectInfoState, player: Int, turnMoves: Seq[PerfectInfoMove]): Seq[PerfectInfoMove] =
    val phase = PhaseMachine.phase(state, turnMoves)
    val board = state.board
    val inv = new PerfectInfoResourceView(state)
    val devView = new PerfectInfoDevCardView(state)
    phase match
      case PhaseMachine.TurnPhase.Setup =>
        SetupValidator.legalMoves(player, PhaseMachine.numPlayers(state), state.setupPlacementOrder, state.vertexBuildingState, state.edgeBuildingState, board)
      case PhaseMachine.TurnPhase.PreRoll =>
        if player != PhaseMachine.activePlayer(state) then Nil
        else devCardMoves(player, turnMoves, devView, board, inv, state)
      case PhaseMachine.TurnPhase.MainPlay(_) =>
        if player != PhaseMachine.activePlayer(state) then Nil
        else mainPlayMoves(player, turnMoves, phase, devView, board, inv, state)
      case PhaseMachine.TurnPhase.DiscardPhase(pending) =>
        if pending.contains(player) then TurnValidator.discardMoves(player, inv) else Nil
      case PhaseMachine.TurnPhase.RobberPhase(roller) =>
        if player != roller then Nil
        else robberMoves(player, state, board, inv)
      case PhaseMachine.TurnPhase.GameOver => Nil

  def isLegal(state: PerfectInfoState, player: Int, turnMoves: Seq[PerfectInfoMove], move: PerfectInfoMove): Boolean =
    val phase = PhaseMachine.phase(state, turnMoves)
    val board = state.board
    val inv = new PerfectInfoResourceView(state)
    val devView = new PerfectInfoDevCardView(state)
    phase match
      case PhaseMachine.TurnPhase.Setup =>
        move match
          case m: InitialPlacementMove =>
            SetupValidator.legalMoves(player, PhaseMachine.numPlayers(state), state.setupPlacementOrder, state.vertexBuildingState, state.edgeBuildingState, board).contains(m)
          case _ => false
      case PhaseMachine.TurnPhase.PreRoll =>
        if player != PhaseMachine.activePlayer(state) then false
        else
          move match
            case r: RollDiceMoveResult => r.result >= 2 && r.result <= 12
            case _                     => devCardMoves(player, turnMoves, devView, board, inv, state).contains(move)
      case PhaseMachine.TurnPhase.MainPlay(_) =>
        if player != PhaseMachine.activePlayer(state) then false
        else
          move match
            case m: PortTradeMove[Resource] => TradeValidator.isLegalPortTrade(player, inv, state.vertexBuildingState, board, m)
            case m: TradeMove[Resource]     => TradeValidator.isLegalTrade(player, inv, m)
            case _ => mainPlayMoves(player, turnMoves, phase, devView, board, inv, state).contains(move)
      case PhaseMachine.TurnPhase.DiscardPhase(pending) =>
        move match
          case m: DiscardMove[Resource] => pending.contains(player) && TurnValidator.discardMoves(player, inv).contains(m)
          case _                        => false
      case PhaseMachine.TurnPhase.RobberPhase(roller) =>
        move match
          case m: PerfectInfoRobberMoveResult[Resource] =>
            player == roller && robberMoves(player, state, board, inv).contains(m)
          case _ => false
      case PhaseMachine.TurnPhase.GameOver => false

  def isTerminal(state: PerfectInfoState): Boolean = PhaseMachine.isTerminal(state)
  def winners(state: PerfectInfoState): Option[Set[Int]] = PhaseMachine.winners(state)

  def portTradeParams(state: PerfectInfoState, player: Int): PortTradeRanges =
    TradeValidator.portTradeParams(player, state.vertexBuildingState, state.board)

  def tradeParams(state: PerfectInfoState, player: Int): TradeRanges =
    TradeValidator.tradeParams(player, allPlayers(state), new PerfectInfoResourceView(state))

  private def allPlayers(state: PerfectInfoState): Seq[Int] =
    (state.playerPoints.points.keySet ++ state.setupPlacementOrder.placements.map(_._1)).toSeq.distinct.sorted

  private def devCardMoves(
    player: Int,
    turnMoves: Seq[PerfectInfoMove],
    devView: DevCardView,
    board: BaseBoard[Resource],
    inv: ResourceView,
    state: PerfectInfoState
  ): Seq[PerfectInfoMove] =
    val currentTurn = state.turn.number
    val buys: Seq[PerfectInfoMove] =
      DevCardValidator.perfectBuyMoves(player, inv, devView, state.developmentCardDeck.cards.headOption)
    val knights: Seq[PerfectInfoMove] =
      DevCardValidator.perfectPlayKnightMoves(player, turnMoves, devView, currentTurn, state.robberLocation, board, state.vertexBuildingState, inv, stealResource(state))
    val monopolies: Seq[PerfectInfoMove] =
      DevCardValidator.perfectPlayMonopolyMoves(player, turnMoves, devView, currentTurn, state.privateInventories)
    val yops: Seq[PerfectInfoMove] =
      DevCardValidator.playYearOfPlentyMoves(player, turnMoves, devView, currentTurn)
    val roads: Seq[PerfectInfoMove] =
      DevCardValidator.playRoadBuilderMoves(player, turnMoves, devView, currentTurn, state.edgeBuildingState, state.vertexBuildingState, board)
    buys ++ knights ++ monopolies ++ yops ++ roads

  private def mainPlayMoves(
    player: Int,
    turnMoves: Seq[PerfectInfoMove],
    phase: PhaseMachine.TurnPhase,
    devView: DevCardView,
    board: BaseBoard[Resource],
    inv: ResourceView,
    state: PerfectInfoState
  ): Seq[PerfectInfoMove] =
    val roads: Seq[PerfectInfoMove] =
      BuildingValidator.roadMoves(player, inv, state.edgeBuildingState, state.vertexBuildingState, board)
    val settlements: Seq[PerfectInfoMove] =
      BuildingValidator.settlementMoves(player, inv, state.vertexBuildingState, state.edgeBuildingState, board)
    val cities: Seq[PerfectInfoMove] =
      BuildingValidator.cityMoves(player, inv, state.vertexBuildingState, board)
    val building: Seq[PerfectInfoMove] = roads ++ settlements ++ cities
    val dev: Seq[PerfectInfoMove] = devCardMoves(player, turnMoves, devView, board, inv, state)
    val endTurn: Seq[PerfectInfoMove] = TurnValidator.endTurnMoves(player, turnMoves, phase)
    building ++ dev ++ endTurn

  private def robberMoves(
    player: Int,
    state: PerfectInfoState,
    board: BaseBoard[Resource],
    inv: ResourceView
  ): Seq[PerfectInfoMove] =
    RobberValidator.placements(player, state.robberLocation, board, state.vertexBuildingState, inv).flatMap {
      case RobberPlacement(hex, victims) if victims.nonEmpty =>
        victims.map { v =>
          val stolen = stealResource(state)(v).map(res => PlayerSteal[Resource](v, res))
          PerfectInfoRobberMoveResult(player, hex, stolen)
        }
      case RobberPlacement(hex, _) =>
        Seq(PerfectInfoRobberMoveResult(player, hex, None))
    }

  private def stealResource(state: PerfectInfoState): Int => Option[Resource] =
    victim => state.privateInventories.m.get(victim).flatMap(_.getTypes.headOption)

object PublicInfoLegalMoves extends LegalMoveGenerator[PublicInfoState, PublicInfoMove]:

  def legalMoves(state: PublicInfoState, player: Int, turnMoves: Seq[PublicInfoMove]): Seq[PublicInfoMove] =
    val phase = PhaseMachine.phase(state, turnMoves)
    val board = state.board
    val inv = new PublicInfoResourceView(state)
    val devView = new PublicInfoDevCardView(state)
    phase match
      case PhaseMachine.TurnPhase.Setup =>
        SetupValidator.legalMoves(player, PhaseMachine.numPlayers(state), state.setupPlacementOrder, state.vertexBuildingState, state.edgeBuildingState, board)
      case PhaseMachine.TurnPhase.PreRoll =>
        if player != PhaseMachine.activePlayer(state) then Nil
        else devCardMoves(player, turnMoves, devView, board, inv, state)
      case PhaseMachine.TurnPhase.MainPlay(_) =>
        if player != PhaseMachine.activePlayer(state) then Nil
        else mainPlayMoves(player, turnMoves, phase, devView, board, inv, state)
      case PhaseMachine.TurnPhase.DiscardPhase(pending) =>
        if pending.contains(player) then TurnValidator.discardMoves(player, inv) else Nil
      case PhaseMachine.TurnPhase.RobberPhase(roller) =>
        if player != roller then Nil
        else RobberValidator.robberMoves(player, state.robberLocation, board, state.vertexBuildingState, inv)
      case PhaseMachine.TurnPhase.GameOver => Nil

  def isLegal(state: PublicInfoState, player: Int, turnMoves: Seq[PublicInfoMove], move: PublicInfoMove): Boolean =
    val phase = PhaseMachine.phase(state, turnMoves)
    val board = state.board
    val inv = new PublicInfoResourceView(state)
    val devView = new PublicInfoDevCardView(state)
    phase match
      case PhaseMachine.TurnPhase.Setup =>
        move match
          case m: InitialPlacementMove =>
            SetupValidator.legalMoves(player, PhaseMachine.numPlayers(state), state.setupPlacementOrder, state.vertexBuildingState, state.edgeBuildingState, board).contains(m)
          case _ => false
      case PhaseMachine.TurnPhase.PreRoll =>
        if player != PhaseMachine.activePlayer(state) then false
        else
          move match
            case r: RollDiceMoveResult => r.result >= 2 && r.result <= 12
            case _                     => devCardMoves(player, turnMoves, devView, board, inv, state).contains(move)
      case PhaseMachine.TurnPhase.MainPlay(_) =>
        if player != PhaseMachine.activePlayer(state) then false
        else
          move match
            case m: PortTradeMove[Resource] => TradeValidator.isLegalPortTrade(player, inv, state.vertexBuildingState, board, m)
            case m: TradeMove[Resource]     => TradeValidator.isLegalTrade(player, inv, m)
            case _ => mainPlayMoves(player, turnMoves, phase, devView, board, inv, state).contains(move)
      case PhaseMachine.TurnPhase.DiscardPhase(pending) =>
        move match
          case m: DiscardMove[Resource] => pending.contains(player) && TurnValidator.discardMoves(player, inv).contains(m)
          case _                        => false
      case PhaseMachine.TurnPhase.RobberPhase(roller) =>
        move match
          case m: RobberMoveResult[Resource] =>
            player == roller && RobberValidator.robberMoves(player, state.robberLocation, board, state.vertexBuildingState, inv).contains(m)
          case _ => false
      case PhaseMachine.TurnPhase.GameOver => false

  def isTerminal(state: PublicInfoState): Boolean = PhaseMachine.isTerminal(state)
  def winners(state: PublicInfoState): Option[Set[Int]] = PhaseMachine.winners(state)

  def portTradeParams(state: PublicInfoState, player: Int): PortTradeRanges =
    TradeValidator.portTradeParams(player, state.vertexBuildingState, state.board)

  def tradeParams(state: PublicInfoState, player: Int): TradeRanges =
    TradeValidator.tradeParams(player, allPlayers(state), new PublicInfoResourceView(state))

  private def allPlayers(state: PublicInfoState): Seq[Int] =
    (state.playerPoints.points.keySet ++ state.setupPlacementOrder.placements.map(_._1)).toSeq.distinct.sorted

  private def devCardMoves(
    player: Int,
    turnMoves: Seq[PublicInfoMove],
    devView: DevCardView,
    board: BaseBoard[Resource],
    inv: ResourceView,
    state: PublicInfoState
  ): Seq[PublicInfoMove] =
    val currentTurn = state.turn.number
    val buys: Seq[PublicInfoMove] = DevCardValidator.buyMoves(player, inv, devView, None)
    val knights: Seq[PublicInfoMove] =
      DevCardValidator.playKnightMoves(player, turnMoves, devView, currentTurn, state.robberLocation, board, state.vertexBuildingState, inv)
    val monopolies: Seq[PublicInfoMove] = DevCardValidator.playMonopolyMoves(player, turnMoves, devView, currentTurn)
    val yops: Seq[PublicInfoMove] = DevCardValidator.playYearOfPlentyMoves(player, turnMoves, devView, currentTurn)
    val roads: Seq[PublicInfoMove] =
      DevCardValidator.playRoadBuilderMoves(player, turnMoves, devView, currentTurn, state.edgeBuildingState, state.vertexBuildingState, board)
    buys ++ knights ++ monopolies ++ yops ++ roads

  private def mainPlayMoves(
    player: Int,
    turnMoves: Seq[PublicInfoMove],
    phase: PhaseMachine.TurnPhase,
    devView: DevCardView,
    board: BaseBoard[Resource],
    inv: ResourceView,
    state: PublicInfoState
  ): Seq[PublicInfoMove] =
    val roads: Seq[PublicInfoMove] =
      BuildingValidator.roadMoves(player, inv, state.edgeBuildingState, state.vertexBuildingState, board)
    val settlements: Seq[PublicInfoMove] =
      BuildingValidator.settlementMoves(player, inv, state.vertexBuildingState, state.edgeBuildingState, board)
    val cities: Seq[PublicInfoMove] =
      BuildingValidator.cityMoves(player, inv, state.vertexBuildingState, board)
    val building: Seq[PublicInfoMove] = roads ++ settlements ++ cities
    val dev: Seq[PublicInfoMove] = devCardMoves(player, turnMoves, devView, board, inv, state)
    val endTurn: Seq[PublicInfoMove] = TurnValidator.endTurnMoves(player, turnMoves, phase)
    building ++ dev ++ endTurn
