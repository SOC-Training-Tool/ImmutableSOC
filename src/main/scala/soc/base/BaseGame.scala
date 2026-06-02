package soc.base

import scala.reflect.ClassTag
import game.{Delta, GameAction, GameState, ImmutableGame, InventorySet, Slice}
import soc.base.DevelopmentCards.*
import soc.base.actions.special.updatedSpecialPlayer
import soc.base.state.*
import soc.core.*
import soc.core.DevTransactions.*
import soc.core.ResourceInventories.*
import soc.core.ResourceSet.*
import soc.core.Resources.*
import soc.core.SOCBoard.SOCBoardOps
import soc.core.Transactions.*
import soc.core.state.*

object BaseGame:

  type BaseVertexBuilding = City.type | Settlement.type
  type BaseEdgeBuilding   = Road.type

  // ─── Delta union types ─────────────────────────────────────────────────────

  type SharedDelta =
    Delta[Bank[Resource]] | Delta[PlayerPoints] | Delta[Turn] | Delta[MoveCount] |
    Delta[RobberLocation] | Delta[SOCLongestRoadPlayer] | Delta[SOCRoadLengths] |
    Delta[LargestArmyPlayer] | Delta[PlayerArmyCount] |
    Delta[VertexBuildingState[BaseVertexBuilding]] | Delta[EdgeBuildingState[BaseEdgeBuilding]]

  type PerfectInfoDelta =
    SharedDelta | Delta[PrivateInventories[Resource]] |
    Delta[PrivateDevCardInv[DevelopmentCard]] | Delta[DevelopmentCardDeck[DevelopmentCard]]

  type PublicInfoDelta =
    SharedDelta | Delta[PublicInventories[Resource]] | Delta[PublicDevCardInv[DevelopmentCard]] |
    Delta[DevelopmentCardDeckSize]

  // ─── State case classes ────────────────────────────────────────────────────

  case class PerfectInfoState(
    robberLocation:       RobberLocation,
    privateInventories:   PrivateInventories[Resource],
    privateDevCardInv:    PrivateDevCardInv[DevelopmentCard],
    developmentCardDeck:  DevelopmentCardDeck[DevelopmentCard],
    bank:                 Bank[Resource],
    turn:                 Turn,
    playerPoints:         PlayerPoints,
    largestArmyPlayer:    LargestArmyPlayer,
    playerArmyCount:      PlayerArmyCount,
    vertexBuildingState:  VertexBuildingState[BaseVertexBuilding],
    socRoadLengths:       SOCRoadLengths,
    socLongestRoadPlayer: SOCLongestRoadPlayer,
    board:                BaseBoard[Resource],
    edgeBuildingState:    EdgeBuildingState[BaseEdgeBuilding],
    moveCount:            MoveCount
  )

  case class PublicInfoState(
    robberLocation:          RobberLocation,
    publicInventories:       PublicInventories[Resource],
    publicDevCardInv:        PublicDevCardInv[DevelopmentCard],
    developmentCardDeckSize: DevelopmentCardDeckSize,
    bank:                    Bank[Resource],
    turn:                    Turn,
    playerPoints:            PlayerPoints,
    largestArmyPlayer:       LargestArmyPlayer,
    playerArmyCount:         PlayerArmyCount,
    vertexBuildingState:     VertexBuildingState[BaseVertexBuilding],
    socRoadLengths:          SOCRoadLengths,
    socLongestRoadPlayer:    SOCLongestRoadPlayer,
    board:                   BaseBoard[Resource],
    edgeBuildingState:       EdgeBuildingState[BaseEdgeBuilding],
    moveCount:               MoveCount
  )

  // ─── Resource costs ────────────────────────────────────────────────────────

  private val SETTLEMENT_COST: Resources = ResourceSet(WOOD, BRICK, WHEAT, SHEEP)
  private val CITY_COST: Resources       = ResourceSet(ORE, ORE, ORE, WHEAT, WHEAT)
  private val ROAD_COST: Resources       = ResourceSet(WOOD, BRICK)
  private val DEV_CARD_COST: Resources   = ResourceSet(ORE, WHEAT, SHEEP)

  // ─── Shared helpers ────────────────────────────────────────────────────────

  private def vertexBuildingResources(b: BaseVertexBuilding): Int = b match
    case _: Settlement.type => 1
    case _: City.type       => 2

  private def specialPlayerDeltas[SP](
    wrap: SpecialPlayer.Delta => SP,
    current: Option[Int],
    updated: Option[Int]
  ): List[SP | Delta[PlayerPoints]] =
    (current, updated) match
      case (None, None)                     => Nil
      case (None, Some(p))                  =>
        wrap(SpecialPlayer.Set(p)) ::
        Delta[PlayerPoints](PlayerPoints.Increment(p)) ::
        Delta[PlayerPoints](PlayerPoints.Increment(p)) :: Nil
      case (Some(o), None)                  =>
        wrap(SpecialPlayer.Remove) ::
        Delta[PlayerPoints](PlayerPoints.Decrement(o)) ::
        Delta[PlayerPoints](PlayerPoints.Decrement(o)) :: Nil
      case (Some(o), Some(n)) if o == n     => Nil
      case (Some(o), Some(n))               =>
        wrap(SpecialPlayer.Remove) ::
        Delta[PlayerPoints](PlayerPoints.Decrement(o)) ::
        Delta[PlayerPoints](PlayerPoints.Decrement(o)) ::
        wrap(SpecialPlayer.Set(n)) ::
        Delta[PlayerPoints](PlayerPoints.Increment(n)) ::
        Delta[PlayerPoints](PlayerPoints.Increment(n)) :: Nil

  // ─── Shared type aliases ───────────────────────────────────────────────────

  private type LongestRoadDelta = (SOCRoadLengths, SOCLongestRoadPlayer, PlayerPoints)
  private type LargestArmyDelta = (PlayerArmyCount, LargestArmyPlayer, PlayerPoints)

  // Fields needed by any action that recalculates longest road
  private type LongestRoadSlice = (
    BaseBoard[Resource],
    SOCRoadLengths,
    SOCLongestRoadPlayer,
    VertexBuildingState[BaseVertexBuilding],
    EdgeBuildingState[BaseEdgeBuilding]
  )

  // ─── Composable special-rule actions ──────────────────────────────────────

  /** Recalculates longest road after any move that places edges or settlements.
   *
   *  The move type M is a phantom — the calculation reads only LongestRoadSlice.
   *  Use `andThen` to compose with any core action sharing the same M.
   */
  private def longestRoadAction[M]
      : GameAction[M, LongestRoadSlice, (SOCRoadLengths, SOCLongestRoadPlayer, PlayerPoints)] =
    GameAction.fromState[M, LongestRoadSlice]((_, s) =>
      val (board, _, socLongestRoadPlayer, vertexBuildingState, edgeBuildingState) = s
      val ops       = new LongestRoadOps(board, edgeBuildingState, vertexBuildingState)
      val updated   = ops.calcLongestRoadLengths()
      val currentLP = socLongestRoadPlayer.player
      val updatedLP = updatedSpecialPlayer(5, currentLP, updated.m)
      val roadDeltas = updated.m.map { case (k, v) => Delta[SOCRoadLengths](SpecialCounts.Set(k, v)) }.toList
      roadDeltas ++ specialPlayerDeltas(d => Delta[SOCLongestRoadPlayer](d), currentLP, updatedLP)
    )

  /** Recalculates largest army after a knight is played.
   *
   *  `getPlayer` extracts the player who played the knight from the move.
   */
  private def largestArmyAction[M](getPlayer: M => Int)
      : GameAction[M, (LargestArmyPlayer, PlayerArmyCount), (PlayerArmyCount, LargestArmyPlayer, PlayerPoints)] =
    GameAction.fromState[M, (LargestArmyPlayer, PlayerArmyCount)]((m, s) =>
      val (largestArmyPlayer, playerArmyCount) = s
      val player        = getPlayer(m)
      val armyDelta     = Delta[PlayerArmyCount](SpecialCounts.Increment(player))
      val updatedCounts = playerArmyCount.m + (player -> (playerArmyCount.m.getOrElse(player, 0) + 1))
      val currentLP     = largestArmyPlayer.player
      val updatedLP     = updatedSpecialPlayer(3, currentLP, updatedCounts)
      armyDelta :: specialPlayerDeltas(d => Delta[LargestArmyPlayer](d), currentLP, updatedLP)
    )

  /** Removes the knight dev card from the player's hand.  Used in playKnight compositions. */
  private def removeKnightCardAction[M, Dev <: GameState[Dev]: ClassTag: DevCardInventory](getPlayer: M => Int)
      : GameAction[M, Turn, Tuple1[Dev]] =
    GameAction.fromState[M, Turn]((m, turn) =>
      List(summon[DevCardInventory[Dev]].playCard(Knight, getPlayer(m), turn.t))
    )

  // ─── Core action factories (no special-rule calculations) ──────────────────

  private def buildSettlementCoreAction[Inv <: GameState[Inv]: ClassTag: ResourceInventory]
      : GameAction[BuildSettlementMove, Unit,
                   (VertexBuildingState[BaseVertexBuilding], Inv, Bank[Resource], PlayerPoints)] =
    GameAction[BuildSettlementMove](m =>
      Delta[VertexBuildingState[BaseVertexBuilding]](BoardBuildingState.add(m.vertex, Settlement, m.player)) ::
      summon[ResourceInventory[Inv]].lose(m.player, SETTLEMENT_COST) ::
      Delta[Bank[Resource]](Bank.Add(SETTLEMENT_COST)) ::
      Delta[PlayerPoints](PlayerPoints.Increment(m.player)) :: Nil
    )

  private def buildRoadCoreAction[Inv <: GameState[Inv]: ClassTag: ResourceInventory]
      : GameAction[BuildRoadMove, Unit,
                   (EdgeBuildingState[BaseEdgeBuilding], Inv, Bank[Resource])] =
    GameAction[BuildRoadMove](m =>
      Delta[EdgeBuildingState[BaseEdgeBuilding]](BoardBuildingState.add(m.edge, Road, m.player)) ::
      summon[ResourceInventory[Inv]].lose(m.player, ROAD_COST) ::
      Delta[Bank[Resource]](Bank.Add(ROAD_COST)) :: Nil
    )

  private def initialPlacementCoreAction[Inv <: GameState[Inv]: ClassTag: ResourceInventory]
      : GameAction[InitialPlacementMove,
                   (PlayerPoints, MoveCount, BaseBoard[Resource],
                    VertexBuildingState[BaseVertexBuilding], EdgeBuildingState[BaseEdgeBuilding]),
                   (VertexBuildingState[BaseVertexBuilding], PlayerPoints, EdgeBuildingState[BaseEdgeBuilding],
                    Inv, Bank[Resource])] =
    GameAction.fromState[InitialPlacementMove,
      (PlayerPoints, MoveCount, BaseBoard[Resource],
       VertexBuildingState[BaseVertexBuilding], EdgeBuildingState[BaseEdgeBuilding])]((m, s) =>
      val (playerPoints, moveCount, board, vertexBuildingState, edgeBuildingState) = s
      val numPlayers    = playerPoints.points.keys.size.max(moveCount.count + 1)
      val isSecondRound = moveCount.count >= numPlayers
      val invDeltas = if isSecondRound then
        val resources = board.hexesForVertex
          .getOrElse(m.vertex, Nil)
          .flatMap(_.hex.getResource)
        val inv = InventorySet.fromList(resources)
        if inv.getTotal == 0 then Nil
        else
          summon[ResourceInventory[Inv]].gain(m.player, inv) ::
          Delta[Bank[Resource]](Bank.Take(inv)) :: Nil
      else Nil
      Delta[VertexBuildingState[BaseVertexBuilding]](BoardBuildingState.add(m.vertex, Settlement, m.player)) ::
      Delta[PlayerPoints](PlayerPoints.Increment(m.player)) ::
      Delta[EdgeBuildingState[BaseEdgeBuilding]](BoardBuildingState.add(m.edge, Road, m.player)) ::
      invDeltas
    )

  private def playRoadBuilderCoreAction[Dev <: GameState[Dev]: ClassTag: DevCardInventory]
      : GameAction[PlayRoadBuilderMove, Turn,
                   (EdgeBuildingState[BaseEdgeBuilding], Dev)] =
    GameAction.fromState[PlayRoadBuilderMove, Turn]((m, turn) =>
      val roads   = List(Some(m.edge1), m.edge2).flatten
      val edgeDlt = roads.map(r => Delta[EdgeBuildingState[BaseEdgeBuilding]](BoardBuildingState.add(r, Road, m.player)))
      val devDelt = summon[DevCardInventory[Dev]].playCard(RoadBuilder, m.player, turn.t)
      edgeDlt :+ devDelt
    )

  // ─── Composed action factories (core + special rules via andThen) ──────────

  private def buildSettlementAction[Inv <: GameState[Inv]: ClassTag: ResourceInventory] =
    buildSettlementCoreAction[Inv].andThen(longestRoadAction[BuildSettlementMove])

  private def buildCityAction[Inv <: GameState[Inv]: ClassTag: ResourceInventory]
      : GameAction[BuildCityMove, Unit,
                   (VertexBuildingState[BaseVertexBuilding], Inv, Bank[Resource], PlayerPoints)] =
    GameAction[BuildCityMove](m =>
      Delta[VertexBuildingState[BaseVertexBuilding]](BoardBuildingState.RemoveBuilding(m.vertex)) ::
      summon[ResourceInventory[Inv]].lose(m.player, CITY_COST) ::
      Delta[Bank[Resource]](Bank.Add(CITY_COST)) ::
      Delta[PlayerPoints](PlayerPoints.Decrement(m.player)) ::
      Delta[VertexBuildingState[BaseVertexBuilding]](BoardBuildingState.add(m.vertex, City, m.player)) ::
      Delta[PlayerPoints](PlayerPoints.Increment(m.player)) ::
      Delta[PlayerPoints](PlayerPoints.Increment(m.player)) :: Nil
    )

  private def buildRoadAction[Inv <: GameState[Inv]: ClassTag: ResourceInventory] =
    buildRoadCoreAction[Inv].andThen(longestRoadAction[BuildRoadMove])

  private def portTradeAction[Inv <: GameState[Inv]: ClassTag: ResourceInventory]
      : GameAction[PortTradeMove[Resource], Unit, (Inv, Bank[Resource])] =
    GameAction[PortTradeMove[Resource]](m =>
      summon[ResourceInventory[Inv]].lose(m.player, m.give) ::
      Delta[Bank[Resource]](Bank.Add(m.give)) ::
      Delta[Bank[Resource]](Bank.Take(m.get)) ::
      summon[ResourceInventory[Inv]].gain(m.player, m.get) :: Nil
    )

  private def discardAction[Inv <: GameState[Inv]: ClassTag: ResourceInventory]
      : GameAction[DiscardMove[Resource], Unit, (Inv, Bank[Resource])] =
    GameAction[DiscardMove[Resource]](m =>
      summon[ResourceInventory[Inv]].lose(m.player, m.set) ::
      Delta[Bank[Resource]](Bank.Add(m.set)) :: Nil
    )

  private def tradeAction[Inv <: GameState[Inv]: ClassTag: ResourceInventory]
      : GameAction[TradeMove[Resource], Unit, Tuple1[Inv]] =
    GameAction[TradeMove[Resource]](m => List(
      summon[ResourceInventory[Inv]].lose(m.player, m.give),
      summon[ResourceInventory[Inv]].lose(m.partner, m.get),
      summon[ResourceInventory[Inv]].gain(m.player, m.get),
      summon[ResourceInventory[Inv]].gain(m.partner, m.give)
    ))

  private def rollDiceAction[Inv <: GameState[Inv]: ClassTag: ResourceInventory]
      : GameAction[RollDiceMoveResult,
                   (RobberLocation, BaseBoard[Resource], VertexBuildingState[BaseVertexBuilding], Bank[Resource]),
                   (Bank[Resource], Inv)] =
    GameAction.fromState[RollDiceMoveResult,
      (RobberLocation, BaseBoard[Resource], VertexBuildingState[BaseVertexBuilding], Bank[Resource])]((m, s) =>
      val (robberLocation, board, vertexBuildingState, bank) = s
      val robberHex = robberLocation.robberHexId
      val (gainedHexes, _) = board.numberHexes
        .getOrElse(m.result, Nil)
        .partition(_.node != robberHex)
      val playerGains = (for
        node     <- gainedHexes
        resource <- node.hex.getResource.toSeq
        vertex   <- node.vertices
        vb       <- vertexBuildingState.map.get(vertex).toSeq
        player    = vb.player
        amt       = vertexBuildingResources(vb.building)
      yield player -> InventorySet.fromMap(Map(resource -> amt)))
        .foldLeft(Map.empty[Int, Resources]) { case (acc, (p, inv)) =>
          acc + (p -> acc.get(p).fold(inv)(_.add(inv)))
        }
      val totalCollected = playerGains.values.foldLeft(InventorySet.empty[Resource, Int])(_.add(_))
      val overflowTypes  = totalCollected.getTypes.filter(r => !bank.b.contains(totalCollected.getAmount(r), r))
      val actual = playerGains.map { case (p, inv) =>
        p -> overflowTypes.foldLeft(inv)((set, r) => set.subtract(set.getAmount(r), r))
      }
      val trueTotal = actual.values.foldLeft(InventorySet.empty[Resource, Int])(_.add(_))
      val gainDeltas = actual.toList.flatMap { case (p, inv) =>
        if inv.getTotal == 0 then Nil
        else List(summon[ResourceInventory[Inv]].gain(p, inv))
      }
      if trueTotal.getTotal > 0 then Delta[Bank[Resource]](Bank.Take(trueTotal)) :: gainDeltas
      else gainDeltas
    )

  private def initialPlacementAction[Inv <: GameState[Inv]: ClassTag: ResourceInventory] =
    initialPlacementCoreAction[Inv].andThen(longestRoadAction[InitialPlacementMove])

  private def playYearOfPlentyAction[Inv <: GameState[Inv]: ClassTag: ResourceInventory,
                                     Dev <: GameState[Dev]: ClassTag: DevCardInventory]
      : GameAction[PlayYearOfPlentyMove[Resource], Turn, (Bank[Resource], Inv, Dev)] =
    GameAction.fromState[PlayYearOfPlentyMove[Resource], Turn]((m, turn) =>
      val inv = InventorySet.fromList(List(m.c1, m.c2))
      List(
        Delta[Bank[Resource]](Bank.Take(inv)),
        summon[ResourceInventory[Inv]].gain(m.player, inv),
        summon[DevCardInventory[Dev]].playCard(YearOfPlenty, m.player, turn.t)
      )
    )

  private def playMonopolyAction[Inv <: GameState[Inv]: ClassTag: ResourceInventory,
                                 Dev <: GameState[Dev]: ClassTag: DevCardInventory]
      : GameAction[PlayMonopolyMoveResult[Resource], Turn, (Inv, Dev)] =
    GameAction.fromState[PlayMonopolyMoveResult[Resource], Turn]((m, turn) =>
      val loseDeltas = m.cardsLost.toList.map { case (p, cards) =>
        summon[ResourceInventory[Inv]].lose(p, InventorySet.fromMap(Map(m.res -> cards)))
      }
      val totalLost = InventorySet.fromMap(Map(m.res -> m.cardsLost.values.sum))
      val gainDelta = summon[ResourceInventory[Inv]].gain(m.player, totalLost)
      val devDelta  = summon[DevCardInventory[Dev]].playCard(Monopoly, m.player, turn.t)
      loseDeltas ++ List(gainDelta, devDelta)
    )

  private def playRoadBuilderAction[Dev <: GameState[Dev]: ClassTag: DevCardInventory] =
    playRoadBuilderCoreAction[Dev].andThen(longestRoadAction[PlayRoadBuilderMove])

  // ─── PerfectInfoGame ───────────────────────────────────────────────────────

  object PerfectInfoGame:

    type MOVES =
      PerfectInfoRobberMoveResult[Resource]            |
      PerfectInfoBuyDevelopmentCardMoveResult[DevelopmentCard] |
      PerfectInfoPlayKnightResult[Resource]            |
      BuildSettlementMove  | BuildCityMove             | BuildRoadMove          |
      EndTurnMove          | PortTradeMove[Resource]   | DiscardMove[Resource]  |
      InitialPlacementMove | RollDiceMoveResult        | TradeMove[Resource]    |
      PlayMonopolyMoveResult[Resource] | PlayRoadBuilderMove   |
      PlayYearOfPlentyMove[Resource]

    // ── Generic instantiations ──────────────────────────────────────────────

    val buildSettlement  = buildSettlementAction[PrivateInventories[Resource]]
    val buildCity        = buildCityAction[PrivateInventories[Resource]]
    val buildRoad        = buildRoadAction[PrivateInventories[Resource]]
    val portTrade        = portTradeAction[PrivateInventories[Resource]]
    val discard          = discardAction[PrivateInventories[Resource]]
    val trade            = tradeAction[PrivateInventories[Resource]]
    val rollDice         = rollDiceAction[PrivateInventories[Resource]]
    val initialPlacement = initialPlacementAction[PrivateInventories[Resource]]
    val playYearOfPlenty = playYearOfPlentyAction[PrivateInventories[Resource], PrivateDevCardInv[DevelopmentCard]]
    val playMonopoly     = playMonopolyAction[PrivateInventories[Resource], PrivateDevCardInv[DevelopmentCard]]
    val playRoadBuilder  = playRoadBuilderAction[PrivateDevCardInv[DevelopmentCard]]

    // ── PerfectInfo-specific actions ────────────────────────────────────────

    val endTurn: GameAction[EndTurnMove, Unit, Tuple1[Turn]] =
      GameAction[EndTurnMove](_ => List(Delta[Turn](1)))

    val perfectRobber: GameAction[PerfectInfoRobberMoveResult[Resource], Unit, (RobberLocation, PrivateInventories[Resource])] =
      GameAction[PerfectInfoRobberMoveResult[Resource]](m =>
        val stealDeltas = m.steal.toList.flatMap { steal =>
          val inv = InventorySet.fromList(Seq(steal.resource))
          List(
            Delta[PrivateInventories[Resource]](Gain(m.player, inv)),
            Delta[PrivateInventories[Resource]](Lose(steal.victim, inv))
          )
        }
        Delta[RobberLocation](m.robberHexId) :: stealDeltas
      )

    val buyDevCard: GameAction[PerfectInfoBuyDevelopmentCardMoveResult[DevelopmentCard], Turn, (PrivateInventories[Resource], Bank[Resource], PrivateDevCardInv[DevelopmentCard], DevelopmentCardDeck[DevelopmentCard], PlayerPoints)] =
      GameAction.fromState[PerfectInfoBuyDevelopmentCardMoveResult[DevelopmentCard], Turn]((m, turn) =>
        val pointBonus =
          if m.card == POINT then List(Delta[PlayerPoints](PlayerPoints.Increment(m.player))) else Nil
        List(
          Delta[PrivateInventories[Resource]](Lose(m.player, DEV_CARD_COST)),
          Delta[Bank[Resource]](Bank.Add(DEV_CARD_COST)),
          Delta[PrivateDevCardInv[DevelopmentCard]](PerfectInfoBuyCard(m.card, m.player, turn.t)),
          Delta[DevelopmentCardDeck[DevelopmentCard]](DevelopmentCardDeck.Remove)
        ) ++ pointBonus
      )

    // playKnight composed as: move robber → remove knight card → recalculate largest army
    val playKnight =
      perfectRobber
        .compose[PerfectInfoPlayKnightResult[Resource]](_.inner)
        .andThen(removeKnightCardAction[PerfectInfoPlayKnightResult[Resource], PrivateDevCardInv[DevelopmentCard]](_.inner.player))
        .andThen(largestArmyAction[PerfectInfoPlayKnightResult[Resource]](_.inner.player))

    // ── ImmutableGame ───────────────────────────────────────────────────────

    val game: ImmutableGame[MOVES, PerfectInfoState, PerfectInfoDelta] =
      new ImmutableGame[MOVES, PerfectInfoState, PerfectInfoDelta]:
        def applyMove(move: MOVES, state: PerfectInfoState): (List[PerfectInfoDelta], PerfectInfoState) =
          val (state1, deltas): (PerfectInfoState, List[PerfectInfoDelta]) = move match
            case m: BuildSettlementMove                              => buildSettlement.applyFull(m, state)
            case m: BuildCityMove                                    => buildCity.applyFull(m, state)
            case m: BuildRoadMove                                    => buildRoad.applyFull(m, state)
            case m: EndTurnMove                                      => endTurn.applyFull(m, state)
            case m: PortTradeMove[Resource @unchecked]               => portTrade.applyFull(m, state)
            case m: DiscardMove[Resource @unchecked]                 => discard.applyFull(m, state)
            case m: InitialPlacementMove                             => initialPlacement.applyFull(m, state)
            case m: RollDiceMoveResult                               => rollDice.applyFull(m, state)
            case m: TradeMove[Resource @unchecked]                   => trade.applyFull(m, state)
            case m: PerfectInfoRobberMoveResult[Resource @unchecked] => perfectRobber.applyFull(m, state)
            case m: PerfectInfoBuyDevelopmentCardMoveResult[DevelopmentCard @unchecked] => buyDevCard.applyFull(m, state)
            case m: PerfectInfoPlayKnightResult[Resource @unchecked] => playKnight.applyFull(m, state)
            case m: PlayMonopolyMoveResult[Resource @unchecked]      => playMonopoly.applyFull(m, state)
            case m: PlayRoadBuilderMove                              => playRoadBuilder.applyFull(m, state)
            case m: PlayYearOfPlentyMove[Resource @unchecked]        => playYearOfPlenty.applyFull(m, state)
          val mcDelta = Delta[MoveCount](1)
          val state2  = state1.copy(moveCount = state1.moveCount(1))
          (deltas :+ mcDelta, state2)

  // ─── PublicInfoGame ────────────────────────────────────────────────────────

  object PublicInfoGame:

    type MOVES =
      RobberMoveResult[Resource]                       |
      BuyDevelopmentCardMoveResult[DevelopmentCard]    |
      PlayPointMove                                    |
      PlayKnightResult[Resource]                       |
      BuildSettlementMove  | BuildCityMove             | BuildRoadMove          |
      EndTurnMove          | PortTradeMove[Resource]   | DiscardMove[Resource]  |
      InitialPlacementMove | RollDiceMoveResult        | TradeMove[Resource]    |
      PlayMonopolyMoveResult[Resource] | PlayRoadBuilderMove   |
      PlayYearOfPlentyMove[Resource]

    // ── Generic instantiations ──────────────────────────────────────────────

    val buildSettlement  = buildSettlementAction[PublicInventories[Resource]]
    val buildCity        = buildCityAction[PublicInventories[Resource]]
    val buildRoad        = buildRoadAction[PublicInventories[Resource]]
    val portTrade        = portTradeAction[PublicInventories[Resource]]
    val discard          = discardAction[PublicInventories[Resource]]
    val trade            = tradeAction[PublicInventories[Resource]]
    val rollDice         = rollDiceAction[PublicInventories[Resource]]
    val initialPlacement = initialPlacementAction[PublicInventories[Resource]]
    val playYearOfPlenty = playYearOfPlentyAction[PublicInventories[Resource], PublicDevCardInv[DevelopmentCard]]
    val playMonopoly     = playMonopolyAction[PublicInventories[Resource], PublicDevCardInv[DevelopmentCard]]
    val playRoadBuilder  = playRoadBuilderAction[PublicDevCardInv[DevelopmentCard]]

    // ── PublicInfo-specific actions ─────────────────────────────────────────

    val endTurn: GameAction[EndTurnMove, Unit, Tuple1[Turn]] =
      GameAction[EndTurnMove](_ => List(Delta[Turn](1)))

    val publicRobber: GameAction[RobberMoveResult[Resource], Unit, (RobberLocation, PublicInventories[Resource])] =
      GameAction[RobberMoveResult[Resource]](m =>
        val stealDelta = m.steal.toList.map { steal =>
          Delta[PublicInventories[Resource]](ImperfectInfoExchange(steal.victim, m.player, steal.resource))
        }
        Delta[RobberLocation](m.robberHexId) :: stealDelta
      )

    val buyDevCard: GameAction[BuyDevelopmentCardMoveResult[DevelopmentCard], Turn, (PublicInventories[Resource], Bank[Resource], PublicDevCardInv[DevelopmentCard], DevelopmentCardDeckSize)] =
      GameAction.fromState[BuyDevelopmentCardMoveResult[DevelopmentCard], Turn]((m, turn) =>
        List(
          Delta[PublicInventories[Resource]](Lose(m.player, DEV_CARD_COST)),
          Delta[Bank[Resource]](Bank.Add(DEV_CARD_COST)),
          Delta[PublicDevCardInv[DevelopmentCard]](ImperfectInfoBuyCard(m.card, m.player, turn.t)),
          Delta[DevelopmentCardDeckSize](DevelopmentCardDeck.Remove)
        )
      )

    val playPoint: GameAction[PlayPointMove, Turn, (PlayerPoints, PublicDevCardInv[DevelopmentCard])] =
      GameAction.fromState[PlayPointMove, Turn]((m, turn) =>
        List(
          Delta[PlayerPoints](PlayerPoints.Increment(m.player)),
          Delta[PublicDevCardInv[DevelopmentCard]](PlayCard(Point, m.player, turn.t))
        )
      )

    // playKnight composed as: move robber → remove knight card → recalculate largest army
    val playKnight =
      publicRobber
        .compose[PlayKnightResult[Resource]](_.inner)
        .andThen(removeKnightCardAction[PlayKnightResult[Resource], PublicDevCardInv[DevelopmentCard]](_.inner.player))
        .andThen(largestArmyAction[PlayKnightResult[Resource]](_.inner.player))

    // ── ImmutableGame ───────────────────────────────────────────────────────

    val game: ImmutableGame[MOVES, PublicInfoState, PublicInfoDelta] =
      new ImmutableGame[MOVES, PublicInfoState, PublicInfoDelta]:
        def applyMove(move: MOVES, state: PublicInfoState): (List[PublicInfoDelta], PublicInfoState) =
          val (state1, deltas): (PublicInfoState, List[PublicInfoDelta]) = move match
            case m: BuildSettlementMove                           => buildSettlement.applyFull(m, state)
            case m: BuildCityMove                                 => buildCity.applyFull(m, state)
            case m: BuildRoadMove                                 => buildRoad.applyFull(m, state)
            case m: EndTurnMove                                   => endTurn.applyFull(m, state)
            case m: PortTradeMove[Resource @unchecked]            => portTrade.applyFull(m, state)
            case m: DiscardMove[Resource @unchecked]              => discard.applyFull(m, state)
            case m: InitialPlacementMove                          => initialPlacement.applyFull(m, state)
            case m: RollDiceMoveResult                            => rollDice.applyFull(m, state)
            case m: TradeMove[Resource @unchecked]                => trade.applyFull(m, state)
            case m: RobberMoveResult[Resource @unchecked]         => publicRobber.applyFull(m, state)
            case m: BuyDevelopmentCardMoveResult[DevelopmentCard @unchecked] => buyDevCard.applyFull(m, state)
            case m: PlayPointMove                                 => playPoint.applyFull(m, state)
            case m: PlayKnightResult[Resource @unchecked]         => playKnight.applyFull(m, state)
            case m: PlayMonopolyMoveResult[Resource @unchecked]   => playMonopoly.applyFull(m, state)
            case m: PlayRoadBuilderMove                           => playRoadBuilder.applyFull(m, state)
            case m: PlayYearOfPlentyMove[Resource @unchecked]     => playYearOfPlenty.applyFull(m, state)
          val mcDelta = Delta[MoveCount](1)
          val state2  = state1.copy(moveCount = state1.moveCount(1))
          (deltas :+ mcDelta, state2)

  type PerfectInfoMoves = PerfectInfoGame.MOVES
  type PublicInfoMoves  = PublicInfoGame.MOVES
