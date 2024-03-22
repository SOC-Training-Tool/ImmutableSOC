package util

import shapeless.HList
import shapeless.ops.hlist.{RemoveAll, Replacer, Selector}

trait DependsOn[S, D <: HList] {
  def get[T](s: S)(implicit selector: Selector[D, T]): T

  def update[T](t: T, s: S)(implicit replacer: Replacer.Aux[D, T, T, (T, D)]): S

  def updateWith[T](s: S)(f: T => T)(implicit selector: Selector[D, T], replacer: Replacer.Aux[D, T, T, (T, D)]): S = {
    update(f(get[T](s)), s)
  }

  def getAll(s: S): D

  def updateAll(s: S)(f: D => D): S

}

object DependsOn {

  def single[L <: HList](implicit dependsOn: DependsOn[L, L]) = dependsOn

  def apply[S <: HList, D <: HList](implicit dependsOn: DependsOn[S, D]): DependsOn[S, D] = dependsOn

  implicit def dependsOn[S <: HList, D <: HList, R <: HList](implicit ra: RemoveAll.Aux[S, D, (D, R)]): DependsOn[S, D] = new DependsOn[S, D] {

    override def get[T](s: S)(implicit selector: Selector[D, T]): T = selector.apply(ra.apply(s)._1)

    override def update[T](t: T, s: S)(implicit replacer: Replacer.Aux[D, T, T, (T, D)]): S = {
      updateAll(s)(d => replacer.apply(d, t)._2)
    }

    override def getAll(s: S): D = ra.apply(s)._1

    override def updateAll(s: S)(f: D => D): S = {
      val (d, rem) = ra.apply(s)
      ra.reinsert((f(d), rem))
    }
  }

  implicit class InnerDependency[S, D <: HList](dp: DependsOn[S, D]) {

    def innerDependency[D1 <: HList](implicit inner: DependsOn[D, D1]): DependsOn[S, D1] = new DependsOn[S, D1] {

      override def get[T](s: S)(implicit selector: Selector[D1, T]): T = {
        implicit val sel = new Selector[D, T] {
          override def apply(t: D): T = inner.get(t)
        }
        dp.get(s)
      }

      override def update[T](t: T, s: S)(implicit replacer: Replacer.Aux[D1, T, T, (T, D1)]): S = {
        implicit val rep: Replacer.Aux[D, T, T, (T, D)] = new Replacer[D, T, T] {
          override type Out = (T, D)

          override def apply(t: D, u: T): Out = {
            val d = inner.update(u, t)
            (u, d)
          }
        }
        dp.update(t, s)
      }

      override def getAll(s: S): D1 = inner.getAll(dp.getAll(s))

      override def updateAll(s: S)(f: D1 => D1): S = dp.updateAll(s)(d => inner.updateAll(d)(f))
    }
  }
}