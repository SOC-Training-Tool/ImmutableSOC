package util

import game.{TupleIndex, TupleSelectAll, TupleRemoveAll}

trait DependsOn[S, D <: Tuple]:
  def get[T](s: S)(using idx: TupleIndex[D, T]): T

  def update[T](t: T, s: S)(using idx: TupleIndex[D, T]): S

  def updateWith[T](s: S)(f: T => T)(using idx: TupleIndex[D, T]): S =
    update(f(get[T](s)), s)

  def getAll(s: S): D

  def updateAll(s: S)(f: D => D): S

object DependsOn:

  extension [S <: Tuple](s: S)
    def updateWith[T](f: T => T)(using dep: DependsOn[S, T *: EmptyTuple]): S =
      dep.updateWith[T](s)(f)

    def getAll[D <: Tuple](using dep: DependsOn[S, D]): D = dep.getAll(s)

    def updateAll[D <: Tuple](f: D => D)(using dep: DependsOn[S, D]): S = dep.updateAll(s)(f)

    def replaceAll[D <: Tuple](d: D)(using dep: DependsOn[S, D]): S = updateAll[D](_ => d)

  def single[L <: Tuple](using dependsOn: DependsOn[L, L]): DependsOn[L, L] = dependsOn

  def apply[S <: Tuple, D <: Tuple](using dependsOn: DependsOn[S, D]): DependsOn[S, D] = dependsOn

  given [S <: Tuple, D <: Tuple](using ra: TupleRemoveAll[S, D]): DependsOn[S, D] with

    def get[T](s: S)(using idx: TupleIndex[D, T]): T =
      val d = ra(s)._1
      d.productElement(idx.index).asInstanceOf[T]

    def update[T](t: T, s: S)(using idx: TupleIndex[D, T]): S =
      updateAll(s) { d =>
        val arr = d.toArray
        arr(idx.index) = t.asInstanceOf[Object]
        Tuple.fromArray(arr).asInstanceOf[D]
      }

    def getAll(s: S): D = ra(s)._1

    def updateAll(s: S)(f: D => D): S =
      val (d, rem) = ra(s)
      ra.reinsert((f(d), rem))

  extension [S, D <: Tuple](dp: DependsOn[S, D])
    def innerDependency[D1 <: Tuple](using inner: DependsOn[D, D1]): DependsOn[S, D1] = new DependsOn[S, D1]:

      def get[T](s: S)(using idx: TupleIndex[D1, T]): T =
        inner.get[T](dp.getAll(s))

      def update[T](t: T, s: S)(using idx: TupleIndex[D1, T]): S =
        dp.updateAll(s)(d => inner.update(t, d))

      def getAll(s: S): D1 = inner.getAll(dp.getAll(s))

      def updateAll(s: S)(f: D1 => D1): S = dp.updateAll(s)(d => inner.updateAll(d)(f))
