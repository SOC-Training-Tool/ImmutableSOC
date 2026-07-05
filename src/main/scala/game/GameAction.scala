package game

import scala.reflect.{ClassTag, TypeTest}

case class Delta[S <: GameState[?]](rawValue: Any)(using val ct: ClassTag[S])

object Delta:
  given [T <: GameState[?] : ClassTag]: TypeTest[Any, Delta[T]] with
    def unapply(x: Any): Option[x.type & Delta[T]] = x match
      case d: Delta[?] if d.ct.runtimeClass == summon[ClassTag[T]].runtimeClass =>
        Some(d.asInstanceOf[x.type & Delta[T]])
      case _ => None

trait GameAction[-M, -In, +Out]:
  def apply(move: M, input: In): Out

case object NoInput
