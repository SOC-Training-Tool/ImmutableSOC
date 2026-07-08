package soc.base

import game.{GameAction, ImmutableGame}
import scala.reflect.ClassTag

type MoveUnion[T <: Tuple] = T match
  case (m, ?) *: tl => m | MoveUnion[tl]
  case EmptyTuple => Nothing

type ComputeOut[M, T <: Tuple] = T match
  case (M, o) *: _ => o
  case _ *: tl => ComputeOut[M, tl]
  case EmptyTuple => Nothing

object ImmutableGameBuilder:
  def apply[S]: ImmutableGameBuilder[S, EmptyTuple] =
    new ImmutableGameBuilder(Vector.empty)

class ImmutableGameBuilder[S, Regs <: Tuple] private (
  handlers: Vector[(Class[?], (Any, S) => (Any, S))]
):

  def register[M, In, Out](action: GameAction[M, In, Out])(
    using ct: ClassTag[M], ext: Extractor[S, In], upd: Updater[S, Out]
  ): ImmutableGameBuilder[S, (M, Out) *: Regs] =
    val cls = ct.runtimeClass
    val handler: (Any, S) => (Any, S) = (rawMove, state) =>
      val m = rawMove.asInstanceOf[M]
      val input = ext.extract(state)
      val output = action(m, input)
      val newState = upd.update(state, output)
      (output, newState)
    new ImmutableGameBuilder(handlers :+ (cls, handler))

  def build: ImmutableGame[MoveUnion[Regs], S] =
    new ImmutableGameImpl[S, Regs](handlers)

private class ImmutableGameImpl[S, Regs <: Tuple](
  handlers: Vector[(Class[?], (Any, S) => (Any, S))]
) extends ImmutableGame[MoveUnion[Regs], S]:
  type OutFor[M <: MoveUnion[Regs]] = ComputeOut[M, Regs]

  def applyMove[M <: MoveUnion[Regs]](move: M, state: S)(using ct: ClassTag[M]): (OutFor[M], S) =
    val cls = ct.runtimeClass
    handlers.find(_._1 == cls) match
      case Some((_, handler)) =>
        val (output, newState) = handler(move, state)
        (output.asInstanceOf[OutFor[M]], newState)
      case None =>
        throw new IllegalArgumentException(
          s"No handler registered for move type: $cls (move: $move)"
        )