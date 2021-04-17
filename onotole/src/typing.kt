sealed class Sort
sealed class MetaType: Sort()

data class PackageRef(val name: String): MetaType()
data class Clazz(val name: String, val tParams: List<RTType> = emptyList()): RTType()
data class MetaClass(val name: String): MetaType()
sealed class FuncRef: MetaType()
data class SpecialFuncRef(val name: String): FuncRef()
data class NamedFuncRef(val name: String): FuncRef()
sealed class TFParam
data class TypParam(val type: RTType): TFParam()
data class PartiallyAppliedFuncRef(val func: NamedFuncRef, val params: List<RTType>): FuncRef()

sealed class RTType: Sort()
data class NamedType(val name: String, val tParams: List<RTType> = emptyList(), val eParams: List<TExpr> = emptyList()): RTType() {
  override fun toString() = if (tParams.size + eParams.size > 0)
    tParams.plus(eParams).joinToString(",", "name<", ">")
  else name
}
data class MetaNamedType(val name: String, val typeVars: List<String> = emptyList()): Sort()
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

val TPyObject = NamedType("object")
val TPyInt = NamedType("int")
val TPyStr = NamedType("str")
val TPyBytes = NamedType("bytes")
val TPyBool = NamedType("bool")
val TPyNone = NamedType("None")

fun TPySet(t: RTType) = NamedType("Set", listOf(t))
fun TPyList(t: RTType) = NamedType("PyList", listOf(t))
fun TPyDict(k: RTType, v: RTType) = NamedType("Dict", listOf(k, v))
fun TPyIterable(t: RTType) = NamedType("Iterable", listOf(t))
fun TPyCollection(t: RTType) = NamedType("Collection", listOf(t))
fun TPySequence(t: RTType) = NamedType("Sequence", listOf(t))
fun TPyTuple(a: RTType, b: RTType) = NamedType("Tuple", listOf(a,b))
fun TPyTuple(p: Pair<RTType,RTType>) = TPyTuple(p.first,p.second)
fun TPyTuple(els: List<RTType>) = NamedType("Tuple", els)
fun TPyMapping(k: RTType, v: RTType) = NamedType("Mapping", listOf(k, v))
fun TPyMutableSequence(t: RTType) = NamedType("MutableSequence", listOf(t))


fun getSeqElemTyp(x: RTType) = getNamedAncestorF(x, "Sequence").tParams[0]

fun getIterableElemType(x: RTType) = getNamedAncestorF(x, "Iterable").tParams[0]

fun getMapLikeValueElemTyp(x: RTType): RTType {
  val ancestors = getNamedAncestors(x)
  return ancestors["Sequence"]?.tParams?.get(0)
          ?: ancestors["Mapping"]?.tParams?.get(1)
          ?: fail("$x is not a subclass of Sequence or Mapping")
}

fun getMappingTypeParams(x: RTType): Pair<RTType,RTType> {
  val anc = getNamedAncestorF(x, "Mapping")
  return anc.tParams[0] to anc.tParams[1]
}

fun getCommonSuperType(ts: Iterable<RTType>): RTType = ts.reduce(::getCommonSuperType)

fun getCommonSuperType(a: RTType, b: RTType): RTType {
  if (a == b) return a
  if (a == TPyObject || b == TPyObject) return TPyObject
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
  "Iterable", "Collection", "Sequence", "Optional" -> Triple(listOf(0), emptyList(), emptyList())
  "Mapping" -> Triple(listOf(0,1), emptyList(), emptyList())
  "Tuple" -> Triple((0 until size).toList(), emptyList(), emptyList())
  "PyList", "List", "Vector", "Set" -> Triple(emptyList(), listOf(0), emptyList())
  "Dict", "PyDict" -> Triple(emptyList(), listOf(0,1), emptyList())
  else -> TODO("Variances for $cls are not implemented")
}

fun unifEx(a: TypeVar, b: RTType): Nothing =
        throw UnificationException(listOf(a to b))
fun isSubType(a: RTType, b: RTType): Boolean = a == b
        || b is NamedType && isSimpleType(b) && _isSubType1(a, b)
        || b is NamedType && isGenType(b) && b.name == "Optional" && (a == TPyNone || isSubType(a, b.tParams[0]))
        || b is NamedType && _isSubType2(a,b)

fun getNamedAncestorF(x: RTType, ancestor: String): NamedType {
  return getNamedAncestors(x)[ancestor] ?: fail("$x is not a subclass of $ancestor")
}

fun getNamedAncestors(x: RTType): Map<String, NamedType> {
  return getAncestorClasses(x).filterIsInstance<NamedType>().map { it.name to it }.toMap()
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

fun asGenType(a: RTType): NamedType = if (a is NamedType && isGenType(a)) a else fail()

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

fun _getExprType(e: TExpr) = TypeResolver.topLevelTyper[e]

class AnonymousFunTI(override val type: FunType): TypeInfo()
