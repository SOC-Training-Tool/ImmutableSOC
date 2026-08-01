package soc.rules

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import soc.base.*
import soc.base.BaseGame.*
import soc.base.BaseGameFixtures
import soc.base.state.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.ResourceSet.*
import soc.core.Resources.*
import soc.core.state.*
import soc.rules.PhaseMachine.TurnPhase
import soc.rules.RulesFixtures.*

class IntegrationSpec extends AnyFunSpec with Matchers:

  describe("PerfectInfoLegalMoves"):

    it("generates every initial placement of the standard setup"):
      var state = initPerfect
      var turnMoves: List[PerfectInfoMove] = Nil
      setupMoves.foreach { m =>
        val generated = PerfectInfoLegalMoves.legalMoves(state, m.move.player, turnMoves)
        generated should contain (m)
        generated.forall(mm => PerfectInfoLegalMoves.isLegal(state, m.move.player, turnMoves, mm)) shouldBe true
        val next = perfectInfoGame.applyMoveAny(m, state)._2
        state = next
        turnMoves = turnMoves :+ m
      }

    it("remains self-consistent across the whole perfect-info fixture replay"):
      var state = initPerfect
      var turnMoves: List[PerfectInfoMove] = Nil
      BaseGameFixtures.perfectInfoFixture.testMoveResults.foreach { m =>
        val player = m.move.player
        val generated = PerfectInfoLegalMoves.legalMoves(state, player, turnMoves)
        generated.forall(mm => PerfectInfoLegalMoves.isLegal(state, player, turnMoves, mm)) shouldBe true
        val next = perfectInfoGame.applyMoveAny(m, state)._2
        state = next
        turnMoves = turnMoves :+ m
        if m.isInstanceOf[EndTurnMove] then turnMoves = Nil
      }

    it("enters MainPlay after the dice are rolled and exposes EndTurnMove"):
      val state = afterPerfect((setupMoves ++ List(RollDiceMoveResult(0, 5))): _*)
      val turnMoves = setupMoves :+ RollDiceMoveResult(0, 5)
      PhaseMachine.phase(state, turnMoves) shouldBe TurnPhase.MainPlay(false)
      val generated = PerfectInfoLegalMoves.legalMoves(state, 0, turnMoves)
      generated should contain (EndTurnMove(0))

    it("groups legal moves by runtime class"):
      val state = afterPerfect((setupMoves ++ List(RollDiceMoveResult(0, 5))): _*)
      val turnMoves = setupMoves :+ RollDiceMoveResult(0, 5)
      val flat = PerfectInfoLegalMoves.legalMoves(state, 0, turnMoves)
      val grouped = PerfectInfoLegalMoves.legalMovesGrouped(state, 0, turnMoves)
      grouped.values.flatten.toSeq should contain theSameElementsAs flat
      grouped.keys.foreach(k => k shouldBe a[Class[?]])

    it("validates a legal build-settlement move via isLegal"):
      val state = afterSetupPerfect.copy(
        privateInventories = PrivateInventories(Map(0 -> ResourceSet(wo = 1, br = 1, wh = 1, sh = 1))),
        edgeBuildingState = EdgeBuildingState(Map(
          Edge(Vertex(17), Vertex(40)) -> PlayerBuilding(0, Road)
        ))
      )
      val turnMoves = setupMoves :+ RollDiceMoveResult(0, 5)
      val move = BuildSettlementMove(0, Vertex(17))
      PerfectInfoLegalMoves.isLegal(state, 0, turnMoves, move) shouldBe true

    it("detects terminal states through isTerminal and winners"):
      val state = afterSetupPerfect.copy(playerPoints = PlayerPoints(Map(0 -> 10, 1 -> 4, 2 -> 4, 3 -> 4)))
      PerfectInfoLegalMoves.isTerminal(state) shouldBe true
      PerfectInfoLegalMoves.winners(state) shouldBe Some(Set(0))

    it("exposes lazy trade parameter ranges"):
      val state = afterSetupPerfect.copy(
        privateInventories = PrivateInventories(Map(0 -> ResourceSet(wo = 3, br = 1))),
        vertexBuildingState = VertexBuildingState(Map(Vertex(0) -> PlayerBuilding(0, Settlement)))
      )
      val port = PerfectInfoLegalMoves.portTradeParams(state, 0)
      port.ratios should contain (4 -> Misc)
      val trades = PerfectInfoLegalMoves.tradeParams(state, 0)
      trades.partners should not contain (0)

  describe("PublicInfoLegalMoves"):

    it("remains self-consistent across the whole public-info fixture replay"):
      var state = initPublic
      var turnMoves: List[PublicInfoMove] = Nil
      BaseGameFixtures.publicInfoFixture.testMoveResults.foreach { m =>
        val player = m.move.player
        val generated = PublicInfoLegalMoves.legalMoves(state, player, turnMoves)
        generated.forall(mm => PublicInfoLegalMoves.isLegal(state, player, turnMoves, mm)) shouldBe true
        val next = publicInfoGame.applyMoveAny(m, state)._2
        state = next
        turnMoves = turnMoves :+ m
        if m.isInstanceOf[EndTurnMove] then turnMoves = Nil
      }

    it("generates initial placements and roll/end-turn flow in public mode"):
      var state = initPublic
      var turnMoves: List[PublicInfoMove] = Nil
      setupMoves.foreach { m =>
        val publicMove = m.asInstanceOf[PublicInfoMove]
        val generated = PublicInfoLegalMoves.legalMoves(state, m.move.player, turnMoves)
        generated should contain (publicMove)
        val next = publicInfoGame.applyMoveAny(publicMove, state)._2
        state = next
        turnMoves = turnMoves :+ publicMove
      }
      val roll = RollDiceMoveResult(0, 5)
      PublicInfoLegalMoves.isLegal(state, 0, turnMoves, roll) shouldBe true

    it("generates discard moves for every player with more than 7 cards"):
      val setupPublic: List[PublicInfoMove] = setupMoves.asInstanceOf[List[PublicInfoMove]]
      val state = afterPublic(setupPublic*).copy(
        publicInventories = PublicInventories(Map(0 -> 3, 1 -> 9, 2 -> 10))
      )
      val turnMoves = setupPublic :+ RollDiceMoveResult(0, 7)
      val for1 = PublicInfoLegalMoves.legalMoves(state, 1, turnMoves)
      val for2 = PublicInfoLegalMoves.legalMoves(state, 2, turnMoves)
      for1 should not be empty
      for2 should not be empty
      for1.forall(_.isInstanceOf[DiscardMove[Resource]]) shouldBe true
      for2.forall(_.isInstanceOf[DiscardMove[Resource]]) shouldBe true

    it("isTerminal matches the public player-points view"):
      val state = initPublic.copy(playerPoints = PlayerPoints(Map(0 -> 11)))
      PublicInfoLegalMoves.isTerminal(state) shouldBe true
      PublicInfoLegalMoves.winners(state) shouldBe Some(Set(0))

  describe("phase gating through the public API"):

    it("rejects moves that belong to a different phase"):
      val state = afterSetupPerfect
      val turnMoves = setupMoves :+ RollDiceMoveResult(0, 7)
      PhaseMachine.phase(state, turnMoves) shouldBe TurnPhase.RobberPhase(0)
      PerfectInfoLegalMoves.isLegal(state, 0, turnMoves, EndTurnMove(0)) shouldBe false
      PerfectInfoLegalMoves.isLegal(state, 0, turnMoves, RollDiceMoveResult(0, 8)) shouldBe false

    it("returns no moves for a player who is not the active player"):
      val state = afterPerfect((setupMoves ++ List(RollDiceMoveResult(0, 5))): _*)
      val turnMoves = setupMoves :+ RollDiceMoveResult(0, 5)
      PerfectInfoLegalMoves.legalMoves(state, 1, turnMoves) shouldBe empty
