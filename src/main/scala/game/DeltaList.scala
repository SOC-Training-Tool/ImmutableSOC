package game

import game.Delta.DeltaGen
import shapeless.{CNil, Coproduct}
import util.opext.{CoproductAdd, CoproductUnion}

class DeltaList[C <: Coproduct](val list: List[C]) {
  self =>

  def add[T]: AddApply[T] = AddApply[T]()

  final case class AddApply[T]() {

    def apply[A](a: A*)(implicit deltaGen: DeltaGen[T, A], addDelta: CoproductAdd[C, Delta[T]]): DeltaList[addDelta.Out] = {
      val addDeltas: List[Delta[T]]     = a.toList.map { value: A => deltaGen.apply(value) }
      val left     : List[addDelta.Out] = list.map { value: C => addDelta.applyLeft(value) }
      val right    : List[addDelta.Out] = addDeltas.map { value: Delta[T] => addDelta.applyRight(value) }
      new DeltaList(left ++ right)
    }

    def apply[Out <: Coproduct]()(implicit addDelta: CoproductAdd.Aux[C, Delta[T], Out]) = new DeltaList[Out](list.map(addDelta.applyLeft))
  }

  def toList: List[C] = list

  def addAll[C2 <: Coproduct](other: DeltaList[C2])(implicit addAll: CoproductUnion[C, C2]): DeltaList[addAll.Out] = {
    val left : List[addAll.Out] = list.map{ value: C => addAll.applyLeft(value) }
    val right: List[addAll.Out] = other.list.map{ value: C2 => addAll.applyRight(value) }
    new DeltaList(left ++ right)
  }
}

object DeltaList {

  def apply(): DeltaList[CNil] = empty[CNil]

  def apply[DL <: Coproduct](l: List[DL]) = new DeltaList[DL](l)

  def empty[DL <: Coproduct] = new DeltaList[DL](Nil)
}
