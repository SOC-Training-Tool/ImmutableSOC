package game

import scala.reflect.{ClassTag, TypeTest}

/** Union of Delta[D_i] for each state type D_i in tuple D.
 *
 *  DeltaOf[(Bank[Resource], PlayerPoints)] =
 *    Delta[Bank[Resource]] | Delta[PlayerPoints]
 *
 *  Used as the public return type of apply/applyFull; internally
 *  List[Any] is used to avoid Scala 3 wildcard-LUB inference for
 *  invariant Delta[_].
 */
type DeltaOf[D <: Tuple] <: Any = D match
  case EmptyTuple => Nothing
  case h *: t     => Delta[h] | DeltaOf[t]

/** Generic delta wrapper for a state component S.
 *
 *  The ClassTag stored at construction enables runtime type discrimination
 *  in pattern matches via the TypeTest given below.
 */
case class Delta[S <: GameState[?]](rawValue: Any)(using val ct: ClassTag[S])

object Delta:
  /** TypeTest enabling `case d: Delta[T]` pattern matching from any scrutinee type. */
  given [T <: GameState[?] : ClassTag]: TypeTest[Any, Delta[T]] with
    def unapply(x: Any): Option[x.type & Delta[T]] = x match
      case d: Delta[?] if d.ct.runtimeClass == summon[ClassTag[T]].runtimeClass =>
        Some(d.asInstanceOf[x.type & Delta[T]])
      case _ => None

/** Applies any delta value (typed as Any) to a Full state by finding the
 *  matching D_i component via ClassTag and Slice.  Derived by induction on D.
 *
 *  H is constrained to GameState[H] (self-referential) so that
 *  `cur.apply(...)` is known to return H, avoiding path-dependent-type errors.
 */
trait ApplyDeltasTo[Full, D <: Tuple]:
  def applyAny(full: Full, delta: Any): Full

object ApplyDeltasTo:

  given [Full]: ApplyDeltasTo[Full, EmptyTuple] with
    def applyAny(full: Full, delta: Any): Full = full  // nothing left to match

  given [Full, H <: GameState[H] : ClassTag, T <: Tuple](
    using hSlice: Slice[Full, H],
    tApply: ApplyDeltasTo[Full, T]
  ): ApplyDeltasTo[Full, H *: T] with
    def applyAny(full: Full, delta: Any): Full = delta match
      case d: Delta[?] if d.ct.runtimeClass == summon[ClassTag[H]].runtimeClass =>
        val cur = hSlice.get(full)
        hSlice.set(full, cur(d.rawValue.asInstanceOf[cur.Delta]))
      case _ => tApply.applyAny(full, delta)

// ── ActionState helpers ────────────────────────────────────────────────────

/** Remove the first occurrence of type T from tuple Ts. */
private type Remove1[T, Ts <: Tuple] <: Tuple = Ts match
  case EmptyTuple => EmptyTuple
  case T *: rest  => rest
  case h *: rest  => h *: Remove1[T, rest]

/** Remove from From every type that appears in ToRemove (one occurrence each). */
private type TupleDiff[From <: Tuple, ToRemove <: Tuple] <: Tuple = ToRemove match
  case EmptyTuple => From
  case h *: t     => TupleDiff[Remove1[h, From], t]

/** Convert S to a Tuple:
 *   Unit / EmptyTuple → EmptyTuple
 *   h *: t            → h *: t  (already a tuple)
 *   A (single type)   → A *: EmptyTuple
 */
private type Lift[S] <: Tuple = S match
  case Unit       => EmptyTuple
  case EmptyTuple => EmptyTuple
  case h *: t     => h *: t
  case _          => S *: EmptyTuple

/** The minimal combined tuple for a GameAction[M, S, D]:
 *   - all fields of S (as a tuple), followed by
 *   - all fields of D not already present in S.
 *
 *  Example: S = (Board, SOCRoadLengths, VBS), D = (VBS, Bank, SOCRoadLengths)
 *  → ActionState = (Board, SOCRoadLengths, VBS, Bank)
 */
