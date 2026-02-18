file://<WORKSPACE>/src/main/scala/soc/base/actions/special/package.scala
### java.lang.AssertionError: assertion failed: NoType

occurred in the presentation compiler.

presentation compiler configuration:


action parameters:
uri: file://<WORKSPACE>/src/main/scala/soc/base/actions/special/package.scala
text:
```scala
package soc.base.actions

import game.Delta.DeltaGen
import game.{Delta, DeltaList, :+:, CNil}
import soc.base.state.SpecialPlayer
import soc.core.state.PlayerPoints

package object special {

  def updatedSpecialPlayer(minCount: Int, currentSpecialPlayer: Option[Int], updatedPlayerCounts: Map[Int, Int]): Option[Int] = {
    updatedPlayerCounts.toSeq
      .groupBy(_._2)
      .view
      .mapValues(_.map(_._1).toList)
      .maxByOption(_._1)
      .flatMap {
        case (length, _) if length < minCount => None
        case (_, p :: Nil)                    => Some(p)
        case (_, players)                     => currentSpecialPlayer.filter(players.contains)
      }
  }

  def specialPlayerDelta[SP](currentSpecialPlayer: Option[Int], updatedSpecialPlayer: Option[Int])(using pGen: DeltaGen[SP, SpecialPlayer.Delta]): DeltaList[Delta[PlayerPoints] :+: Delta[SP] :+: CNil] = {
    (currentSpecialPlayer, updatedSpecialPlayer) match {
      case (None, None)       =>
        DeltaList()
          .add[SP]()
          .add[PlayerPoints]()
      case (None, Some(p))    =>
        DeltaList()
          .add[SP](SpecialPlayer.Set(p))
          .add[PlayerPoints](PlayerPoints.Increment(p), PlayerPoints.Increment(p))
      case (Some(p), None)    =>
        DeltaList()
          .add[SP](SpecialPlayer.Set(p))
          .add[PlayerPoints](PlayerPoints.Increment(p), PlayerPoints.Increment(p))
      case (Some(o), Some(n)) if o == n =>
        DeltaList()
          .add[SP]()
          .add[PlayerPoints]()
      case (Some(o), Some(n)) =>
        DeltaList()
          .add[SP](SpecialPlayer.Remove)
          .add[PlayerPoints](PlayerPoints.Decrement(o), PlayerPoints.Decrement(o))
          .add[SP](SpecialPlayer.Set(n))
          .add[PlayerPoints](PlayerPoints.Increment(n), PlayerPoints.Increment(n))
    }

  }
}

```



#### Error stacktrace:

