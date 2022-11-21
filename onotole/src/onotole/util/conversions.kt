package onotole.util

import onotole.ClassVal
import onotole.ConstExpr
import onotole.ExprContext
import onotole.FunType
import onotole.Index
import onotole.Name
import onotole.NamedType
import onotole.Sort
import onotole.Subscript
import onotole.TExpr
import onotole.Tuple
import onotole.asClassVal
import onotole.fail
import onotole.type_inference.FAtom
import onotole.type_inference.FTerm
import onotole.type_inference.FVar
import onotole.typelib.TLTCallable
import onotole.typelib.TLTClass
import onotole.typelib.TLTConst
import onotole.typelib.TLTVar
import onotole.typelib.TLType

fun Sort.toFAtom(): FAtom {
  return when(this) {
    is NamedType -> toFAtom(this.toTLTClass())
    is FunType -> FAtom("pylib.Callable", argTypes.plus(retType).map { it.toFAtom() })
    else -> TODO()
  }
}

fun NamedType.toTLTClass(): TLTClass {
  return TLTClass(name, tParams.map { (it as NamedType).toTLTClass() }.plus(eParams.map { TLTConst(ConstExpr(it)) }))
}


fun ClassVal.toFAtom() = this.toTLTClass().toFAtom(emptyMap()) as FAtom

fun ClassVal.toTLTClass(): TLTClass = TLTClass(this.name,
    this.tParams.map { it.asClassVal().toTLTClass() }.plus(this.eParams.map { TLTConst(it) }))

fun ClassVal.toTExpr(): TExpr {
  val name = Name(this.name, ExprContext.Load)
  return if (this.tParams.size + this.eParams.size == 0)
    name
  else {
    val elems = this.tParams.map { when(it) { is ClassVal -> it.toTExpr(); else -> TODO()
    } }.plus(this.eParams.map { it.e })
    Subscript(name, Index(if (elems.size == 1) elems[0] else Tuple(elems, ExprContext.Load)), ExprContext.Load)
  }
}


fun TLType.toFAtom(tvm: Map<String, FTerm>): FTerm = when(this) {
  is TLTVar -> tvm[this.name] ?: FVar(this.name)
  is TLTClass -> FAtom(this.name, this.params.filter { it !is TLTConst }.map { it.toFAtom(tvm) })
  is TLTCallable -> FAtom("pylib.Callable", this.args.plus(this.ret).map { it.toFAtom(tvm) })
  else -> TODO()
}

fun toFTerm(t: TLType): FTerm = when(t) {
  is TLTClass -> toFAtom(t)
  is TLTVar -> FVar(t.name)
  else -> TODO()
}
fun toFAtom(cls: TLTClass): FAtom = FAtom(cls.name, cls.params.filter { it !is TLTConst }.map { toFTerm(it) })


fun FTerm.toClassVal(): ClassVal = when(this) {
  is FVar ->
    fail()
  is FAtom -> this.toClassVal()
}
fun FAtom.toClassVal(): ClassVal {
  return ClassVal(this.n, this.ps.map { it.toClassVal() })
}

