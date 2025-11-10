package soc.base

import game.ImmutableGame
import shapeless.{:+:, ::, CNil, HNil}
import soc.base.DevelopmentCards._
import soc.base.actions._
import soc.base.actions.build._
import soc.base.actions.developmentcards.PlayDevelopmentCardAction.PlayDevelopmentCardActionOps
import soc.base.actions.developmentcards._
import soc.base.actions.special.{LargestArmyExtension, LongestRoadExtension}
import soc.base.state._
import soc.core._
import soc.core.state._
import soc.core.DevelopmentCardInventories._
import soc.core.ResourceInventories._
import soc.core.Resources._
import soc.core.Transactions.PerfectInfo
import soc.core.VertexBuilding.{cityValue, settlementValue}
import soc.core.actions.{DiscardAction, EndTurnAction, MoveCountExtension, TradeAction}
import soc.core.state.{EdgeBuildingState, VertexBuildingState}

object BaseGame {

  type BaseVertexBuilding = City.type :+: Settlement.type :+: CNil
  type BaseEdgeBuilding   = Road.type :+: CNil

  val longestRoadExtension = new LongestRoadExtension[Resource, BaseVertexBuilding, BaseEdgeBuilding, BaseBoard[Resource]]()
  class CoreImplicits[Inv[_], DInv[_]](implicit inv: ResourceInventories[Resource, PerfectInfo[Resource], Inv], dInv: DevelopmentCardInventories[DevelopmentCard, DInv]) {

    implicit val buildSettlementAction = new BuildSettlementAction[Resource, Inv, BaseVertexBuilding](ResourceSet(WOOD, BRICK, WHEAT, SHEEP))
      .exposeS(longestRoadExtension) { (move: BuildSettlementMove, state: BaseBoard[Resource] :: HNil) =>
        import SOCBoard.SOCBoardOps
        state.select[BaseBoard[Resource]].edgesFromVertex.getOrElse(move.vertex, Seq.empty)
      }
    implicit val buildCityAction = new BuildCityAction[Resource, Inv, BaseVertexBuilding](ResourceSet(ORE, ORE, ORE, WHEAT, WHEAT))
    implicit val buildRoadAction = new BuildRoadAction[Resource, Inv, BaseEdgeBuilding](ResourceSet(WOOD, BRICK))
      .expose(longestRoadExtension)(move => Seq(move.edge))
    implicit val endTurnAction = EndTurnAction
    implicit val portTradeAction = new PortTradeAction[Resource, Inv]
    implicit val discardAction = new DiscardAction[Resource, Inv]
    implicit val initialPlacementAction = new InitialPlacementAction[Resource, Inv, BaseVertexBuilding, BaseEdgeBuilding, BaseBoard[Resource]]
      .expose(longestRoadExtension)(move => Seq(move.edge))
    implicit val rollDiceAction = new RollDiceAction[Resource, BaseVertexBuilding, BaseBoard[Resource], Inv]
    implicit val tradeAction = new TradeAction[Resource, Inv]

    implicit val playMonopolyAction = new PlayMonopolyAction[Resource, PublicInventories]().playDevelopmentCard[DInv](MONOPOLY)
    implicit val playRoadBuilderAction = new PlayRoadBuilderAction[BaseEdgeBuilding]().playDevelopmentCard[DInv](ROAD_BUILDER)
      .expose(longestRoadExtension)(move => Seq(Some(move.edge1), move.edge2).flatten)
    implicit val playYearOfPlentyAction = new PlayYearOfPlentyAction[Resource, PublicInventories]().playDevelopmentCard[DInv](YEAR_OF_PLENTY)

    type MOVES = BuildSettlementMove :+: BuildCityMove :+: BuildRoadMove :+: EndTurnMove :+: PortTradeMove[Resource] :+: DiscardMove[Resource] :+: InitialPlacementMove :+: RollDiceMoveResult :+: TradeMove[Resource] :+: PlayMonopolyMoveResult[Resource] :+: PlayRoadBuilderMove :+: PlayYearOfPlentyMove[Resource] :+: CNil
  }

