

data class ClassDescr(
        val name: String,
        val baseType: RTType?,
        val typeParams: Pair<Int,Int> = Pair(0,0),
        val comparable: Boolean? = null,
        val attrs: List<Pair<String, Sort>> = emptyList())


interface ClsBuilder {
    val cls: String
    fun t(n: String) = NamedType(n)
    fun fh(n: String) = NamedFuncRef(n)
    fun staticAttr(name: String, type: Sort)
    fun staticAttr(name: String, type: String) = staticAttr(name, t(type))
    fun staticMeth(name: String) = staticAttr(name, fh("$cls.$name"))
    fun attr(name: String, type: Sort)
    fun attr(name: String, type: String) = attr(name, t(type))
    fun meth(name: String) = attr(name, fh("$cls:$name"))
}

fun classDef(name: String,
             base: String? = null,
             baseType: RTType? = null,
             nTParms: Int = 0,
             nEParams: Int = 0,
             baseParams: List<Int> = emptyList(),
             comparable: Boolean? = null,
             f: (ClsBuilder.() -> Unit)? = null) {
    val attrs = mutableListOf<Pair<String,Sort>>()
    if (f != null) {
        val builder = object : ClsBuilder {
            override val cls = name
            override fun staticAttr(name: String, type: Sort) {
                attrs.add(name to type)
            }
            override fun attr(name: String, type: Sort) {
                attrs.add(name to type)
            }
        }
        builder.f()
    }
    if (nTParms == 0 && nEParams == 0) {
        TypeResolver.register(convertToTInfo(ClassDescr(name,
                baseType ?: base?.let { NamedType(it) },
                attrs = attrs, comparable = comparable)))
    } else {
        TypeResolver.register(MetaClassTInfo(name, nTParms, nEParams, _attrs = attrs) { tps ->
            NamedType(base ?: "object", baseParams.map { tps[it] })
        })
    }
}

fun convertToTInfo(cd: ClassDescr): TypeInfo {
    val ctors = cd.baseType?.let { base ->
        FuncCollection(listOf(
                FunSignature(listOf(FArg("value", base)), NamedType(cd.name)),
                FunSignature(emptyList(), NamedType(cd.name)),
        ))
    }
    return DataTInfo(
            cd.name,
            baseType = cd.baseType,
            attrs = cd.attrs,
            _comparable = cd.comparable,
            //_coercableToBool = if (cd.name == "boolean" || cd.name == "bool") true else null,
            //ctors = ctors
    )
}


fun funDef(name: String, args: List<String>, retType: String): FunDecl {
    fun parseArg(i: Int, a: String): FArg {
        val parts = a.split(":")
        return when (parts.size) {
            1 -> FArg("_$i", NamedType(parts[0]))
            2 -> FArg(parts[0], NamedType(parts[1]))
            else -> fail("wrong format")
        }
    }
    return FunDecl(name, args.mapIndexed(::parseArg), NamedType(retType))
}

fun funDef(name: String, args: List<RTType>, retType: RTType): FunDecl {
    return FunDecl(name, args.mapIndexed { i: Int, t: RTType -> FArg("_$i", t) }, retType)
}
