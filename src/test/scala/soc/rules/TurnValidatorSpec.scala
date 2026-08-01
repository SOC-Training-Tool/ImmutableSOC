package soc.rules

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import soc.base.BaseGame.*
import soc.base.state.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.ResourceSet.*
import soc.core.Resources.*
import soc.core.state.*
import soc.rules.PhaseMachine.TurnPhase
import soc.rules.RulesFixtures.*
import soc.rules.validators.TurnValidator

class TurnValidatorSpec extends AnyFunSpec with Matchers:

  private def invFor(player: Int, resources: Resources): PerfectInfoResourceView =
    new PerfectInfoResourceView(afterSetupPerfect.copy(
      privateInventories = PrivateInventories(Map(player -> resources))
    ))

  describe("endTurnMoves"):

    it("returns EndTurnMove in MainPlay after the dice have been rolled"):
      TurnValidator.endTurnMoves(0, Seq(RollDiceMoveResult(0, 5)), TurnPhase.MainPlay(false)) shouldBe Seq(EndTurnMove(0))

    it("rejects EndTurnMove before the dice have been rolled"):
      TurnValidator.endTurnMoves(0, Nil, TurnPhase.PreRoll) shouldBe empty

    it("rejects EndTurnMove during discard and robber phases"):
      TurnValidator.endTurnMoves(0, Seq(RollDiceMoveResult(0, 7)), TurnPhase.DiscardPhase(Set(1))) shouldBe empty
      TurnValidator.endTurnMoves(0, Seq(RollDiceMoveResult(0, 7)), TurnPhase.RobberPhase(0)) shouldBe empty

  describe("discardMoves"):

    it("returns no discards for players at or below 7 cards"):
      TurnValidator.discardMoves(0, invFor(0, ResourceSet(wo = 7))) shouldBe empty
      TurnValidator.discardMoves(0, invFor(0, ResourceSet(wo = 4, br = 3))) shouldBe empty

    it("requires exactly floor(total/2) cards"):
      val moves = TurnValidator.discardMoves(0, invFor(0, ResourceSet(wo = 5, br = 5)))
      moves should not be empty
      moves.forall(_.set.getTotal == 5) shouldBe true

    def discardMovesFor(state: PerfectInfoState, player: Int): Seq[DiscardMove[Resource]] =
      TurnValidator.discardMoves(player, new PerfectInfoResourceView(state))

    it("enumerates valid combinations bounded by the player's hand"):
      val state = afterSetupPerfect.copy(
        privateInventories = PrivateInventories(Map(0 -> ResourceSet(wo = 4, br = 4)))
      )
      val moves = discardMovesFor(state, 0)
      moves.map(_.set.amountMap).toSet should contain (Map(Wood -> 4))
      moves.map(_.set.amountMap).toSet should contain (Map(Wood -> 3, Brick -> 1))
      moves.map(_.set.amountMap).toSet should contain (Map(Brick -> 4))

    it("supports any affected player discarding first"):
      val state = afterSetupPerfect.copy(
        privateInventories = PrivateInventories(Map(1 -> ResourceSet(wo = 12)))
      )
      discardMovesFor(state, 1) should not be empty
