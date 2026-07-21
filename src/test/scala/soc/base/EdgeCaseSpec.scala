package soc.base

import game.InventorySet
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import soc.base.DevelopmentCards.*
import soc.base.state.*
import soc.core.*
import soc.core.SOCBoard.SOCBoardOps
import soc.core.ResourceInventories.*
import soc.core.ResourceSet.*
import soc.core.Resources.*
import soc.core.Transactions.*

class EdgeCaseSpec extends AnyFunSpec with Matchers:

  private val sampleBoard: BaseBoard[Resource] = BaseBoard(
    List[Hex[Resource]](
      ResourceHex(WHEAT, 6), ResourceHex(ORE, 2), ResourceHex(SHEEP, 5),
      ResourceHex(ORE, 8), ResourceHex(WOOD, 4), ResourceHex(BRICK, 11),
      ResourceHex(SHEEP, 12), ResourceHex(ORE, 9), ResourceHex(SHEEP, 10),
      ResourceHex(BRICK, 8), Desert, ResourceHex(WHEAT, 3),
      ResourceHex(SHEEP, 9), ResourceHex(BRICK, 10), ResourceHex(WOOD, 3),
      ResourceHex(WOOD, 6), ResourceHex(WHEAT, 5), ResourceHex(WOOD, 4),
      ResourceHex(WHEAT, 11)
    ),
    List(Ports.MISC, Ports.ORE, Ports.MISC, Ports.WHEAT, Ports.MISC, Ports.BRICK, Ports.WOOD, Ports.SHEEP, Ports.MISC)
  )

  describe("DevelopmentCardDeck"):

    it("keeps an empty deck empty when removing a card"):
      val deck = DevelopmentCardDeck[DevelopmentCard](Nil)
      val next = deck(DevelopmentCardDeck.Remove)
      next.cards shouldBe Nil

  describe("ResourceInventories"):

    it("leaves public inventories unchanged when losing from a missing player"):
      val inventories = PublicInventories[Resource](Map(0 -> 3))
      val next = inventories(Lose(1, ResourceSet(WOOD)))
      next.m shouldBe Map(0 -> 3)

    it("leaves private inventories unchanged when losing from a missing player"):
      val inventories = PrivateInventories[Resource](Map(0 -> ResourceSet(wo = 2)))
      val next = inventories(Lose(1, ResourceSet(WOOD)))
      next.m shouldBe Map(0 -> ResourceSet(wo = 2))

  describe("BaseBoard"):

    it("returns empty vertices for a hex index missing from the vertex map"):
      val board = BaseBoard(List.fill(20)(Desert: Hex[Resource]), Nil)
      board.hexesWithNodes.last.vertices shouldBe Nil

    it("keeps the existing happy path unchanged for known vertices"):
      val board = BaseBoard(List(Desert: Hex[Resource]), Nil)
      board.hexesWithNodes.head.vertices should not be empty
