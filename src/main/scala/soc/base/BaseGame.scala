package soc.base

import game.Delta.DeltaGen
import game.{Delta, GameAction, ImmutableGame, ImmutableGameBuilder, :+:, CNil}
import game.select
import soc.base.DevelopmentCards._
import soc.base.actions._
import soc.base.actions.build._
import soc.base.actions.developmentcards.PlayDevelopmentCardAction.playDevelopmentCard
import soc.base.actions.developmentcards._
import soc.base.actions.special.{LargestArmyExtension, LongestRoadExtension}
import soc.base.state.{DevelopmentCardDeck, DevelopmentCardDeckSize, LargestArmyPlayer, PlayerArmyCount, RobberLocation, SOCLongestRoadPlayer, SOCRoadLengths}
import soc.core.DevTransactions.{PlayCard, PerfectInfoBuyCard, ImperfectInfoBuyCard}
import soc.core.ResourceInventories._
import soc.core.Resources._
import soc.core.Transactions.PerfectInfo
import soc.core.VertexBuilding.{cityValue, settlementValue}
import soc.core._
import soc.core.actions.{DiscardAction, EndTurnAction, MoveCountExtension, TradeAction}
import soc.core.state.{Bank, EdgeBuildingState, MoveCount, PlayerPoints, Turn, VertexBuildingState}

object BaseGame {

  type BaseVertexBuilding = City.type :+: Settlement.type :+: CNil
  type BaseEdgeBuilding = Road.type :+: CNil

  val longestRoadExtension = LongestRoadExtension[Resource, BaseVertexBuilding, BaseEdgeBuilding, BaseBoard[Resource]]()

  trait CoreImplicits[Inv[_], DInv[_]] {

    given rDG: DeltaGen[Inv[Resource], PerfectInfo[Resource]]
    given devDG: DeltaGen[DInv[DevelopmentCard], PlayCard[DevelopmentCard]]

    given buildSettlementAction: GameAction[BuildSettlementMove, ?, ?] =
      BuildSettlementAction[Resource, Inv, BaseVertexBuilding](ResourceSet(WOOD, BRICK, WHEAT, SHEEP))(using rDG)
        .andThen(longestRoadExtension.composeS { (move: BuildSettlementMove, state: Tuple1[BaseBoard[Resource]]) =>
          import soc.core.SOCBoard.edgesFromVertex
          state._1.edgesFromVertex.getOrElse(move.vertex, Seq.empty)
        })
    given buildCityAction: GameAction[BuildCityMove, EmptyTuple, Delta[PlayerPoints] :+: Delta[Bank[Resource]] :+: Delta[Inv[Resource]] :+: Delta[VertexBuildingState[BaseVertexBuilding]] :+: CNil] =
      BuildCityAction[Resource, Inv, BaseVertexBuilding](ResourceSet(ORE, ORE, ORE, WHEAT, WHEAT))(using rDG)
    given buildRoadAction: GameAction[BuildRoadMove, ?, ?] =
      BuildRoadAction[Resource, Inv, BaseEdgeBuilding](ResourceSet(WOOD, BRICK))(using rDG)
        .andThen(longestRoadExtension.compose(move => Seq(move.edge)))
    given endTurnAction: GameAction[EndTurnMove, EmptyTuple, Delta[Turn] :+: CNil] =
      EndTurnAction.action
    given portTradeAction: GameAction[PortTradeMove[Resource], EmptyTuple, Delta[Bank[Resource]] :+: Delta[Inv[Resource]] :+: CNil] =
      PortTradeAction[Resource, Inv]()(using rDG)
    given discardAction: GameAction[DiscardMove[Resource], EmptyTuple, Delta[Inv[Resource]] :+: Delta[Bank[Resource]] :+: CNil] =
      DiscardAction[Resource, Inv]()(using rDG)
    given initialPlacementAction: GameAction[InitialPlacementMove, ?, ?] =
      InitialPlacementAction[Resource, Inv, BaseBoard[Resource], BaseEdgeBuilding, BaseVertexBuilding]()(using rDG)
        .andThen(longestRoadExtension.compose(move => Seq(move.edge)))
    given rollDiceAction: GameAction[RollDiceMoveResult, ?, ?] =
      RollDiceAction[Resource, Inv, BaseBoard[Resource], BaseVertexBuilding]()
    given tradeAction: GameAction[TradeMove[Resource], EmptyTuple, Delta[Inv[Resource]] :+: CNil] =
      TradeAction[Resource, Inv]()(using rDG)

