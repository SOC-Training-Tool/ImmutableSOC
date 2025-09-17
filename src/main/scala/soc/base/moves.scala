package soc.base

import game._
import soc.core.{Edge, Vertex}

case class PlayerSteal[R](victim: Int, resource: R)

case class PortTradeMove[II](player: Int, give: InventorySet[II, Int], get: InventorySet[II, Int]) extends PerfectInformationGameMove[PortTradeMove[II]]

case class RobberMove[Res](player: Int, robberHexId: Int, victim: Option[Int]) extends GameMove

case class RobberMoveResult[Res](player: Int, robberHexId: Int, steal: Option[PlayerSteal[Option[Res]]]) extends GameMoveResult {
  override type A = RobberMove[Res]

  override def move: RobberMove[Res] = RobberMove(player, robberHexId, steal.map(_.victim))
}

case class PerfectInfoRobberMoveResult[Res](player: Int, robberHexId: Int, steal: Option[PlayerSteal[Res]]) extends PerfectInfoMoveResult {
  override type A                       = RobberMove[Res]
  override type PublicInfoMoveResult = RobberMoveResult[Res]

  override def move: A = RobberMove(player, robberHexId, steal.map(_.victim))

  override def getPerspectiveResults(playerIds: Seq[Int]): Map[Int, PublicInfoMoveResult] = playerIds.map { p =>
    val resource = steal.filter(s => player == p || player == s.victim).map(_.resource)
    p -> RobberMoveResult[Res](player, robberHexId, steal.map(s => PlayerSteal(s.victim, resource)))
  }.toMap
}

case class BuyDevelopmentCardMove[Card](player: Int) extends GameMove

case class BuyDevelopmentCardMoveResult[Card](player: Int, card: Option[Card]) extends GameMoveResult {
  override type A = BuyDevelopmentCardMove[Card]

  override def move: BuyDevelopmentCardMove[Card] = BuyDevelopmentCardMove(player)
}

case class PerfectInfoBuyDevelopmentCardMoveResult[Card](player: Int, card: Card) extends PerfectInfoMoveResult {
  override type PublicInfoMoveResult = BuyDevelopmentCardMoveResult[Card]

  override def getPerspectiveResults(playerIds: Seq[Int]): Map[Int, BuyDevelopmentCardMoveResult[Card]] = {
    playerIds.map {
      case `player` => player -> BuyDevelopmentCardMoveResult[Card](player, Some(card))
      case p        => p      -> BuyDevelopmentCardMoveResult[Card](player, None)
    }.toMap
  }

  override type A = BuyDevelopmentCardMove[Card]

  override def move: BuyDevelopmentCardMove[Card] = BuyDevelopmentCardMove(player)
}

case class PlayKnightMove[Res](inner: RobberMove[Res]) extends GameMove {
  val player = inner.player
}

case class PlayKnightResult[Res](inner: RobberMoveResult[Res]) extends GameMoveResult {
  override type A = PlayKnightMove[Res]

  override def move: PlayKnightMove[Res] = PlayKnightMove(inner.move)
}

case class PerfectInfoPlayKnightResult[Res](inner: PerfectInfoRobberMoveResult[Res]) extends PerfectInfoMoveResult {
  override type PublicInfoMoveResult = PlayKnightResult[Res]

  override def getPerspectiveResults(playerIds: Seq[Int]): Map[Int, PlayKnightResult[Res]] =
    inner.getPerspectiveResults(playerIds).view.mapValues(PlayKnightResult.apply).toMap

  override type A = PlayKnightMove[Res]

  override def move: PlayKnightMove[Res] = PlayKnightMove(inner.move)
}

case class PlayMonopolyMove[Res](player: Int, res: Res) extends GameMove

case class PlayMonopolyMoveResult[Res](player: Int, res: Res, cardsLost: Map[Int, Int]) extends PerfectInfoMoveResult {
  override type PublicInfoMoveResult = PlayMonopolyMoveResult[Res]

  override def getPerspectiveResults(playerIds: Seq[Int]): Map[Int, PlayMonopolyMoveResult[Res]] = playerIds.map(_ -> this).toMap

  override type A = PlayMonopolyMove[Res]

  override def move: PlayMonopolyMove[Res] = PlayMonopolyMove[Res](player, res)
}

case class PlayPointMove(player: Int) extends PerfectInformationGameMove[PlayPointMove]

case class PlayRoadBuilderMove(player: Int, edge1: Edge, edge2: Option[Edge]) extends PerfectInformationGameMove[PlayRoadBuilderMove]

case class PlayYearOfPlentyMove[Res](player: Int, c1: Res, c2: Res) extends PerfectInformationGameMove[PlayYearOfPlentyMove[Res]]
