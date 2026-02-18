package util

import scala.quoted.*

object GameActionCompositionMacro:

  /** Macro for .andThen - computes merged types at compile time */
  inline def andThenMacro[MOVE, S1 <: Tuple, D1, S2 <: Tuple, D2](
    receiver: game.GameAction[MOVE, S1, D1],
    other: game.GameAction[MOVE, S2, D2]
  ): game.GameAction[MOVE, ?, ?] =
    ${ andThenMacroImpl[MOVE, S1, D1, S2, D2]('receiver, 'other) }

  private def andThenMacroImpl[MOVE: Type, S1 <: Tuple: Type, D1: Type, S2 <: Tuple: Type, D2: Type](
    receiver: Expr[game.GameAction[MOVE, S1, D1]],
    other: Expr[game.GameAction[MOVE, S2, D2]]
  )(using Quotes): Expr[game.GameAction[MOVE, ?, ?]] =
    import quotes.reflect.*

    // Helper methods defined inside macro where TypeRepr is in scope

    /** Check if a tuple type contains a specific type */
    def containsType(tuple: TypeRepr, target: TypeRepr): Boolean =
      tuple match
        case AppliedType(_, List(head, tail)) if tuple.typeSymbol.name == "*:" =>
          (head =:= target) || containsType(tail, target)
        case _ if tuple =:= TypeRepr.of[EmptyTuple] =>
          false
        case _ =>
          false

    /** Apply *: to create h *: t */
    def appliedTupleType(head: TypeRepr, tail: TypeRepr): TypeRepr =
      val consSymbol = TypeRepr.of[*:].typeSymbol
      AppliedType(consSymbol.typeRef, List(head, tail))

    /** Compute TupleOps.Union[T1, T2] */
    def computeTupleUnion(t1: TypeRepr, t2: TypeRepr): TypeRepr =
      // Union is computed by recursively adding elements from T2 to T1, skipping duplicates
      def addToTuple(base: TypeRepr, toAdd: TypeRepr): TypeRepr =
        toAdd match
          case AppliedType(_, List(head, tail)) if toAdd.typeSymbol.name == "*:" =>
            // Check if head is already in base
            if containsType(base, head) then
              addToTuple(base, tail)
            else
              // Prepend head to the result of adding the tail
              val tailResult = addToTuple(base, tail)
              appliedTupleType(head, tailResult)
          case _ if toAdd =:= TypeRepr.of[EmptyTuple] =>
            base
          case _ =>
            report.errorAndAbort(s"Expected tuple type, got: ${toAdd.show}")

      addToTuple(t1, t2)

    /** Check if a coproduct contains a specific type */
    def containsCoproductType(coprod: TypeRepr, target: TypeRepr): Boolean =
      coprod match
        case AppliedType(consType, List(head, tail)) if consType.typeSymbol.fullName == "game.$colon$plus$colon" =>
          (head =:= target) || containsCoproductType(tail, target)
        case _ if coprod.typeSymbol.fullName == "game.CNil" =>
          false
        case _ =>
          false

    /** Apply :+: to create h :+: t */
    def appliedCoproductType(head: TypeRepr, tail: TypeRepr): TypeRepr =
      val coproductSymbol = Symbol.requiredClass("game.$colon$plus$colon")
      AppliedType(coproductSymbol.typeRef, List(head, tail))

    /** Compute CoproductOps.Union[C1, C2] */
    def computeCoproductUnion(c1: TypeRepr, c2: TypeRepr): TypeRepr =
      // Similar to tuple union, but for coproducts
      def addToCoproduct(base: TypeRepr, toAdd: TypeRepr): TypeRepr =
        toAdd match
          case AppliedType(consType, List(head, tail)) if consType.typeSymbol.fullName == "game.$colon$plus$colon" =>
            // Check if head is already in base
            if containsCoproductType(base, head) then
              addToCoproduct(base, tail)
            else
              // Prepend head
              val tailResult = addToCoproduct(base, tail)
              appliedCoproductType(head, tailResult)
          case _ if c2.typeSymbol.fullName == "game.CNil" =>
            base
          case _ =>
            report.errorAndAbort(s"Expected coproduct type, got: ${toAdd.show}")

      addToCoproduct(c1, c2)

    // Get the actual types
    val s1Type = TypeRepr.of[S1]
    val s2Type = TypeRepr.of[S2]
    val d1Type = TypeRepr.of[D1]
    val d2Type = TypeRepr.of[D2]

    // Compute merged state type: TupleOps.Union[S1, S2]
    val mergedStateType = computeTupleUnion(s1Type, s2Type)

    // Compute merged delta type: CoproductOps.Union[D1, D2]
    val mergedDeltaType = computeCoproductUnion(d1Type, d2Type)

    // Generate the composition code
    mergedStateType.asType match
      case '[mergedState] =>
        // Assert mergedState is a Tuple (it is, by construction)
        given Type[mergedState & Tuple] = Type.of[mergedState].asInstanceOf[Type[mergedState & Tuple]]

        mergedDeltaType.asType match
          case '[mergedDelta] =>
            // Generate MergeState and MergeDelta instances using the macro
            // These instances will be summoned at the call site
            Expr.summon[game.MergeState[S1, S2]] match
              case Some(msExpr) =>
                Expr.summon[game.MergeDelta[D1, D2]] match
                  case Some(mdExpr) =>
                    '{
                      val ms = $msExpr
                      val md = $mdExpr

                      val left = $receiver.actions.map { action =>
                        (move: MOVE, state: (mergedState & Tuple)) =>
                          val (state1, _) = ms.split(state.asInstanceOf[ms.Out])
                          action(move, state1).map(d => md.applyLeft(d).asInstanceOf[mergedDelta])
                      }

                      val right = $other.actions.map { action =>
                        (move: MOVE, state: (mergedState & Tuple)) =>
                          val (_, state2) = ms.split(state.asInstanceOf[ms.Out])
                          action(move, state2).map(d => md.applyRight(d).asInstanceOf[mergedDelta])
                      }

                      new game.GameAction[MOVE, mergedState & Tuple, mergedDelta](left ++ right)
                    }
                  case None =>
                    report.errorAndAbort(s"Could not find MergeDelta[${Type.show[D1]}, ${Type.show[D2]}]")
              case None =>
                report.errorAndAbort(s"Could not find MergeState[${Type.show[S1]}, ${Type.show[S2]}]")
