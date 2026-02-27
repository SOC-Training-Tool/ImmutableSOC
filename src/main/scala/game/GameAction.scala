package game

import game.Delta.ExtractState
import game.MergeState.Aux
import shapeless.ops.{coproduct, hlist}
import shapeless.ops.hlist.{SelectAll, Union}
import shapeless.{:+:, ::, CNil, Coproduct, HList, HNil, Poly1}
import util.opext.CoproductUnion

class GameAction[MOVE, S, DELTA] (val actions: List[(MOVE, S) => List[DELTA]]) {


  def liftState[S2](implicit lift: Lift[S, S2]): GameAction[MOVE, S2, DELTA] =
    new GameAction[MOVE, S2, DELTA](actions.map(f => Function.untupled(f.tupled.compose[(MOVE, S2)] { case (m, s) => (m, lift.apply(s))} )))

  def liftDelta[D2](implicit lift: Lift[D2, DELTA]): GameAction[MOVE, S, D2] =
    new GameAction[MOVE, S, D2](actions.map(f => Function.untupled(f.tupled.andThen(_.map(lift.apply)))))

  def compose[M](f: M => MOVE): GameAction[M, S, DELTA] = {
    val newActions = actions.map(_.tupled.compose[(M, S)]{case (m, s) => (f(m), s)})
      .map((f_ : ((M, S)) => List[DELTA]) => Function.untupled(f_))
    new GameAction(newActions)
  }

  def composeS[M, S2, Out](f: (M, S2) => MOVE)(implicit merge: MergeState.Aux[S, S2, Out]): GameAction[M, Out, DELTA] = {
    val newActions = actions.map { action =>
      (move: M, state: Out) =>
        val (state1, state2) = merge.split(state)
        action.apply(f(move, state2), state1)
    }
    new GameAction(newActions)
  }

  def map[D2](f: DELTA => D2): GameAction[MOVE, S, D2] = {
    val gas = actions.map { action =>
      (move: MOVE, state: S) =>
        action.apply(move, state).map(f)
    }
    new GameAction(gas)
  }

  def andThen[S2, D2](ga: GameAction[MOVE, S2, D2])(implicit ms: MergeState[S, S2], md: MergeDelta[DELTA, D2]): GameAction[MOVE, ms.Out, md.Out] = {
    val left = actions.map { action =>
      (move: MOVE, state: ms.Out) =>
        val (state1, _) = ms.split(state)
        action(move, state1).map(md.applyLeft)
    }
    val right = ga.actions.map { action =>
      (move: MOVE, state: ms.Out) =>
        val (_, state2) = ms.split(state)
        action(move, state2).map(md.applyRight)
    }
    new GameAction(left ++ right)
  }
}


object GameAction {

  def empty: GameAction[Any, HNil, CNil] = GameAction.apply[Any] (_ => Nil)

  def fromState[Move, S] = FromStateApply[Move, S]()

  def apply[MOVE] = GameActionApply[MOVE]()

  case class FromStateApply[MOVE, S]() {
    def apply[DL](f: (MOVE, S) => List[DL]): GameAction[MOVE, S, DL] =
      new GameAction(List(f))
  }

  case class GameActionApply[MOVE]() {
    def apply[DL](f: MOVE => List[DL]): GameAction[MOVE, HNil, DL] =
      new GameAction(List(Function.untupled(f.compose[(MOVE, HNil)](_._1))))
  }
}

trait MergeState[S1, S2] { self =>
  type Out

  def split(s: Out): (S1, S2)

  def flip: Aux[S2, S1, Out] = new MergeState[S2, S1] {
    override type Out = self.Out

    override def split(s: self.Out): (S2, S1) = self.split(s).swap
  }
}

object MergeState {

  type Aux[S1, S2, Out0] = MergeState[S1, S2] {type Out = Out0}

  def apply[S1, S2](implicit mergeState: MergeState[S1, S2]): Aux[S1, S2, mergeState.Out] = mergeState

  implicit def hlist[S1 <: HList, S2 <: HList, Out0 <: HList](implicit un: Union.Aux[S1, S2, Out0], s1: SelectAll[Out0, S1], s2: SelectAll[Out0, S2]): Aux[S1, S2, Out0] = new MergeState[S1, S2] {
    override type Out = Out0

    override def split(s: Out0): (S1, S2) = (s1.apply(s), s2.apply(s))
  }
}

trait MergeDelta[D1, D2] {
  type Out

  def applyLeft(left: D1): Out
  def applyRight(right: D2): Out
}

object MergeDelta {
  type Aux[D1, D2, Out0] = MergeDelta[D1, D2] { type Out = Out0}

  def apply[D1, D2](implicit m: MergeDelta[D1, D2]): Aux[D1, D2, m.Out] = m

  implicit def coproduct[D1 <: Coproduct, D2 <: Coproduct](implicit un: CoproductUnion[D1, D2]): Aux[D1, D2, un.Out] = new MergeDelta[D1, D2] {
    override type Out = un.Out

    override def applyLeft(left: D1): Out = un.applyLeft(left)
    override def applyRight(right: D2): Out = un.applyRight(right)
  }

//  implicit def cnil[D <: Coproduct]: MergeDelta[D, CNil] = new MergeDelta[D, CNil] {
//    override type Out = D
//
//    override def applyLeft(left: D): Out = left
//    override def applyRight(right: CNil): Out = right.embed[D]
//  }
}

trait Lift[A, B] extends (B => A)

object Lift {

  def apply[A, B](implicit l: Lift[A, B]): Lift[A, B] = l

  implicit def hlist[A <: HList, B <: HList](implicit sa: shapeless.ops.hlist.SelectAll[B, A]): Lift[A, B] = sa.apply
  implicit def coproduct[A <: Coproduct, B <: Coproduct](implicit ba: shapeless.ops.coproduct.Basis[A, B]): Lift[A, B] = _.embed[A]
}

trait DeltaState[S] {
  type Out
}

object DeltaState {
  type Aux[S, Out0] = DeltaState[S] { type Out = Out0 }

  def apply[S](implicit ds: DeltaState[S]): Aux[S, ds.Out] = ds

  implicit def hlist[H, T <: HList, Out0 <: Coproduct](implicit next: Aux[T, Out0]): Aux[H :: T, Delta[H] :+: Out0] = new DeltaState[H :: T] {
    type Out = Delta[H] :+: Out0
  }

  implicit val hnil: Aux[HNil, CNil] = new DeltaState[HNil] { type Out = CNil }


}