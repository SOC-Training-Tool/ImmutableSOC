package soc

import game.PerfectInfoMoveResult
import soc.base.*
import soc.base.BaseGame
import soc.base.BaseGame.*
import soc.base.DevelopmentCards.*

/** Resolves a public-info move to a perfect-info move given the perfect-info state.
 *  Moves that are already perfect-info pass through unchanged.
 *  Moves that require state lookup (e.g. RobberMoveResult) are resolved to their
 *  PerfectInfo equivalents.
 */
object MoveResolver:

  def apply(move: PublicInfoGame.MOVES, state: PerfectInfoState): PerfectInfoGame.MOVES =
    move match
      // Already perfect-info moves — pass through directly
      case m: PerfectInfoMoveResult                => m.asInstanceOf[PerfectInfoGame.MOVES]

      // Imperfect-info robber → resolve using state
      case m: RobberMoveResult[?] =>
        resolveRobber(m.asInstanceOf[RobberMoveResult[soc.core.Resource]], state)

      // Imperfect-info buy dev card → resolve using state (deck top card)
      case m: BuyDevelopmentCardMoveResult[?] =>
        resolveBuyDevCard(m.asInstanceOf[BuyDevelopmentCardMoveResult[DevelopmentCard]], state)

      // Public knight → resolve using state
      case m: PlayKnightResult[?] =>
        val inner = resolveRobber(m.asInstanceOf[PlayKnightResult[soc.core.Resource]].inner, state)
        PerfectInfoPlayKnightResult(inner)

      // All shared moves pass through (they are also in PerfectInfoGame.MOVES)
      case m => m.asInstanceOf[PerfectInfoGame.MOVES]

  private def resolveRobber(
    m: RobberMoveResult[soc.core.Resource],
    state: PerfectInfoState
  ): PerfectInfoRobberMoveResult[soc.core.Resource] =
    val steal = m.steal.map { s =>
      val resource = s.resource.orElse(
        state.privateInventories.m
          .get(s.victim)
          .flatMap(_.getTypes.headOption)
      ).getOrElse(throw new IllegalStateException(s"Cannot resolve stolen resource for victim ${s.victim}"))
      PlayerSteal(s.victim, resource)
    }
    PerfectInfoRobberMoveResult(m.player, m.robberHexId, steal)

  private def resolveBuyDevCard(
    m: BuyDevelopmentCardMoveResult[DevelopmentCard],
    state: PerfectInfoState
  ): PerfectInfoBuyDevelopmentCardMoveResult[DevelopmentCard] =
    val card = state.developmentCardDeck.cards.headOption
      .getOrElse(throw new IllegalStateException("Development card deck is empty"))
    PerfectInfoBuyDevelopmentCardMoveResult(m.player, card)
