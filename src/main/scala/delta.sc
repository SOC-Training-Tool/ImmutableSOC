import game.{Delta, GameState}
import shapeless.{:+:, CNil, Coproduct, HNil}

case class Foo(s: Int) extends GameState[Foo] {
  override type Delta = String :+: Double :+: CNil

  override def apply(delta: String :+: Double :+: CNil): Foo = Foo(s + delta.select[String].fold(0)(_.toInt))
}

val fooDelta1: Delta[Foo] = Delta[Foo](Coproduct[String :+: CNil]("1"))
val fooDelta2: Delta[Foo] = Delta[Foo]("1")

val f = Foo(0) :: HNil

val b = Delta.applyState(f, List(Coproduct[Delta[Foo] :+: CNil](fooDelta1)))
