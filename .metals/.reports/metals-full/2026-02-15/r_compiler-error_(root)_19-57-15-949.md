file://<WORKSPACE>/src/main/scala/soc/core/state/package.scala
### scala.MatchError: val <none> (of class dotty.tools.dotc.core.Symbols$NoSymbol$)

occurred in the presentation compiler.

presentation compiler configuration:


action parameters:
offset: 1009
uri: file://<WORKSPACE>/src/main/scala/soc/core/state/package.scala
text:
```scala
package soc.core

import game.{:+:, CNil, Inl, Inr, CoproductInject, GameState, InventorySet}
import game.ImmutableGame.StateInitializer

package object state {

  case class PlayerIds(players: Seq[Int]) extends GameState[PlayerIds] {
    override type Delta = Nothing
    override def apply(delta: Nothing): PlayerIds = this
  }

  case class MoveCount(count: Int) extends GameState[MoveCount] {
    override type Delta = Int
    override def apply(delta: Int): MoveCount = MoveCount(count + delta)
  }

  case class PlayerBuilding[BB](player: Int, building: BB)
  object BoardBuildingState {

    type BuildingMap[BB, T] = Map[T, PlayerBuilding[BB]]

    case class AddBuilding[BB, T](location: T, player: Int, building: BB)
    case class RemoveBuilding[T](location: T)

    def add[L, B <: BoardBuilding[L], BB](location: L, building: B, player: Int)(using inject: CoproductInject[BB, B]) = {
      AddBuilding[BB, L](location, player, inject(building))
    }

    type Delta[BB, T] = AddBuilding[BB, T] :@@+: RemoveBuilding[T] :+: CNil

    given initBoardBuildingState[BB, T]: StateInitializer[BuildingMap[BB, T]] with
      def apply(): BuildingMap[BB, T] = Map.empty
  }

  case class VertexBuildingState[BB](map: BoardBuildingState.BuildingMap[BB, Vertex]) extends GameState[VertexBuildingState[BB]] {
    type Delta = BoardBuildingState.Delta[BB, Vertex]
    override def apply(delta: Delta): VertexBuildingState[BB] = VertexBuildingState(delta match {
      case Inl(add)          => map + (add.location -> PlayerBuilding(add.player, add.building))
      case Inr(Inl(rm))      => map - rm.location
      case Inr(Inr(cnil))    => cnil.impossible
    })
  }

  case class EdgeBuildingState[BB](map: BoardBuildingState.BuildingMap[BB, Edge]) extends GameState[EdgeBuildingState[BB]] {
    type Delta = BoardBuildingState.Delta[BB, Edge]
    override def apply(delta: Delta): EdgeBuildingState[BB] = EdgeBuildingState(delta match {
      case Inl(add)          => map + (add.location -> PlayerBuilding(add.player, add.building))
      case Inr(Inl(rm))      => map - rm.location
      case Inr(Inr(cnil))    => cnil.impossible
    })
  }

  case class Bank[II](b: InventorySet[II, Int]) extends GameState[Bank[II]] {
    override type Delta = Bank.Add[II] :+: Bank.Take[II] :+: CNil
    override def apply(delta: Delta): Bank[II] = Bank(delta match {
      case Inl(add)       => b.add(add.inv)
      case Inr(Inl(take)) => b.subtract(take.inv)
      case Inr(Inr(cnil)) => cnil.impossible
    })
  }

  object Bank {

    case class Add[II](inv: InventorySet[II, Int])
    case class Take[II](inv: InventorySet[II, Int])
  }

  case class Turn(t: Int) extends GameState[Turn] {
    override type Delta = Int

    override def apply(delta: Int): Turn = Turn(t + delta)
  }

  object Turn {
    given initTurnStat: StateInitializer[Turn] with
      def apply(): Turn = Turn(0)
  }

  case class PlayerPoints(points: PlayerMap[Int]) extends GameState[PlayerPoints] {
    override type Delta = PlayerPoints.Increment :+: PlayerPoints.Decrement :+: CNil
    override def apply(delta: Delta): PlayerPoints = PlayerPoints(delta match {
      case Inl(inc) => points + (inc.player -> (points.getOrElse(inc.player, 0) + 1))
      case Inr(Inl(dec)) => points + (dec.player -> points.get(dec.player).fold(0)(_ - 1))
      case Inr(Inr(cnil)) => cnil.impossible
    })
  }

  object PlayerPoints {

    case class Increment(player: Int)
    case class Decrement(player: Int)

    given initPlayerPoints(using ids: PlayerIds): StateInitializer[PlayerPoints] with
      def apply(): PlayerPoints = PlayerPoints(ids.players.map(_ -> 0).toMap)
  }
}

```



#### Error stacktrace:

