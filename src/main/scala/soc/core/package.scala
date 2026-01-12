package soc


import game.InventorySet
import shapeless.{:+:, CNil, Coproduct}
import soc.core.Resources._
package object core {

  type PlayerMap[A] = Map[Int, A]

  case object Wood

  case object Brick

  case object Sheep

  case object Wheat

  case object Ore

  case object Misc

  type Resource = Wood.type :+: Brick.type :+: Sheep.type :+: Wheat.type :+: Ore.type :+: CNil

  type Port = Misc.type :+: Resource

  object Resources {
    val WOOD: Resource = Coproduct[Resource](Wood)
    val BRICK: Resource = Coproduct[Resource](Brick)
    val SHEEP: Resource = Coproduct[Resource](Sheep)
    val WHEAT: Resource = Coproduct[Resource](Wheat)
    val ORE: Resource = Coproduct[Resource](Ore)

    val all: Seq[Resource] = WOOD :: BRICK :: SHEEP :: WHEAT :: ORE :: Nil
  }

  object Ports {
    val WOOD: Port = Coproduct[Port](Wood)
    val BRICK: Port = Coproduct[Port](Brick)
    val SHEEP: Port = Coproduct[Port](Sheep)
    val WHEAT: Port = Coproduct[Port](Wheat)
    val ORE: Port = Coproduct[Port](Ore)
    val MISC: Port = Coproduct[Port](Misc)

    val all: Seq[Port] = WOOD :: BRICK :: SHEEP :: WHEAT :: ORE :: MISC :: Nil
  }

  object ResourceSet {
    type ResourceSet[T] = InventorySet[Resource, T]
    type Resources = ResourceSet[Int]

    def apply[T: Numeric](br: T = 0, or: T = 0, sh: T = 0, wh: T = 0, wo: T = 0): ResourceSet[T] = ResourceSet[T](Map[Resource, T](BRICK -> br, WOOD -> wo, ORE -> or, SHEEP -> sh, WHEAT -> wh))

    def apply[T: Numeric](resMap: Map[Resource, T]): ResourceSet[T] = InventorySet.fromMap(resMap)

    def apply(resources: Resource*): Resources = InventorySet.fromList(resources.toSeq)

    def empty[T: Numeric]: ResourceSet[T] = InventorySet.empty[Resource, T]
  }

  trait BoardBuilding[Loc]

  case object Settlement extends BoardBuilding[Vertex]

  case object City extends BoardBuilding[Vertex]

  case object Road extends BoardBuilding[Edge]

  trait VertexBuildingValue[A] {
    def apply: Int
  }

  type VertexBuilding = Settlement.type :+: City.type :+: CNil

  type EdgeBuilding = Road.type :+: CNil

  object VertexBuilding {

    val SETTLEMENT: VertexBuilding = Coproduct[VertexBuilding](Settlement)
    val CITY: VertexBuilding = Coproduct[VertexBuilding](City)

    implicit val settlementValue: VertexBuildingValue[Settlement.type] = new VertexBuildingValue[Settlement.type] {
      override def apply: Int = 1
    }

    implicit val cityValue: VertexBuildingValue[City.type] = new VertexBuildingValue[City.type] {
      override def apply: Int = 2
    }
  }

  object EdgeBuilding {
    val ROAD = Coproduct[EdgeBuilding](Road)
  }


}
