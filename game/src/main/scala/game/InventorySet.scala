package game

case class InventorySet[A, T] protected (amountMap: Map[A, T])(using num: Numeric[T]):

  lazy val getTotal: T       = amountMap.values.sum
  lazy val getTypes: Seq[A]  = amountMap.keys.toSeq
  lazy val getTypeCount: Int = amountMap.values.count(num.toDouble(_) > 0)
  lazy val isEmpty: Boolean  = getTotal == num.zero

  def add(amt: T, a: A): InventorySet[A, T] =
    val curAmount = amountMap.get(a)
    InventorySet.fromMap(curAmount.fold(amountMap + (a -> amt)) { amount =>
      (amountMap - a) + (a -> num.plus(amount, amt))
    })

  def add(set: InventorySet[A, T]): InventorySet[A, T] =
    set.getTypes.foldLeft(this) { case (newSet, a) => newSet.add(set(a), a) }

  def subtract(amt: T, a: A): InventorySet[A, T] =
    val curAmount = amountMap.get(a)
    val newMap = curAmount.fold(amountMap) { amount =>
      (amountMap - a) + (a -> num.max(num.zero, num.minus(amount, amt)))
    }
    InventorySet.fromMap(newMap)

  def subtract(set: InventorySet[A, T]): InventorySet[A, T] =
    set.getTypes.foldLeft(this.copy(amountMap)) { case (newSet, a) => newSet.subtract(set(a), a) }

  def contains(amt: T, a: A): Boolean = amountMap.get(a).fold(false)(num.gteq(_, amt))
  def contains(a: A): Boolean         = amountMap.get(a).fold(false)(num.gt(_, num.zero))
  def contains(set: InventorySet[A, T]): Boolean =
    set.getTypes.filter(set.contains).forall(res => contains(set.getAmount(res), res))

  def getAmount(a: A): T = amountMap.getOrElse(a, num.zero)

  def apply(a: A): T = getAmount(a)

object InventorySet:

  type InventoryMap[T] = InventorySet[T, Int]

  def empty[A, T: Numeric]: InventorySet[A, T] = InventorySet(Map.empty[A, T])

  def toList[A](set: InventoryMap[A]): Seq[A] =
    set.amountMap.toList.flatMap { case (a, amt) => (1 to amt).map(_ => a) }

  def fromMap[A, T: Numeric](invMap: Map[A, T]): InventorySet[A, T] =
    val numeric = summon[Numeric[T]]
    InventorySet(invMap.filter { case (_, t) => numeric.gt(t, numeric.zero) })

  def fromList[A](list: Seq[A]): InventoryMap[A] =
    fromMap(list.groupBy(f => f).view.mapValues(_.length).toMap)

  def sum[A, T: Numeric](sets: Seq[InventorySet[A, T]]): InventorySet[A, T] =
    sets.foldLeft(empty[A, T]) { case (x, y) => x.add(y) }
