package onotole.type_inference

import onotole.Arguments
import onotole.Assign
import onotole.Attribute
import onotole.BinOp
import onotole.CTV
import onotole.Call
import onotole.ClassTemplate
import onotole.ClassVal
import onotole.ConstExpr
import onotole.ExTypeVar
import onotole.Expr
import onotole.ExprContext
import onotole.FuncInst
import onotole.FuncTempl
import onotole.FunctionDef
import onotole.Index
import onotole.Lambda
import onotole.Name
import onotole.NameConstant
import onotole.Num
import onotole.PkgVal
import onotole.PyDict
import onotole.PyList
import onotole.Slice
import onotole.Stmt
import onotole.Str
import onotole.Subscript
import onotole.TExpr
import onotole.Tuple
import onotole.TypeResolver
import onotole.fail
import onotole.mkAttribute
import onotole.mkCall
import onotole.mkIndex
import onotole.mkName
import onotole.mkSubscript
import onotole.pyPrintFunc
import onotole.rewrite.ExprTransformRule
import onotole.rewrite.RuleSetTransformer
import onotole.rewrite.StmtTransformRule
import onotole.rewrite.mkExprTransformRule
import onotole.typelib.TLSig
import onotole.typelib.TLTClass
import onotole.typelib.TLTVar
import onotole.typelib.TestResolver
import onotole.typelib.renameTVars

interface ResultEmmiter {
  fun rewrite(vararg s: Stmt)
}
fun tr(f: ResultEmmiter.(Stmt) -> Unit): StmtTransformRule {
  return { tmpVars, s ->
    var acc: MutableList<Stmt>? = null
    val re = object : ResultEmmiter {
      override fun rewrite(vararg s: Stmt) {
        if (acc == null) {
          acc = mutableListOf()
        }
        acc!!.addAll(s)
      }
    }
    re.f(s)
    acc
  }
}

interface ЕExprContext {
  fun eval(e: TExpr): TExpr
  fun result(e: TExpr): Nothing
  fun freshName(prefix: String = ""): String
}

fun <Ctx> exprRule(f: ЕExprContext.(TExpr, Ctx) -> Unit): ExprTransformRule<Ctx> {
  TODO()
}

fun exprRule(f: ЕExprContext.(TExpr) -> Unit): ExprTransformRule<Unit> {
  TODO()
}

fun <T: TExpr, Ctx> exprRuleFor(f: ЕExprContext.(T, Ctx) -> Unit): ExprTransformRule<Unit> {
  TODO()
}
fun <T: TExpr> exprRuleFor(f: ЕExprContext.(T) -> Unit): ExprTransformRule<Unit> {
  TODO()
}

val _aliasRule = exprRuleFor<Name, TestResolver> { e, ctx ->
  val resolvedName = ctx.resolveAlias(e.id) ?: e.id
  if (resolvedName != e.id)
    result(e.copy(id = resolvedName))
}
val _nameResolveRule = exprRuleFor<Name, TestResolver> { e, ctx ->
  val value = ctx.getVal(e.id)
  if (value != null)
    result(value)
}
val _staticNameResolver = exprRuleFor<Attribute> { e ->
  if (e.value is CTV) {
    when (val v = e.value.v) {
      is PkgVal -> result(Name(v.pkg + "." + e.attr, e.ctx))
      is ClassVal -> result(Name(v.name + "#" + e.attr, e.ctx))
      is ClassTemplate -> TODO()
      else -> {}
    }
  }
}
val _subscriptRule = exprRuleFor<Subscript> { e ->
  val value = eval(e.value)
  if (value is CTV && value.v is ClassTemplate) {
    val index = if (e.slice is Index) e.slice.value else fail()
    val indices = if (index is Tuple) index.elts else listOf(index)
    val clsTempl = value.v.clsTempl
    val (tparams, eparams) = if (clsTempl.name == "pylib.Tuple") {
      indices to emptyList()
    } else {
      if (indices.size != clsTempl.noTParams + clsTempl.noEParams)
        fail()
      val tparams = indices.subList(0, clsTempl.noTParams)
      val eparams = indices.subList(clsTempl.noTParams, clsTempl.noTParams + clsTempl.noEParams)
      tparams to eparams
    }
    if (!tparams.all { it is CTV && it.v is ClassVal }) fail()
    val eparams3 = eparams.map { if (it is CTV && it.v is ConstExpr) it else CTV(ConstExpr(it)) }
    val tparams2 = tparams.map { (it as CTV).v as ClassVal }
    val eparams2 = eparams3.map { it.v as ConstExpr }
    result(CTV(ClassVal(clsTempl.name, tparams2, eparams2)))
  } else {
    result(value)
  }
}
val _callRule = exprRuleFor<Call> { e ->
  val func = e.func
  fun isConstExprOrClassVal(e: TExpr) = e is CTV && (e.v is ConstExpr || e.v is ClassVal)
  if (func !is CTV && func !is Attribute && !(func is Name && func.id in TypeResolver.specialFuncNames))
    fail()
  if (func is Name && func.id in TypeResolver.specialFuncNames && e.args.all(::isConstExprOrClassVal)) {
    if (e.keywords.isNotEmpty()) fail()
    result(CTV(ConstExpr(e)))
  } else {
    if (func is CTV && func.v is FuncTempl) {
      val sigs = func.v.func.sigs
      fun matchSig(s: TLSig, noPosArgs: Int, kwds: List<String>): Boolean {
        if (noPosArgs > s.args.size)
          return false
        val kwdArgs = s.args.subList(noPosArgs, s.args.size).map { it.first }
        return kwds.minus(kwdArgs).isEmpty()
      }
      val sig = sigs.find { matchSig(it, e.args.size, e.keywords.map { it.arg!! }) }
          ?: fail()
      val tvRemap = sig.tParams.filterIsInstance<TLTVar>().associate { it.name to freshName("?$it") }
      result(e.copy(func = CTV(FuncInst(func.v.func.name, renameTVars(sig, tvRemap)))))
    }
  }
}
val _listRule = exprRuleFor<PyList> { e ->
  val tvv = TLTVar(freshName("?T"))
  val sig = TLSig(listOf(tvv), e.elts.indices.map { "_$it" to tvv }, TLTClass("pylib.PyList", listOf(tvv)))
  result(mkCall(CTV(FuncInst("pylib.PyList", sig)), e.elts))
}
val _dictRule = exprRuleFor<PyDict> { e ->
  val ktv = TLTVar(freshName("?K"))
  val vtv = TLTVar(freshName("?V"))
  val keys = Tuple(elts = e.keys, ctx = ExprContext.Load)
  val values = Tuple(elts = e.values, ctx = ExprContext.Load)
  val argTypes = listOf(
      "keys" to TLTClass("pylib.Sequence", listOf(ktv)),
      "values" to TLTClass("pylib.Sequence", listOf(vtv)))
  val resType = TLTClass("pylib.Dict", listOf(ktv, vtv))
  result(mkCall(CTV(FuncInst("pylib.Dict", TLSig(listOf(ktv, vtv), argTypes, resType))), listOf(keys, values)))
}
val _lambdaRule = exprRuleFor<Lambda> { e ->
  result(e.copy(
      args = e.args.copy(args = e.args.args.map {
        if (it.annotation != null) it
        else it.copy(annotation = CTV(ExTypeVar(freshName("?L"))))
      }),
      returns = e.returns ?: CTV(ExTypeVar(freshName("?R"))),
  ))
}

