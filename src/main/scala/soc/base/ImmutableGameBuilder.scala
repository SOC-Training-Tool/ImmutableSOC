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

type OutputUnion[T <: Tuple] = T match
  case (m, o) *: tl => o | OutputUnion[tl]
  case EmptyTuple => Nothing

private trait RegisteredActionBase[State <: Product]:
  def applyIfMatches(move: Any, state: State): Option[(Any, State)]

private final case class RegisteredAction[State <: Product, Move, In, Out <: Product](
    action: GameAction[Move, In, Out],
    classTag: ClassTag[Move],
    extractor: Extractor[State, In],
    outputApplier: OutputApplier[State, Out]
) extends RegisteredActionBase[State]:
  def applyIfMatches(move: Any, state: State): Option[(Any, State)] =
    classTag.unapply(move).map: typedMove =>
      val output = action(typedMove, extractor.extract(state))
      (output, outputApplier(state, output))

final class ImmutableGameBuilder[State <: Product, Registry <: Tuple] private (
    private val actions: List[RegisteredActionBase[State]]
):
  def register[Move, In, Out <: Product](action: GameAction[Move, In, Out])(using
      classTag: ClassTag[Move],
      extractor: Extractor[State, In],
      outputApplier: OutputApplier[State, Out]
  ): ImmutableGameBuilder[State, (Move, Out) *: Registry] =
    val registered = RegisteredAction(action, classTag, extractor, outputApplier)
    new ImmutableGameBuilder(registered :: actions)

  def build: ImmutableGame[MoveUnion[Registry], State] {
    type OutFor[M <: MoveUnion[Registry]] = ComputeOut[M, Registry]
    type AllOutputs = OutputUnion[Registry]
  } =
    val registeredActions = actions

    new ImmutableGame[MoveUnion[Registry], State] {
      type OutFor[M <: MoveUnion[Registry]] = ComputeOut[M, Registry]
      type AllOutputs = OutputUnion[Registry]

      private def dispatch(move: Any, state: State): (Any, State) =
        registeredActions.iterator
          .map(_.applyIfMatches(move, state))
          .collectFirst { case Some(result) => result }
          .getOrElse(throw new IllegalArgumentException(s"No action registered for move: $move"))

      def applyMove[M <: MoveUnion[Registry]](move: M, state: State)(using classTag: ClassTag[M]): (OutFor[M], State) =
        val (output, newState) = dispatch(move, state)
        (output.asInstanceOf[OutFor[M]], newState)

      def applyMoveAny(move: MoveUnion[Registry], state: State): (AllOutputs, State) =
        val (output, newState) = dispatch(move, state)
        (output.asInstanceOf[AllOutputs], newState)
    }

object ImmutableGameBuilder:
  def apply[State <: Product]: ImmutableGameBuilder[State, EmptyTuple] =
    new ImmutableGameBuilder(Nil)
