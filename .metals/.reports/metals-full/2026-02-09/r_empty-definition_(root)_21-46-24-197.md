error id: file://<WORKSPACE>/src/main/scala/soc/base/BaseGame.scala:game/ImmutableGameBuilder#build().
file://<WORKSPACE>/src/main/scala/soc/base/BaseGame.scala
empty definition using pc, found symbol in pc: 
found definition using semanticdb; symbol game/ImmutableGameBuilder#build().
empty definition using fallback
non-local guesses:

offset: 11597
uri: file://<WORKSPACE>/src/main/scala/soc/base/BaseGame.scala
text:
```scala
package soc.base

import game.Delta.DeltaGen
import game.ImmutableGame.TypeBox
import game.utils.{ExtractDelta, ExtractGameTypes, ExtractMoves, ExtractState, LiftAllMapper}
import game.{Delta, GameAction, ImmutableGame, utils}
import shapeless.ops.hlist
import shapeless.ops.hlist.Mapper
import shapeless.ops.hlist.ZipConst.Aux
import shapeless.{:+:, ::, CNil, HNil, PolyDefns}
import soc.base.DevelopmentCards._
import soc.base.actions._
import soc.base.actions.build._
import soc.base.actions.developmentcards.PlayDevelopmentCardAction.PlayDevelopmentCardActionOps
import soc.base.actions.developmentcards._
import soc.base.actions.special.{LargestArmyExtension, LongestRoadExtension}
import soc.base.state._
import soc.core.DevTransactions.PlayCard
import soc.core.ResourceInventories._
import soc.core.Resources._
import soc.core.Transactions.PerfectInfo
import soc.core.VertexBuilding.{cityValue, settlementValue}
import soc.core._
import soc.core.actions.{DiscardAction, EndTurnAction, MoveCountExtension, TradeAction}
import soc.core.state._

object BaseGame {

  type BaseVertexBuilding = City.type :+: Settlement.type :+: CNil
  type BaseEdgeBuilding = Road.type :+: CNil

  val longestRoadExtension = LongestRoadExtension[Resource, BaseVertexBuilding, BaseEdgeBuilding, BaseBoard[Resource]]()

  trait CoreImplicits[Inv[_], DInv[_]] {

    implicit val rDG  : DeltaGen[Inv[Resource], PerfectInfo[Resource]]
    implicit val devDG: DeltaGen[DInv[DevelopmentCard], PlayCard[DevelopmentCard]]

    implicit val buildSettlementAction : GameAction[BuildSettlementMove, SOCRoadLengths :: SOCLongestRoadPlayer :: BaseBoard[Resource] :: VertexBuildingState[BaseVertexBuilding] :: EdgeBuildingState[BaseEdgeBuilding] :: HNil, Delta[SOCRoadLengths] :+: Delta[SOCLongestRoadPlayer] :+: Delta[PlayerPoints] :+: Delta[Bank[Resource]] :+: Delta[Inv[Resource]] :+: Delta[VertexBuildingState[BaseVertexBuilding]] :+: CNil]                                                                              = BuildSettlementAction[Resource, Inv, BaseVertexBuilding](ResourceSet(WOOD, BRICK, WHEAT, SHEEP))
      .andThen(longestRoadExtension.composeS { (move: BuildSettlementMove, state: BaseBoard[Resource] :: HNil) =>
        import SOCBoard.SOCBoardOps
        state.select[BaseBoard[Resource]].edgesFromVertex.getOrElse(move.vertex, Seq.empty)
      })
    implicit val buildCityAction       : GameAction[BuildCityMove, HNil, Delta[PlayerPoints] :+: Delta[Bank[Resource]] :+: Delta[Inv[Resource]] :+: Delta[VertexBuildingState[BaseVertexBuilding]] :+: CNil]                                                                                                                                                                                                                                                                                                 = BuildCityAction[Resource, Inv, BaseVertexBuilding](ResourceSet(ORE, ORE, ORE, WHEAT, WHEAT))
    implicit val buildRoadAction       : GameAction[BuildRoadMove, SOCRoadLengths :: SOCLongestRoadPlayer :: BaseBoard[Resource] :: VertexBuildingState[BaseVertexBuilding] :: EdgeBuildingState[BaseEdgeBuilding] :: HNil, Delta[SOCRoadLengths] :+: Delta[PlayerPoints] :+: Delta[SOCLongestRoadPlayer] :+: Delta[EdgeBuildingState[BaseEdgeBuilding]] :+: Delta[Bank[Resource]] :+: Delta[Inv[Resource]] :+: CNil]                                                                                        = BuildRoadAction[Resource, Inv, BaseEdgeBuilding](ResourceSet(WOOD, BRICK))
      .andThen(longestRoadExtension.compose(move => Seq(move.edge)))
    implicit val endTurnAction         : GameAction[EndTurnMove, HNil, Delta[Turn] :+: CNil]                                                                                                                                                                                                                                                                                                                                                                                                                 = EndTurnAction.action
    implicit val portTradeAction       : GameAction[PortTradeMove[Resource], HNil, Delta[Bank[Resource]] :+: Delta[Inv[Resource]] :+: CNil]                                                                                                                                                                                                                                                                                                                                                                  = PortTradeAction[Resource, Inv]()
    implicit val discardAction         : GameAction[DiscardMove[Resource], HNil, Delta[Inv[Resource]] :+: Delta[Bank[Resource]] :+: CNil]                                                                                                                                                                                                                                                                                                                                                                    = DiscardAction[Resource, Inv]()
    implicit val initialPlacementAction: GameAction[InitialPlacementMove, BaseBoard[Resource] :: MoveCount :: PlayerPoints :: SOCRoadLengths :: SOCLongestRoadPlayer :: VertexBuildingState[BaseVertexBuilding] :: EdgeBuildingState[BaseEdgeBuilding] :: HNil, Delta[SOCRoadLengths] :+: Delta[SOCLongestRoadPlayer] :+: Delta[Bank[Resource]] :+: Delta[Inv[Resource]] :+: Delta[EdgeBuildingState[BaseEdgeBuilding]] :+: Delta[PlayerPoints] :+: Delta[VertexBuildingState[BaseVertexBuilding]] :+: CNil] = InitialPlacementAction[Resource, Inv, BaseBoard[Resource], BaseEdgeBuilding, BaseVertexBuilding]()
      .andThen(longestRoadExtension.compose(move => Seq(move.edge)))
    implicit val rollDiceAction        : GameAction[RollDiceMoveResult, BaseBoard[Resource] :: RobberLocation :: Bank[Resource] :: VertexBuildingState[BaseVertexBuilding] :: HNil, Delta[Inv[Resource]] :+: Delta[Bank[Resource]] :+: CNil]                                                                                                                                                                                                                                                                 = RollDiceAction[Resource, Inv, BaseBoard[Resource], BaseVertexBuilding]()
    implicit val tradeAction           : GameAction[TradeMove[Resource], HNil, Delta[Inv[Resource]] :+: CNil]                                                                                                                                                                                                                                                                                                                                                                                                = TradeAction[Resource, Inv]()

    implicit val playMonopolyAction    : GameAction[PlayMonopolyMoveResult[Resource], Turn :: HNil, Delta[Inv[Resource]] :+: Delta[DInv[DevelopmentCard]] :+: CNil]                                                                                                                                                                                                                                               = PlayMonopolyAction[Resource, Inv]()
      .playDevelopmentCard[DevelopmentCard, DInv](Monopoly)
    implicit val playRoadBuilderAction : GameAction[PlayRoadBuilderMove, Turn :: SOCRoadLengths :: SOCLongestRoadPlayer :: BaseBoard[Resource] :: VertexBuildingState[BaseVertexBuilding] :: EdgeBuildingState[BaseEdgeBuilding] :: HNil, Delta[SOCRoadLengths] :+: Delta[PlayerPoints] :+: Delta[SOCLongestRoadPlayer] :+: Delta[EdgeBuildingState[BaseEdgeBuilding]] :+: Delta[DInv[DevelopmentCard]] :+: CNil] = PlayRoadBuilderAction[BaseEdgeBuilding]().playDevelopmentCard[DevelopmentCard, DInv](RoadBuilder)
      .andThen(longestRoadExtension.compose(move => Seq(Some(move.edge1), move.edge2).flatten))
    implicit val playYearOfPlentyAction: GameAction[PlayYearOfPlentyMove[Resource], Turn :: HNil, Delta[Bank[Resource]] :+: Delta[Inv[Resource]] :+: Delta[DInv[DevelopmentCard]] :+: CNil]                                                                                                                                                                                                                       = PlayYearOfPlentyAction[Resource, Inv]().playDevelopmentCard[DevelopmentCard, DInv](YearOfPlenty)

    type CORE_MOVES = BuildSettlementMove :+: BuildCityMove :+: BuildRoadMove :+: EndTurnMove :+: PortTradeMove[Resource] :+: DiscardMove[Resource] :+: InitialPlacementMove :+: RollDiceMoveResult :+: TradeMove[Resource] :+: PlayMonopolyMoveResult[Resource] :+: PlayRoadBuilderMove :+: PlayYearOfPlentyMove[Resource] :+: CNil
  }


  object PerfectInfoGame extends CoreImplicits[PrivateInventories, PrivateDevCardInv] {

    override implicit val rDG  : DeltaGen[PrivateInventories[Resource], PerfectInfo[Resource]]           = implicitly[DeltaGen[PrivateInventories[Resource], PerfectInfo[Resource]]]
    override implicit val devDG: DeltaGen[PrivateDevCardInv[DevelopmentCard], PlayCard[DevelopmentCard]] = implicitly[DeltaGen[PrivateDevCardInv[DevelopmentCard], PlayCard[DevelopmentCard]]]

    implicit val moveRobberAndStealAction: GameAction[PerfectInfoRobberMoveResult[Resource], HNil, Delta[PrivateInventories[Resource]] :+: Delta[RobberLocation] :+: CNil]                                                                                                                                                  = MoveRobberAndStealAction.perfect[Resource, PrivateInventories]
    implicit val buyDevelopmentCardAction: GameAction[PerfectInfoBuyDevelopmentCardMoveResult[DevelopmentCard], Turn :: HNil, Delta[PlayerPoints] :+: Delta[DevelopmentCardDeck[DevelopmentCard]] :+: Delta[PrivateDevCardInv[DevelopmentCard]] :+: Delta[Bank[Resource]] :+: Delta[PrivateInventories[Resource]] :+: CNil] = BuyDevelopmentCardAction.perfect[Resource, PrivateInventories, DevelopmentCard, PrivateDevCardInv](ResourceSet(ORE, WHEAT, SHEEP))
      .andThen(PlayPointAction.onPerfectBuy[DevelopmentCard])

    implicit val playKnightAction: GameAction[PerfectInfoPlayKnightResult[Resource], Turn :: LargestArmyPlayer :: PlayerArmyCount :: HNil, Delta[PlayerArmyCount] :+: Delta[PlayerPoints] :+: Delta[LargestArmyPlayer] :+: Delta[RobberLocation] :+: Delta[PrivateInventories[Resource]] :+: Delta[PrivateDevCardInv[DevelopmentCard]] :+: CNil] = moveRobberAndStealAction.compose[PerfectInfoPlayKnightResult[Resource]](_.inner)
      .playDevelopmentCard[DevelopmentCard, PrivateDevCardInv](Knight)
      .andThen(LargestArmyExtension(3).compose(_.move.player))

    type MOVES = PerfectInfoRobberMoveResult[Resource] :+: PerfectInfoBuyDevelopmentCardMoveResult[DevelopmentCard] :+: PerfectInfoPlayKnightResult[Resource] :+: super.CORE_MOVES
    type STATE = RobberLocation :: PrivateInventories[Resource] :: PrivateDevCardInv[DevelopmentCard] :: DevelopmentCardDeck[DevelopmentCard] :: Bank[Resource] :: Turn :: PlayerPoints :: LargestArmyPlayer :: PlayerArmyCount :: VertexBuildingState[BaseVertexBuilding] :: SOCRoadLengths :: SOCLongestRoadPlayer :: BaseBoard[Resource] :: EdgeBuildingState[BaseEdgeBuilding] :: MoveCount :: PublicInventories[Resource] :: HNil

    //val builder = ImmutableGame.apply[BuildSettlementMove :+: BuildCityMove :+: BuildRoadMove :+: CNil]().addGlobalAction(MoveCountExtension())
    val builder = ImmutableGame.apply[MOVES]().addGlobalAction(MoveCountExtension())
    val game = builder.bu@@ild()

  }

  object PublicInfoGame extends CoreImplicits[PublicInventories, PublicDevCardInv] {

    override implicit val rDG  : DeltaGen[PublicInventories[Resource], PerfectInfo[Resource]]           = implicitly[DeltaGen[PublicInventories[Resource], PerfectInfo[Resource]]]
    override implicit val devDG: DeltaGen[PublicDevCardInv[DevelopmentCard], PlayCard[DevelopmentCard]] = implicitly[DeltaGen[PublicDevCardInv[DevelopmentCard], PlayCard[DevelopmentCard]]]

    implicit val moveRobberAndStealAction: GameAction[RobberMoveResult[Resource], HNil, Delta[PublicInventories[Resource]] :+: Delta[RobberLocation] :+: CNil]                                                                                                                                                                              =
      MoveRobberAndStealAction.public[Resource, PublicInventories]
    implicit val buyDevelopmentCardAction: GameAction[BuyDevelopmentCardMoveResult[DevelopmentCard], Turn :: HNil, Delta[DevelopmentCardDeckSize] :+: Delta[PublicDevCardInv[DevelopmentCard]] :+: Delta[Bank[Resource]] :+: Delta[PublicInventories[Resource]] :+: CNil]                                                                   = BuyDevelopmentCardAction.public[Resource, PublicInventories, DevelopmentCard, PublicDevCardInv](ResourceSet(ORE, WHEAT, SHEEP))
    implicit val playPointAction         : GameAction[PlayPointMove, Turn :: HNil, Delta[PlayerPoints] :+: Delta[PublicDevCardInv[DevelopmentCard]] :+: CNil]                                                                                                                                                                               = PlayPointAction.public.playDevelopmentCard[DevelopmentCard, PublicDevCardInv](Point)
    implicit val playKnightAction        : GameAction[PlayKnightResult[Resource], Turn :: LargestArmyPlayer :: PlayerArmyCount :: HNil, Delta[PlayerArmyCount] :+: Delta[PlayerPoints] :+: Delta[LargestArmyPlayer] :+: Delta[RobberLocation] :+: Delta[PublicInventories[Resource]] :+: Delta[PublicDevCardInv[DevelopmentCard]] :+: CNil] = moveRobberAndStealAction.compose[PlayKnightResult[Resource]](_.inner)
      .playDevelopmentCard[DevelopmentCard, PublicDevCardInv](Knight)
      .andThen(LargestArmyExtension(3).compose(_.move.player))

    type MOVES = RobberMoveResult[Resource] :+: BuyDevelopmentCardMoveResult[DevelopmentCard] :+: PlayPointMove :+: PlayKnightResult[Resource] :+: super.CORE_MOVES
    type STATE = RobberLocation :: PublicInventories[Resource] :: PublicDevCardInv[DevelopmentCard] :: DevelopmentCardDeckSize :: state.Bank[Resource] :: state.Turn :: state.PlayerPoints :: LargestArmyPlayer :: PlayerArmyCount :: VertexBuildingState[BaseVertexBuilding] :: SOCRoadLengths :: SOCLongestRoadPlayer :: BaseBoard[Resource] :: EdgeBuildingState[BaseEdgeBuilding] :: state.MoveCount :: HNil

    //val game = ImmutableGame.apply[MOVES]().addGlobalAction(MoveCountExtension()).build().align[MOVES, STATE]()
  }

  type PerfectInfoMoves = PerfectInfoGame.MOVES
  type PublicInfoMoves = PublicInfoGame.MOVES
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: 