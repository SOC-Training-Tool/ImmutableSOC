package soc.rules
package validators

import soc.base.*
import soc.base.state.*
import soc.core.*
import soc.core.state.*
import soc.rules.*
import soc.rules.CachedBoard

object TradeValidator:

  case class PortTradeRanges(
    ratios: Seq[(Int, Port)],
    giveOptions: Seq[Resource],
    getOptions: Seq[Resource]
  )

  case class TradeRanges(
    partners: Seq[Int],
    giveResources: Seq[Resource],
    getResources: Seq[Resource],
    maxGiveAmounts: Map[Resource, Int]
  )

  def availablePorts(
    player: Int,
    vertexState: VertexBuildingState[BaseVertexBuilding],
    cached: CachedBoard[Resource]
  ): Seq[Port] =
    val playerVertices = vertexState.map.collect { case (v, pb) if pb.player == player => v }
    val incidentEdges = playerVertices.flatMap(v => cached.edgesFromVertex.getOrElse(v, Nil)).toSet
    cached.portEdges.collect { case (e, p) if incidentEdges(e) => p }.toSeq

  def portTradeParams(
    player: Int,
    vertexState: VertexBuildingState[BaseVertexBuilding],
    cached: CachedBoard[Resource]
  ): PortTradeRanges =
    val specificRatios = availablePorts(player, vertexState, cached).flatMap {
      case Misc        => Seq((3, Misc))
      case r: Resource => Seq((2, r))
    }
    PortTradeRanges(specificRatios :+ (4, Misc), Resources.all, Resources.all)

  def isLegalPortTrade(
    player: Int,
    inv: CachedBoard.ResourceView,
    vertexState: VertexBuildingState[BaseVertexBuilding],
    cached: CachedBoard[Resource],
    move: PortTradeMove[Resource]
  ): Boolean =
    val giveCount = move.give.getTotal
    val getCount  = move.get.getTotal
    giveCount >= 2 &&
      getCount == 1 &&
      move.give.getTypeCount == 1 &&
      inv.hasEnough(player, move.give) &&
      ratioAvailable(player, move.give.getTypes.headOption, giveCount, vertexState, cached)

  private def ratioAvailable(
    player: Int,
    givenResource: Option[Resource],
    giveCount: Int,
    vertexState: VertexBuildingState[BaseVertexBuilding],
    cached: CachedBoard[Resource]
  ): Boolean =
    givenResource match
      case None => false
      case Some(g) =>
        val ports = availablePorts(player, vertexState, cached)
        giveCount match
          case 2 => ports.contains(g)
          case 3 => ports.contains(Misc)
          case _ => giveCount == 4

  def tradeParams(player: Int, allPlayers: Seq[Int], inv: CachedBoard.ResourceView): TradeRanges =
    val partners = allPlayers.filter(p => p != player && inv.getTotal(p) > 0)
    val maxGiveAmounts = Resources.all.map(r => r -> inv.resourceAmount(player, r)).toMap
    TradeRanges(partners, Resources.all, Resources.all, maxGiveAmounts)

  def isLegalTrade(player: Int, inv: CachedBoard.ResourceView, move: TradeMove[Resource]): Boolean =
    move.partner != player &&
      move.give.getTotal > 0 &&
      move.get.getTotal > 0 &&
      inv.hasEnough(player, move.give) &&
      inv.hasEnough(move.partner, move.get)
