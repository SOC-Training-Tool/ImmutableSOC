package game

// Core Coproduct types — lightweight replacement for shapeless Coproduct
// Preserves exhaustive match checking via sealed + positional tagging (Inl/Inr)

sealed trait :+:[+H, +T]
final case class Inl[+H, +T](head: H) extends (H :+: T)
final case class Inr[+H, +T](tail: T) extends (H :+: T)

sealed trait CNil:
  def impossible: Nothing

// Type-level operations on Coproducts (match types)
object CoproductOps:

  /** Check if type T exists in coproduct C */
  type Contains[C, T] <: Boolean = C match
    case T :+: _ => true
    case _ :+: t => Contains[t, T]
    case CNil     => false

  /** Append two coproducts: (A :+: B :+: CNil) ++ (C :+: D :+: CNil) = A :+: B :+: C :+: D :+: CNil */
  type Concat[C1, C2] = C1 match
    case h :+: t => h :+: Concat[t, C2]
    case CNil     => C2

  /** Union two coproducts (deduplicated) */
  type Union[C1, C2] = C2 match
    case h :+: t => Union[AddIfAbsent[C1, h], t]
    case CNil     => C1

  /** Add type T to coproduct C if not already present */
  type AddIfAbsent[C, T] = Contains[C, T] match
    case true  => C
    case false => T :+: C

  /** Extract inner type from Delta[T] coproduct to a Tuple */
  type DeltaInnerTypes[C] <: Tuple = C match
    case Delta[h] :+: t => h *: DeltaInnerTypes[t]
    case CNil            => EmptyTuple


// Value-level operations on Coproducts (given instances)

/** Inject a value of type T into coproduct C */
trait CoproductInject[C, T]:
  def apply(t: T): C

object CoproductInject:
  given headInject[T, Tail]: CoproductInject[T :+: Tail, T] with
    def apply(t: T): T :+: Tail = Inl(t)

  given tailInject[H, Tail, T](using tail: CoproductInject[Tail, T]): CoproductInject[H :+: Tail, T] with
    def apply(t: T): H :+: Tail = Inr(tail(t))


/** Embed sub-coproduct into super-coproduct (replaces shapeless Basis) */
trait CoproductBasis[Super, Sub]:
  def embed(s: Sub): Super

object CoproductBasis:
  given [S]: CoproductBasis[S, CNil] with
    def embed(s: CNil): S = s.impossible

  given [H, SubTail, Super](using
    head: CoproductInject[Super, H],
    tail: CoproductBasis[Super, SubTail]
  ): CoproductBasis[Super, H :+: SubTail] with
    def embed(s: H :+: SubTail): Super = s match
      case Inl(h) => head(h)
      case Inr(t) => tail.embed(t)


/** Select a value of type T from coproduct C */
trait CoproductSelector[C, T]:
  def apply(c: C): Option[T]

object CoproductSelector:
  given headSelector[T, Tail]: CoproductSelector[T :+: Tail, T] with
    def apply(c: T :+: Tail): Option[T] = c match
      case Inl(h) => Some(h)
      case Inr(_) => None

  given tailSelector[H, Tail, T](using tail: CoproductSelector[Tail, T]): CoproductSelector[H :+: Tail, T] with
    def apply(c: H :+: Tail): Option[T] = c match
      case Inl(_) => None
      case Inr(t) => tail(t)


/** Add a type T to coproduct C. If T is already in C, Out = C. Otherwise Out = T :+: C. */
trait CoproductAdd[C, T]:
  type Out
  def applyLeft(c: C): Out
  def applyRight(t: T): Out

object CoproductAdd extends CoproductAddLowPriority:
  type Aux[C, T, Out0] = CoproductAdd[C, T] { type Out = Out0 }

  // Higher priority: T is already in C, so Out = C
  given alreadyPresent[C, T](using inject: CoproductInject[C, T]): CoproductAdd[C, T] with
    type Out = C
    def applyLeft(c: C): C = c
    def applyRight(t: T): C = inject(t)

trait CoproductAddLowPriority:
  // Lower priority: T not in C, so prepend
  given notPresent[C, T](using basis: CoproductBasis[T :+: C, C]): CoproductAdd[C, T] with
    type Out = T :+: C
    def applyLeft(c: C): T :+: C = basis.embed(c)
    def applyRight(t: T): T :+: C = Inl(t)


/** Union two coproducts (value-level) */
trait CoproductUnionOp[C1, C2]:
  type Out
  def applyLeft(c: C1): Out
  def applyRight(c: C2): Out

object CoproductUnionOp:
  type Aux[C1, C2, Out0] = CoproductUnionOp[C1, C2] { type Out = Out0 }

  given cnil[C]: CoproductUnionOp[C, CNil] with
    type Out = C
    def applyLeft(c: C): C = c
    def applyRight(c: CNil): C = c.impossible

  given ccons[C, H, T, Added](using
    add: CoproductAdd.Aux[C, H, Added],
    next: CoproductUnionOp[Added, T]
  ): CoproductUnionOp[C, H :+: T] with
    type Out = next.Out
    def applyLeft(c: C): next.Out = next.applyLeft(add.applyLeft(c))
    def applyRight(c: H :+: T): next.Out = c match
      case Inl(h) => next.applyLeft(add.applyRight(h))
      case Inr(t) => next.applyRight(t)


// Extension methods for ergonomic use
extension [C](c: C)
  def inject[Super](using inj: CoproductInject[Super, C]): Super = inj(c)

extension [Sub](s: Sub)
  def embed[Super](using basis: CoproductBasis[Super, Sub]): Super = basis.embed(s)

extension [C](c: C)
  def select[T](using sel: CoproductSelector[C, T]): Option[T] = sel(c)
