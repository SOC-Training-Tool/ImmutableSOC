package game

import shapeless.ops.coproduct.Inject
import shapeless.ops.{coproduct, hlist}
import shapeless.{:+:, ::, CNil, Coproduct, HList, HNil, Poly1}
import util.opext.Embedder

trait GameState[T] {
  self: T =>
  type Delta

  def apply(delta: Delta): T
}

case class Delta[T <: GameState[T]] private(delta: T#Delta)

object Delta {

  def apply[T <: GameState[T]] = DeltaApply[T]()

  case class DeltaApply[T <: GameState[T]]() {
    def apply[D](delta: D)(implicit gen: DeltaGen[T, D]): Delta[T] = gen.apply(delta)
  }

  trait DeltaGen[T <: GameState[T], D] {
    def apply(d: D): Delta[T]
  }

  trait LowPriorityDeltaGen {
    implicit def base[T <: GameState[T], D](implicit ev: D =:= T#Delta): DeltaGen[T, D] = (d: D) => new Delta[T](ev.apply(d))

  }


  object DeltaGen extends LowPriorityDeltaGen {

    def apply[T <: GameState[T], D](implicit d: DeltaGen[T, D]): DeltaGen[T, D] = d

    implicit def coproduct[T <: GameState[T], Super <: Coproduct, Sub <: Coproduct](implicit ev: Super =:= T#Delta, embedder: Embedder[Super, Sub]): DeltaGen[T, Sub] =
      (sub: Sub) => new Delta[T](ev.apply(embedder.embed(sub)))

    implicit def coproductInject[T <: GameState[T], C <: Coproduct, D](implicit gen: DeltaGen[T, C], inject: Inject[C, D]): DeltaGen[T, D] =
      (d: D) => gen.apply(inject.apply(d))
  }

  object ApplyDeltas extends Poly1 {
    implicit def onDelta[T <: GameState[T], S <: HList](implicit modifier: hlist.Modifier.Aux[S, T, T, (T, S)]): Case.Aux[(Delta[T], S), S] =
      at { case (delta: Delta[T], state: S) => state.updateWith[T, T, S] ( t => t.apply(delta.delta.asInstanceOf[t.Delta])) }
  }

  trait LiftAllDelta[L <: HList] {
    type Out <: Coproduct
  }

  object LiftAllDelta {

    type Aux[L <: HList, Out0 <: Coproduct] = LiftAllDelta[L] {type Out = Out0}

    def apply[L <: HList](implicit l: LiftAllDelta[L]): Aux[L, l.Out] = l

    implicit def recur[H <: GameState[H], T <: HList](implicit next: LiftAllDelta[T]): Aux[H :: T, Delta[H] :+: next.Out] =
      new LiftAllDelta[H :: T] {
        type Out = Delta[H] :+: next.Out
      }

    implicit val hnil: Aux[HNil, CNil] = new LiftAllDelta[HNil] {
      type Out = CNil
    }
  }

  trait ExtractState[C <: Coproduct] {
    type Out <: HList
  }

  object ExtractState {
    type Aux[C <: Coproduct, Out0 <: HList] = ExtractState[C] {type Out = Out0}

    def apply[C <: Coproduct](implicit e: ExtractState[C]): Aux[C, e.Out] = e

    implicit def recur[H, S <: GameState[S], T <: Coproduct](implicit ev: H <:< Delta[S], next: ExtractState[T]): Aux[H :+: T, S :: next.Out] =
      new ExtractState[H :+: T] {
        type Out = S :: next.Out
      }

    implicit val cnil: Aux[CNil, HNil] = new ExtractState[CNil] {
      type Out = HNil
    }
  }
}
