//package soc.base.actions.developmentcards
//
//import game.{ExtendAction, GameAction, GameMove, GameMoveResult}
//import shapeless.ops.hlist
//import shapeless.{::, Coproduct, HList, HNil}
//import soc.core.DevelopmentCardInventories
//import soc.core.DevelopmentCardInventories.DevelopmentCardInventoriesOps
//import soc.core.state.Turn
//import util.DependsOn
//
//object PlayDevelopmentCardAction {
//
//  implicit class PlayDevelopmentCardActionOps[M <: GameMoveResult, S <: HList, A](action: A)(implicit ev: A <:< GameAction[M, S]) {
//    def playDevelopmentCard[Inv[_]] = PlayApply[Inv, M, S](action)
//  }
//
//  final case class PlayApply[Inv[_], M <: GameMoveResult, S <: HList](action: GameAction[M, S]) {
//    def apply[Dev <: Coproduct, Out](card: Dev)(implicit dev: DevelopmentCardInventories[Dev, Inv], extend: ExtendAction.Aux[GameAction[M, S], GameAction[M, Inv[Dev] :: Turn :: HNil], Out]) = {
//      action.expose(new PlayDevelopmentCardActionExtension[Dev, Inv]())(move => (move.move.player, card))
//    }
//  }
//}
//
//class PlayDevelopmentCardActionExtension[Dev <: Coproduct, Inv[_]](implicit dev: DevelopmentCardInventories[Dev, Inv])
//  extends GameAction[(Int, Dev), Inv[Dev] :: Turn :: HNil] {
//
//  override def apply(move: (Int, Dev), state: STATE): STATE = {
//    state.updateWith[Inv[Dev], Inv[Dev], STATE](_.playCard(move._1, move._2))
//  }
//}
//
