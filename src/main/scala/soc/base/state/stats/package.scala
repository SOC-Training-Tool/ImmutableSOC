package soc.base.state

import game.InventorySet
import game.InventorySet.InventoryMap
import soc.core.PlayerMap

package object stats {

  case class RollDiceStats[Res](
      rolls: PlayerMap[Map[Int, Int]],
      gained: PlayerMap[InventoryMap[Res]],
      blocked: PlayerMap[InventoryMap[Res]],
      expectedGains: PlayerMap[InventorySet[Res, Double]]
  )

  case class DotsStats[Res](dots: PlayerMap[InventoryMap[Res]])
}
