package soc.base

import game.InventorySet
import org.scalatest.{FunSpec, Matchers}
import soc.base.BaseGameFixtures.imperfectInfoGame
import soc.base.state.stats.RollDiceStats
import soc.core.{PlayerMap, Resource}
import soc.core.state._

import scala.language.postfixOps

class BaseGameSpec extends FunSpec with Matchers {
  import BaseGameFixtures._

  describe("PerfectInfoGame") {
    it("should initialize correctly") {
      perfectInfoGame.initPerfectInfoState should not be null
    }

    it("should apply all test moves without error") {
      perfectInfoGame.perfectResult should not be null
    }

    it("should track player points correctly") {
      val points = perfectInfoGame.perfectResult.select[PlayerPoints]
      points.points should not be empty
    }

    it("should advance turn counter") {
      val turn = perfectInfoGame.perfectResult.select[Turn]
      turn.t should be > 0
    }

    it("should have valid bank state") {
      val bank = perfectInfoGame.perfectResult.select[Bank[Resource]]
      bank.b should not be null
    }
  }

  describe("PublicInfoGame") {
    it("should initialize correctly") {
      imperfectInfoGame.initPublicInfoState should not be null
    }

    it("should apply all test moves without error") {
      imperfectInfoGame.publicResult should not be null
    }

    it("should track player points correctly") {
      val points = imperfectInfoGame.publicResult.select[PlayerPoints]
      points.points should not be empty
    }

    it("should advance turn counter") {
      val turn = imperfectInfoGame.publicResult.select[Turn]
      turn.t should be > 0
    }

    it("should have valid bank state") {
      val bank = imperfectInfoGame.publicResult.select[Bank[Resource]]
      bank.b should not be null
    }
  }
}
