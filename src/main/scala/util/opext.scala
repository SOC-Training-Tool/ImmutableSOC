package util

import shapeless.ops.{coproduct, hlist}
import shapeless.{:+:, ::, CNil, Coproduct, HList, HNil}

object opext {

  trait CoproductAdd[C <: Coproduct, T] {
    type Out <: Coproduct

    def applyLeft(c: C): Out
    def applyRight(t: T): Out
  }

  trait LowPriorityCoproductAdd {
    type Aux[C <: Coproduct, T, Out0 <: Coproduct] = CoproductAdd[C, T] { type Out = Out0}

    def apply[C <: Coproduct, T](implicit add: CoproductAdd[C, T]): Aux[C, T, add.Out] = add

    implicit def lowPriorityAdd[C <: Coproduct, T](implicit basis: coproduct.Basis[T :+: C, C]): Aux[C, T, T :+: C] = new CoproductAdd[C, T] {
      override type Out = T :+: C

      override def applyLeft(c: C): Out = c.embed[T :+: C]
      override def applyRight(t: T): Out = Coproduct[Out](t)
    }
  }

  object CoproductAdd extends LowPriorityCoproductAdd {

    implicit def add[C <: Coproduct, T](implicit inject: coproduct.Inject[C, T]): Aux[C, T, C] = new CoproductAdd[C, T] {
      override type Out = C

      override def applyLeft(c: C): Out = c
      override def applyRight(t: T): Out = inject.apply(t)
    }
  }

  trait CoproductUnion[C1 <: Coproduct, C2 <: Coproduct] {
    type Out <: Coproduct

    def applyLeft(c: C1): Out
    def applyRight(c: C2): Out
  }

  object CoproductUnion {
    type Aux[C1 <: Coproduct, C2 <: Coproduct, Out0 <: Coproduct] = CoproductUnion[C1, C2] { type Out = Out0 }

    def apply[C1 <: Coproduct, C2 <: Coproduct](implicit un: CoproductUnion[C1, C2]): Aux[C1, C2, un.Out] = un

    implicit def recur[C <: Coproduct, H, T <: Coproduct, Out0 <: Coproduct](implicit add: CoproductAdd.Aux[C, H, Out0], next: CoproductUnion[Out0, T]): Aux[C, H :+: T, next.Out] = new CoproductUnion[C, H :+: T]  {
      type Out = next.Out

      override def applyLeft(c: C): next.Out = next.applyLeft(add.applyLeft(c))
      override def applyRight(c: H :+: T): next.Out = c.eliminate(h => next.applyLeft(add.applyRight(h)), next.applyRight)
    }

    implicit def cnil[C <: Coproduct]: Aux[C, CNil, C] = new CoproductUnion[C, CNil] {
      override type Out = C

      override def applyLeft(c: C): Out = c
      override def applyRight(c: CNil): Out = c.embed[C]
    }
  }

  trait HListDistinct[L <: HList] {type Out <: HList}

  trait LowPriortiyHListDistinct {
    type Aux[L <: HList, Out0 <: HList] = HListDistinct[L] { type Out = Out0 }

    implicit def lowPriorityDistinct[H, T <: HList](implicit next: HListDistinct[T]): Aux[H :: T, H :: next.Out] = new HListDistinct[H :: T] { type Out = H :: next.Out}
  }

  object HListDistinct extends LowPriortiyHListDistinct {

    def apply[L <: HList](implicit d: HListDistinct[L]): Aux[L, d.Out] = d

    implicit def distinct[H, T <: HList](implicit selector: hlist.Selector[T, H], next: HListDistinct[T]): Aux[H :: T, next.Out] = new HListDistinct[H :: T] { type Out = next.Out}

    implicit val hnil: Aux[HNil, HNil] = new HListDistinct[HNil] { type Out = HNil}

  }

  trait CoproductDistinct[L <: Coproduct] {type Out <: Coproduct}

  trait LowPriortiyCoproductDistinct {
    type Aux[L <: Coproduct, Out0 <: Coproduct] = CoproductDistinct[L] { type Out = Out0 }

    implicit def lowPriorityDistinct[H, T <: Coproduct](implicit next: CoproductDistinct[T]): Aux[H :+: T, H :+: next.Out] = new CoproductDistinct[H :+: T] { type Out = H :+: next.Out}
  }

  object CoproductDistinct extends LowPriortiyCoproductDistinct {

    def apply[L <: Coproduct](implicit d: CoproductDistinct[L]): Aux[L, d.Out] = d

    implicit def distinct[H, T <: Coproduct](implicit selector: coproduct.Inject[T, H], next: CoproductDistinct[T]): Aux[H :+: T, next.Out] = new CoproductDistinct[H :+: T] { type Out = next.Out}

    implicit val cnil: Aux[CNil, CNil] = new CoproductDistinct[CNil] { type Out = CNil}
  }


  trait Embedder[Super <: Coproduct, Sub <: Coproduct] {

    def select[T](s: Super)(implicit sel: coproduct.Selector[Sub, T]): Option[T] = {
      deembed(s).flatMap(_.select[T])
    }

    def inject[T](t: T)(implicit inj: coproduct.Inject[Sub, T]) = {
      embed(inj.apply(t))
    }

    def embed(s: Sub): Super

    def deembed(s: Super): Option[Sub]
  }

  object Embedder {

//    implicit def noEmbed[C <: Coproduct] = new Embedder[C, C] {
//      override def embed(s: C): C = s
//
//      override def deembed(s: C): Option[C] = Some(s)
//    }

    implicit def embedder[Super <: Coproduct, Sub <: Coproduct](implicit basis: coproduct.Basis[Super, Sub]) = new Embedder[Super, Sub] {
      override def embed(s: Sub): Super = s.embed[Super]

      override def deembed(s: Super): Option[Sub] = s.deembed[Sub].toOption
    }

  }

  implicit class SubEmbedder[Super <: Coproduct, Sub <: Coproduct](e: Embedder[Super, Sub]) {

    def innerEmbed[C <: Coproduct](implicit b: coproduct.Basis[Sub, C]): Embedder[Super, C] = new Embedder[Super, C] {
      override def embed(s: C): Super = e.embed(s.embed[Sub])

      override def deembed(s: Super): Option[C] = e.deembed(s).flatMap(_.deembed[C].toOption)
    }
  }

}
