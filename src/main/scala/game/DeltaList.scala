package game

import game.Delta.DeltaGen
import shapeless.{CNil, Coproduct}
import util.opext.{CoproductAdd, CoproductUnion}

class DeltaList[C <: Coproduct](list: List[C]) { self =>

  def add[T <: GameState[T]]: AddApply[T] = AddApply[T]()

  final case class AddApply[T <: GameState[T]]() {

    def apply[A](a: A*)(implicit deltaGen: DeltaGen[T, A], addDelta: CoproductAdd[C, Delta[T]]): DeltaList[addDelta.Out] = {
      val deltas = list.map(addDelta.applyLeft) ++ a.toList.map(deltaGen.apply).map(addDelta.applyRight)
      new DeltaList(deltas)
    }

    def apply[Out <: Coproduct]()(implicit addDelta: CoproductAdd.Aux[C, T, Out]) = new DeltaList[Out](list.map(addDelta.applyLeft))
  }

  def toList: List[C] = list

  def ++[C2 <: Coproduct](other: DeltaList[C2])(implicit addAll: CoproductUnion[C, C2]): DeltaList[addAll.Out] = {
    new DeltaList(list.map(addAll.applyLeft) ++ other.list.map(addAll.applyRight))
  }
}

object DeltaList {

  def apply(): DeltaList[CNil] = empty[CNil]
  def apply[DL <: Coproduct](l: List[DL]) = new DeltaList[DL](l)
  def empty[DL <: Coproduct] = new DeltaList[DL](Nil)
}
