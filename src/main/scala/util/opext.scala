package util

import shapeless.Coproduct
import shapeless.ops.coproduct

object opext {

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
