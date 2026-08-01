package soc.rules

import game.InventorySet
import game.ImmutableGame
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import soc.base.*
import soc.base.BaseGame.*
import soc.base.DevelopmentCards.*
import soc.base.state.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.ResourceSet.*
import soc.core.Resources.*
import soc.core.state.*
import soc.rules.PhaseMachine.TurnPhase
import scala.annotation.tailrec
import scala.util.Random

class EndToEndSpec extends AnyFunSpec with Matchers:

  private val bank: Resources =
    InventorySet.fromMap(Map(WOOD -> 19, BRICK -> 19, SHEEP -> 19, WHEAT -> 19, ORE -> 19))

  private val devDeck: List[DevelopmentCard] = List(
    KNIGHT, POINT, KNIGHT, POINT, POINT, KNIGHT, KNIGHT, ROAD_BUILDER,
    POINT, KNIGHT, MONOPOLY, YEAR_OF_PLENTY, YEAR_OF_PLENTY, KNIGHT, KNIGHT,
    KNIGHT, ROAD_BUILDER, MONOPOLY, KNIGHT, KNIGHT, KNIGHT, POINT, KNIGHT,
    KNIGHT, KNIGHT
  )

  private val players: Int = 4

  private def makeBoard: BaseBoard[Resource] =
    val ports: List[Port] =
      import Ports.*
      List(MISC, ORE, MISC, WHEAT, MISC, BRICK, WOOD, SHEEP, MISC)
    BaseBoard(
      List[Hex[Resource]](
        ResourceHex(WHEAT, 6), ResourceHex(ORE, 2), ResourceHex(SHEEP, 5),
        ResourceHex(ORE, 8), ResourceHex(WOOD, 4), ResourceHex(BRICK, 11),
        ResourceHex(SHEEP, 12), ResourceHex(ORE, 9), ResourceHex(SHEEP, 10),
        ResourceHex(BRICK, 8), Desert, ResourceHex(WHEAT, 3),
        ResourceHex(SHEEP, 9), ResourceHex(BRICK, 10), ResourceHex(WOOD, 3),
        ResourceHex(WOOD, 6), ResourceHex(WHEAT, 5), ResourceHex(WOOD, 4),
        ResourceHex(WHEAT, 11)
      ),
      ports
    )

  private def initPerfect: PerfectInfoState = PerfectInfoState(
    robberLocation       = RobberLocation(10),
    privateInventories   = PrivateInventories(Map.empty),
    privateDevCardInv    = PrivateDevCardInv(Map.empty),
    developmentCardDeck  = DevelopmentCardDeck(devDeck),
    bank                 = Bank(bank),
    turn                 = Turn(0),
    playerPoints         = PlayerPoints((0 until players).map(_ -> 0).toMap),
    largestArmyPlayer    = LargestArmyPlayer(None),
    playerArmyCount      = PlayerArmyCount(Map.empty),
    vertexBuildingState  = VertexBuildingState(Map.empty),
    socRoadLengths       = SOCRoadLengths(Map.empty),
    socLongestRoadPlayer = SOCLongestRoadPlayer(None),
    board                = makeBoard,
    edgeBuildingState    = EdgeBuildingState(Map.empty),
    moveCount            = MoveCount(0),
    setupPlacementOrder  = SetupPlacementOrder(Nil)
  )

  private def initPublic: PublicInfoState = PublicInfoState(
    robberLocation          = RobberLocation(10),
    publicInventories       = PublicInventories(Map.empty),
    publicDevCardInv        = PublicDevCardInv(Map.empty),
    developmentCardDeckSize = DevelopmentCardDeckSize(25),
    bank                    = Bank(bank),
    turn                    = Turn(0),
    playerPoints            = PlayerPoints((0 until players).map(_ -> 0).toMap),
    largestArmyPlayer       = LargestArmyPlayer(None),
    playerArmyCount         = PlayerArmyCount(Map.empty),
    vertexBuildingState     = VertexBuildingState(Map.empty),
    socRoadLengths          = SOCRoadLengths(Map.empty),
    socLongestRoadPlayer    = SOCLongestRoadPlayer(None),
    board                   = makeBoard,
    edgeBuildingState       = EdgeBuildingState(Map.empty),
    moveCount               = MoveCount(0),
    setupPlacementOrder     = SetupPlacementOrder(Nil)
  )

  private trait PhaseRules[STATE, MOVE]:
    def phase(state: STATE, turnMoves: Seq[MOVE]): TurnPhase
    def setupPlayer(state: STATE): Int
    def activePlayer(state: STATE): Int
    def rollMove(player: Int, result: Int): MOVE
    def endsTurn(move: MOVE): Boolean

  private given perfectPhaseRules: PhaseRules[PerfectInfoState, PerfectInfoMove] with
    def phase(state: PerfectInfoState, turnMoves: Seq[PerfectInfoMove]): TurnPhase =
      PhaseMachine.phase(state, turnMoves)
    def setupPlayer(state: PerfectInfoState): Int =
      PhaseMachine.setupActivePlayer(state.setupPlacementOrder.placements.map(_._1), PhaseMachine.numPlayers(state))
    def activePlayer(state: PerfectInfoState): Int = PhaseMachine.activePlayer(state)
    def rollMove(player: Int, result: Int): PerfectInfoMove = RollDiceMoveResult(player, result)
    def endsTurn(move: PerfectInfoMove): Boolean = move.isInstanceOf[EndTurnMove]

  private given publicPhaseRules: PhaseRules[PublicInfoState, PublicInfoMove] with
    def phase(state: PublicInfoState, turnMoves: Seq[PublicInfoMove]): TurnPhase =
      PhaseMachine.phase(state, turnMoves)
    def setupPlayer(state: PublicInfoState): Int =
      PhaseMachine.setupActivePlayer(state.setupPlacementOrder.placements.map(_._1), PhaseMachine.numPlayers(state))
    def activePlayer(state: PublicInfoState): Int = PhaseMachine.activePlayer(state)
    def rollMove(player: Int, result: Int): PublicInfoMove = RollDiceMoveResult(player, result)
    def endsTurn(move: PublicInfoMove): Boolean = move.isInstanceOf[EndTurnMove]

  private case class GameResult[STATE](state: STATE, moves: Int, phasesSeen: Set[TurnPhase])

  private def playGame[STATE, MOVE](
    init: STATE,
    game: ImmutableGame[MOVE, STATE],
    legal: LegalMoveGenerator[STATE, MOVE],
    seed: Long,
    maxMoves: Int
  )(using rules: PhaseRules[STATE, MOVE]): GameResult[STATE] =
    val rng = new Random(seed)

    @tailrec
    def loop(
      state: STATE,
      turnMoves: List[MOVE],
      count: Int,
      phasesSeen: Set[TurnPhase]
    ): GameResult[STATE] =
      if legal.isTerminal(state) then GameResult(state, count, phasesSeen)
      else if count >= maxMoves then
        fail(s"game did not reach a terminal state after $maxMoves moves (seed $seed)")
      else
        val phase = rules.phase(state, turnMoves)
        phase match
          case TurnPhase.Setup =>
            val move = chooseLegal(legal, state, rules.setupPlayer(state), turnMoves, phase, rng, count)
            val next = game.applyMoveAny(move, state)._2
            loop(next, turnMoves :+ move, count + 1, phasesSeen + phase)
          case TurnPhase.PreRoll =>
            val player = rules.activePlayer(state)
            val move = rules.rollMove(player, rng.nextInt(6) + rng.nextInt(6) + 2)
            val next = game.applyMoveAny(move, state)._2
            loop(next, turnMoves :+ move, count + 1, phasesSeen + phase)
          case TurnPhase.DiscardPhase(pending) =>
            val move = chooseLegal(legal, state, pending.min, turnMoves, phase, rng, count)
            val next = game.applyMoveAny(move, state)._2
            loop(next, turnMoves :+ move, count + 1, phasesSeen + phase)
          case TurnPhase.RobberPhase(roller) =>
            val move = chooseLegal(legal, state, roller, turnMoves, phase, rng, count)
            val next = game.applyMoveAny(move, state)._2
            loop(next, turnMoves :+ move, count + 1, phasesSeen + phase)
          case TurnPhase.MainPlay(_) =>
            val move = chooseMainPlay(legal, state, rules.activePlayer(state), turnMoves, phase, rules, rng, count)
            val next = game.applyMoveAny(move, state)._2
            val nextTurnMoves = if rules.endsTurn(move) then Nil else turnMoves :+ move
            loop(next, nextTurnMoves, count + 1, phasesSeen + phase)
          case TurnPhase.GameOver =>
            GameResult(state, count, phasesSeen)

    loop(init, Nil, 0, Set.empty)

  private def chooseLegal[STATE, MOVE](
    legal: LegalMoveGenerator[STATE, MOVE],
    state: STATE,
    player: Int,
    turnMoves: Seq[MOVE],
    phase: TurnPhase,
    rng: Random,
    count: Int
  ): MOVE =
    val moves = legal.legalMoves(state, player, turnMoves)
    if moves.isEmpty then fail(s"no legal moves for player $player in phase $phase at move $count")
    else moves(rng.nextInt(moves.length))

  private def chooseMainPlay[STATE, MOVE](
    legal: LegalMoveGenerator[STATE, MOVE],
    state: STATE,
    player: Int,
    turnMoves: Seq[MOVE],
    phase: TurnPhase,
    rules: PhaseRules[STATE, MOVE],
    rng: Random,
    count: Int
  ): MOVE =
    val moves = legal.legalMoves(state, player, turnMoves)
    if moves.isEmpty then fail(s"no legal moves for player $player in phase $phase at move $count")
    else
      val productive = moves.filterNot(rules.endsTurn)
      if productive.nonEmpty then productive(rng.nextInt(productive.length))
      else moves.head

  describe("end-to-end game"):

    it("creates a board, 4 players, and plays an entire perfect-info game to 10 VP"):
      val result = playGame[PerfectInfoState, PerfectInfoMove](
        initPerfect, perfectInfoGame, PerfectInfoLegalMoves, seed = 20260710L, maxMoves = 5000
      )
      val state = result.state

      PerfectInfoLegalMoves.isTerminal(state) shouldBe true
      PerfectInfoLegalMoves.winners(state) should not be empty
      result.moves should be > 0
      state.turn.t should be > 0
      state.setupPlacementOrder.placements.length shouldBe players * 2
      state.playerPoints.points.keySet should contain allOf (0, 1, 2, 3)
      result.phasesSeen should contain (TurnPhase.Setup)
      result.phasesSeen should contain (TurnPhase.PreRoll)
      result.phasesSeen.exists(_.isInstanceOf[TurnPhase.MainPlay]) shouldBe true
      PerfectInfoLegalMoves.winners(state).get.forall(p => PhaseMachine.totalVPs(state)(p) >= 10) shouldBe true

    it("creates a board, 4 players, and plays an entire public-info game to 10 VP"):
      val result = playGame[PublicInfoState, PublicInfoMove](
        initPublic, publicInfoGame, PublicInfoLegalMoves, seed = 20260710L, maxMoves = 5000
      )
      val state = result.state

      PublicInfoLegalMoves.isTerminal(state) shouldBe true
      PublicInfoLegalMoves.winners(state) should not be empty
      result.moves should be > 0
      state.turn.t should be > 0
      state.setupPlacementOrder.placements.length shouldBe players * 2
      state.playerPoints.points.keySet should contain allOf (0, 1, 2, 3)
      result.phasesSeen should contain (TurnPhase.Setup)
      result.phasesSeen should contain (TurnPhase.PreRoll)
      result.phasesSeen.exists(_.isInstanceOf[TurnPhase.MainPlay]) shouldBe true
      PublicInfoLegalMoves.winners(state).get.forall(p => state.playerPoints.points(p) >= 10) shouldBe true
