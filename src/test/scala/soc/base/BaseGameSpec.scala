package soc.base

import game.InventorySet
import org.scalatest.{FunSpec, Matchers}
import soc.base.BaseGameFixtures.imperfectInfoGame
import soc.base.state.stats.RollDiceStats
import soc.core.{PlayerMap, Resource}
import soc.core.state._

import scala.language.postfixOps

class BaseGameSpec extends FunSpec with Matchers {

  println(imperfectInfoGame.publicResult.select[PlayerPoints])

  val initState = imperfectInfoGame.initPublicInfoState

//  val statsOverGame: Seq[(Int, RollDiceStats[Resource], PlayerMap[Int])] = imperfectInfoGame.testMoveResults
//    .foldLeft((initState, List.empty[(Int, RollDiceStats[Resource], PlayerMap[Int])])) { case ((state, stats), m) =>
//      val newState  = BaseGame.PublicInfoGame.game.applyMove(m, state)
//      val turn      = newState.select[Turn].t
//      val gameStats = newState.select[RollDiceStats[Resource]]
//      val points    = newState.select[PlayerPoints].points
//      (newState, stats :+ (turn, gameStats, points))
//    }
//    ._2
//    .distinctBy(_._1)

//  def getCollectionStatsForPlayer(player: Int): (List[Int], List[Double], List[Int]) = {
//    val zipped = statsOverGame
//      .map { case (turn, stats, points) =>
//        val gained = stats.gained.getOrElse(player, InventorySet.empty[Resource, Int])
//        val ev     = stats.expectedGains.getOrElse(player, InventorySet.empty[Resource, Double])
//        val point = points.getOrElse(player, 0)
//        turn -> (gained.getTotal, ev.getTotal, point)
//      }
//      .toList
//      .sortBy(_._1)
//      .map(_._2)
//    (zipped.map(_._1), zipped.map(_._2), zipped.map(_._3))
//
//  }

//  def printGameCollection(player: Int) = {
//    val (g, e, _) = getCollectionStatsForPlayer(player)
//    //println(player)
//    println(g.mkString(player.toString + ",", ",", ""))
//    println(e.mkString(player.toString + ",", ",", ""))
//    //println(p.mkString(player.toString + ",", ",", ""))
//  }
//
//  println((0 until (statsOverGame.length - 1)).mkString(","))
//  printGameCollection(0)
//  printGameCollection(1)
//  printGameCollection(2)
//  printGameCollection(3)

//
//  val diceRollStats = perfectResult.select[RollDiceStats[Resource]]
//  val gains = diceRollStats.gained.view.mapValues(_.getTotal).toMap
//  val ev = diceRollStats.expectedGains.view.mapValues(_.getTotal).toMap
//  println((gains, ev))
}
