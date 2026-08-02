package soc.rules
package validators

import game.InventorySet
import soc.base.*
import soc.core.*
import soc.rules.*

object TurnValidator:

  def endTurnMoves(player: Int, turnMoves: Seq[Any], phase: PhaseMachine.TurnPhase): Seq[EndTurnMove] =
    phase match
      case PhaseMachine.TurnPhase.MainPlay(_) if PhaseMachine.diceRolled(turnMoves) =>
        Seq(EndTurnMove(player))
      case _ => Nil

  def discardMoves(player: Int, inv: ResourceView): Seq[DiscardMove[Resource]] =
    val total = inv.getTotal(player)
    if total <= 7 then Nil
    else
      val amount = total / 2
      discardCombinations(player, inv, amount).map(set => DiscardMove[Resource](player, set))

  private def discardCombinations(
    player: Int,
    inv: ResourceView,
    amount: Int
  ): Seq[InventorySet[Resource, Int]] =
    def gen(resources: List[Resource], remaining: Int, acc: Map[Resource, Int]): Seq[Map[Resource, Int]] =
      if remaining == 0 then Seq(acc)
      else
        resources match
          case Nil     => Nil
          case r :: rs =>
            val max = math.min(inv.resourceAmount(player, r), remaining)
            (0 to max).flatMap(take => gen(rs, remaining - take, acc + (r -> take)))
    gen(Resources.all.toList, amount, Map.empty)
      .map(m => InventorySet.fromMap(m))
      .filter(_.getTotal == amount)
