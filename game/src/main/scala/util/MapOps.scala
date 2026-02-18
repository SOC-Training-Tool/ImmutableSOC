package util

import game.InventorySet

object MapOps:

  trait MapOperand[T]:
    def zero: T
    def plus(t1: T, t2: T): T

  given numericMapOperand[T: Numeric]: MapOperand[T] with
    def zero: T               = summon[Numeric[T]].zero
    def plus(t1: T, t2: T): T = summon[Numeric[T]].plus(t1, t2)

  given inventorySetMapOperand[A, T: Numeric]: MapOperand[InventorySet[A, T]] with
    def zero: InventorySet[A, T]                                                 = InventorySet.empty[A, T]
    def plus(t1: InventorySet[A, T], t2: InventorySet[A, T]): InventorySet[A, T] = t1.add(t2)

  given mapMapOperand[K, T: MapOperand]: MapOperand[Map[K, T]] with
    def zero: Map[K, T]                               = Map.empty
    def plus(t1: Map[K, T], t2: Map[K, T]): Map[K, T] = t1.add(t2)

  extension [K, T](map: Map[K, T])(using operand: MapOperand[T])

    def merge[T2](other: Map[K, T])(f: (T, T) => T2): Map[K, T2] =
      (map.keys ++ other.keys).toList.distinct.map { key =>
        key -> f(map.getOrElse(key, operand.zero), other.getOrElse(key, operand.zero))
      }.toMap

    def add(other: Map[K, T]): Map[K, T] =
      merge(other)(operand.plus)
