package game

import scala.compiletime.constValue
import scala.compiletime.ops.{int as intOps}
import scala.deriving.Mirror

/**
 * Type-level index of `T` in the tuple type `Ts`.
 * Reduces to a singleton `Int` literal at compile time.
 * Leaves the match stuck (compile error) if `T` is not present.
 */
private type IndexOf[T, Ts <: Tuple] <: Int = Ts match
  case T *: ?  => 0
  case ? *: ts => intOps.+[1, IndexOf[T, ts]]

trait Slice[Full, Part]:
  def get(full: Full): Part
  def set(full: Full, part: Part): Full

object Slice extends LowPrioritySlice:

  /** Slice for the empty tuple: get returns EmptyTuple, set is a no-op. */
  given [F]: Slice[F, EmptyTuple] with
    def get(f: F): EmptyTuple  = EmptyTuple
    def set(f: F, t: EmptyTuple): F = f

  /** Slice for H *: T tuples: recurse on head and tail. Handles all arities. */
  given [F, H, T <: Tuple](using sh: Slice[F, H], st: Slice[F, T]): Slice[F, H *: T] with
    def get(f: F): H *: T        = sh.get(f) *: st.get(f)
    def set(f: F, t: H *: T): F  = st.set(sh.set(f, t.head), t.tail)

  given [F]: Slice[F, Unit] with
    def get(f: F)          = ()
    def set(f: F, u: Unit) = f

/** Lower priority than the specific tuple/unit givens above. */
private trait LowPrioritySlice:

  /**
   * Derive `Slice[Full, A]` for any field type `A` of a case class `Full`.
   * The field index is resolved via the `IndexOf` match type at compile time,
   * then passed to a non-inline factory to avoid anonymous-class duplication.
   *
   * Composition is automatic: `Slice[Full, (A, B)]` is resolved by
   * `tupleSlice` using two derived `Slice[Full, A]` / `Slice[Full, B]`.
   */
  inline given productFieldSlice[Full <: Product, A](
    using m: Mirror.ProductOf[Full]
  ): Slice[Full, A] =
    makeFieldSlice[Full, A](constValue[IndexOf[A, m.MirroredElemTypes]], m)

  private def makeFieldSlice[Full <: Product, A](idx: Int, m: Mirror.ProductOf[Full]): Slice[Full, A] =
    new Slice[Full, A]:
      def get(full: Full): A =
        full.productElement(idx).asInstanceOf[A]

      def set(full: Full, a: A): Full =
        val arr = full.productIterator.toArray
        arr(idx) = a.asInstanceOf[Any]
        m.fromProduct(new Product:
          def productArity           = arr.length
          def productElement(n: Int) = arr(n)
          def canEqual(that: Any)    = true
        )
