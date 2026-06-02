package soc

import game.InventorySet

package object core:

  type PlayerMap[A] = Map[Int, A]

  case object Wood
  case object Brick
  case object Sheep
  case object Wheat
  case object Ore
  case object Misc

  type Resource = Wood.type | Brick.type | Sheep.type | Wheat.type | Ore.type
  type Port     = Misc.type | Resource

  object Resources:
    val WOOD:  Resource = Wood
    val BRICK: Resource = Brick
    val SHEEP: Resource = Sheep
    val WHEAT: Resource = Wheat
    val ORE:   Resource = Ore
    val all: Seq[Resource] = Seq(WOOD, BRICK, SHEEP, WHEAT, ORE)

  object Ports:
    val WOOD:  Port = Wood
    val BRICK: Port = Brick
    val SHEEP: Port = Sheep
    val WHEAT: Port = Wheat
    val ORE:   Port = Ore
    val MISC:  Port = Misc
    val all: Seq[Port] = Seq(WOOD, BRICK, SHEEP, WHEAT, ORE, MISC)

  object ResourceSet:
    type ResourceSet[T] = InventorySet[Resource, T]
    type Resources      = ResourceSet[Int]

    // Named-arg version for int-valued resource sets (ResourceSet(br = 4), etc.)
    def apply(br: Int = 0, or: Int = 0, sh: Int = 0, wh: Int = 0, wo: Int = 0): Resources =
      InventorySet.fromMap(Map[Resource, Int](
        Brick -> br, Wood -> wo, Ore -> or, Sheep -> sh, Wheat -> wh
      ))

    def apply[T: Numeric](resMap: Map[Resource, T]): ResourceSet[T] = InventorySet.fromMap(resMap)

    // Varargs version: ResourceSet(WOOD, BRICK, WHEAT, SHEEP)
    def apply(resources: Resource*): Resources = InventorySet.fromList(resources.toSeq)

    def empty[T: Numeric]: ResourceSet[T] = InventorySet.empty[Resource, T]

  trait BoardBuilding[Loc]

  case object Settlement extends BoardBuilding[Vertex]
  case object City       extends BoardBuilding[Vertex]
  case object Road       extends BoardBuilding[Edge]

  trait VertexBuildingValue[A]:
    def apply: Int

  type VertexBuilding = Settlement.type | City.type
  type EdgeBuilding   = Road.type

  object VertexBuilding:
    val SETTLEMENT: VertexBuilding = Settlement
    val CITY:       VertexBuilding = City

    given settlementValue: VertexBuildingValue[Settlement.type] with
      def apply: Int = 1

    given cityValue: VertexBuildingValue[City.type] with
      def apply: Int = 2

  object EdgeBuilding:
    val ROAD: EdgeBuilding = Road