```
dotty.tools.dotc.core.SymDenotations$ClassDenotation.computeMemberNames$$anonfun$1(SymDenotations.scala:2366)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.immutable.List.foreach(List.scala:334)
	dotty.tools.dotc.core.SymDenotations$ClassDenotation.computeMemberNames(SymDenotations.scala:2362)
	dotty.tools.dotc.core.SymDenotations$MemberNamesImpl.apply(SymDenotations.scala:2958)
	dotty.tools.dotc.core.SymDenotations$ClassDenotation.memberNames(SymDenotations.scala:2355)
	dotty.tools.dotc.core.SymDenotations$ClassDenotation.computeMemberNames$$anonfun$1(SymDenotations.scala:2365)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.immutable.List.foreach(List.scala:334)
	dotty.tools.dotc.core.SymDenotations$ClassDenotation.computeMemberNames(SymDenotations.scala:2362)
	dotty.tools.dotc.core.SymDenotations$MemberNamesImpl.apply(SymDenotations.scala:2958)
	dotty.tools.dotc.core.SymDenotations$ClassDenotation.memberNames(SymDenotations.scala:2355)
	dotty.tools.dotc.core.Types$Type.memberNames(Types.scala:993)
	dotty.tools.dotc.core.Types$Type.memberDenots(Types.scala:1010)
	dotty.tools.dotc.core.Types$Type.implicitMembers(Types.scala:1078)
	dotty.tools.dotc.typer.Typer.implementDeferredGivens$1(Typer.scala:3063)
	dotty.tools.dotc.typer.Typer.typedClassDef(Typer.scala:3097)
	dotty.tools.dotc.typer.Typer.typedTypeOrClassDef$1(Typer.scala:3403)
	dotty.tools.dotc.typer.Typer.typedNamed$1(Typer.scala:3407)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3499)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3577)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3581)
	dotty.tools.dotc.typer.Typer.traverse$1(Typer.scala:3603)
	dotty.tools.dotc.typer.Typer.typedStats(Typer.scala:3649)
	dotty.tools.dotc.typer.Typer.typedClassDef(Typer.scala:3097)
	dotty.tools.dotc.typer.Typer.typedTypeOrClassDef$1(Typer.scala:3403)
	dotty.tools.dotc.typer.Typer.typedNamed$1(Typer.scala:3407)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3499)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3577)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3581)
	dotty.tools.dotc.typer.Typer.traverse$1(Typer.scala:3603)
	dotty.tools.dotc.typer.Typer.typedStats(Typer.scala:3649)
	dotty.tools.dotc.typer.Typer.typedClassDef(Typer.scala:3097)
	dotty.tools.dotc.typer.Typer.typedTypeOrClassDef$1(Typer.scala:3403)
	dotty.tools.dotc.typer.Typer.typedNamed$1(Typer.scala:3407)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3499)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3577)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3581)
	dotty.tools.dotc.typer.Typer.traverse$1(Typer.scala:3603)
	dotty.tools.dotc.typer.Typer.typedStats(Typer.scala:3649)
	dotty.tools.dotc.typer.Typer.typedPackageDef(Typer.scala:3230)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3449)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3500)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3577)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3581)
	dotty.tools.dotc.typer.Typer.traverse$1(Typer.scala:3630)
	dotty.tools.dotc.typer.Typer.typedStats(Typer.scala:3649)
	dotty.tools.dotc.typer.Typer.typedPackageDef(Typer.scala:3230)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3449)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3500)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3577)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3581)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3692)
	dotty.tools.dotc.typer.TyperPhase.typeCheck$$anonfun$1(TyperPhase.scala:47)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	dotty.tools.dotc.core.Phases$Phase.monitor(Phases.scala:503)
	dotty.tools.dotc.typer.TyperPhase.typeCheck(TyperPhase.scala:53)
	dotty.tools.dotc.typer.TyperPhase.$anonfun$4(TyperPhase.scala:99)
	scala.collection.Iterator$$anon$6.hasNext(Iterator.scala:479)
	scala.collection.Iterator$$anon$9.hasNext(Iterator.scala:583)
	scala.collection.immutable.List.prependedAll(List.scala:152)
	scala.collection.immutable.List$.from(List.scala:685)
	scala.collection.immutable.List$.from(List.scala:682)
	scala.collection.IterableOps$WithFilter.map(Iterable.scala:900)
	dotty.tools.dotc.typer.TyperPhase.runOn(TyperPhase.scala:98)
	dotty.tools.dotc.Run.runPhases$1$$anonfun$1(Run.scala:343)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.ArrayOps$.foreach$extension(ArrayOps.scala:1323)
	dotty.tools.dotc.Run.runPhases$1(Run.scala:336)
	dotty.tools.dotc.Run.compileUnits$$anonfun$1(Run.scala:384)
	dotty.tools.dotc.Run.compileUnits$$anonfun$adapted$1(Run.scala:396)
	dotty.tools.dotc.util.Stats$.maybeMonitored(Stats.scala:69)
	dotty.tools.dotc.Run.compileUnits(Run.scala:396)
	dotty.tools.dotc.Run.compileSources(Run.scala:282)
	dotty.tools.dotc.interactive.InteractiveDriver.run(InteractiveDriver.scala:161)
	dotty.tools.pc.MetalsDriver.run(MetalsDriver.scala:47)
	dotty.tools.pc.HoverProvider$.hover(HoverProvider.scala:40)
	dotty.tools.pc.ScalaPresentationCompiler.hover$$anonfun$1(ScalaPresentationCompiler.scala:393)
```
#### Short summary: 

scala.MatchError: val <none> (of class dotty.tools.dotc.core.Symbols$NoSymbol$)