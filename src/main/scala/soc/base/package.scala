package soc

import game.{:+:, CNil, Inl, Inr}
import game.inject

package object base {

  case object Knight

  case object Monopoly

  case object YearOfPlenty

  case object RoadBuilder

  case object Point

  object DevelopmentCards {

    type DevelopmentCard = Knight.type :+: Monopoly.type :+: YearOfPlenty.type :+: RoadBuilder.type :+: Point.type :+: CNil

    val KNIGHT = Knight.inject[DevelopmentCard]
    val MONOPOLY = Monopoly.inject[DevelopmentCard]
    val YEAR_OF_PLENTY = YearOfPlenty.inject[DevelopmentCard]
    val ROAD_BUILDER = RoadBuilder.inject[DevelopmentCard]
    val POINT = Point.inject[DevelopmentCard]
  }

  type DevelopmentCard = DevelopmentCards.DevelopmentCard
}
