package util

import game.InventorySet

object MapOps {

  trait MapOperand[T] {
    def zero: T
    def plus(t1: T, t2: T): T
  }

  implicit def numericMapOperand[T: Numeric]: MapOperand[T] = new MapOperand[T] {
    override def zero: T               = implicitly[Numeric[T]].zero
    override def plus(t1: T, t2: T): T = implicitly[Numeric[T]].plus(t1, t2)
  }

  implicit def inventorySetMapOperand[A, T: Numeric]: MapOperand[InventorySet[A, T]] = new MapOperand[InventorySet[A, T]] {
    override def zero: InventorySet[A, T]                                                 = InventorySet.empty[A, T]
    override def plus(t1: InventorySet[A, T], t2: InventorySet[A, T]): InventorySet[A, T] = t1.add(t2)
  }

  implicit def mapMapOperand[K, T: MapOperand]: MapOperand[Map[K, T]] = new MapOperand[Map[K, T]] {
    override def zero: Map[K, T]                               = Map.empty
    override def plus(t1: Map[K, T], t2: Map[K, T]): Map[K, T] = t1.add(t2)
  }

  implicit class MapOps[K, T](map: Map[K, T])(implicit operand: MapOperand[T]) {

    def merge[T2](other: Map[K, T])(f: (T, T) => T2): Map[K, T2] = {
      (map.keys ++ other.keys).toList.distinct.map { key =>
        key -> f(map.getOrElse(key, operand.zero), other.getOrElse(key, operand.zero))
      }.toMap
    }

    def add(other: Map[K, T]): Map[K, T] = {
      merge(other)(operand.plus)
    }
  }

}
