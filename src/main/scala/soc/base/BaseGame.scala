package soc.base

import game.ImmutableGame
import soc.base.DevelopmentCards.*
import soc.base.actions.*
import soc.base.state.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.DevTransactions.*
import soc.core.state.*

object BaseGame:

  type BaseVertexBuilding = City.type | Settlement.type
  type BaseEdgeBuilding   = Road.type

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
    moveCount:            MoveCount,
    setupPlacementOrder:  SetupPlacementOrder
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
    moveCount:               MoveCount,
    setupPlacementOrder:     SetupPlacementOrder
  )

  val perfectInfoGame: ImmutableGame[PerfectInfoGame.Move, PerfectInfoState] =
    PerfectInfoGame.game

  val publicInfoGame: ImmutableGame[PublicInfoGame.Move, PublicInfoState] =
    PublicInfoGame.game

  /** The standard 25-card development card deck:
    * 14 Knights, 5 Points, 2 Monopoly, 2 Road Builder, 2 Year of Plenty.
    */
  val standardDevCardDeck: List[DevelopmentCard] =
    import DevelopmentCards.*
    List.fill(14)(KNIGHT) ++ List.fill(5)(POINT) ++
      List.fill(2)(MONOPOLY) ++ List.fill(2)(YEAR_OF_PLENTY) ++
      List.fill(2)(ROAD_BUILDER)

  /** Find the desert hex index in a board, if present. */
  def findDesert(board: BaseBoard[?]): Option[Int] =
    board.hexes.zipWithIndex.collectFirst { case (Desert, idx) => idx }

  /** Construct a fresh `PerfectInfoState` with sensible defaults.
    *
    * @param board                the hex layout and ports
    * @param robberLocation       robber starting hex; defaults to the desert hex if `None`
    * @param devCardDeck          development card deck; defaults to [[standardDevCardDeck]]
    * @param bankResourcesPerType how many of each resource the bank starts with (19 for 3-4 players)
    */
  def perfectInfoInitialState(
    board: BaseBoard[Resource],
    robberLocation: Option[Int] = None,
    devCardDeck: Option[List[DevelopmentCard]] = None,
    bankResourcesPerType: Int = 19
  ): PerfectInfoState =
    val robber = robberLocation.orElse(findDesert(board))
      .getOrElse(throw IllegalArgumentException("Must provide robberLocation when board has no desert"))
    val deck = devCardDeck.getOrElse(standardDevCardDeck)
    val bankInventory = ResourceSet(Map[Resource, Int](
      Wood -> bankResourcesPerType, Brick -> bankResourcesPerType,
      Sheep -> bankResourcesPerType, Wheat -> bankResourcesPerType,
      Ore -> bankResourcesPerType
    ))
    PerfectInfoState(
      robberLocation       = RobberLocation(robber),
      privateInventories   = PrivateInventories(Map.empty),
      privateDevCardInv    = PrivateDevCardInv(Map.empty),
      developmentCardDeck  = DevelopmentCardDeck(deck),
      bank                 = Bank(bankInventory),
      turn                 = Turn(0),
      playerPoints         = PlayerPoints(Map.empty),
      largestArmyPlayer    = LargestArmyPlayer(None),
      playerArmyCount      = PlayerArmyCount(Map.empty),
      vertexBuildingState  = VertexBuildingState(Map.empty),
      socRoadLengths       = SOCRoadLengths(Map.empty),
      socLongestRoadPlayer = SOCLongestRoadPlayer(None),
      board                = board,
      edgeBuildingState    = EdgeBuildingState(Map.empty),
      moveCount            = MoveCount(0),
      setupPlacementOrder  = SetupPlacementOrder(Nil)
    )

  /** Construct a fresh `PublicInfoState` with sensible defaults.
    *
    * @param board                the hex layout and ports
    * @param robberLocation       robber starting hex; defaults to the desert hex if `None`
    * @param deckSize             number of cards remaining in the dev card deck (default 25)
    * @param bankResourcesPerType how many of each resource the bank starts with (19 for 3-4 players)
    */
  def publicInfoInitialState(
    board: BaseBoard[Resource],
    robberLocation: Option[Int] = None,
    deckSize: Int = 25,
    bankResourcesPerType: Int = 19
  ): PublicInfoState =
    val robber = robberLocation.orElse(findDesert(board))
      .getOrElse(throw IllegalArgumentException("Must provide robberLocation when board has no desert"))
    val bankInventory = ResourceSet(Map[Resource, Int](
      Wood -> bankResourcesPerType, Brick -> bankResourcesPerType,
      Sheep -> bankResourcesPerType, Wheat -> bankResourcesPerType,
      Ore -> bankResourcesPerType
    ))
    PublicInfoState(
      robberLocation          = RobberLocation(robber),
      publicInventories       = PublicInventories(Map.empty),
      publicDevCardInv        = PublicDevCardInv(Map.empty),
      developmentCardDeckSize = DevelopmentCardDeckSize(deckSize),
      bank                    = Bank(bankInventory),
      turn                    = Turn(0),
      playerPoints            = PlayerPoints(Map.empty),
      largestArmyPlayer       = LargestArmyPlayer(None),
      playerArmyCount         = PlayerArmyCount(Map.empty),
      vertexBuildingState     = VertexBuildingState(Map.empty),
      socRoadLengths          = SOCRoadLengths(Map.empty),
      socLongestRoadPlayer    = SOCLongestRoadPlayer(None),
      board                   = board,
      edgeBuildingState       = EdgeBuildingState(Map.empty),
      moveCount               = MoveCount(0),
      setupPlacementOrder     = SetupPlacementOrder(Nil)
    )

  type PerfectInfoMove = PerfectInfoGame.Move
  type PublicInfoMove  = PublicInfoGame.Move

  private object PerfectInfoGame:

    val game =
      ImmutableGameBuilder[PerfectInfoState]
        .register(EndTurnAction())
        .register(BuildSettlementCoreAction())
        .register(BuildCityAction())
        .register(BuildRoadCoreAction())
        .register(InitialPlacementCoreAction())
        .register(RollDiceAction())
        .register(PortTradeAction())
        .register(TradeAction())
        .register(DiscardAction())
        .register(PerfectRobberAction())
        .register(PerfectBuyDevCardAction())
        .register(PlayPerfectPointAction())
        .register(PlayMonopolyAction())
        .register(PlayRoadBuilderCoreAction())
        .register(PlayYearOfPlentyAction())
        .register(PlayPerfectKnightAction())
        .build

    type Move = EndTurnMove | BuildSettlementMove | BuildCityMove | BuildRoadMove |
      InitialPlacementMove | RollDiceMoveResult | PortTradeMove[Resource] |
      TradeMove[Resource] | DiscardMove[Resource] |
      PerfectInfoRobberMoveResult[Resource] |
      PerfectInfoBuyDevelopmentCardMoveResult[DevelopmentCard] |
      PlayPointMove | PlayMonopolyMoveResult[Resource] | PlayRoadBuilderMove |
      PlayYearOfPlentyMove[Resource] | PerfectInfoPlayKnightResult[Resource]

  private object PublicInfoGame:

    val game =
      ImmutableGameBuilder[PublicInfoState]
        .register(EndTurnAction())
        .register(BuildSettlementCoreAction())
        .register(BuildCityAction())
        .register(BuildRoadCoreAction())
        .register(InitialPlacementCoreAction())
        .register(RollDiceAction())
        .register(PortTradeAction())
        .register(TradeAction())
        .register(DiscardAction())
        .register(PublicRobberAction())
        .register(PublicBuyDevCardAction())
        .register(PlayPublicPointAction())
        .register(PlayMonopolyAction())
        .register(PlayRoadBuilderCoreAction())
        .register(PlayYearOfPlentyAction())
        .register(PlayPublicKnightAction())
        .build

    type Move = EndTurnMove | BuildSettlementMove | BuildCityMove | BuildRoadMove |
      InitialPlacementMove | RollDiceMoveResult | PortTradeMove[Resource] |
      TradeMove[Resource] | DiscardMove[Resource] |
      RobberMoveResult[Resource] | BuyDevelopmentCardMoveResult[DevelopmentCard] |
      PlayPointMove | PlayMonopolyMoveResult[Resource] | PlayRoadBuilderMove |
      PlayYearOfPlentyMove[Resource] | PlayKnightResult[Resource]