  object PerfectInfoGame {
    val core = new CoreImplicits[PrivateInventories, PrivateDevelopmentCards]
    import core._

    implicit val moveRobberAndStealAction = new PerfectInfoMoveRobberAndStealAction[Resource, PrivateInventories]
    implicit val buyDevelopmentCardAction = new PerfectInfoBuyDevelopmentCardAction[Resource, PrivateInventories, DevelopmentCard, PrivateDevelopmentCards](ResourceSet(ORE, WHEAT, SHEEP))
      .extend(PlayPointAction.extension[DevelopmentCard])
    implicit val playKnightAction = moveRobberAndStealAction.compose[PerfectInfoPlayKnightResult[Resource]](_.inner)
      .playDevelopmentCard[PrivateDevelopmentCards](KNIGHT)
      .extend(new LargestArmyExtension[Resource, PerfectInfoPlayKnightResult[Resource]](3))

    type MOVES = PerfectInfoRobberMoveResult[Resource] :+: PerfectInfoBuyDevelopmentCardMoveResult[DevelopmentCard] :+: PerfectInfoPlayKnightResult[Resource] :+: core.MOVES
    type STATE = RobberLocation :: PrivateInventories[Resource] :: PrivateDevelopmentCards[DevelopmentCard] :: DevelopmentCardDeck[DevelopmentCard] :: Bank[Resource] :: Turn :: PlayerPoints :: LargestArmyPlayer :: PlayerArmyCount :: VertexBuildingState[BaseVertexBuilding] :: SOCRoadLengths :: SOCLongestRoadPlayer :: BaseBoard[Resource] :: EdgeBuildingState[BaseEdgeBuilding] :: MoveCount :: PublicInventories[Resource] :: HNil

    val game: ImmutableGame[MOVES, STATE] = ImmutableGame.apply[MOVES]().addGlobalAction(MoveCountExtension).align[MOVES, STATE]()
  }

  object PublicInfoGame {
    val core = new CoreImplicits[PublicInventories, PublicDevelopmentCards]
    import core._

    implicit val moveRobberAndStealAction = new MoveRobberAndStealAction[Resource, PublicInventories]
    implicit val buyDevelopmentCardAction = new BuyDevelopmentCardAction[Resource, PublicInventories, DevelopmentCard, PublicDevelopmentCards](ResourceSet(ORE, WHEAT, SHEEP))
    implicit val playPointAction = PlayPointAction.apply().playDevelopmentCard[PublicDevelopmentCards](POINT)
    implicit val playKnightAction = moveRobberAndStealAction.compose[PlayKnightResult[Resource]](_.inner)
      .playDevelopmentCard[PublicDevelopmentCards](KNIGHT)
      .extend(new LargestArmyExtension[Resource, PlayKnightResult[Resource]](3))

    type MOVES = RobberMoveResult[Resource] :+: BuyDevelopmentCardMoveResult[DevelopmentCard] :+: PlayPointMove :+: PlayKnightResult[Resource] :+: core.MOVES
    type STATE = RobberLocation :: PublicInventories[Resource] :: PublicDevelopmentCards[DevelopmentCard] :: DevelopmentCardDeckSize :: state.Bank[Resource] :: state.Turn :: state.PlayerPoints :: LargestArmyPlayer :: PlayerArmyCount :: VertexBuildingState[BaseVertexBuilding] :: SOCRoadLengths :: SOCLongestRoadPlayer :: BaseBoard[Resource] :: EdgeBuildingState[BaseEdgeBuilding] :: state.MoveCount :: HNil

    val game: ImmutableGame[MOVES, STATE] = ImmutableGame.apply[MOVES]().addGlobalAction(MoveCountExtension).align[MOVES, STATE]()
  }

  type PerfectInfoMoves = PerfectInfoGame.MOVES
  type PublicInfoMoves = PublicInfoGame.MOVES
}
