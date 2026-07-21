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
