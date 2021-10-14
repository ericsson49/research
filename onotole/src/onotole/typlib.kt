package onotole

import onotole.lib_defs.FunDecl

sealed class TVInst
data class TVIndex(val i: Int): TVInst()
data class TVConcrete(val t: RTType): TVInst()

interface PackageBuilder {
    fun classDef(name: String,
                 base: String? = null,
                 baseType: RTType? = null,
                 nTParms: Int = 0,
                 nEParams: Int = 0,
                 baseParams: List<TVInst> = emptyList(),
                 comparable: Boolean? = null,
                 f: (ClsBuilder.() -> Unit)? = null)
    fun funDef(name: String, args: List<RTType>, retType: RTType)
    fun registerName(name: String)
}
fun packageDef(packageName: String, f: PackageBuilder.() -> Unit) {
    val aliases = mutableMapOf<String,String>()
    val clsBuilder = object : PackageBuilder {
        override fun classDef(name: String, base: String?, baseType: RTType?, nTParms: Int, nEParams: Int, baseParams: List<TVInst>, comparable: Boolean?, f: (ClsBuilder.() -> Unit)?) {
            val fullName = packageName + "." + name
            _classDef(fullName, aliases[base] ?: base, baseType, nTParms, nEParams, baseParams, comparable, f)
            registerName(name)
        }
        override fun funDef(name: String, args: List<RTType>, retType: RTType) {
            val fullName = packageName + "." + name
            _funDef(fullName, args, retType)
            registerName(name)
        }
        override fun registerName(name: String) {
            aliases[name] = packageName + "." + name
        }
    }
    clsBuilder.f()
    val attrs = aliases.map { it.key to TypeResolver.resolveNameTyp(it.value)!! }
    TypeResolver.registerPackage(packageName, attrs.toMap())
    TypeResolver.register(PackageTI(packageName, attrs = attrs))
}

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
    fun ctor()
}

fun classDef(name: String,
              base: String? = null,
              baseType: RTType? = null,
              nTParms: Int = 0,
              nEParams: Int = 0,
              baseParams: List<TVInst> = emptyList(),
              comparable: Boolean? = null,
              f: (ClsBuilder.() -> Unit)? = null) {
    _classDef(name, base, baseType, nTParms, nEParams, baseParams, comparable, f)
}
fun _classDef(name: String,
             base: String? = null,
             baseType: RTType? = null,
             nTParms: Int = 0,
             nEParams: Int = 0,
             baseParams: List<TVInst> = emptyList(),
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
            override fun ctor() {
                funDef(name, emptyList(), name)
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
            NamedType(base ?: "object", baseParams.map {
                when (it) {
                    is TVIndex -> tps[it.i]
                    is TVConcrete -> it.t
                }
            })
        })
    }
}

fun convertToTInfo(cd: ClassDescr): TypeInfo {
    return DataTInfo(
            cd.name,
            baseType = cd.baseType,
            attrs = cd.attrs,
            _comparable = cd.comparable,
            //_coercableToBool = if (cd.name == "boolean" || cd.name == "bool") true else null,
            //ctors = ctors
    )
}


fun funDef(name: String, args: List<String>, retType: String) {
    _funDef(name, args, retType)
}
fun _funDef(name: String, args: List<String>, retType: String) {
    fun parseArg(i: Int, a: String): FArg {
        val parts = a.split(":")
        return when (parts.size) {
            1 -> FArg("_$i", NamedType(parts[0]))
            2 -> FArg(parts[0], NamedType(parts[1]))
            else -> fail("wrong format")
        }
    }
    TypeResolver.registerFunc(FunDecl(name, args.mapIndexed(::parseArg), NamedType(retType)))
}

fun funDef(name: String, args: List<RTType>, retType: RTType) {
    _funDef(name, args, retType)
}
fun _funDef(name: String, args: List<RTType>, retType: RTType) {
    TypeResolver.registerFunc(FunDecl(name, args.mapIndexed { i: Int, t: RTType -> FArg("_$i", t) }, retType))
}
