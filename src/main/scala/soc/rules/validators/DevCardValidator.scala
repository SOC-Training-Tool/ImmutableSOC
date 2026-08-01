package soc.rules
package validators

import soc.base.*
import soc.base.DevelopmentCards.*
import soc.base.state.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.state.*
import soc.rules.*
import soc.rules.CachedBoard
import RobberValidator.*

object DevCardValidator:

  def canBuy(player: Int, inv: CachedBoard.ResourceView, devView: CachedBoard.DevCardView): Boolean =
    inv.hasEnough(player, CachedBoard.DEV_CARD_COST) && devView.deckNonEmpty

  def canPlay(
    player: Int,
    turnMoves: Seq[Any],
    devView: CachedBoard.DevCardView,
    card: DevelopmentCard,
    currentTurn: Int
  ): Boolean =
    devView.hasUnexpiredCard(player, card, currentTurn) &&
      !PhaseMachine.hasPlayedDevCardThisTurn(turnMoves)

  def buyMoves[Card](
    player: Int,
    inv: CachedBoard.ResourceView,
    devView: CachedBoard.DevCardView,
    topCard: Option[Card]
  ): Seq[BuyDevelopmentCardMoveResult[Card]] =
    if canBuy(player, inv, devView) then topCard.toSeq.map(c => BuyDevelopmentCardMoveResult(player, Some(c)))
    else Nil

  def perfectBuyMoves[Card](
    player: Int,
    inv: CachedBoard.ResourceView,
    devView: CachedBoard.DevCardView,
    topCard: Option[Card]
  ): Seq[PerfectInfoBuyDevelopmentCardMoveResult[Card]] =
    if canBuy(player, inv, devView) then topCard.toSeq.map(c => PerfectInfoBuyDevelopmentCardMoveResult(player, c))
    else Nil

  def playKnightMoves(
    player: Int,
    turnMoves: Seq[Any],
    devView: CachedBoard.DevCardView,
    currentTurn: Int,
    robberLocation: RobberLocation,
    cached: CachedBoard[Resource],
    vertexState: VertexBuildingState[BaseVertexBuilding],
    inv: CachedBoard.ResourceView
  ): Seq[PlayKnightResult[Resource]] =
    if !canPlay(player, turnMoves, devView, KNIGHT, currentTurn) then Nil
    else
      RobberValidator.placements(player, robberLocation, cached, vertexState, inv).flatMap {
        case RobberPlacement(hex, victims) if victims.nonEmpty =>
          victims.map(v => PlayKnightResult(RobberMoveResult[Resource](player, hex, Some(PlayerSteal[Option[Resource]](v, None)))))
        case RobberPlacement(hex, _) =>
          Seq(PlayKnightResult(RobberMoveResult[Resource](player, hex, None)))
      }

  def perfectPlayKnightMoves(
    player: Int,
    turnMoves: Seq[Any],
    devView: CachedBoard.DevCardView,
    currentTurn: Int,
    robberLocation: RobberLocation,
    cached: CachedBoard[Resource],
    vertexState: VertexBuildingState[BaseVertexBuilding],
    inv: CachedBoard.ResourceView,
    stealResource: Int => Option[Resource]
  ): Seq[PerfectInfoPlayKnightResult[Resource]] =
    if !canPlay(player, turnMoves, devView, KNIGHT, currentTurn) then Nil
    else
      RobberValidator.placements(player, robberLocation, cached, vertexState, inv).flatMap {
        case RobberPlacement(hex, victims) if victims.nonEmpty =>
          victims.map { v =>
            val stolen = stealResource(v).map(res => PlayerSteal[Resource](v, res))
            PerfectInfoPlayKnightResult(PerfectInfoRobberMoveResult[Resource](player, hex, stolen))
          }
        case RobberPlacement(hex, _) =>
          Seq(PerfectInfoPlayKnightResult(PerfectInfoRobberMoveResult[Resource](player, hex, None)))
      }

  def playMonopolyMoves(
    player: Int,
    turnMoves: Seq[Any],
    devView: CachedBoard.DevCardView,
    currentTurn: Int
  ): Seq[PlayMonopolyMoveResult[Resource]] =
    if !canPlay(player, turnMoves, devView, MONOPOLY, currentTurn) then Nil
    else Resources.all.map(r => PlayMonopolyMoveResult(player, r, Map.empty))

  def perfectPlayMonopolyMoves(
    player: Int,
    turnMoves: Seq[Any],
    devView: CachedBoard.DevCardView,
    currentTurn: Int,
    privateInventories: PrivateInventories[Resource]
  ): Seq[PlayMonopolyMoveResult[Resource]] =
    if !canPlay(player, turnMoves, devView, MONOPOLY, currentTurn) then Nil
    else
      Resources.all.map { r =>
        val loss = privateInventories.m.collect {
          case (p, inv) if p != player => p -> inv.getAmount(r)
        }.filter(_._2 > 0).toMap
        PlayMonopolyMoveResult(player, r, loss)
      }

  def playYearOfPlentyMoves(
    player: Int,
    turnMoves: Seq[Any],
    devView: CachedBoard.DevCardView,
    currentTurn: Int
  ): Seq[PlayYearOfPlentyMove[Resource]] =
    if !canPlay(player, turnMoves, devView, YEAR_OF_PLENTY, currentTurn) then Nil
    else for r1 <- Resources.all; r2 <- Resources.all yield PlayYearOfPlentyMove(player, r1, r2)

  def playRoadBuilderMoves(
    player: Int,
    turnMoves: Seq[Any],
    devView: CachedBoard.DevCardView,
    currentTurn: Int,
    edgeState: EdgeBuildingState[BaseEdgeBuilding],
    vertexState: VertexBuildingState[BaseVertexBuilding],
    cached: CachedBoard[Resource]
  ): Seq[PlayRoadBuilderMove] =
    if !canPlay(player, turnMoves, devView, ROAD_BUILDER, currentTurn) then Nil
    else
      val firstEdges = BuildingValidator.roadPlacementEdges(player, edgeState, vertexState, cached)
        .filter(_ => BuildingValidator.roadCount(player, edgeState) < 15)
      firstEdges.flatMap { e1 =>
        val withFirst = EdgeBuildingState(edgeState.map + (e1 -> PlayerBuilding(player, Road)))
        val single = Seq(PlayRoadBuilderMove(player, e1, None))
        val secondEdges = BuildingValidator.roadPlacementEdges(player, withFirst, vertexState, cached)
          .filter(_ => BuildingValidator.roadCount(player, edgeState) < 14)
        single ++ secondEdges.map(e2 => PlayRoadBuilderMove(player, e1, Some(e2)))
      }
