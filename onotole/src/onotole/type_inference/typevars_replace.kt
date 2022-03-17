package onotole.type_inference

import onotole.CTV
import onotole.ClassVal
import onotole.ExprTransformer
import onotole.FuncInst
import onotole.TExpr
import onotole.typelib.TLTCallable
import onotole.typelib.TLTClass
import onotole.typelib.TLTConst
import onotole.typelib.TLTVar
import onotole.typelib.TLType

fun replaceTypeVars(t: TLType, tvars: Map<String,TLType>): TLType = when(t) {
  is TLTConst -> t
  is TLTVar -> tvars[t.name]!!
  is TLTClass -> t.copy(params = t.params.map { replaceTypeVars(it, tvars) })
  is TLTCallable -> t.copy(args = t.args.map { replaceTypeVars(it, tvars) }, ret = replaceTypeVars(t.ret, tvars))
}
fun ClassVal.toTLTClass(): TLTClass = TLTClass(this.name,
    this.tParams.map { it.toTLTClass() }.plus(this.eParams.map { TLTConst(it) }))
class TypeVarReplacer(val typeVars: Map<String,ClassVal>): ExprTransformer<Unit>() {
  override fun merge(a: Unit, b: Unit) {}

  override fun transform(e: TExpr, ctx: Unit, store: Boolean): TExpr {
    return when(e) {
      is CTV -> {
        when(e.v) {
          is FuncInst ->
            if (e.v.sig.tParams.isNotEmpty()) {
              val remap = e.v.sig.tParams.filterIsInstance<TLTVar>().map {
                it.name to (typeVars[it.name]?.toTLTClass()
                    ?: it)
              }.toMap()
              val tparams = e.v.sig.tParams.map { replaceTypeVars(it, remap) }
              val args = e.v.sig.args.map { it.copy(second = replaceTypeVars(it.second, remap)) }
              val ret = replaceTypeVars(e.v.sig.ret, remap)
              e.copy(v = e.v.copy(sig = e.v.sig.copy(tParams = tparams, args = args, ret = ret)))
            }
            else defaultTransform(e, ctx, store)
          else -> defaultTransform(e, ctx, store)
        }
      }
      else -> defaultTransform(e, ctx, store)
    }
  }
}