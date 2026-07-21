package soc.base

import game.{GameState, NoInput, Slice, StateField}
import soc.base.BaseGame.{BaseEdgeBuilding, BaseVertexBuilding}
import soc.base.DevelopmentCards.DevelopmentCard
import soc.base.state.*
import soc.core.*
import soc.core.ResourceInventories.*
import soc.core.Transactions.*
import soc.core.DevTransactions.*
import soc.core.state.*

import scala.util.NotGiven
import scala.compiletime.{constValue, erasedValue, summonInline}
import scala.deriving.Mirror


trait Extractor[S, In]:
  def extract(state: S): In

object Extractor extends LowPriorityExtractor:

  given [S]: Extractor[S, NoInput.type] with
    def extract(state: S) = NoInput

private trait LowPriorityExtractor:
  inline given derived[S <: Product, In <: Product](using m: Mirror.ProductOf[In]): Extractor[S, In] =
    new Extractor[S, In]:
      def extract(state: S): In =
        m.fromProduct(extractTuple[S, m.MirroredElemTypes](state))

  private inline def extractTuple[S, Elems <: Tuple](state: S): Elems =
    inline erasedValue[Elems] match
      case _: EmptyTuple => EmptyTuple.asInstanceOf[Elems]
      case _: (head *: tail) =>
        (summonInline[Slice[S, head]].get(state).asInstanceOf[head] *: extractTuple[S, tail](state)).asInstanceOf[Elems]

import scala.compiletime.{erasedValue, summonInline}

// 2. Updated Type Class: Capture Delta as an explicit type parameter 'Del'
trait TupleUpdater[T <: Tuple, D]:
  type Out <: Tuple

  def update(t: T, delta: D): Out

object TupleUpdater:

  given headMatches[H <: GameState[H] {type Delta = FieldDelta}, T <: Tuple, D, FieldDelta](using ev: D <:< FieldDelta): TupleUpdater[H *: T, D] with
    type Out = H *: T

    def update(t: H *: T, delta: D): H *: T =
      val head = t.head
      // Safe to cast because 'ev' proves at compile-time that D fits into FieldDelta
      val updatedHead = head.apply(delta.asInstanceOf[head.Delta])
      updatedHead *: t.tail

  // Recursive case stays identical
  given tailMatches[H, T <: Tuple, D](using next: TupleUpdater[T, D]): TupleUpdater[H *: T, D] with
    type Out = H *: next.Out

    def update(t: H *: T, delta: D): H *: next.Out =
      t.head *: next.update(t.tail, delta)

trait BulkTupleUpdater[T <: Tuple, Deltas <: Tuple]:
  type Out <: Tuple

  def updateAll(t: T, deltas: Deltas): Out

object BulkTupleUpdater:
  // Base case: No deltas left to apply
  given emptyDeltas[T <: Tuple]: BulkTupleUpdater[T, EmptyTuple] with
    type Out = T

    def updateAll(t: T, deltas: EmptyTuple): T = t

  // Recursive case: Apply the head delta, then process the remaining tail deltas
  given recursiveDeltas[T <: Tuple, DH, DT <: Tuple](using
                                                     single: TupleUpdater[T, DH],
                                                     bulk: BulkTupleUpdater[single.Out, DT]
                                                    ): BulkTupleUpdater[T, DH *: DT] with
    type Out = bulk.Out

    def updateAll(t: T, deltas: DH *: DT): bulk.Out =
      val updatedOnce = single.update(t, deltas.head)
      bulk.updateAll(updatedOnce, deltas.tail)

trait FieldApplier[S <: Product, F]:
  def apply(state: S, field: F): S

object FieldApplier extends LowPriorityFieldApplier:
  given optional[S <: Product, F](using applier: FieldApplier[S, F]): FieldApplier[S, Option[F]] with
    def apply(state: S, field: Option[F]): S =
      field match
        case Some(value) => applier.apply(state, value)
        case None        => state

  given list[S <: Product, F](using applier: FieldApplier[S, F]): FieldApplier[S, List[F]] with
    def apply(state: S, field: List[F]): S =
      field.foldLeft(state)(applier.apply)

