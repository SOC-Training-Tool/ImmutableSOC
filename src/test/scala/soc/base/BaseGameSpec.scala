/*
package soc.base

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import soc.base.BaseGame.*
import soc.base.BaseGameFixtures.perfectInfoGame
import soc.base.BaseGameFixtures.imperfectInfoGame
import soc.core.state.*

class BaseGameSpec extends AnyFunSpec with Matchers:

  describe("PerfectInfoGame"):

    it("should initialize correctly"):
      perfectInfoGame.initPerfectInfoState should not be null

    it("should apply all test moves without error"):
      perfectInfoGame.perfectResult should not be null

    it("should accumulate player points from initial placements"):
      val points = perfectInfoGame.perfectResult.playerPoints.points
      points should not be empty

    it("should advance turn counter"):
      perfectInfoGame.perfectResult.turn.t should be > 0

    it("should have valid bank state"):
      perfectInfoGame.perfectResult.bank should not be null

  describe("PublicInfoGame"):

    it("should initialize correctly"):
      imperfectInfoGame.initPublicInfoState should not be null

    it("should apply all test moves without error"):
      imperfectInfoGame.publicResult should not be null

    it("should accumulate player points from initial placements"):
      val points = imperfectInfoGame.publicResult.playerPoints.points
      points should not be empty

    it("should advance turn counter"):
      imperfectInfoGame.publicResult.turn.t should be > 0

    it("should have valid bank state"):
      imperfectInfoGame.publicResult.bank should not be null
*/