    given playMonopolyAction: GameAction[PlayMonopolyMoveResult[Resource], ?, ?] =
      PlayMonopolyAction[Resource, Inv]()(using rDG)
        .playDevelopmentCard[DevelopmentCard, DInv](Monopoly)
    given playRoadBuilderAction: GameAction[PlayRoadBuilderMove, ?, ?] =
      PlayRoadBuilderAction[BaseEdgeBuilding]().playDevelopmentCard[DevelopmentCard, DInv](RoadBuilder)
        .andThen(longestRoadExtension.compose(move => Seq(Some(move.edge1), move.edge2).flatten))
    given playYearOfPlentyAction: GameAction[PlayYearOfPlentyMove[Resource], ?, ?] =
      PlayYearOfPlentyAction[Resource, Inv]()(using rDG).playDevelopmentCard[DevelopmentCard, DInv](YearOfPlenty)

    type CORE_MOVES = BuildSettlementMove :+: BuildCityMove :+: BuildRoadMove :+: EndTurnMove :+: PortTradeMove[Resource] :+: DiscardMove[Resource] :+: InitialPlacementMove :+: RollDiceMoveResult :+: TradeMove[Resource] :+: PlayMonopolyMoveResult[Resource] :+: PlayRoadBuilderMove :+: PlayYearOfPlentyMove[Resource] :+: CNil
  }


  object PerfectInfoGame extends CoreImplicits[PrivateInventories, PrivateDevCardInv] {

    override given rDG: DeltaGen[PrivateInventories[Resource], PerfectInfo[Resource]] =
      DeltaGen.gameState[PerfectInfo[Resource], PrivateInventories[Resource]]

    // Full Delta type for PrivateDevCardInv
    type PrivateDevDelta = PerfectInfoBuyCard[DevelopmentCard] :+: PlayCard[DevelopmentCard] :+: CNil

    given baseDG: DeltaGen[PrivateDevCardInv[DevelopmentCard], PrivateDevDelta] =
      DeltaGen.gameState[PrivateDevDelta, PrivateDevCardInv[DevelopmentCard]]

    override given devDG: DeltaGen[PrivateDevCardInv[DevelopmentCard], PlayCard[DevelopmentCard]] with
      def apply(d: PlayCard[DevelopmentCard]) = baseDG(game.Inr(game.Inl(d)))

    // Explicit DeltaGen for PerfectInfoBuyCard
    given DeltaGen[PrivateDevCardInv[DevelopmentCard], PerfectInfoBuyCard[DevelopmentCard]] with
      def apply(d: PerfectInfoBuyCard[DevelopmentCard]) = baseDG(game.Inl(d))

    given moveRobberAndStealAction: GameAction[PerfectInfoRobberMoveResult[Resource], EmptyTuple, Delta[PrivateInventories[Resource]] :+: Delta[RobberLocation] :+: CNil] =
      MoveRobberAndStealAction.perfect[Resource, PrivateInventories]
    given buyDevelopmentCardAction: GameAction[PerfectInfoBuyDevelopmentCardMoveResult[DevelopmentCard], ?, ?] =
      BuyDevelopmentCardAction.perfect[Resource, PrivateInventories, DevelopmentCard, PrivateDevCardInv](ResourceSet(ORE, WHEAT, SHEEP))
        .andThen(PlayPointAction.onPerfectBuy[DevelopmentCard])

    given playKnightAction: GameAction[PerfectInfoPlayKnightResult[Resource], ?, ?] =
      moveRobberAndStealAction.compose[PerfectInfoPlayKnightResult[Resource]](_.inner)
        .playDevelopmentCard[DevelopmentCard, PrivateDevCardInv](Knight)
        .andThen(LargestArmyExtension(3).compose(_.move.player))

    type MOVES = PerfectInfoRobberMoveResult[Resource] :+: PerfectInfoBuyDevelopmentCardMoveResult[DevelopmentCard] :+: PerfectInfoPlayKnightResult[Resource] :+: super.CORE_MOVES
    type STATE = (RobberLocation, PrivateInventories[Resource], PrivateDevCardInv[DevelopmentCard], DevelopmentCardDeck[DevelopmentCard], Bank[Resource], Turn, PlayerPoints, LargestArmyPlayer, PlayerArmyCount, VertexBuildingState[BaseVertexBuilding], SOCRoadLengths, SOCLongestRoadPlayer, BaseBoard[Resource], EdgeBuildingState[BaseEdgeBuilding], MoveCount, PublicInventories[Resource])

