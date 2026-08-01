package soc.rules

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import soc.base.*
import soc.base.BaseGame.*
import soc.base.DevelopmentCards.POINT
import soc.base.state.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.ResourceSet.*
import soc.core.Resources.*
import soc.core.state.*
import soc.rules.PhaseMachine.TurnPhase
import soc.rules.RulesFixtures.*

class PhaseMachineSpec extends AnyFunSpec with Matchers:

  describe("PhaseMachine.phase"):

    it("derives Setup for a fresh game"):
      PhaseMachine.phase(initPerfect, Nil) shouldBe TurnPhase.Setup

    it("derives PreRoll once setup is complete and no dice rolled"):
      val state = afterSetupPerfect
      PhaseMachine.phase(state, setupMoves) shouldBe TurnPhase.PreRoll

    it("derives MainPlay after a non-7 roll"):
      val state = afterSetupPerfect
      PhaseMachine.phase(state, setupMoves :+ RollDiceMoveResult(0, 5)) shouldBe TurnPhase.MainPlay(false)

    it("derives DiscardPhase when a 7 is rolled and a player holds more than 7 cards"):
      val state = afterSetupPerfect.copy(
        privateInventories = PrivateInventories(Map(1 -> ResourceSet(wo = 12)))
      )
      PhaseMachine.phase(state, setupMoves :+ RollDiceMoveResult(0, 7)) shouldBe TurnPhase.DiscardPhase(Set(1))

    it("derives RobberPhase when a 7 is rolled and nobody needs to discard"):
      PhaseMachine.phase(afterSetupPerfect, setupMoves :+ RollDiceMoveResult(0, 7)) shouldBe TurnPhase.RobberPhase(0)

    it("returns to RobberPhase after all discards are done"):
      val state = afterSetupPerfect.copy(
        privateInventories = PrivateInventories(Map(1 -> ResourceSet(wo = 12)))
      )
      val turnMoves = setupMoves :+ RollDiceMoveResult(0, 7)
      val afterDiscard = afterPerfect(setupMoves ++ List(RollDiceMoveResult(0, 7)): _*).copy(
        privateInventories = PrivateInventories(Map(1 -> ResourceSet(wo = 6)))
      )
      PhaseMachine.phase(afterDiscard, turnMoves :+ DiscardMove[Resource](1, ResourceSet(wo = 6))) shouldBe TurnPhase.RobberPhase(0)

    it("derives MainPlay after the robber has moved"):
      val turnMoves = setupMoves :+ RollDiceMoveResult(0, 7) :+ PerfectInfoRobberMoveResult(0, 5, None)
      PhaseMachine.phase(afterSetupPerfect, turnMoves) shouldBe TurnPhase.MainPlay(false)

    it("tracks that a dev card was played this turn"):
      val turnMoves = setupMoves :+ RollDiceMoveResult(0, 5) :+ PlayYearOfPlentyMove[Resource](0, WOOD, WOOD)
      PhaseMachine.phase(afterSetupPerfect, turnMoves) shouldBe TurnPhase.MainPlay(true)

    it("derives GameOver when any player reaches 10 VP"):
      val state = afterSetupPerfect.copy(playerPoints = PlayerPoints(Map(0 -> 10, 1 -> 3, 2 -> 3, 3 -> 3)))
      PhaseMachine.phase(state, setupMoves :+ RollDiceMoveResult(0, 5)) shouldBe TurnPhase.GameOver

  describe("PhaseMachine helpers"):

    it("detects a rolled dice in the move log"):
      PhaseMachine.diceRolled(Nil) shouldBe false
      PhaseMachine.diceRolled(Seq(RollDiceMoveResult(0, 5))) shouldBe true
      PhaseMachine.hasSevenRolled(Seq(RollDiceMoveResult(0, 7))) shouldBe true
      PhaseMachine.hasSevenRolled(Seq(RollDiceMoveResult(0, 5))) shouldBe false

    it("detects robber moves in the move log"):
      PhaseMachine.robberMoved(Seq(RollDiceMoveResult(0, 7))) shouldBe false
      PhaseMachine.robberMoved(Seq(PerfectInfoRobberMoveResult(0, 5, None))) shouldBe true
      PhaseMachine.robberMoved(Seq(PerfectInfoPlayKnightResult(PerfectInfoRobberMoveResult(0, 5, None)))) shouldBe true

    it("detects dev card plays in the move log"):
      PhaseMachine.hasPlayedDevCardThisTurn(Nil) shouldBe false
      PhaseMachine.hasPlayedDevCardThisTurn(Seq(PlayMonopolyMoveResult(0, WHEAT, Map.empty))) shouldBe true
      PhaseMachine.hasPlayedDevCardThisTurn(Seq(PlayRoadBuilderMove(0, Edge(Vertex(1), Vertex(2)), None))) shouldBe true

    it("derives the active player during setup in 1-2-3-4-4-3-2-1 order"):
      PhaseMachine.setupActivePlayer(Nil, 4) shouldBe 0
      PhaseMachine.setupActivePlayer(List(0), 4) shouldBe 1
      PhaseMachine.setupActivePlayer(List(0, 1, 2), 4) shouldBe 3
      PhaseMachine.setupActivePlayer(List(0, 1, 2, 3), 4) shouldBe 3
      PhaseMachine.setupActivePlayer(List(0, 1, 2, 3, 3, 2, 1), 4) shouldBe 0

    it("derives the active player from the turn counter after setup"):
      val state = afterSetupPerfect.copy(turn = Turn(2))
      PhaseMachine.activePlayer(state) shouldBe 2

    it("inSetup reports true only during initial placement"):
      PhaseMachine.inSetup(initPerfect) shouldBe true
      PhaseMachine.inSetup(afterSetupPerfect) shouldBe false

    it("numPlayers is derived from playerPoints"):
      PhaseMachine.numPlayers(initPerfect) shouldBe 4

    it("playersWithTooManyCards reports players above the 7-card threshold"):
      val state = afterSetupPerfect.copy(
        privateInventories = PrivateInventories(Map(1 -> ResourceSet(wo = 8), 2 -> ResourceSet(wo = 7)))
      )
      PhaseMachine.playersWithTooManyCards(state) shouldBe Set(1)
      PhaseMachine.stillNeedDiscards(state) shouldBe true

  describe("isTerminal and winners"):

    it("isTerminal is false at 0 VP"):
      PhaseMachine.isTerminal(afterSetupPerfect) shouldBe false

    it("isTerminal is true at exactly 10 VP"):
      val state = afterSetupPerfect.copy(playerPoints = PlayerPoints(Map(0 -> 10)))
      PhaseMachine.isTerminal(state) shouldBe true

    it("isTerminal counts unplayed point cards in perfect info"):
      val state = afterSetupPerfect.copy(
        playerPoints = PlayerPoints(Map(0 -> 9)),
        privateDevCardInv = PrivateDevCardInv(Map(0 -> Seq((POINT, 0))))
      )
      PhaseMachine.isTerminal(state) shouldBe true
      PhaseMachine.winners(state) shouldBe Some(Set(0))

    it("winners is None while the game is ongoing"):
      PhaseMachine.winners(afterSetupPerfect) shouldBe None

    it("winners returns all players at 10+ VP"):
      val state = afterSetupPerfect.copy(playerPoints = PlayerPoints(Map(0 -> 10, 1 -> 12)))
      PhaseMachine.winners(state) shouldBe Some(Set(0, 1))
