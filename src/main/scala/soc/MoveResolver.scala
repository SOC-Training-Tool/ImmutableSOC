package soc

import game.{:+:, CNil, Inl, Inr, PerfectInfoMoveResult}

trait MoveResolver[Move, PerfectInfoState]:
  type Out
  def apply(move: Move, state: PerfectInfoState): Out

object MoveResolver:
  type Aux[Move, PerfectInfoState, Out0] = MoveResolver[Move, PerfectInfoState] { type Out = Out0 }

  // Base case: CNil
  given cnilResolver[S]: MoveResolver[CNil, S] with
    type Out = CNil
    def apply(move: CNil, state: S): CNil = move.impossible

  // Recursive case: H is already a PerfectInfoMoveResult
  given perfectInfoMove[H <: PerfectInfoMoveResult, T, S, TailOut](using
    tailResolver: MoveResolver[T, S] { type Out = TailOut }
  ): MoveResolver[H :+: T, S] with
    type Out = H :+: TailOut
    def apply(move: H :+: T, state: S): H :+: TailOut = move match
      case Inl(h) => Inl(h)
      case Inr(t) => Inr(tailResolver(t, state))

  // Lower priority: H needs resolution via MoveResolver instance
  // Scala 3 will choose perfectInfoMove over this when H <: PerfectInfoMoveResult
  given defaultResolver[H, T, S, HOut, TailOut](using
    hResolver: MoveResolver[H, S] { type Out = HOut },
    tailResolver: MoveResolver[T, S] { type Out = TailOut }
  ): MoveResolver[H :+: T, S] with
    type Out = HOut :+: TailOut
    def apply(move: H :+: T, state: S): HOut :+: TailOut = move match
      case Inl(h) => Inl(hResolver(h, state))
      case Inr(t) => Inr(tailResolver(t, state))