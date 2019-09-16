import board._
import core.Roll
import inventory._

object CatanFixtures {

  val singleHexBoard = {
    val vertexMap = Map(0 -> List(0, 1, 2, 3, 4, 5))
    def portMapFunc(ports: List[Port]) = Map(0 -> ports(0), 1 -> ports(0))
    val hexes = List(ResourceHex(Wood, Roll(6)))
    val ports = List(Misc)
    CatanBoard(vertexMap, portMapFunc, hexes, ports)
  }

  val baseBoardConfig = {
    val hexes = List(
      ResourceHex(Wheat, Roll(6)),
      ResourceHex(Ore, Roll(2)),
      ResourceHex(Sheep, Roll(5)),
      ResourceHex(Ore, Roll(8)),
      ResourceHex(Wood, Roll(4)),
      ResourceHex(Brick, Roll(11)),
      ResourceHex(Sheep, Roll(12)),
      ResourceHex(Ore, Roll(9)),
      ResourceHex(Sheep, Roll(10)),
      ResourceHex(Brick, Roll(8)),
      Desert,
      ResourceHex(Wheat, Roll(3)),
      ResourceHex(Sheep, Roll(9)),
      ResourceHex(Brick, Roll(10)),
      ResourceHex(Wood, Roll(3)),
      ResourceHex(Wood, Roll(6)),
      ResourceHex(Wheat, Roll(5)),
      ResourceHex(Wood, Roll(4)),
      ResourceHex(Wheat, Roll(11))
    )

    val ports: List[Port] = List(Misc, Ore, Misc, Wheat, Misc, Brick, Wood, Sheep, Misc)
    BaseBoardConfiguration(hexes, ports)
  }

  val baseBoard = BaseCatanBoard(baseBoardConfig)

}