   // val actions: GameAction[BuildSettlementMove, SOCRoadLengths :: SOCLongestRoadPlayer :: BaseBoard[Resource] :: VertexBuildingState[BaseVertexBuilding] :: EdgeBuildingState[BaseEdgeBuilding] :: HNil, Delta[SOCRoadLengths] :+: Delta[SOCLongestRoadPlayer] :+: Delta[PlayerPoints] :+: Delta[Bank[Resource]] :+: Delta[PrivateInventories[Resource]] :+: Delta[VertexBuildingState[BaseVertexBuilding]] :+: CNil] :: GameAction[BuildCityMove, HNil, Delta[PlayerPoints] :+: Delta[Bank[Resource]] :+: Delta[PrivateInventories[Resource]] :+: Delta[VertexBuildingState[BaseVertexBuilding]] :+: CNil] :: GameAction[BuildRoadMove, SOCRoadLengths :: SOCLongestRoadPlayer :: BaseBoard[Resource] :: VertexBuildingState[BaseVertexBuilding] :: EdgeBuildingState[BaseEdgeBuilding] :: HNil, Delta[SOCRoadLengths] :+: Delta[PlayerPoints] :+: Delta[SOCLongestRoadPlayer] :+: Delta[EdgeBuildingState[BaseEdgeBuilding]] :+: Delta[Bank[Resource]] :+: Delta[PrivateInventories[Resource]] :+: CNil] :: HNil = FetchActions[BuildSettlementMove :+: BuildCityMove :+: BuildRoadMove :+: CNil].instances
   // val es     : Aux[GameAction[BuildSettlementMove, SOCRoadLengths :: SOCLongestRoadPlayer :: BaseBoard[Resource] :: VertexBuildingState[BaseVertexBuilding] :: EdgeBuildingState[BaseEdgeBuilding] :: HNil, Delta[SOCRoadLengths] :+: Delta[SOCLongestRoadPlayer] :+: Delta[PlayerPoints] :+: Delta[Bank[Resource]] :+: Delta[PrivateInventories[Resource]] :+: Delta[VertexBuildingState[BaseVertexBuilding]] :+: CNil] :: GameAction[BuildCityMove, HNil, Delta[PlayerPoints] :+: Delta[Bank[Resource]] :+: Delta[PrivateInventories[Resource]] :+: Delta[VertexBuildingState[BaseVertexBuilding]] :+: CNil] :: GameAction[BuildRoadMove, SOCRoadLengths :: SOCLongestRoadPlayer :: BaseBoard[Resource] :: VertexBuildingState[BaseVertexBuilding] :: EdgeBuildingState[BaseEdgeBuilding] :: HNil, Delta[SOCRoadLengths] :+: Delta[PlayerPoints] :+: Delta[SOCLongestRoadPlayer] :+: Delta[EdgeBuildingState[BaseEdgeBuilding]] :+: Delta[Bank[Resource]] :+: Delta[PrivateInventories[Resource]] :+: CNil] :: HNil, SOCRoadLengths :: SOCLongestRoadPlayer :: BaseBoard[Resource] :: VertexBuildingState[BaseVertexBuilding] :: EdgeBuildingState[BaseEdgeBuilding] :: PlayerPoints :: Bank[Resource] :: PrivateInventories[Resource] :: HNil] =
   //   ExtractState[GameAction[BuildSettlementMove, SOCRoadLengths :: SOCLongestRoadPlayer :: BaseBoard[Resource] :: VertexBuildingState[BaseVertexBuilding] :: EdgeBuildingState[BaseEdgeBuilding] :: HNil, Delta[SOCRoadLengths] :+: Delta[SOCLongestRoadPlayer] :+: Delta[PlayerPoints] :+: Delta[Bank[Resource]] :+: Delta[PrivateInventories[Resource]] :+: Delta[VertexBuildingState[BaseVertexBuilding]] :+: CNil] :: GameAction[BuildCityMove, HNil, Delta[PlayerPoints] :+: Delta[Bank[Resource]] :+: Delta[PrivateInventories[Resource]] :+: Delta[VertexBuildingState[BaseVertexBuilding]] :+: CNil] :: GameAction[BuildRoadMove, SOCRoadLengths :: SOCLongestRoadPlayer :: BaseBoard[Resource] :: VertexBuildingState[BaseVertexBuilding] :: EdgeBuildingState[BaseEdgeBuilding] :: HNil, Delta[SOCRoadLengths] :+: Delta[PlayerPoints] :+: Delta[SOCLongestRoadPlayer] :+: Delta[EdgeBuildingState[BaseEdgeBuilding]] :+: Delta[Bank[Resource]] :+: Delta[PrivateInventories[Resource]] :+: CNil] :: HNil]

