package soc

import game.PerfectInfoMoveResult
import shapeless.{Coproduct, Poly1}
import shapeless.ops.coproduct

trait ToPublicInfo[Public, Perfect] extends shapeless.DepFn1[Perfect] {
  override type Out = Public
}

object ToPublicInfo {

  private object PerfectToImperfectMovesPoly extends Poly1 {
    implicit def toPublic[P <: PerfectInfoMoveResult]: PerfectToImperfectMovesPoly.Case.Aux[P, P#PublicInfoMoveResult] = at[P](_.getPerspectiveResults(Seq(-1)).head._2)

    implicit def default[Perfect, Imperfect](implicit toPublic: ToPublicInfo[Imperfect, Perfect]): Case.Aux[Perfect, Imperfect] = at[Perfect](toPublic.apply)

  }

  implicit def movesCoproduct[PublicInfoMoves <: Coproduct, PerfectInfoMoves <: Coproduct, MapOut <: Coproduct]
  (implicit mapper: coproduct.Mapper.Aux[PerfectToImperfectMovesPoly.type, PerfectInfoMoves, MapOut],
   basis: coproduct.Basis[PublicInfoMoves, MapOut]
  ): ToPublicInfo[PublicInfoMoves, PerfectInfoMoves] =
    new ToPublicInfo[PublicInfoMoves, PerfectInfoMoves] {
      override def apply(perfectInfoMove: PerfectInfoMoves): PublicInfoMoves = {
        val outCoproduct: MapOut = perfectInfoMove.map(PerfectToImperfectMovesPoly)
        outCoproduct.embed[PublicInfoMoves]
      }
    }
}
