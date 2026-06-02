package soc

package object base:

  case object Knight
  case object Monopoly
  case object YearOfPlenty
  case object RoadBuilder
  case object Point

  object DevelopmentCards:
    type DevelopmentCard =
      Knight.type | Monopoly.type | YearOfPlenty.type | RoadBuilder.type | Point.type

    val KNIGHT:         DevelopmentCard = Knight
    val MONOPOLY:       DevelopmentCard = Monopoly
    val YEAR_OF_PLENTY: DevelopmentCard = YearOfPlenty
    val ROAD_BUILDER:   DevelopmentCard = RoadBuilder
    val POINT:          DevelopmentCard = Point

  type DevelopmentCard = DevelopmentCards.DevelopmentCard
