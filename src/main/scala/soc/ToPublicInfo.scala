package soc

import game.{:+:, CNil, Inl, Inr, PerfectInfoMoveResult, CoproductBasis}

trait ToPublicInfo[Public, Perfect]:
  def apply(perfect: Perfect): Public

object ToPublicInfo:

  // Base case
  given cnilToPublic: ToPublicInfo[CNil, CNil] with
    def apply(perfect: CNil): CNil = perfect.impossible

  // Helper to extract PublicInfoMoveResult from PerfectInfoMoveResult
  private def toPublic[P <: PerfectInfoMoveResult](p: P): p.PublicInfoMoveResult =
    p.getPerspectiveResults(Seq(-1)).head._2.asInstanceOf[p.PublicInfoMoveResult]

  // Recursive case for PerfectInfoMoveResult types
  given perfectInfoConverter[H <: PerfectInfoMoveResult, T, PublicT](using
    tailConverter: ToPublicInfo[PublicT, T]
  ): ToPublicInfo[H#PublicInfoMoveResult :+: PublicT, H :+: T] with
    def apply(perfect: H :+: T): H#PublicInfoMoveResult :+: PublicT = perfect match
      case Inl(h) => Inl(toPublic(h))
      case Inr(t) => Inr(tailConverter(t))

  // Lower priority: custom converter
  // Scala 3 will choose perfectInfoConverter over this when H <: PerfectInfoMoveResult
  given defaultConverter[H, T, PublicH, PublicT](using
    hConverter: ToPublicInfo[PublicH, H],
    tailConverter: ToPublicInfo[PublicT, T]
  ): ToPublicInfo[PublicH :+: PublicT, H :+: T] with
    def apply(perfect: H :+: T): PublicH :+: PublicT = perfect match
      case Inl(h) => Inl(hConverter(h))
      case Inr(t) => Inr(tailConverter(t))

  // Final embedding into target coproduct
  given embedConverter[Public, Perfect, Intermediate](using
    converter: ToPublicInfo[Intermediate, Perfect],
    basis: CoproductBasis[Public, Intermediate]
  ): ToPublicInfo[Public, Perfect] with
    def apply(perfect: Perfect): Public = basis.embed(converter(perfect))
