package game

import game.Delta.ExtractState
import game.MergeState.Aux
import shapeless.{:+:, ::, CNil, Coproduct, DepFn2, HList, HNil}
import shapeless.ops.hlist
import shapeless.ops.hlist.{SelectAll, Union}
import util.DependsOn
import util.opext.CoproductUnion

class GameAction[MOVE, S, DELTA] (val actions: List[(MOVE, S) => List[DELTA]]) {

  def compose[M](f: M => MOVE): GameAction[M, S, DELTA] = {
    val newActions = actions.map(_.tupled.compose[(M, S)]{case (m, s) => (f(m), s)}).map(Function.untupled)
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

  def andThen[S2, D2, SOut, DOut](ga: GameAction[MOVE, S2, D2])(implicit ms: MergeState.Aux[S, S2, SOut], md: MergeDelta.Aux[DELTA, D2, DOut]): GameAction[MOVE, SOut, DOut] = {
    val left = actions.map { action =>
      (move: MOVE, state: SOut) =>
        val (state1, _) = ms.split(state)
        action(move, state1).map(md.applyLeft)
    }
    val right = ga.actions.map { action =>
      (move: MOVE, state: SOut) =>
        val (_, state2) = ms.split(state)
        action(move, state2).map(md.applyRight)
    }
    new GameAction(left ++ right)
  }



  //def extend[S2, D2, Out](action: GameAction[MOVE, S2, D2])(implicit m1: MergeState.Aux[S, S2, Out],  m2: MergeState.Aux[S2, S, Out]) = ???

  //
  //  def extend[A, Out](action: A)(implicit extend: ExtendAction.Aux[GameAction[MOVE, S], A, Out]): Out = extend.apply(self, action)
  //
  //  def expose[M, S2 <: HList, A, Out](action: A)(f: MOVE => M)(implicit
  //                                                         ev: A <:< GameAction[M, S2],
  //                                                         extendAction: ExtendAction.Aux[GameAction[MOVE, STATE], GameAction[MOVE, S2], Out]
  //  ): Out = extend(action.compose(f))
  //
  //  def exposeS[M, S1 <: HList, S2 <: HList, A, OutS <: HList, Out](action: A)(f: (MOVE, S1) => M)(implicit
  //    ev: A <:< GameAction[M, S2],
  //    un: Union.Aux[S2, S1, OutS],
  //    s1: SelectAll[OutS, S1],
  //    s2: SelectAll[OutS, S2],
  //    extendAction: ExtendAction.Aux[GameAction[MOVE, STATE], GameAction[MOVE, OutS], Out]
  //  ): Out = extend( action.composeS(f))
  //
  //  def andThen[S2 <: HList, U <: HList](f: S => S2)(implicit un: Union.Aux[S, S2, U], sa: SelectAll[U, S]): GameAction[MOVE, U] = { (_: MOVE, s: U) =>
  //    val state: S = sa.apply(s)
  //    state.union(f.apply(state))
  //  }
}


object GameAction {

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

trait GAction[M] {
  type S
  type DL

  def apply(): List[(M, S) => List[DL]]
}

object GAction {

  type Aux[M, S0, DL0] = GAction[M] {
    type S = S0
    type DL = DL0
  }

  def apply[M](implicit gAction: GAction[M]): Aux[M, gAction.S, gAction.DL] = gAction

  implicit def fromAction[M, SOut, DLOut](implicit gameAction: GameAction[M, SOut, DLOut]): Aux[M, SOut, DLOut] = new GAction[M] {
    override type S = SOut
    override type DL = DLOut

    override def apply(): List[(M, SOut) => List[DLOut]] = gameAction.actions
  }
}

trait CollectActionState[MOVES <: Coproduct] {
  type Out <: HList
}

object CollectActionState {

  type Aux[MOVES <: Coproduct, S <: HList] = CollectActionState[MOVES] {
    type Out = S
  }

  def apply[MOVES <: Coproduct](implicit c: CollectActionState[MOVES]): Aux[MOVES, c.Out] = c

  implicit def recur[M, S <: HList, D <: Coproduct, T <: Coproduct, DS <: HList, UOut1 <: HList, UOut2 <: HList](implicit gaAction: GAction.Aux[M, S, D], next: CollectActionState.Aux[T, UOut1], extract: ExtractState.Aux[D, DS], un1: hlist.Union.Aux[S, DS, UOut2], un2: hlist.Union[UOut1, UOut2]): Aux[M :+: T, un2.Out] = new CollectActionState[M :+: T] {
    type Out = un2.Out
  }

  implicit val cnil: Aux[CNil, HNil] = new CollectActionState[CNil] {
    type Out = HNil
  }
}

trait MergeState[S1, S2] {
  type Out

  def split(s: Out): (S1, S2)
}

object MergeState {

  type Aux[S1, S2, Out0] = MergeState[S1, S2] {type Out = Out0}

  def apply[S1, S2](implicit mergeState: MergeState[S1, S2]): Aux[S1, S2, mergeState.Out] = mergeState

  implicit def hlist[S1 <: HList, S2 <: HList, Out0 <: HList](implicit un: Union.Aux[S1, S2, Out0], s1: SelectAll[Out0, S1], s2: SelectAll[Out0, S2]): Aux[S1, S2, Out0] = new MergeState[S1, S2] {
    override type Out = Out0

    override def split(s: Out0): (S1, S2) = (s1.apply(s), s2.apply(s))
  }

  implicit def flip[S1, S2](implicit merge: MergeState[S1, S2]): MergeState.Aux[S2, S1, merge.Out] = new MergeState[S2, S1] {
    override type Out = merge.Out

    override def split(s: merge.Out): (S2, S1) = merge.split(s).swap
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
}

trait FullState[A] {
  type Out
}

object FullState {
  type Aux[A, Out0] = FullState[A] { type Out = Out0}

  def apply[A](implicit fs: FullState[A]): Aux[A, fs.Out] = fs

  implicit def gameAction[M, S <: HList, DL <: Coproduct, DOut <: HList](implicit extractState: ExtractState.Aux[DL, DOut], un: Union[S, DOut]): Aux[GameAction[M, S, DL], un.Out] = new FullState[GameAction[M, S, DL]] {
    type Out = un.Out
  }


}



//trait ExtendAction[A1, A2] extends DepFn2[A1, A2]
//
//object ExtendAction {
//
//  type Aux[A, B, Out0] = ExtendAction[A, B] { type Out = Out0 }
//
//  def apply[A, B](implicit ex: ExtendAction[A, B]): Aux[A, B, ex.Out] = ex
//
//  implicit def extend[M1, S1 <: HList, A1, M2, S2 <: HList, A2, S <: HList](implicit
//    ev: A1 <:< GameAction[M1, S1],
//    ev2: A2 <:< GameAction[M2, S2],
//    ev3: M1 <:< M2,
//    un: hlist.Union.Aux[S1, S2, S],
//    dep1: DependsOn[S, S1],
//    dep2: DependsOn[S, S2],
//   ): Aux[A1, A2, GameAction[M1, S]] = new ExtendAction[A1, A2] {
//    override type Out = GameAction[M1, S]
//
//    override def apply(self: A1, action: A2): Out = { (move: M1, state: S) =>
//      val s1 = dep1.updateAll(state)(self.apply(move, _))
//      dep2.updateAll(s1)(action.apply(ev3.apply(move), _))
//    }
//  }
//}