type ActionState[S, D <: Tuple] = Tuple.Concat[Lift[S], TupleDiff[D, Lift[S]]]

/** A typed game action.
 *
 *  @tparam M  Move type
 *  @tparam S  State slice the action reads (Unit, a single type, or a tuple)
 *  @tparam D  Tuple of state types whose Deltas this action may produce
 *             e.g. (Bank[Resource], PlayerPoints) means the action produces
 *             Delta[Bank[Resource]] and/or Delta[PlayerPoints] values.
 *
 *  Internally, run stores List[Any] to sidestep Scala 3's wildcard-LUB
 *  inference for invariant Delta[_] in list literals.  The public API
 *  casts back to List[DeltaOf[D]] for type-safe consumption.
 */
class GameAction[M, S, Out]:

  /** Run the action, returning only the deltas (state not modified). */
  def apply(move: M, input: S): Out 
   

//  /** Run the action against a full state: extracts S, produces deltas,
//   *  applies each delta back to Full via ApplyDeltasTo, returns updated Full + deltas.
//   */
//  def applyFull[Full](move: M, full: Full)(
//    using sSlice: Slice[Full, S],
//    applyD: ApplyDeltasTo[Full, D]
//  ): (Full, List[DeltaOf[D]]) =
//    val rawDeltas = run(move, sSlice.get(full))
//    val newFull   = rawDeltas.foldLeft(full)(applyD.applyAny)
//    (newFull, rawDeltas.asInstanceOf[List[DeltaOf[D]]])
//
//  /** Run the action against the minimal combined state ActionState[S, D] —
//   *  the deduplicated union of S's fields and D's state component types.
//   *
//   *  Unlike applyFull (which requires a Full game state), this works with
//   *  exactly the fields the action needs, nothing more.
//   *
//   *  The compiler derives Slice[ActionState[S,D], S] and
//   *  ApplyDeltasTo[ActionState[S,D], D] automatically from the concrete types.
//   */
//  def apply_(move: M, state: ActionState[S, D])(
//    using sSlice: Slice[ActionState[S, D], S],
//    applyD: ApplyDeltasTo[ActionState[S, D], D]
//  ): (ActionState[S, D], List[DeltaOf[D]]) =
//    val rawDeltas = run(move, sSlice.get(state))
//    val newState  = rawDeltas.foldLeft(state)(applyD.applyAny)
//    (newState, rawDeltas.asInstanceOf[List[DeltaOf[D]]])
//
//  /** Sequence two actions: state becomes a pair, D becomes Tuple.Concat[D, D2]. */
//  def andThen[S2, D2 <: Tuple](other: GameAction[M, S2, D2]): GameAction[M, (S, S2), Tuple.Concat[D, D2]] =
//    new GameAction[M, (S, S2), Tuple.Concat[D, D2]]((m, s) =>
//      run(m, s._1) ++ other.run(m, s._2)
//    )
//
//  /** Transform the move type. */
//  def compose[M2](f: M2 => M): GameAction[M2, S, D] =
//    new GameAction((m2, s) => run(f(m2), s))
//
//  /** Transform the move type using additional state. */
//  def composeS[M2, S2](f: (M2, S2) => M): GameAction[M2, (S, S2), D] =
//    new GameAction((m2, s) => run(f(m2, s._2), s._1))

object GameAction:

  /** Create a stateless action (no state needed). */
  def apply[M]: GameActionApply[M] = GameActionApply[M]()

  /** Create a stateful action. */
  def fromState[M, S]: FromStateApply[M, S] = FromStateApply[M, S]()

  final class GameActionApply[M]:
    def apply[D <: Tuple](f: M => List[Any]): GameAction[M, Unit, D] =
      new GameAction((m, _) => f(m))

  final class FromStateApply[M, S]:
    def apply[D <: Tuple](f: (M, S) => List[Any]): GameAction[M, S, D] =
      new GameAction(f)
