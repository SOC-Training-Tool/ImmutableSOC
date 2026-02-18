### java.lang.AssertionError: assertion failed: TypeBounds(TypeRef(ThisType(TypeRef(NoPrefix,module class scala)),class Nothing),TypeRef(ThisType(TypeRef(NoPrefix,module class scala)),class Any))

occurred in the presentation compiler.

presentation compiler configuration:


action parameters:
<NONE>


#### Error stacktrace:

```
scala.runtime.Scala3RunTime$.assertFailed(Scala3RunTime.scala:8)
	dotty.tools.dotc.core.Types$TypeBounds.<init>(Types.scala:5641)
	dotty.tools.dotc.core.Types$RealTypeBounds.<init>(Types.scala:5717)
	dotty.tools.dotc.core.Types$TypeBounds$.apply(Types.scala:5758)
	dotty.tools.dotc.core.Types$TypeBounds.derivedTypeBounds(Types.scala:5649)
	dotty.tools.dotc.core.Types$ApproximatingTypeMap.derivedTypeBounds(Types.scala:6586)
	dotty.tools.dotc.core.Types$TypeMap.mapOver(Types.scala:6307)
	dotty.tools.dotc.core.TypeOps$AsSeenFromMap.apply(TypeOps.scala:111)
	dotty.tools.dotc.core.TypeOps$AsSeenFromMap.apply(TypeOps.scala:69)
	scala.collection.immutable.List.mapConserve(List.scala:473)
	dotty.tools.dotc.core.Types$TypeMap.mapOverLambda(Types.scala:6259)
	dotty.tools.dotc.core.TypeOps$AsSeenFromMap.apply(TypeOps.scala:105)
	dotty.tools.dotc.core.TypeOps$.asSeenFrom(TypeOps.scala:55)
	dotty.tools.dotc.core.Types$Type.asSeenFrom(Types.scala:1118)
	dotty.tools.dotc.core.Denotations$SingleDenotation.derived$1(Denotations.scala:1103)
	dotty.tools.dotc.core.Denotations$SingleDenotation.computeAsSeenFrom(Denotations.scala:1130)
	dotty.tools.dotc.core.Denotations$SingleDenotation.computeAsSeenFrom(Denotations.scala:1083)
	dotty.tools.dotc.core.Denotations$PreDenotation.asSeenFrom(Denotations.scala:137)
	dotty.tools.dotc.core.SymDenotations$ClassDenotation.findMember(SymDenotations.scala:2186)
	dotty.tools.dotc.core.Types$Type.go$1(Types.scala:783)
	dotty.tools.dotc.core.Types$Type.findMember(Types.scala:964)
	dotty.tools.dotc.core.Types$Type.memberBasedOnFlags(Types.scala:756)
	dotty.tools.dotc.core.Types$Type.membersBasedOnFlags$$anonfun$1(Types.scala:1099)
	scala.runtime.function.JProcedure2.apply(JProcedure2.java:15)
	scala.runtime.function.JProcedure2.apply(JProcedure2.java:10)
	dotty.tools.dotc.core.Types$.dotty$tools$dotc$core$Types$Type$$_$memberDenots$$anonfun$1(Types.scala:1010)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.immutable.HashSet.foreach(HashSet.scala:951)
	dotty.tools.dotc.core.Types$Type.memberDenots(Types.scala:1010)
	dotty.tools.dotc.core.Types$Type.membersBasedOnFlags(Types.scala:1099)
	dotty.tools.pc.SymbolInformationProvider.info(SymbolInformationProvider.scala:59)
	dotty.tools.pc.ScalaPresentationCompiler.info$$anonfun$1(ScalaPresentationCompiler.scala:230)
```
#### Short summary: 

java.lang.AssertionError: assertion failed: TypeBounds(TypeRef(ThisType(TypeRef(NoPrefix,module class scala)),class Nothing),TypeRef(ThisType(TypeRef(NoPrefix,module class scala)),class Any))