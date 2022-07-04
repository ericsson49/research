package onotole.eval

import onotole.Attribute
import onotole.BinOp
import onotole.BoolOp
import onotole.Bytes
import onotole.CTVal
import onotole.ClassVal
import onotole.Compare
import onotole.Constant
import onotole.Index
import onotole.NameConstant
import onotole.Num
import onotole.RTType
import onotole.Slice
import onotole.Sort
import onotole.Str
import onotole.Subscript
import onotole.TExpr
import onotole.TPyBool
import onotole.TPyBytes
import onotole.TPyInt
import onotole.TPyNone
import onotole.TPyStr
import onotole.TypeResolver
import onotole.UnaryOp
import onotole.asType
import onotole.fail
import onotole.identifier
import onotole.mkCall
import onotole.resolveAttrType
import onotole.type_inference.FAtom
import onotole.type_inference.TypingCtx
import onotole.type_inference.resolveAttributeGet
import onotole.type_inference.resolveIndexGet
import onotole.type_inference.resolveSliceGet
import onotole.type_inference.toClassVal
import onotole.type_inference.toFAtom
import onotole.type_inference.toTLTClass

interface NCtx<V> {
  fun resolve(n: String): V?
  fun updated(vs: Collection<Pair<String, V>>): NCtx<V>
}

interface BaseExprEvaluator<Sort, T> {
  val boolT: T
  fun evalConstant(c: Constant): T
  fun evalAttr(v: T, attr: identifier): T
  fun evalIndexGet(v: T, idx: T): T
  fun evalSliceGet(v: T, lower: T?, upper: T?, step: T?): T

  fun eval(e: TExpr): T {
    return when(e) {
      is Constant -> evalConstant(e)
      is Attribute -> evalAttr(eval(e.value), e.attr)
      is Subscript -> {
        val valueV = eval(e.value)
        when(e.slice) {
          is Index -> evalIndexGet(valueV, eval(e.slice.value))
          is Slice -> evalSliceGet(valueV,
                e.slice.lower?.let { eval(it) },
                e.slice.upper?.let { eval(it) },
                e.slice.step?.let { eval(it) })
          else -> fail()
        }
      }
      is BinOp -> eval(mkCall("<${e.op}>", listOf(e.left, e.right)))
      is BoolOp -> boolT
      is Compare -> boolT
      is UnaryOp -> eval(mkCall("<${e.op}>", listOf(e.operand)))
      else -> TODO()
    }
  }
}

class RTTBaseExprEvaluator(): BaseExprEvaluator<Sort, RTType> {
  override val boolT: RTType = TPyBool
  override fun evalConstant(c: Constant): RTType {
    return when(c) {
      is Num -> TPyInt
      is Str -> TPyStr
      is Bytes -> TPyBytes
      is NameConstant -> when (c.value) {
        null -> TPyNone
        true, false -> boolT
        else -> fail("unsupported $c")
      }
    }
  }
  override fun evalAttr(v: RTType, attr: identifier): RTType {
    return v.resolveAttrType(attr).asType()
  }

  override fun evalIndexGet(v: RTType, idx: RTType): RTType {
    return TypeResolver.resolveSubscriptType(v, listOf(idx)).asType()
  }

  override fun evalSliceGet(v: RTType, lower: RTType?, upper: RTType?, step: RTType?): RTType {
    return TypeResolver.resolveSubscriptType(v, lower, upper, step)
  }
}

class CTVBaseExprEvaluator(val typingCtx: TypingCtx): BaseExprEvaluator<CTVal, ClassVal> {
  override val boolT: ClassVal = ClassVal("pylib.bool")
  override fun evalConstant(c: Constant): ClassVal {
    return when(c) {
      is Str -> ClassVal("pylib.str")
      is Bytes -> ClassVal("pylib.bytes")
      is Num -> ClassVal("pylib.int")
      is NameConstant -> when (c.value) {
        null -> ClassVal("pylib.None")
        is Boolean -> boolT
        else -> TODO()
      }
    }
  }

  private fun ClassVal.toFAtom() = this.toTLTClass().toFAtom(emptyMap()) as FAtom

  override fun evalAttr(v: ClassVal, attr: identifier): ClassVal {
    return resolveAttributeGet(v.toFAtom(), attr).toClassVal()
  }

  override fun evalIndexGet(v: ClassVal, idx: ClassVal): ClassVal {
    with(typingCtx) {
      return resolveIndexGet(v.toFAtom(), idx.toFAtom()).toClassVal()
    }
  }

  override fun evalSliceGet(v: ClassVal, lower: ClassVal?, upper: ClassVal?, step: ClassVal?): ClassVal {
    return resolveSliceGet(v.toFAtom(), lower?.toFAtom(), upper?.toFAtom(), step?.toFAtom()).toClassVal()
  }
}