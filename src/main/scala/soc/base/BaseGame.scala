package soc.base

import soc.base.DevelopmentCards.*
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
