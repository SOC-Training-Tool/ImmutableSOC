package soc

import shapeless.{:+:, CNil, Coproduct}

package object base {

  case object Knight

  case object Monopoly

  case object YearOfPlenty

  case object RoadBuilder

  case object Point

  object DevelopmentCards {

    type DevelopmentCard = Knight.type :+: Monopoly.type :+: YearOfPlenty.type :+: RoadBuilder.type :+: Point.type :+: CNil

    val KNIGHT = Coproduct[DevelopmentCard](Knight)
    val MONOPOLY = Coproduct[DevelopmentCard](Monopoly)
    val YEAR_OF_PLENTY = Coproduct[DevelopmentCard](YearOfPlenty)
    val ROAD_BUILDER = Coproduct[DevelopmentCard](RoadBuilder)
    val POINT = Coproduct[DevelopmentCard](Point)
  }

  type DevelopmentCard = DevelopmentCards.DevelopmentCard


}
