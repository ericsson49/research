package onotole

import java.util.*

class TypeInference(val cfg: CFGraphImpl, val typer: ExprTyper) {
  fun merge(a: RTType, b: RTType): RTType = getCommonSuperType(a,b)
  fun merge(ts: List<RTType>): RTType = ts.reduce(::merge)

  fun typingF(e: TExpr, varTypes: Map<String, RTType>): RTType {
    val et = typer.updated(varTypes)
    return et[e].asType()
  }

  fun inferResultType(s: TExpr, varTypes: Map<String, RTType>): RTType =
      if(s is Call && s.func is Name && s.func.id == "<Phi>") {
        merge(s.args.map { varTypes[(it as Name).id]!! })
      } else typingF(s, varTypes)

  fun inferTypes(init: Map<String,RTType>): Map<String,RTType> {
    val stmts = cfg.allStmts

    val varTypes: MutableMap<String, RTType> = stmts.flatMap { it.lNames }.map {  it to TPyNothing }.toMap().toMutableMap()
    varTypes.putAll(init)
    do {
      var updated = false
      stmts.forEach { s ->
        val t = if (s.lval is VarLVal && s.lval.t != null)
          parseType(typer, s.lval.t)
        else if (s.lval is EmptyLVal)
          TPyNone
        else
          inferResultType(s.rval, varTypes)
        val updates: List<Pair<String,RTType>> = when (s.lval) {
          is EmptyLVal -> emptyList()
          is VarLVal -> listOf(s.lval.v to t)
          is FieldLVal -> emptyList()
          is SubscriptLVal -> emptyList()
        }
        updates.forEach { (v, t) ->
          val prev = varTypes[v] ?: TPyNothing
          if (prev != t) {
            val mt = merge(t, prev)
            if (prev != mt) {
              updated = true
              varTypes[v] = mt
            }
          }
        }
      }
    } while (updated)
    val nothings = varTypes.filterValues { it == TPyNothing }.keys
    if (nothings.isNotEmpty())
      fail("Cannot infer type for $nothings")
    return varTypes
  }
}

val inferTypes_FD = MemoizingFunc(::_inferTypes_FunctionDef)

fun _inferTypes_FunctionDef(f: FunctionDef): Map<String,RTType> {
  val cfg = convertToCFG(f)
  val ssa = convertToSSA(cfg)
  return _inferTypes_CFG(ssa)
}

fun _inferTypes_CFG(ssa: CFGraphImpl): Map<String, RTType> {
  val typer = TypeResolver.topLevelTyper

  val init = ssa.get(ssa.entry).stmts.map {
    val lv = it.lval as VarLVal
    lv.v to parseType(typer, lv.t!!)
  }.toMap()
  return TypeInference(ssa, typer).inferTypes(init)
}