```
scala.runtime.Scala3RunTime$.assertFailed(Scala3RunTime.scala:8)
	dotty.tools.dotc.core.Types$TypeBounds.<init>(Types.scala:5642)
	dotty.tools.dotc.core.Types$RealTypeBounds.<init>(Types.scala:5717)
	dotty.tools.dotc.core.Types$TypeBounds$.apply(Types.scala:5758)
	dotty.tools.dotc.core.Types$TypeBounds.derivedTypeBounds(Types.scala:5649)
	dotty.tools.dotc.core.Types$TypeMap.derivedTypeBounds(Types.scala:6211)
	dotty.tools.dotc.core.Types$TypeMap.mapOver(Types.scala:6307)
	dotty.tools.dotc.core.Types$TypeMap.mapOver(Types.scala:6382)
	dotty.tools.dotc.typer.ImplicitRunInfo$$anon$1.apply(Implicits.scala:841)
	dotty.tools.dotc.core.Types$TypeMap.op$proxy19$1(Types.scala:6301)
	dotty.tools.dotc.core.Types$TypeMap.mapOver(Types.scala:6301)
	dotty.tools.dotc.typer.ImplicitRunInfo$$anon$1.apply(Implicits.scala:841)
	dotty.tools.dotc.core.Types$TypeMap.mapOver(Types.scala:6333)
	dotty.tools.dotc.typer.ImplicitRunInfo$$anon$1.apply(Implicits.scala:841)
	dotty.tools.dotc.typer.ImplicitRunInfo.implicitScope(Implicits.scala:843)
	dotty.tools.dotc.typer.ImplicitRunInfo.implicitScope$(Implicits.scala:624)
	dotty.tools.dotc.Run.implicitScope(Run.scala:43)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.implicitScope(Implicits.scala:1805)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1690)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.searchImplicit$$anonfun$2(Implicits.scala:1774)
	dotty.tools.dotc.typer.Implicits$SearchResult.recoverWith(Implicits.scala:430)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1760)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.bestImplicit(Implicits.scala:1801)
	dotty.tools.dotc.typer.Implicits.inferImplicit(Implicits.scala:1109)
	dotty.tools.dotc.typer.Implicits.inferImplicit$(Implicits.scala:860)
	dotty.tools.dotc.typer.Typer.inferImplicit(Typer.scala:145)
	dotty.tools.dotc.typer.Implicits.inferImplicitArg(Implicits.scala:928)
	dotty.tools.dotc.typer.Implicits.inferImplicitArg$(Implicits.scala:860)
	dotty.tools.dotc.typer.Typer.inferImplicitArg(Typer.scala:145)
	dotty.tools.dotc.typer.Typer.implicitArgs$1(Typer.scala:4094)
	dotty.tools.dotc.typer.Typer.addImplicitArgs$1(Typer.scala:4166)
	dotty.tools.dotc.typer.Typer.adaptNoArgsImplicitMethod$1(Typer.scala:4222)
	dotty.tools.dotc.typer.Typer.adaptNoArgs$1(Typer.scala:4429)
	dotty.tools.dotc.typer.Typer.adapt1(Typer.scala:4683)
	dotty.tools.dotc.typer.Typer.adapt(Typer.scala:3955)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3577)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3581)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3692)
	dotty.tools.dotc.typer.Typer.typeSelectOnTerm$1(Typer.scala:961)
	dotty.tools.dotc.typer.Typer.typedSelect(Typer.scala:1003)
	dotty.tools.dotc.typer.Typer.typedNamed$1(Typer.scala:3390)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3499)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3577)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3581)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3692)
	dotty.tools.dotc.typer.Applications.typedTypeApply(Applications.scala:1286)
	dotty.tools.dotc.typer.Applications.typedTypeApply$(Applications.scala:434)
	dotty.tools.dotc.typer.Typer.typedTypeApply(Typer.scala:145)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3435)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3500)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3577)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3581)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3692)
	dotty.tools.dotc.typer.Applications.realApply$1(Applications.scala:1042)
	dotty.tools.dotc.typer.Applications.typedApply(Applications.scala:1233)
	dotty.tools.dotc.typer.Applications.typedApply$(Applications.scala:434)
	dotty.tools.dotc.typer.Typer.typedApply(Typer.scala:145)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3415)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3500)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3577)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3581)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3692)
	dotty.tools.dotc.typer.Typer.typedBlock(Typer.scala:1406)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3423)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3500)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3577)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3581)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3692)
	dotty.tools.dotc.typer.Typer.caseRest$1(Typer.scala:2177)
	dotty.tools.dotc.typer.Typer.typedCase(Typer.scala:2193)
	dotty.tools.dotc.typer.Typer.typedCases$$anonfun$1(Typer.scala:2121)
	dotty.tools.dotc.core.Decorators$.loop$1(Decorators.scala:99)
	dotty.tools.dotc.core.Decorators$.mapconserve(Decorators.scala:115)
	dotty.tools.dotc.typer.Typer.typedCases(Typer.scala:2120)
	dotty.tools.dotc.typer.Typer.$anonfun$39(Typer.scala:2111)
	dotty.tools.dotc.typer.Applications.harmonic(Applications.scala:2559)
	dotty.tools.dotc.typer.Applications.harmonic$(Applications.scala:434)
	dotty.tools.dotc.typer.Typer.harmonic(Typer.scala:145)
	dotty.tools.dotc.typer.Typer.typedMatchFinish(Typer.scala:2111)
	dotty.tools.dotc.typer.Typer.typedMatch(Typer.scala:2040)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3430)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3500)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3577)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3581)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3692)
	dotty.tools.dotc.typer.Typer.typedBlock(Typer.scala:1406)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3423)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3500)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3577)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3581)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3692)
	dotty.tools.dotc.typer.Typer.$anonfun$64(Typer.scala:2834)
	dotty.tools.dotc.inlines.PrepareInlineable$.dropInlineIfError(PrepareInlineable.scala:256)
	dotty.tools.dotc.typer.Typer.typedDefDef(Typer.scala:2834)
	dotty.tools.dotc.typer.Typer.typedNamed$1(Typer.scala:3397)
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
	dotty.tools.pc.WithCompilationUnit.<init>(WithCompilationUnit.scala:31)
	dotty.tools.pc.PcReferencesProvider.<init>(PcReferencesProvider.scala:24)
	dotty.tools.pc.ScalaPresentationCompiler.references$$anonfun$1(ScalaPresentationCompiler.scala:197)
```
#### Short summary: 

java.lang.AssertionError: assertion failed: NoType