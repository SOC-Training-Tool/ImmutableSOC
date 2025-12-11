package soc

import game.PerfectInfoMoveResult
import shapeless.ops.coproduct
import shapeless.{Coproduct, DepFn2, Poly1}

trait MoveResolver[Move, PerfectInfoState] extends DepFn2[Move, PerfectInfoState]

object MoveResolver {

  type Aux[Move, PerfectInfoState, PerfectInfoMoveResult] = MoveResolver[Move, PerfectInfoState] {
    type Out = PerfectInfoMoveResult
  }

  object MoveResolverPoly extends Poly1 {
    implicit def perfectInfoMove[P <: PerfectInfoMoveResult, S]: Case.Aux[(P, S), P] =
      at[(P, S)] { case (move, _) => move }

    implicit def default[M, S](implicit resolver: MoveResolver[M, S]): Case.Aux[(M, S), resolver.Out] =
      at[(M, S)] { case (m, s) => resolver.apply(m, s) }
  }

  implicit def movesCoproduct[Move <: Coproduct, PerfectInfoState, PerfectInfoMoveResult <: Coproduct, ZipOut <: Coproduct, MapOut <: Coproduct]
  (implicit zipConst: coproduct.ZipConst.Aux[PerfectInfoState, Move, ZipOut],
   mapper: coproduct.Mapper.Aux[MoveResolverPoly.type, ZipOut, MapOut],
   align: coproduct.Align[MapOut, PerfectInfoMoveResult]): Aux[Move, PerfectInfoState, PerfectInfoMoveResult] =
    new MoveResolver[Move, PerfectInfoState] {
      override type Out = PerfectInfoMoveResult
      override def apply(t: Move, u: PerfectInfoState): Out = {
        t.zipConst(u).map(MoveResolverPoly).align[PerfectInfoMoveResult]
      }
    }
}