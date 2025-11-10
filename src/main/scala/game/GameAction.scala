package game

import shapeless.{DepFn2, HList}
import shapeless.ops.hlist
import shapeless.ops.hlist.{SelectAll, Union}
import util.DependsOn

trait GameAction[MOVE, S <: HList] extends ((MOVE, S) => S) { self =>

  type STATE = S

  //def apply(move: MOVE, state: S): S

  //def apply(move: MOVE, pre: S, state: S): S = apply(move, state)

  def composeS[M, S2 <: HList, Out <: HList](f: (M, S2) => MOVE)(implicit un: Union.Aux[S, S2, Out], s1: SelectAll[Out, S], s2: SelectAll[Out, S2]): GameAction[M, Out] = { (move: M, state: Out) =>
    val state1: S = s1.apply(state)
    val state2: S2 = s2.apply(state)
    val result = self.apply(f(move, state2), state1)
    un.apply(result, state2)
  }

  def compose[M](f: M => MOVE): GameAction[M, S] = (move: M, state: S) => self.apply(f(move), state)

  def extend[A, Out](action: A)(implicit extend: ExtendAction.Aux[GameAction[MOVE, S], A, Out]): Out = extend.apply(self, action)

  def expose[M, S2 <: HList, A, Out](action: A)(f: MOVE => M)(implicit
                                                         ev: A <:< GameAction[M, S2],
                                                         extendAction: ExtendAction.Aux[GameAction[MOVE, STATE], GameAction[MOVE, S2], Out]
  ): Out = extend(action.compose(f))

  def exposeS[M, S1 <: HList, S2 <: HList, A, OutS <: HList, Out](action: A)(f: (MOVE, S1) => M)(implicit
    ev: A <:< GameAction[M, S2],
    un: Union.Aux[S2, S1, OutS],
    s1: SelectAll[OutS, S1],
    s2: SelectAll[OutS, S2],
    extendAction: ExtendAction.Aux[GameAction[MOVE, STATE], GameAction[MOVE, OutS], Out]
  ): Out = extend( action.composeS(f))

  def andThen[S2 <: HList, U <: HList](f: S => S2)(implicit un: Union.Aux[S, S2, U], sa: SelectAll[U, S]): GameAction[MOVE, U] = { (_: MOVE, s: U) =>
    val state: S = sa.apply(s)
    state.union(f.apply(state))
  }
}

trait GAction[M] {
  type S <: HList

  def apply(m: M, s: S): S
}

object GameAction {

  def apply[MOVE, S <: HList](f: (MOVE, S) => S): GameAction[MOVE, S] = (move: MOVE, state: S) => f(move, state)
}

object GAction {

  type Aux[M, S0 <: HList] = GAction[M] { type S = S0 }

  def apply[M](implicit gAction: GAction[M]): Aux[M, gAction.S] = gAction

  implicit def fromAction[M, SOut <: HList](implicit gameAction: GameAction[M, SOut]): Aux[M, SOut] = new GAction[M] {
    override type S = SOut

    override def apply(m: M, s: S): S = gameAction.apply(m, s)
  }
}

trait ExtendAction[A1, A2] extends DepFn2[A1, A2]

object ExtendAction {

  type Aux[A, B, Out0] = ExtendAction[A, B] { type Out = Out0 }

  def apply[A, B](implicit ex: ExtendAction[A, B]): Aux[A, B, ex.Out] = ex

  implicit def extend[M1, S1 <: HList, A1, M2, S2 <: HList, A2, S <: HList](implicit
    ev: A1 <:< GameAction[M1, S1],
    ev2: A2 <:< GameAction[M2, S2],
    ev3: M1 <:< M2,
    un: hlist.Union.Aux[S1, S2, S],
    dep1: DependsOn[S, S1],
    dep2: DependsOn[S, S2],
   ): Aux[A1, A2, GameAction[M1, S]] = new ExtendAction[A1, A2] {
    override type Out = GameAction[M1, S]

    override def apply(self: A1, action: A2): Out = { (move: M1, state: S) =>
      val s1 = dep1.updateAll(state)(self.apply(move, _))
      dep2.updateAll(s1)(action.apply(ev3.apply(move), _))
    }
  }
}

