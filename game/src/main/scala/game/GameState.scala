package game

trait GameState[T]:
  self: T =>
  type Delta

  def apply(delta: Delta): T

class Delta[T] private[game] (private[game] val delta: Any)

object Delta:

  def apply[T <: GameState[T]]: DeltaApply[T] = DeltaApply[T]()

  case class DeltaApply[T]():
    def apply[D](delta: D)(using gen: DeltaGen[T, D]): Delta[T] = gen(delta)

  /** Type class for converting a delta value D into a Delta[T] wrapper.
    * Priority: gameState (direct match) > coproductInject (single type) > coproduct (sub-coproduct) */
  trait DeltaGen[T, D]:
    def apply(d: D): Delta[T]

  trait LowPriorityDeltaGen:
    given coproductEmbed[A, Super, Sub](using gen: DeltaGen[A, Super], basis: CoproductBasis[Super, Sub]): DeltaGen[A, Sub] with
      def apply(d: Sub): Delta[A] = gen(basis.embed(d))

    given coproductInject[A, C, D](using gen: DeltaGen[A, C], inject: CoproductInject[C, D]): DeltaGen[A, D] with
      def apply(d: D): Delta[A] = gen(inject(d))

  object DeltaGen extends LowPriorityDeltaGen:
    def apply[T, D](using d: DeltaGen[T, D]): DeltaGen[T, D] = d

    given gameState[D, A <: GameState[A] { type Delta = D }]: DeltaGen[A, D] with
      def apply(d: D): Delta[A] = new Delta[A](d)
