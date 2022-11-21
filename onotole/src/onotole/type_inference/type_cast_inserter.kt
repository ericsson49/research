package onotole.type_inference

import onotole.Assert
import onotole.Assign
import onotole.Attribute
import onotole.Break
import onotole.CTV
import onotole.Call
import onotole.ClassVal
import onotole.Constant
import onotole.Continue
import onotole.Expr
import onotole.ExprTyper
import onotole.FunType
import onotole.FuncInst
import onotole.FunctionDef
import onotole.If
import onotole.Lambda
import onotole.Name
import onotole.Pass
import onotole.RTType
import onotole.Return
import onotole.Stmt
import onotole.Subscript
import onotole.TExpr
import onotole.TPyBool
import onotole.While
import onotole.asType
import onotole.checkSigTypes
import onotole.fail
import onotole.matchSig
import onotole.mkCall
import onotole.parseType
import onotole.util.deconstructS
import onotole.util.mergeExprTypers
import onotole.util.toClassVal
import onotole.util.toFAtom
import onotole.util.toTExpr

abstract class TypeCastInserter {
  abstract fun castFunc(from: RTType, to: RTType): TExpr?
  fun tryCast(e: TExpr, from: RTType, to: RTType): TExpr {
    return castFunc(from, to)?.let { mkCall(it, listOf(e)) } ?: e
  }

  fun merge(a: ExprTyper, b: ExprTyper) = mergeExprTypers(a, b)

  context (TypingCtx)
  fun procExpr(e: TExpr, typer: ExprTyper): TExpr {
    return when(e) {
      is CTV -> e
      is Lambda -> TODO()
      is Constant, is Name -> e
      is Attribute, is Subscript -> {
        val (exprs, assembler) = deconstructS(e)
        val newExprs = exprs.map { procExpr(it, typer) }
        assembler.assemble(newExprs)
      }
      is Call -> {
        val (exprs, assembler) = deconstructS(e)
        val fh = exprs[0]
        val args = exprs.subList(1, exprs.size)
        val argTypes = when {
          e.func is CTV && (e.func.v is ClassVal || e.func.v is FuncInst) -> {
            if (e.keywords.isNotEmpty()) TODO()
            if (e.func.v is ClassVal) {
              val kwdNames = e.keywords.map { it.arg!! }
              val argTypes = e.args.map { typer[it].asType() }
              val kwdTypes = e.keywords.map { it.arg!! to typer[it.value].asType() }
              val sigs = getCtorSignatures(e.func.v).filter { matchSig(it, e.args.size, kwdNames) && checkSigTypes(it, argTypes, kwdTypes) }
              if (sigs.size != 1) fail()
              val sig = sigs.first()
              sig.args.map { parseType(typer, it.second.toFAtom(emptyMap()).toClassVal().toTExpr()) }
            } else (typer.get(e.func) as FunType).argTypes
          }
          else -> null
        }
        val newFH = procExpr(fh, typer)
        val newArgs = if (argTypes != null)
          args.zip(argTypes).map { (a,b) -> procExpr(a, b, typer) }
        else
          args.map { procExpr(it, typer) }
        assembler.assemble(listOf(newFH).plus(newArgs))
      }
      else -> TODO()
    }
  }

  context (TypingCtx)
  fun procExpr(e: TExpr, expected: RTType, typer: ExprTyper): TExpr {
    val res = procExpr(e, typer)
    val t = typer[res].asType()
    return if (t != expected) tryCast(res, t, expected) else res
  }

  context (TypingCtx)
  fun procStmts(cs: Collection<Stmt>, typer: ExprTyper): Pair<List<Stmt>, ExprTyper> {
    val res = mutableListOf<Stmt>()
    var currCtx = typer
    cs.forEach {
      val (newS, newCtx) = procStmt(it, currCtx)
      res.add(newS)
      currCtx = newCtx
    }
    return res to currCtx
  }

  context (TypingCtx)
  fun procStmt(s: Stmt, typer: ExprTyper): Pair<Stmt, ExprTyper> {
    return when(s) {
      is Expr -> s.copy(value = procExpr(s.value, typer)) to typer
      is Assert -> s.copy(test = procExpr(s.test, typer), msg = s.msg?.let { procExpr(it, typer) }) to typer
      is Return -> s.copy(value = s.value?.let { procExpr(it, typer) }) to typer
      is If -> {
        val test = procExpr(s.test, TPyBool, typer)
        val (body, bodyCtx) = procStmts(s.body, typer)
        val (orelse, orelseCtx) = procStmts(s.orelse, typer)
        val newCtx = merge(bodyCtx, orelseCtx)
        s.copy(test = test, body = body, orelse = orelse) to newCtx
      }
      is While -> {
        val test = procExpr(s.test, TPyBool, typer)
        val (body, _) = s.body.map { procStmt(it, typer) }.unzip()
        s.copy(test = test, body = body) to typer
      }
      is Pass, is Break, is Continue -> s to typer
      is Assign -> {
        when(s.target) {
          is Name -> {
            val newCtx = typer.updated(listOf(s.target.id to typer.get(s.value)))
            s.copy(value = procExpr(s.value, typer)) to newCtx
          }
          is Attribute -> {
            val expectedType = typer.get(s.target).asType()
            val value = procExpr(s.value, expectedType, typer)
            val target = s.target.copy(value = procExpr(s.target.value, typer))
            s.copy(target = target, value = value) to typer
          }
          is Subscript -> TODO()
          else -> TODO()
        }
      }
      else -> TODO()
    }
  }
}

context (TypingCtx)
fun explicitifyImplicitConversions(f: FunctionDef, globalTyper: ExprTyper): FunctionDef {
  val argTypes = f.args.args.map { it.arg to parseType(globalTyper, it.annotation!!) }
  val typer = globalTyper.updated(argTypes)
  val tci = object : TypeCastInserter() {
    override fun castFunc(from: RTType, to: RTType): TExpr? {
      return null
    }
  }
  return f.copy(body = tci.procStmts(f.body, typer).first)
}