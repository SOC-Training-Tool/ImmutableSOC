package game

import shapeless.Coproduct

/**
 * Base trait for state components that define their own delta types.
 *
 * Each state component (Bank, PlayerPoints, RobberLocation, etc.) extends this trait
 * and specifies:
 * - Delta: A Coproduct of all possible changes that can be applied to this state
 * - applyDelta: How to apply each delta variant to produce a new state
 *
 * This pattern allows:
 * - Type-safe deltas: Can't apply wrong delta to wrong state component
 * - Inspectable changes: GameActions can expose what deltas they will apply
 * - Reusable building blocks: Delta types are shared across actions
 * - Self-describing state: Each state knows what operations are valid on it
 *
 * @tparam Self The concrete state type (for F-bounded polymorphism)
 * @tparam D The Coproduct of delta types this state accepts
 */
trait StateComponent[Self, D <: Coproduct] {

  type Delta = D

  /**
   * Apply a single delta to produce a new state.
   * Implementation typically uses Poly1 with fold to handle each case in the Coproduct.
   */
  def applyDelta(delta: D): Self

  /**
   * Apply multiple deltas in sequence.
   */
  def applyDeltas(deltas: List[D]): Self =
    deltas.foldLeft(this.asInstanceOf[Self]) { (state, delta) =>
      state.asInstanceOf[StateComponent[Self, D]].applyDelta(delta)
    }
}