private trait LowPriorityFieldApplier:
  inline given atomic[S <: Product, F](using
      m: Mirror.ProductOf[S],
      updater: TupleUpdater[m.MirroredElemTypes, F]
  ): FieldApplier[S, F] =
    new FieldApplier[S, F]:
      def apply(state: S, field: F): S =
        state.applyDelta(field)(using updater)

trait OutputApplier[S <: Product, D <: Product]:
  def apply(state: S, output: D): S

object OutputApplier:
  inline given derived[S <: Product, D <: Product](using dm: Mirror.ProductOf[D]): OutputApplier[S, D] =
    new OutputApplier[S, D]:
      private val fields = summonInline[OutputFieldsApplier[S, dm.MirroredElemTypes]]

      def apply(state: S, output: D): S =
        fields.apply(state, Tuple.fromProductTyped(output))

private trait OutputFieldsApplier[S <: Product, Fields <: Tuple]:
  def apply(state: S, fields: Fields): S

private object OutputFieldsApplier:
  given empty[S <: Product]: OutputFieldsApplier[S, EmptyTuple] with
    def apply(state: S, fields: EmptyTuple): S = state

  inline given cons[S <: Product, Head, Tail <: Tuple](using
      headApplier: FieldApplier[S, Head],
      tailApplier: OutputFieldsApplier[S, Tail]
  ): OutputFieldsApplier[S, Head *: Tail] with
    def apply(state: S, fields: Head *: Tail): S =
      tailApplier.apply(headApplier.apply(state, fields.head), fields.tail)

// ==========================================
// 4. Extension Methods for State Containers
// ==========================================
extension [T <: Product](t: T)(using m: Mirror.ProductOf[T])
  // Single Delta
  inline def applyDelta[D](delta: D)(using updater: TupleUpdater[m.MirroredElemTypes, D]): T =
    val tupleRep = Tuple.fromProductTyped(t)
    val updated = updater.update(tupleRep.asInstanceOf[m.MirroredElemTypes], delta)
    m.fromProduct(updated).asInstanceOf[T]

  // Tuple of Deltas
  inline def applyDelta[Deltas <: Tuple](deltas: Deltas)(using updater: BulkTupleUpdater[m.MirroredElemTypes, Deltas])(using DummyImplicit): T =
    val tupleRep = Tuple.fromProductTyped(t)
    val updated = updater.updateAll(tupleRep.asInstanceOf[m.MirroredElemTypes], deltas)
    m.fromProduct(updated).asInstanceOf[T]

  // Case Class of Deltas
  inline def applyDeltaProduct[DP <: Product](deltas: DP)(using dm: Mirror.ProductOf[DP], updater: BulkTupleUpdater[m.MirroredElemTypes, dm.MirroredElemTypes]): T =
    val tupleRep = Tuple.fromProductTyped(t)
    val deltaTuple = Tuple.fromProductTyped(deltas)
    val updated = updater.updateAll(
      tupleRep.asInstanceOf[m.MirroredElemTypes],
      deltaTuple.asInstanceOf[dm.MirroredElemTypes]
    )
    m.fromProduct(updated).asInstanceOf[T]

object StateTransformer:

  def update[M, S <: Product, D <: Product](f: (M, S) => D)(using m: Mirror.ProductOf[S])(using dm: Mirror.ProductOf[D], updater: BulkTupleUpdater[m.MirroredElemTypes, dm.MirroredElemTypes]): (M, S) => (D, S) = (move: M, state: S) =>
    val deltas = f(move, state)
    (deltas, state.applyDeltaProduct(deltas))

  inline def updateFlexible[M, S <: Product, D <: Product](f: (M, S) => D)(using applier: OutputApplier[S, D]): (M, S) => (D, S) = (move: M, state: S) =>
    val deltas = f(move, state)
    (deltas, applier.apply(state, deltas))
