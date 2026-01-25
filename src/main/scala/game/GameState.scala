package game

import shapeless.ops.coproduct.Inject
import shapeless.ops.{coproduct, hlist}
import shapeless.{:+:, ::, CNil, Coproduct, HList, HNil, Poly1, Poly2}

trait GameState[T] {
  self: T =>
  type Delta

  def apply(delta: Delta): T
}

class Delta[T] private(delta: Any) {
  def getValue[GS <: GameState[GS]](implicit ev: T =:= GS): GS#Delta = delta.asInstanceOf[GS#Delta]
}

object Delta {

  def apply[T <: GameState[T]] = DeltaApply[T]()

  case class DeltaApply[T]() {
    def apply[D](delta: D)(implicit gen: DeltaGen[T, D]): Delta[T] = gen.apply(delta)
  }

  trait DeltaGen[T, D] {
    def apply(d: D): Delta[T]
  }

  trait LowPriorityDeltaGen {
    implicit def coproduct[A, Super <: Coproduct, Sub <: Coproduct](implicit gen: DeltaGen[A, Super], embedder: shapeless.ops.coproduct.Basis[Super, Sub]): DeltaGen[A, Sub] = new DeltaGen[A, Sub] {
      override def apply(d: Sub): Delta[A] = gen.apply(d.embed[Super])
    }

    implicit def coproductInject[A, C <: Coproduct, D](implicit gen: DeltaGen[A, C], inject: Inject[C, D]): DeltaGen[A, D] = new DeltaGen[A, D] {
      override def apply(d: D): Delta[A] = gen.apply(inject.apply(d))
    }
  }


  object DeltaGen extends LowPriorityDeltaGen {

    def apply[T, D](implicit d: DeltaGen[T, D]): DeltaGen[T, D] = d

    implicit def gameState[A <: GameState[A], D](implicit ev: D =:= A#Delta): DeltaGen[A, D] = (d: D) => new Delta[A](d)
  }

  object ApplyDeltas extends Poly2 {
    implicit def onDelta[T <: GameState[T], S <: HList](implicit modifier: hlist.Modifier.Aux[S, T, T, (T, S)]): Case.Aux[S, Delta[T], S] =
      at[S, Delta[T]] { (state, delta) => state.updateWith[T, T, S] ( t => t.apply(delta.getValue.asInstanceOf[t.Delta])) }
  }

  trait ExtractState[C <: Coproduct] {
    type Out <: HList
  }

  object ExtractState {
    type Aux[C <: Coproduct, Out0 <: HList] = ExtractState[C] {type Out = Out0}

    def apply[C <: Coproduct](implicit e: ExtractState[C]): Aux[C, e.Out] = e

    implicit def recur[H, T <: Coproduct](implicit next: ExtractState[T]): Aux[Delta[H] :+: T, H :: next.Out] =
      new ExtractState[Delta[H] :+: T] {
        type Out = H :: next.Out
      }

    implicit val cnil: Aux[CNil, HNil] = new ExtractState[CNil] {
      type Out = HNil
    }
  }
}
