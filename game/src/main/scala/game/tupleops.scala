package game

import scala.compiletime.*
import scala.compiletime.ops.int.*

// Match types for Tuple-level operations

object TupleOps:

  /** Check if type E exists in tuple T */
  type Contains[T <: Tuple, E] <: Boolean = T match
    case E *: _       => true
    case _ *: t       => Contains[t, E]
    case EmptyTuple   => false

  /** Find the index of type E in tuple T */
  type IndexOf[T <: Tuple, E] <: Int = T match
    case E *: _  => 0
    case _ *: t  => S[IndexOf[t, E]]

  /** Deduplicate types in a Tuple (preserves first occurrence order) */
  type Distinct[T <: Tuple] <: Tuple = T match
    case EmptyTuple   => EmptyTuple
    case h *: t       => DistinctAdd[Distinct[t], h]

  /** Add type H to Tuple T if not already present (prepend) */
  type DistinctAdd[T <: Tuple, H] <: Tuple = Contains[T, H] match
    case true  => T
    case false => H *: T

  /** Merge two tuples with deduplication (all of T1 + unique elements of T2) */
  type Union[T1 <: Tuple, T2 <: Tuple] <: Tuple = T2 match
    case EmptyTuple   => T1
    case h *: t       => Union[DistinctAdd[T1, h], t]

  /** Compute DeltaOf: map each Tuple element T to Delta[T] in a Coproduct */
  type DeltaOf[S <: Tuple] = S match
    case h *: t       => Delta[h] :+: DeltaOf[t]
    case EmptyTuple   => CNil

end TupleOps


// Value-level Tuple operations

/** Select an element by type from a Tuple */
inline def tupleSelect[T <: Tuple, E](t: T): E =
  t.productElement(constValue[TupleOps.IndexOf[T, E]]).asInstanceOf[E]

/** Modify an element by type in a Tuple, returning the updated Tuple */
inline def tupleUpdate[T <: Tuple, E](t: T, value: E): T =
  val idx = constValue[TupleOps.IndexOf[T, E]]
  val arr = t.toArray
  arr(idx) = value.asInstanceOf[Object]
  Tuple.fromArray(arr).asInstanceOf[T]

/** Extension method for .select[T] syntax on Tuples (replaces shapeless HList.select) */
extension [T <: Tuple](t: T)
  inline def select[E]: E = tupleSelect[T, E](t)

/** Modify an element by type in a Tuple via a function */
inline def tupleModify[T <: Tuple, E](t: T, f: E => E): T =
  val idx = constValue[TupleOps.IndexOf[T, E]]
  val arr = t.toArray
  arr(idx) = f(arr(idx).asInstanceOf[E]).asInstanceOf[Object]
  Tuple.fromArray(arr).asInstanceOf[T]


// --- Given-based type classes (for use where inline is not possible) ---

/** Find the index of type E in tuple T (given-resolved, not inline) */
trait TupleIndex[T <: Tuple, E]:
  val index: Int

object TupleIndex:
  given headIndex[E, T <: Tuple]: TupleIndex[E *: T, E] with
    val index = 0

  given tailIndex[H, T <: Tuple, E](using tail: TupleIndex[T, E]): TupleIndex[H *: T, E] with
    val index = tail.index + 1


/** Select all elements of Target from Source tuple (replaces shapeless SelectAll) */
trait TupleSelectAll[Source <: Tuple, Target <: Tuple]:
  def apply(s: Source): Target

object TupleSelectAll:
  given [S <: Tuple]: TupleSelectAll[S, EmptyTuple] with
    def apply(s: S): EmptyTuple = EmptyTuple

  given [S <: Tuple, H, T <: Tuple](using
    headIdx: TupleIndex[S, H],
    tail: TupleSelectAll[S, T]
  ): TupleSelectAll[S, H *: T] with
    def apply(s: S): H *: T =
      s.productElement(headIdx.index).asInstanceOf[H] *: tail(s)


/** Modify an element by type in a Tuple (given-based, replaces shapeless Modifier) */
trait TupleModifier[T <: Tuple, E]:
  def apply(t: T, f: E => E): T

object TupleModifier:
  given headModifier[E, T <: Tuple]: TupleModifier[E *: T, E] with
    def apply(t: E *: T, f: E => E): E *: T =
      f(t.head) *: t.tail

  given tailModifier[H, T <: Tuple, E](using tail: TupleModifier[T, E]): TupleModifier[H *: T, E] with
    def apply(t: H *: T, f: E => E): H *: T =
      t.head *: tail(t.tail, f)


/** Extract a subset tuple from a larger tuple (replaces shapeless RemoveAll) */
trait TupleRemoveAll[S <: Tuple, Sub <: Tuple]:
  type Remainder <: Tuple
  def apply(s: S): (Sub, Remainder)
  def reinsert(pair: (Sub, Remainder)): S

object TupleRemoveAll:
  type Aux[S <: Tuple, Sub <: Tuple, R <: Tuple] = TupleRemoveAll[S, Sub] { type Remainder = R }

  given empty[S <: Tuple]: TupleRemoveAll[S, EmptyTuple] with
    type Remainder = S
    def apply(s: S): (EmptyTuple, S) = (EmptyTuple, s)
    def reinsert(pair: (EmptyTuple, S)): S = pair._2

  given cons[S <: Tuple, H, T <: Tuple](using
    headIdx: TupleIndex[S, H],
    tailRemove: TupleRemoveAll[S, T]
  ): TupleRemoveAll[S, H *: T] with
    type Remainder = tailRemove.Remainder
    def apply(s: S): (H *: T, tailRemove.Remainder) =
      val (sub, rem) = tailRemove.apply(s)
      val h = s.productElement(headIdx.index).asInstanceOf[H]
      (h *: sub, rem)
    def reinsert(pair: (H *: T, tailRemove.Remainder)): S =
      val (sub, rem) = pair
      val base = tailRemove.reinsert((sub.tail, rem))
      val arr = base.toArray
      arr(headIdx.index) = sub.head.asInstanceOf[Object]
      Tuple.fromArray(arr).asInstanceOf[S]


/** Fill a Tuple with default values (replaces shapeless FillWith + Poly0) */
trait TupleFill[T <: Tuple]:
  def apply(): T

object TupleFill:
  given TupleFill[EmptyTuple] with
    def apply(): EmptyTuple = EmptyTuple

  given [H, T <: Tuple](using headDefault: DefaultValue[H], tailFill: TupleFill[T]): TupleFill[H *: T] with
    def apply(): H *: T = headDefault.value *: tailFill()

/** Type class for default values (replaces InitializeOp cases) */
trait DefaultValue[T]:
  def value: T

object DefaultValue:
  given DefaultValue[Int] with
    def value: Int = 0
  given DefaultValue[Double] with
    def value: Double = 0.0
  given DefaultValue[String] with
    def value: String = ""
  given [A]: DefaultValue[List[A]] with
    def value: List[A] = List.empty
  given [K, V]: DefaultValue[Map[K, V]] with
    def value: Map[K, V] = Map.empty
  given [A]: DefaultValue[Option[A]] with
    def value: Option[A] = None