// beacon chain specific:
// `forOps` inline
// phase merge

// define name resolution order
// alias resolve
// mod+attr resolve
// class+attr resolve
// func template instantiation
// class template instantiation
// class & func name to CT value


// translation phases
// - phase merge + pre-processing
// - simplifications
// - compile time calc, name resolving, inserting type variables
// - SSA
// - type inference, type checking
// - type var inline
// - type dependent simplifications

// steps:
// - `forOps` inline
// - `enumerate` inline
// - alias resolution
// - static names resolution
// - name resolution
// - SSA
// - var declarations
// - type inference
// - func name decoration with type vars
// - class name decoration with type vars
// - class ctor handle resolution
// - func params reorg, fun handle resolution
// - implicit coercions
// - binops resolution, ops resolution in general
// - attribute resolution
// - subscript resolution
// - unique var names??
// - a-normal form


object AARuleSet {
  fun mkCallFromAttr(t: Attribute, v: TExpr) = Expr(mkCall(
      mkAttribute(t.value, "<set_attr>"),
      listOf(CTV(ConstExpr(Str(t.attr))), v)
  ))
  val NONE = CTV(ConstExpr(NameConstant(null)))
  fun mkCallFromSubscr(t: Subscript, v: TExpr): Expr {
    val idx = when(t.slice) {
      is Index -> t.slice.value
      is Slice -> mkCall("pylib.slice", listOf(
          t.slice.lower ?: NONE, t.slice.upper ?: NONE, t.slice.step ?: NONE))
      else -> TODO()
    }
    return Expr(mkCall(
        mkAttribute(t.value, "<set_at>"),
        listOf(idx, v)
    ))
  }
  val r1 = tr { s ->
    if (s is Assign && s.target is Attribute) {
      rewrite(mkCallFromAttr(s.target, s.value))
    }
  }
  val r4 = tr { s ->
    if (s is Assign && s.target is Subscript) {
      rewrite(mkCallFromSubscr(s.target, s.value))
    }
  }
}

fun makeCoercionInserter(typeOf: (TExpr) -> ClassVal): ExprTransformRule<ClassVal> {
  fun convert(e: TExpr, from: ClassVal, to: ClassVal): TExpr = TODO()
  return mkExprTransformRule { e, expectedType ->
    val exprType = typeOf(e)
    if (exprType != expectedType)
      convert(e, exprType, expectedType)
    else null
  }
}

class TypeDependentSimplifier(val typeOf: (TExpr) -> ClassVal) {
  fun isSubType(a: ClassVal, b: ClassVal): Boolean {
    TODO()
  }
  val binOp: ExprTransformRule<Unit> = mkExprTransformRule { e, _ ->
    if (e is BinOp) {
      val lt = typeOf(e.left)
      val rt = typeOf(e.right)
      if (isSubType(rt, lt) && lt != rt) {
        mkCall(mkAttribute(e.right, "__r" + e.op.name + "__"), listOf(e.left))
      } else {
        mkCall(mkAttribute(e.left, "__" + e.op.name + "__"), listOf(e.right))
      }
    } else null
  }
}

fun main() {
  val f = FunctionDef("t", args = Arguments(), body = listOf(
      Assign(mkAttribute("a","f"), mkName("e")),
      Assign(mkSubscript("c", mkIndex(0)), Num(1)),
      Assign(mkSubscript("d", Slice(null, Num(10), Num(1))), mkName("x"))
  ))
  pyPrintFunc(f)
  println("---")
  val ft = RuleSetTransformer(listOf(AARuleSet.r1, AARuleSet.r4)).transform(f)
  pyPrintFunc(ft)
}