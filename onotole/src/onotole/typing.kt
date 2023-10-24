package onotole

import java.util.*

sealed class Sort
sealed class MetaType: Sort()

data class PackageRef(val name: String): MetaType()
data class Clazz(val name: String, val tParams: List<RTType> = emptyList()): MetaType()
data class MetaClass(val name: String): MetaType()
sealed class FuncRef: MetaType()
data class SpecialFuncRef(val name: String): FuncRef()
data class NamedFuncRef(val name: String): FuncRef()
data class PartiallyAppliedFuncRef(val func: NamedFuncRef, val params: List<RTType>): FuncRef()

sealed class RTType: Sort()
data class NamedType(val name: String, val tParams: List<RTType> = emptyList(), val eParams: List<TExpr> = emptyList()): RTType() {
  override fun toString() = if (tParams.size + eParams.size > 0)
    tParams.plus(eParams).joinToString(",", "$name<", ">")
  else name
}
class UnificationException(val unif: List<Pair<TypeVar,RTType>>): RuntimeException("unification request")

data class TypeVar(val name: String) : RTType() {
  override fun toString() = "'" + name
}

data class FunType(val argTypes: List<RTType>, val retType: RTType) : RTType() {
  override fun toString() = argTypes.joinToString(",", "(", ")") + "->" + retType
}

data class FArg(val name: String, val type: RTType, val default: TExpr? = null) {
  override fun toString() = name + ": " + type + (default?.let { " = $it" } ?: "")
}

val TPyNothing = NamedType("pylib.Nothing")
val TPyObject = NamedType("pylib.object")
val TPyInt = NamedType("pylib.int")
val TPyStr = NamedType("pylib.str")
val TPyBytes = NamedType("pylib.bytes")
val TPyBool = NamedType("pylib.bool")
val TPyNone = NamedType("pylib.None")

fun TPySet(t: RTType) = NamedType("pylib.Set", listOf(t))
fun TPyList(t: RTType) = NamedType("pylib.PyList", listOf(t))
fun TPyDict(k: RTType, v: RTType) = NamedType("pylib.Dict", listOf(k, v))
fun TPyIterator(t: RTType) = NamedType("pylib.Iterator", listOf(t))
fun TPyIterable(t: RTType) = NamedType("pylib.Iterable", listOf(t))
fun TPyCollection(t: RTType) = NamedType("pylib.Collection", listOf(t))
fun TPySequence(t: RTType) = NamedType("pylib.Sequence", listOf(t))
fun TPyTuple(a: RTType, b: RTType) = NamedType("pylib.Tuple", listOf(a,b))
fun TPyTuple(p: Pair<RTType,RTType>) = TPyTuple(p.first,p.second)
fun TPyTuple(els: List<RTType>) = NamedType("pylib.Tuple", els)
fun TPyMapping(k: RTType, v: RTType) = NamedType("pylib.Mapping", listOf(k, v))
fun TPyMutableSequence(t: RTType) = NamedType("pylib.MutableSequence", listOf(t))
fun TPyOptional(t: RTType) = NamedType("pylib.Optional", listOf(t))


fun RTType.toTExpr(): TExpr = when(this) {
  is NamedType -> {
    if (this == TPyNone) {
      NameConstant(null)
    } else if (this.tParams.isEmpty()) {
      mkName(this.name)
    } else {
      val t = mkName(this.name)
      val i = if (this.tParams.size == 1) this.tParams[0].toTExpr() else Tuple(this.tParams.map { it.toTExpr() }, ExprContext.Load)
      mkSubscript(t, Index(i))
    }
  }
  else -> TODO()
}

fun getSeqElemTyp(x: RTType) = getNamedAncestorF(x, "pylib.Sequence").tParams[0]

fun getIterableElemType(x: RTType) = getNamedAncestorF(x, "pylib.Iterable").tParams[0]

fun getMapLikeValueElemTyp(x: RTType): RTType {
  val ancestors = getNamedAncestors(x)
  return ancestors["pylib.Sequence"]?.tParams?.get(0)
          ?: ancestors["pylib.Mapping"]?.tParams?.get(1)
          ?: fail("$x is not a subclass of Sequence or Mapping")
}

fun getMappingTypeParams(x: RTType): Pair<RTType,RTType> {
  val anc = getNamedAncestorF(x, "pylib.Mapping")
  return anc.tParams[0] to anc.tParams[1]
}

fun getCommonSuperType(ts: Iterable<RTType>): RTType =
    ts.reduce(::getCommonSuperType)

fun getCommonSuperType(a: RTType, b: RTType): RTType {
  if (a == b) return a
  if (a == TPyNothing) return b
  if (b == TPyNothing) return a
  if (a == TPyObject || b == TPyObject) return TPyObject
  fun testOptional(t: RTType) = t == TPyNone || t is NamedType && t.name == "Optional"
  fun extractOptionalTypeParam(t: RTType): RTType = when {
    t == TPyNone -> TPyNothing
    t is NamedType && t.name == "pylib.Optional" -> t.tParams[0]
    else -> t
  }
  if (testOptional(a) || testOptional(b)) {
    return TPyOptional(getCommonSuperType(extractOptionalTypeParam(a), extractOptionalTypeParam(b)))
  }
  val commonSuperTypes = getAncestorClasses(a).intersect(getAncestorClasses(b))
  return commonSuperTypes.sortedWith { o1, o2 -> if (o1 == o2) 0 else if (isSubType(o1!!, o2!!)) -1 else 1 }.first()
}

