package game

import game.Delta.DeltaGen

class DeltaList[C](val list: List[C]):

  def add[T]: AddApply[T] = AddApply[T]()

  final case class AddApply[T]():

    def apply[A](a: A*)(using deltaGen: DeltaGen[T, A], addDelta: CoproductAdd[C, Delta[T]]): DeltaList[addDelta.Out] =
      val addDeltas: List[Delta[T]]     = a.toList.map(deltaGen.apply)
      val left:      List[addDelta.Out] = list.map(addDelta.applyLeft)
      val right:     List[addDelta.Out] = addDeltas.map(addDelta.applyRight)
      new DeltaList(left ++ right)

    def apply[Out]()(using addDelta: CoproductAdd.Aux[C, Delta[T], Out]): DeltaList[Out] =
      new DeltaList[Out](list.map(addDelta.applyLeft))

  def toList: List[C] = list

  def addAll[C2](other: DeltaList[C2])(using addAll: CoproductUnionOp[C, C2]): DeltaList[addAll.Out] =
    val left:  List[addAll.Out] = list.map(addAll.applyLeft)
    val right: List[addAll.Out] = other.list.map(addAll.applyRight)
    new DeltaList(left ++ right)

object DeltaList:

  def apply(): DeltaList[CNil] = empty[CNil]

  def apply[DL](l: List[DL]): DeltaList[DL] = new DeltaList[DL](l)

  def empty[DL]: DeltaList[DL] = new DeltaList[DL](Nil)
