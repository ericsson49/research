package onotole.type_inference

import onotole.BinOp
import onotole.CTV
import onotole.Call
import onotole.ClassVal
import onotole.ConstExpr
import onotole.FuncInst
import onotole.Name
import onotole.NameConstant
import onotole.Num
import onotole.Str
import onotole.TExpr
import onotole.TypeResolver
import onotole.fail
import onotole.typelib.TLTClass
import onotole.typelib.TLTConst


fun classValToTLType(c: ClassVal): TLTClass {
  val tps = c.tParams.map { classValToTLType(it) }
  val eps = c.eParams.map { TLTConst(it) }
  return TLTClass(c.name, tps.plus(eps))
}
context (TypingCtx)
fun calcConstExprType(e: TExpr, cache: Map<String,FAtom>, consts: Map<String,TExpr>): FAtom = when(e) {
  is CTV -> when(e.v) {
    is ConstExpr -> calcConstExprType(e.v.e, cache, consts)
    else -> TODO()
  }
  is NameConstant -> when(e.value) {
    null -> FAtom("pylib.None")
    true, false -> FAtom("pylib.bool")
    else -> TODO()
  }
  is Str -> FAtom("pylib.str")
  is Num -> FAtom("pylib.int")
  is Name -> cache[e.id]
      ?: TODO()
  is BinOp -> {
    val left = calcConstExprType(e.left, cache, consts)
    val right = calcConstExprType(e.right, cache, consts)
    resolveAttributeCall(left, "__" + e.op.name + "__").first as FAtom
  }
  is Call -> {
    if (e.func is CTV) {
      when(e.func.v) {
        is ClassVal -> {
          if (e.func.v.eParams.isNotEmpty())
            fail()
          classValToTLType(e.func.v).toFAtom(emptyMap()) as FAtom
        }
        is FuncInst -> {
          e.func.v.sig.ret.toFAtom(emptyMap()) as FAtom
        }
        else -> TODO()
      }
    } else if (e.func is Name && e.func.id in TypeResolver.specialFuncNames) {
      val op = e.func.id.substring(1, e.func.id.length-1)
      if (op in TypeResolver.binOps) {
        if (e.args.size != 2) fail()
        if (e.keywords.isNotEmpty()) fail()
        val left = calcConstExprType(e.args[0], cache, consts)
        val right = calcConstExprType(e.args[1], cache, consts)
        resolveAttributeCall(left, "__" + op + "__").first as FAtom
      } else TODO()
    } else TODO()
  }
  else -> TODO()
}

fun inferConstTypes(consts: Collection<Pair<String, TExpr>>): Map<String, FAtom> {
  val cache = mutableMapOf<String, FAtom>()
  cache.putAll(TypingContext.constTypes)
  val constMap = consts.toMap()
  with(TypingContext) {
    return consts.associate { (c, e) ->
      val t = calcConstExprType(e, cache, constMap)
      cache[c] = t
      c to t
    }
  }
}