val ancestorsCache = mutableMapOf<RTType,List<RTType>>()
fun _getAncestorClasses(a: RTType): List<RTType> {
  val r = listOf(a)
  return if (a == TPyObject) r else r.plus(getAncestorClasses(getSuperClass(a)))
}
fun getAncestorClasses(a: RTType): List<RTType> = ancestorsCache.getOrPut(a) { _getAncestorClasses(a) }

fun getSuperClass(a: RTType) = a.typeInfo.baseType?.asType() ?: TPyObject

fun _isSubType1(a: RTType, b: NamedType): Boolean = b in getAncestorClasses(a)

fun getTypePramVariances(cls: String, size: Int): Triple<List<Int>,List<Int>,List<Int>> = when(cls) {
  "pylib.Iterable", "pylib.Iterator", "pylib.Collection", "pylib.Sequence", "pylib.Optional" -> Triple(listOf(0), emptyList(), emptyList())
  "pylib.Mapping" -> Triple(listOf(0,1), emptyList(), emptyList())
  "pylib.Tuple" -> Triple((0 until size).toList(), emptyList(), emptyList())
  "pylib.PyList", "List", "Vector", "pylib.Set" -> Triple(emptyList(), listOf(0), emptyList())
  "pylib.Dict" -> Triple(emptyList(), listOf(0,1), emptyList())
  "<Outcome>" -> Triple(listOf(0), emptyList(), emptyList())
  else -> TODO("Variances for $cls are not implemented")
}

fun unifEx(a: TypeVar, b: RTType): Nothing =
        throw UnificationException(listOf(a to b))
fun isSubType(a: RTType, b: RTType): Boolean = a == b
        || b is NamedType && a == TPyNone
        || b is NamedType && isSimpleType(b) && _isSubType1(a, b)
        || b is NamedType && isGenType(b) && b.name == "Optional" && (a == TPyNone || isSubType(a, b.tParams[0]))
        || b is NamedType && isGenType(b) && b.name == "<Outcome>"
              && a is NamedType && (a.name == "<Exception>" || a.name == "<Result>" && isSubType(a.tParams[0], b.tParams[0]))
        || b is NamedType && _isSubType2(a,b)

fun getNamedAncestorF(x: RTType, ancestor: String): NamedType {
  return getNamedAncestors(x)[ancestor] ?: fail("$x is not a subclass of $ancestor")
}

fun getNamedAncestors(x: RTType): Map<String, NamedType> {
  return getAncestorClasses(x).filterIsInstance<NamedType>().associateBy { it.name }
}

fun _isSubType2(a: RTType, b: NamedType): Boolean {
  val matchingSCofA = getNamedAncestors(a)[b.name]
  return if (matchingSCofA != null) {
    if (matchingSCofA.tParams.size != b.tParams.size) fail("${b.name} type parameter do not match")
    val (coVar, inVar, contraVar) = getTypePramVariances(b.name, b.tParams.size)
    (coVar.all { canAssignOrCoerceTo(matchingSCofA.tParams[it], b.tParams[it]) }
            && inVar.all { matchingSCofA.tParams[it] == b.tParams[it] }
            && contraVar.all { isSubType(b.tParams[it], matchingSCofA.tParams[it]) })
  } else {
    false
  }
}

fun isSimpleType(a: RTType) = a is NamedType && a.tParams.isEmpty()
fun isGenType(a: RTType) = a is NamedType && a.tParams.isNotEmpty()

fun asGenType(a: RTType): NamedType =
    if (a is NamedType && isGenType(a))
      a
    else
      fail()

data class FunSignature(val args: List<FArg>, val retType: RTType) {
  val defaults: List<TExpr> = args.filter { it.default != null }.map { it.default!! }
}

abstract class TypeInfo {
  val comparable: Boolean
    get() = _comparable ?: base?.comparable ?: false

  open val _comparable: Boolean? = null

  abstract val type: Sort
  open val baseType: Sort? = null
  val base: TypeInfo? get() = baseType?.typeInfo
  open val attrs: List<Pair<String,Sort>> = emptyList()
  open fun getAttr(a: String): TypeInfo? {
    return getLocalAttr(a) ?: base?.getAttr(a)
  }
  private fun getLocalAttr(a: String): TypeInfo? = attrs.find { it.first == a }?.second?.typeInfo
}

val TypeInfo.coercibleToBool: Boolean get() {
  return this.getAttr("__bool__") != null || this.base?.coercibleToBool ?: false
}

class DataTInfo(
        name: String,
        tParams: List<RTType> = emptyList(),
        override val baseType: RTType? = null,
        override val attrs: List<Pair<String,Sort>> = emptyList(),
        override val _comparable: Boolean? = null,
): TypeInfo() {
  override val type = NamedType(name, tParams)
}

class PackageTI(name: String, override val attrs: List<Pair<String,Sort>> = emptyList()): TypeInfo() {
  override val type = PackageRef(name)
}

class MetaClassTInfo(
        val name: String,
        val nTParams: Int? = null, val nEParams: Int = 0,
        val _attrs: List<Pair<String, Sort>> = emptyList(),
        val baseClassF: (List<RTType>) -> RTType): TypeInfo() {
  override val type = MetaClass(name)
}

open class FuncRefTI(override val type: FuncRef): TypeInfo() {
  constructor(name: String): this(NamedFuncRef(name))
  constructor(fullFunc: NamedFuncRef, params: List<RTType>): this(PartiallyAppliedFuncRef(fullFunc, params))
}

class AnonymousFunTI(override val type: FunType): TypeInfo()
