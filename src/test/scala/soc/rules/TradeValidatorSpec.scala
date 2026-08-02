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
import soc.rules.RulesFixtures.*
import soc.rules.validators.TradeValidator

class TradeValidatorSpec extends AnyFunSpec with Matchers:

  private val board = BaseGameFixtures.perfectInfoFixture.board

  describe("port trade ranges"):

    it("always includes the 4:1 domestic trade"):
      val params = TradeValidator.portTradeParams(0, initPerfect.vertexBuildingState, board)
      params.ratios should contain (4 -> Misc)

    it("exposes 2:1 ratios only for ports the player has settled"):
      val state = initPerfect.copy(
        vertexBuildingState = VertexBuildingState(Map(Vertex(3) -> PlayerBuilding(0, Settlement)))
      )
      val params = TradeValidator.portTradeParams(0, state.vertexBuildingState, board)
      params.ratios should contain (2 -> Ore)
      params.ratios should not contain (2 -> Wood)

    it("exposes 3:1 ratios for misc ports"):
      val state = initPerfect.copy(
        vertexBuildingState = VertexBuildingState(Map(Vertex(7) -> PlayerBuilding(0, Settlement)))
      )
      val params = TradeValidator.portTradeParams(0, state.vertexBuildingState, board)
      params.ratios should contain (3 -> Misc)

  describe("isLegalPortTrade"):

    it("accepts a 2:1 trade on a settled resource port"):
      val state = initPerfect.copy(
        privateInventories = PrivateInventories(Map(0 -> ResourceSet(or = 2))),
        vertexBuildingState = VertexBuildingState(Map(Vertex(3) -> PlayerBuilding(0, Settlement)))
      )
      val inv = new PerfectInfoResourceView(state)
      val move = PortTradeMove[Resource](0, ResourceSet(or = 2), ResourceSet(wo = 1))
      TradeValidator.isLegalPortTrade(0, inv, state.vertexBuildingState, board, move) shouldBe true

    it("rejects a 2:1 trade without the matching port"):
      val state = initPerfect.copy(
        privateInventories = PrivateInventories(Map(0 -> ResourceSet(or = 2)))
      )
      val inv = new PerfectInfoResourceView(state)
      val move = PortTradeMove[Resource](0, ResourceSet(or = 2), ResourceSet(wo = 1))
      TradeValidator.isLegalPortTrade(0, inv, state.vertexBuildingState, board, move) shouldBe false

    it("accepts a 4:1 trade without any port"):
      val state = initPerfect.copy(
        privateInventories = PrivateInventories(Map(0 -> ResourceSet(wo = 4)))
      )
      val inv = new PerfectInfoResourceView(state)
      val move = PortTradeMove[Resource](0, ResourceSet(wo = 4), ResourceSet(or = 1))
      TradeValidator.isLegalPortTrade(0, inv, state.vertexBuildingState, board, move) shouldBe true

    it("rejects a trade the player cannot afford"):
      val state = initPerfect.copy(
        privateInventories = PrivateInventories(Map(0 -> ResourceSet(wo = 3)))
      )
      val inv = new PerfectInfoResourceView(state)
      val move = PortTradeMove[Resource](0, ResourceSet(wo = 4), ResourceSet(or = 1))
      TradeValidator.isLegalPortTrade(0, inv, state.vertexBuildingState, board, move) shouldBe false

  describe("player trade ranges"):

    it("excludes self and empty-hand partners"):
      val state = initPerfect.copy(
        privateInventories = PrivateInventories(Map(0 -> ResourceSet(wo = 2), 1 -> ResourceSet(sh = 1)))
      )
      val inv = new PerfectInfoResourceView(state)
      val params = TradeValidator.tradeParams(0, Seq(0, 1, 2, 3), inv)
      params.partners should not contain (0)
      params.partners should contain (1)

    it("caps maxGiveAmounts by the player's inventory"):
      val state = initPerfect.copy(
        privateInventories = PrivateInventories(Map(0 -> ResourceSet(wo = 3, br = 1)))
      )
      val inv = new PerfectInfoResourceView(state)
      val params = TradeValidator.tradeParams(0, Seq(0, 1), inv)
      params.maxGiveAmounts(Wood) shouldBe 3
      params.maxGiveAmounts(Brick) shouldBe 1
      params.maxGiveAmounts(Ore) shouldBe 0

  describe("isLegalTrade"):

    it("accepts a balanced trade both players can afford"):
      val state = initPerfect.copy(
        privateInventories = PrivateInventories(Map(0 -> ResourceSet(wo = 1), 1 -> ResourceSet(br = 1)))
      )
      val inv = new PerfectInfoResourceView(state)
      val move = TradeMove[Resource](0, 1, ResourceSet(wo = 1), ResourceSet(br = 1))
      TradeValidator.isLegalTrade(0, inv, move) shouldBe true

    it("rejects trading with yourself"):
      val state = initPerfect.copy(
        privateInventories = PrivateInventories(Map(0 -> ResourceSet(wo = 1)))
      )
      val inv = new PerfectInfoResourceView(state)
      val move = TradeMove[Resource](0, 0, ResourceSet(wo = 1), ResourceSet(wo = 1))
      TradeValidator.isLegalTrade(0, inv, move) shouldBe false

    it("rejects a trade the partner cannot afford"):
      val state = initPerfect.copy(
        privateInventories = PrivateInventories(Map(0 -> ResourceSet(wo = 1), 1 -> ResourceSet(br = 1)))
      )
      val inv = new PerfectInfoResourceView(state)
      val move = TradeMove[Resource](0, 1, ResourceSet(wo = 1), ResourceSet(or = 1))
      TradeValidator.isLegalTrade(0, inv, move) shouldBe false