    //val result: GameAction[BuildSettlementMove, SOCRoadLengths :: SOCLongestRoadPlayer :: BaseBoard[Resource] :: VertexBuildingState[BaseVertexBuilding] :: EdgeBuildingState[BaseEdgeBuilding] :: HNil, Delta[MoveCount] :+: Delta[SOCRoadLengths] :+: Delta[SOCLongestRoadPlayer] :+: Delta[PlayerPoints] :+: Delta[Bank[Resource]] :+: Delta[PrivateInventories[Resource]] :+: Delta[VertexBuildingState[BaseVertexBuilding]] :+: CNil] :: GameAction[BuildCityMove, HNil, Delta[MoveCount] :+: Delta[PlayerPoints] :+: Delta[Bank[Resource]] :+: Delta[PrivateInventories[Resource]] :+: Delta[VertexBuildingState[BaseVertexBuilding]] :+: CNil] :: GameAction[BuildRoadMove, SOCRoadLengths :: SOCLongestRoadPlayer :: BaseBoard[Resource] :: VertexBuildingState[BaseVertexBuilding] :: EdgeBuildingState[BaseEdgeBuilding] :: HNil, Delta[MoveCount] :+: Delta[SOCRoadLengths] :+: Delta[PlayerPoints] :+: Delta[SOCLongestRoadPlayer] :+: Delta[EdgeBuildingState[BaseEdgeBuilding]] :+: Delta[Bank[Resource]] :+: Delta[PrivateInventories[Resource]] :+: CNil] :: HNil = (buildSettlementAction :: buildCityAction :: buildRoadAction :: HNil).zipConst(MoveCountExtension()).map(AddGlobalActionPoly)

    val builder = ImmutableGame.apply[MOVES]().addGlobalAction(MoveCountExtension())
    val game = builder.build()

  }

  object PublicInfoGame extends CoreImplicits[PublicInventories, PublicDevCardInv] {

    override given rDG: DeltaGen[PublicInventories[Resource], PerfectInfo[Resource]] =
      DeltaGen.gameState[PerfectInfo[Resource], PublicInventories[Resource]]

    // Full Delta type for PublicDevCardInv
    type PublicDevDelta = ImperfectInfoBuyCard[DevelopmentCard] :+: PlayCard[DevelopmentCard] :+: CNil

    given baseDG: DeltaGen[PublicDevCardInv[DevelopmentCard], PublicDevDelta] =
      DeltaGen.gameState[PublicDevDelta, PublicDevCardInv[DevelopmentCard]]

    override given devDG: DeltaGen[PublicDevCardInv[DevelopmentCard], PlayCard[DevelopmentCard]] with
      def apply(d: PlayCard[DevelopmentCard]) = baseDG(game.Inr(game.Inl(d)))

    // Explicit DeltaGen for ImperfectInfoBuyCard
    given DeltaGen[PublicDevCardInv[DevelopmentCard], ImperfectInfoBuyCard[DevelopmentCard]] with
      def apply(d: ImperfectInfoBuyCard[DevelopmentCard]) = baseDG(game.Inl(d))

    given moveRobberAndStealAction: GameAction[RobberMoveResult[Resource], EmptyTuple, Delta[PublicInventories[Resource]] :+: Delta[RobberLocation] :+: CNil] =
      MoveRobberAndStealAction.public[Resource, PublicInventories]
    given buyDevelopmentCardAction: GameAction[BuyDevelopmentCardMoveResult[DevelopmentCard], Tuple1[Turn], Delta[DevelopmentCardDeckSize] :+: Delta[PublicDevCardInv[DevelopmentCard]] :+: Delta[Bank[Resource]] :+: Delta[PublicInventories[Resource]] :+: CNil] =
      BuyDevelopmentCardAction.public[Resource, PublicInventories, DevelopmentCard, PublicDevCardInv](ResourceSet(ORE, WHEAT, SHEEP))
    given playPointAction: GameAction[PlayPointMove, ?, ?] =
      PlayPointAction.public.playDevelopmentCard[DevelopmentCard, PublicDevCardInv](Point)
    given playKnightAction: GameAction[PlayKnightResult[Resource], ?, ?] =
      moveRobberAndStealAction.compose[PlayKnightResult[Resource]](_.inner)
        .playDevelopmentCard[DevelopmentCard, PublicDevCardInv](Knight)
        .andThen(LargestArmyExtension(3).compose(_.move.player))

    type MOVES = RobberMoveResult[Resource] :+: BuyDevelopmentCardMoveResult[DevelopmentCard] :+: PlayPointMove :+: PlayKnightResult[Resource] :+: super.CORE_MOVES
    type STATE = (RobberLocation, PublicInventories[Resource], PublicDevCardInv[DevelopmentCard], DevelopmentCardDeckSize, Bank[Resource], Turn, PlayerPoints, LargestArmyPlayer, PlayerArmyCount, VertexBuildingState[BaseVertexBuilding], SOCRoadLengths, SOCLongestRoadPlayer, BaseBoard[Resource], EdgeBuildingState[BaseEdgeBuilding], MoveCount)

    //val game = ImmutableGame.apply[MOVES]().addGlobalAction(MoveCountExtension()).build().align[MOVES, STATE]()
  }

  type PerfectInfoMoves = PerfectInfoGame.MOVES
  type PublicInfoMoves = PublicInfoGame.MOVES
}
