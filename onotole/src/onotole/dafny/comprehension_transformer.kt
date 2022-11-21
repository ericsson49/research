package onotole.dafny

import onotole.AnnAssign
import onotole.Assign
import onotole.BoolOp
import onotole.CTV
import onotole.EBoolOp
import onotole.ExTypeVar
import onotole.Expr
import onotole.ExprTyper
import onotole.For
import onotole.FreshNames
import onotole.FuncInst
import onotole.FunctionDef
import onotole.GeneratorExp
import onotole.If
import onotole.Name
import onotole.PyList
import onotole.SimpleStmtTransformer
import onotole.Stmt
import onotole.TExpr
import onotole.While
import onotole.checkSideEffects
import onotole.exceptions.ExcnChecker
import onotole.getFunArgs
import onotole.mkAssign
import onotole.mkAttribute
import onotole.mkCall
import onotole.mkName
import onotole.parseType
import onotole.typelib.TLSig
import onotole.typelib.TLTClass
import onotole.typelib.TLTVar
import onotole.util.deconstruct
import onotole.util.mergeExprTypers

class ComprehensionTransformer(val typer: ExprTyper) {
  var freshNames = FreshNames()
  fun transform(e: GeneratorExp): Pair<TExpr, List<Stmt>> {
    if (e.generators.size != 1) TODO()
    val c = e.generators[0]
    val tgt = c.target as Name
    if (c.ifs.size > 1) TODO()
    val res = mutableListOf<Stmt>()
    val acc = freshNames.fresh("acc")
    val filterIter = freshNames.fresh("iter")

    val mapStmt = Expr(mkCall(mkAttribute(mkName(acc), "append"), listOf(e.elt)))
    val ifStmt = when(c.ifs.size) {
      0 -> mapStmt
      1 -> If(c.ifs[0], listOf(mapStmt))
      else -> If(BoolOp(EBoolOp.And, c.ifs), listOf(mapStmt))
    }
    val iterElemTV = freshNames.fresh("?CIE")
    val resElemTV = freshNames.fresh("?CRE")
    val iterFI = FuncInst("pylib.iter", TLSig(listOf(TLTVar(iterElemTV)),
        args = listOf("a" to TLTClass("pylib.Iterable", listOf(TLTVar(iterElemTV)))), TLTClass("pylib.Iterator", listOf(TLTVar(iterElemTV)))))
    val hasNextFI = FuncInst("pylib.has_next", TLSig(listOf(TLTVar(iterElemTV)),
        args = listOf("a" to TLTClass("pylib.Iterator", listOf(TLTVar(iterElemTV)))), TLTClass("pylib.bool", listOf())))
    val nextFI = FuncInst("pylib.next", TLSig(listOf(TLTVar(iterElemTV)),
        args = listOf("a" to TLTClass("pylib.Iterator", listOf(TLTVar(iterElemTV)))), TLTVar(iterElemTV)))
    res.add(mkAssign(mkName(acc, true), PyList(emptyList(), valueAnno = ExTypeVar(resElemTV))))
    res.add(mkAssign(mkName(filterIter, true), mkCall(CTV(iterFI), listOf(c.iter))))
    res.add(While(mkCall(CTV(hasNextFI), listOf(mkName(filterIter))), listOf(
        mkAssign(mkName(tgt.id, true), mkCall(CTV(nextFI), listOf(mkName(filterIter)))),
        ifStmt)
    ))
    return mkName(acc) to res
  }

  fun transformExpr(e: TExpr, ctx: ExprTyper): Pair<TExpr, List<Stmt>> {
    val (exprs, assembler) = deconstruct(e)
    val (transformedExprs, stmts) = exprs.map { transformExpr(it, ctx) }.unzip()
    val res = assembler.assemble(transformedExprs)
    val (res2, stmts2) = if (res is GeneratorExp && checkSideEffects(res.elt, ctx, true)) {
      transform(res)
    } else res to emptyList()
    return res2 to stmts.flatten().plus(stmts2)
  }

  fun doTransform(s: Stmt, ctx: ExprTyper): List<Stmt>? {
    if (s is AnnAssign) {
      val (e, stmts) = transformExpr(s.value!!, ctx)
      return stmts.plus(s.copy(value = e))
    } else if (s is Assign) {
      val (e, stmts) = transformExpr(s.value, ctx)
      return stmts.plus(s.copy(value = e))
    } else return null
  }

  fun transform(c: List<Stmt>, ctx: ExprTyper): Pair<ExprTyper, List<Stmt>> {
    val res = mutableListOf<Stmt>()
    var foundNew = false
    var currCtx = ctx
    for(s in c) {
      val (newCtx, newS) = transform(s, currCtx)
      if (newS.size != 1 || newS[0] != s) {
        foundNew = true
      }
      res.addAll(newS)
      currCtx = newCtx
    }
    return currCtx to (if (foundNew) res else c)
  }

  fun transform(s: Stmt, ctx: ExprTyper): Pair<ExprTyper, List<Stmt>> {
    val res = doTransform(s, ctx)
    return if (res != null && (res.size != 1 || res[0] != s)) {
      ctx to res
    } else {
      defaultTransform(s, ctx)
    }
  }

  fun defaultTransform(s: Stmt, ctx: ExprTyper): Pair<ExprTyper, List<Stmt>> {
    return when (s) {
      is If -> {
        val (bodyCtx, newBody) = transform(s.body, ctx)
        val (elseCtx, newOrelse) = transform(s.orelse, ctx)
        val newCtx = mergeExprTypers(bodyCtx, elseCtx)
        newCtx to listOf(if (newBody == s.body && newOrelse == s.orelse) s
        else s.copy(body = newBody, orelse = newOrelse))
      }
      is While -> {
        val (bodyCtx, newBody) = transform(s.body, ctx)
        ctx to listOf(if (newBody == s.body) s else s.copy(body = newBody))
      }
      is For -> {
        val (bodyCtx, newBody) = transform(s.body, ctx)
        ctx to listOf(if (newBody == s.body) s else s.copy(body = newBody))
      }
      else -> ctx to listOf(s)
    }
  }

  fun transform(f: FunctionDef): FunctionDef {
    val typer = typer.updated(getFunArgs(f).map { it.first.arg to parseType(typer, it.first.annotation!!) })
    val (_, newBody) = transform(f.body, typer)
    return if (newBody == f.body) f else f.copy(body = newBody)
  }
}