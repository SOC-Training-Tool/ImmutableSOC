package util

import scala.reflect.macros.whitebox

/**
 * Macro implementation for ImmutableGameBuilder.BuildWithApply.apply.
 *
 * Replaces 7 shapeless implicit chains (ZipConst×2, Mapper×2, ZipWith,
 * Folder, LeftFolder) with compile-time code generation.
 *
 * DeltaState.Aux[STATE, D] is kept as a normal implicit so that D is
 * properly inferred and visible to callers.
 */
object BuildWithMacro {

  def impl(c: whitebox.Context)()(ds: c.Expr[_]): c.Tree = {
    import c.universe._

    // ---------------------------------------------------------------
    // Shapeless class symbols (used for type decomposition)
    // ---------------------------------------------------------------
    val hconsSym  = c.mirror.staticClass("shapeless.$colon$colon")
    val hnilSym   = c.mirror.staticClass("shapeless.HNil")
    val ccprodSym = c.mirror.staticClass("shapeless.$colon$plus$colon")
    val cnilSym   = c.mirror.staticClass("shapeless.CNil")

    def decomposeHList(tpe: Type): List[Type] = tpe.dealias match {
      case TypeRef(_, sym, _)           if sym == hnilSym   => Nil
      case TypeRef(_, sym, h :: t :: _) if sym == hconsSym  => h :: decomposeHList(t)
      case _ => c.abort(c.enclosingPosition, s"Expected HList, got: $tpe")
    }

    def decomposeCoproduct(tpe: Type): List[Type] = tpe.dealias match {
      case TypeRef(_, sym, _)           if sym == cnilSym   => Nil
      case TypeRef(_, sym, h :: t :: _) if sym == ccprodSym => h :: decomposeCoproduct(t)
      case _ => c.abort(c.enclosingPosition, s"Expected Coproduct, got: $tpe")
    }

    // ---------------------------------------------------------------
    // Extract types from the call context
    // ---------------------------------------------------------------

    // BuildWithApply[MOVES, STATE, ACTIONS2, GS2] – all 4 type args directly readable.
    val prefixTpe = c.prefix.actualType.dealias
    val List(movesTpe, stateTpe, actionsTpe, gsTpe) = prefixTpe.typeArgs.take(4)

    // D from the resolved DeltaState.Aux[STATE, D]
    val dTpe = ds.actualType
      .member(TypeName("Out"))
      .info
      .asSeenFrom(ds.actualType, ds.actualType.typeSymbol)
      .dealias

    // ---------------------------------------------------------------
    // Decompose into element-type lists
    // ---------------------------------------------------------------

    val movesTypes   = decomposeCoproduct(movesTpe)
    val stateTypes   = decomposeHList(stateTpe)
    val actionsTypes = decomposeHList(actionsTpe)

    if (movesTypes.length != actionsTypes.length)
      c.abort(c.enclosingPosition,
        s"MOVES has ${movesTypes.length} types but ACTIONS has ${actionsTypes.length} entries")

    // ---------------------------------------------------------------
    // Per-action position lookup
    // ---------------------------------------------------------------

    val gameActionSym = c.mirror.staticClass("game.GameAction")
    val deltaSym      = c.mirror.staticClass("game.Delta")

    def statePos(t: Type, hint: String): Int = {
      val i = stateTypes.indexWhere(_ =:= t)
      if (i < 0) c.abort(c.enclosingPosition, s"$hint: type $t not found in STATE $stateTpe")
      i
    }

    def innerDelta(dt: Type): Type = dt.baseType(deltaSym).typeArgs.head

    case class ActionInfo(sPositions: List[Int], dPositions: List[Int], rawDeltaTpe: Type)

    val actionInfos: List[ActionInfo] = actionsTypes.map { at =>
      val base = at.baseType(gameActionSym)
      if (base == NoType) c.abort(c.enclosingPosition, s"$at is not a GameAction")
      val sPos = decomposeHList(base.typeArgs(1)).map(t => statePos(t, s"action state"))
      val dPos = decomposeCoproduct(base.typeArgs(2)).map(dt => statePos(innerDelta(dt), s"action delta"))
      ActionInfo(sPos, dPos, base.typeArgs(2))
    }

    val gsBase       = gsTpe.baseType(gameActionSym)
    val gsStatePos   = decomposeHList(gsBase.typeArgs(1)).map(t => statePos(t, "global state"))
    val gsDeltaPos   = decomposeCoproduct(gsBase.typeArgs(2)).map(dt => statePos(innerDelta(dt), "global delta"))
    val gsDeltaTpe   = gsBase.typeArgs(2)

    // ---------------------------------------------------------------
    // Tree-generation helpers
    // ---------------------------------------------------------------

    def tailN(n: Int, base: Tree): Tree =
      (0 until n).foldLeft(base)((t, _) => q"$t.tail")

    def headAt(n: Int, base: Tree): Tree = q"${tailN(n, base)}.head"

    // Inl/Inr injection at position `pos`
    def injectAt(pos: Int, v: Tree): Tree =
      (0 until pos).foldLeft(q"_root_.shapeless.Inl($v)": Tree)(
        (t, _) => q"_root_.shapeless.Inr($t)")

    // Pattern for source position `pos`: Inl(bnd), Inr(Inl(bnd)), …
    def srcPat(pos: Int, bnd: TermName): Tree = {
      val inl = pq"_root_.shapeless.Inl($bnd)"
      (0 until pos).foldLeft(inl: Tree)((p, _) => pq"_root_.shapeless.Inr($p)")
    }

    // Exhaustion pattern: Inr^n(x: CNil)
    def cnilPat(nWraps: Int, binder: TermName): Tree = {
      val base = pq"$binder: _root_.shapeless.CNil"
      (0 until nWraps).foldLeft(base: Tree)((p, _) => pq"_root_.shapeless.Inr($p)")
    }

    // Build HList from `stateTree` by selecting each position
    def buildHList(positions: List[Int], stateTree: Tree): Tree =
      positions.foldRight(q"_root_.shapeless.HNil": Tree) { (pos, acc) =>
        q"${headAt(pos, stateTree)} :: $acc"
      }

    // Helper: create a PARAM ValDef
    def param(name: TermName, tpe: Type): ValDef =
      ValDef(Modifiers(Flag.PARAM), name, TypeTree(tpe), EmptyTree)

    // Match on `scrut` (a Tree) and embed each element into D.
    // srcLen = number of elements in source coproduct.
    // targetPositions(i) = where element i goes in D.
    def embedMatch(scrut: Tree, srcLen: Int, targetPositions: List[Int]): Tree = {
      val bnd = TermName("_embedV")
      val regularCases = targetPositions.zipWithIndex.map { case (tgt, src) =>
        cq"${srcPat(src, bnd)} => (${injectAt(tgt, q"$bnd")} : $dTpe)"
      }
      val cnilBnd  = TermName("_cnilX")
      val exhaustCase = cq"${cnilPat(srcLen, cnilBnd)} => $cnilBnd.impossible"
      q"$scrut match { case ..${regularCases :+ exhaustCase} }"
    }

    // Returns a Function tree (diTpe) => D  that embeds one element into D
    def embedFn(elemTpe: Type, srcLen: Int, targetPositions: List[Int]): Tree = {
      val elemName = TermName("_elem")
      Function(
        List(param(elemName, elemTpe)),
        embedMatch(q"$elemName", srcLen, targetPositions)
      )
    }

    // Function tree: (s: STATE, d: D) => STATE  that applies one delta to state.
    // D-position i ↔ STATE-position i (DeltaState guarantees this correspondence).
    val applyOneDeltaFn: Tree = {
      val sBnd = TermName("_apS")
      val dBnd = TermName("_apD")
      val dv   = TermName("_apDv")

      val cases = stateTypes.zipWithIndex.map { case (hTpe, i) =>
        val prefix   = (0 until i).map(j => headAt(j, q"$sBnd"))
        val updated  = q"${headAt(i, q"$sBnd")}.apply($dv.getValue[$hTpe](implicitly))"
        val suffix   = tailN(i + 1, q"$sBnd")
        val newState = (prefix :+ updated).foldRight(suffix: Tree)((h, t) => q"$h :: $t")
        cq"${srcPat(i, dv)} => $newState"
      }
      val cnilBnd = TermName("_cnilA")
      val exhaustCase = cq"${cnilPat(stateTypes.length, cnilBnd)} => $cnilBnd.impossible"

      Function(
        List(param(sBnd, stateTpe), param(dBnd, dTpe)),
        q"($dBnd: $dTpe) match { case ..${cases :+ exhaustCase} }"
      )
    }

    // ---------------------------------------------------------------
    // Generate one match case per move
    // ---------------------------------------------------------------

    val mBnd = TermName("_mv")

    val moveCases: List[Tree] = actionsTypes.zipWithIndex.map { case (_, i) =>
      val info = actionInfos(i)

      val actionTree = headAt(i, q"_cb.actions")
      val si         = buildHList(info.sPositions, q"state")
      val gss        = buildHList(gsStatePos,      q"state")

      val rawD = TermName(s"_rawD$i")
      val gaD  = TermName(s"_gaD$i")

      val body =
        q"""
          {
            val $rawD = $actionTree.actions.flatMap(_.apply($mBnd, $si))
            val $gaD  = _cb.globalActions.actions.flatMap(
                          _.apply($mBnd.asInstanceOf[_root_.scala.Any], $gss))
            val _allDeltas: _root_.scala.List[$dTpe] =
              $rawD.map(${embedFn(info.rawDeltaTpe, info.dPositions.length, info.dPositions)}) ++
              $gaD.map(${embedFn(gsDeltaTpe,        gsDeltaPos.length,      gsDeltaPos)})
            val _finalState = _allDeltas.foldLeft(state)($applyOneDeltaFn)
            (_allDeltas, _finalState)
          }
        """

      cq"${srcPat(i, mBnd)} => $body"
    }

    val cnilBnd      = TermName("_cnilM")
    val moveCnilCase = cq"${cnilPat(movesTypes.length, cnilBnd)} => $cnilBnd.impossible"

    // ---------------------------------------------------------------
    // Assemble the anonymous ImmutableGame
    // ---------------------------------------------------------------

    q"""
      {
        val _cb = ${c.prefix.tree}._builder
        new _root_.game.ImmutableGame[$movesTpe, $stateTpe] {
          override type DELTA = $dTpe
          override def applyMove(
            move  : $movesTpe,
            state : $stateTpe
          ): (_root_.scala.List[$dTpe], $stateTpe) =
            (move: $movesTpe) match {
              case ..${moveCases :+ moveCnilCase}
            }
        }
      }
    """
  }
}
