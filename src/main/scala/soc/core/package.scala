package soc


import game.{:+:, CNil, Inl, Inr, CoproductInject, InventorySet}
import game.inject

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
    val WOOD: Resource = Wood.inject[Resource]
    val BRICK: Resource = Brick.inject[Resource]
    val SHEEP: Resource = Sheep.inject[Resource]
    val WHEAT: Resource = Wheat.inject[Resource]
    val ORE: Resource = Ore.inject[Resource]

    val all: Seq[Resource] = WOOD :: BRICK :: SHEEP :: WHEAT :: ORE :: Nil
  }

  object Ports {
    val WOOD: Port = Wood.inject[Port]
    val BRICK: Port = Brick.inject[Port]
    val SHEEP: Port = Sheep.inject[Port]
    val WHEAT: Port = Wheat.inject[Port]
    val ORE: Port = Ore.inject[Port]
    val MISC: Port = Misc.inject[Port]

    val all: Seq[Port] = WOOD :: BRICK :: SHEEP :: WHEAT :: ORE :: MISC :: Nil
  }

  import Resources._

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

    val SETTLEMENT: VertexBuilding = Settlement.inject[VertexBuilding]
    val CITY: VertexBuilding = City.inject[VertexBuilding]

    given settlementValue: VertexBuildingValue[Settlement.type] with
      def apply: Int = 1

    given cityValue: VertexBuildingValue[City.type] with
      def apply: Int = 2
  }

  object EdgeBuilding {
    val ROAD: EdgeBuilding = Road.inject[EdgeBuilding]
  }
}
