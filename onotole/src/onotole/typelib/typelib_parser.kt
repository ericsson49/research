package onotole.typelib

import antlr.TypeExprLexer
import antlr.TypeExprParser
import onotole.AliasResolver
import onotole.AnnAssign
import onotole.Assign
import onotole.Attribute
import onotole.CTV
import onotole.ClassDef
import onotole.ClassVal
import onotole.ConstExpr
import onotole.FunctionDef
import onotole.Index
import onotole.Name
import onotole.NameConstant
import onotole.Pass
import onotole.Subscript
import onotole.TExpr
import onotole.Tuple
import onotole.classParseCassInfo
import onotole.fail
import onotole.mkName
import onotole.toTExpr
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

class TLModLoader(val name: String, val deps: Collection<String>, val load: (List<TLModule>) -> TLModule)
data class TLModule(val name: String, val declarations: Collection<TLModDecl>, val definitions: Collection<TLModDef> = emptyList()) {
  val constantDecls = declarations.filterIsInstance<TLConstDecl>()
  val constantDefs = definitions.filterIsInstance<TLConstDef>()
}

sealed class TLModEntry {
  abstract val name: String
}
sealed class TLModDef: TLModEntry()
sealed class TLModDecl: TLModEntry()

data class TLConstDef(override val name: String, val value: TLTConst): TLModDef()
data class TLClassDef(val cls: ClassDef): TLModDef() {
  override val name = cls.name
}
data class TLFuncDef(val func: FunctionDef): TLModDef() {
  override val name = func.name
}

data class TLConstDecl(override val name: String, val type: TLTClass): TLModDecl()
data class TLClassDecl(val head: TLClassHead, val parent: TLTClass?, val attrs: Map<String,TLType>): TLModDecl() {
  override val name = head.name
}

data class TLClassHead(val name: String, val tvars: List<String> = emptyList()) {
  val noTParams = tvars.filter { it[0].isUpperCase() }.size
  val noEParams = tvars.filter { it[0].isLowerCase() }.size
}
data class TLSig(val tParams: List<TLType>, val args: List<Pair<String,TLType>>, val ret: TLType)
data class TLFuncDecl(override val name: String, val sigs: List<TLSig>): TLModDecl()
sealed class TLType
data class TLTConst(val const: ConstExpr): TLType()
data class TLTVar(val name: String): TLType() {
  override fun toString() = name
}
data class TLTClass(val name: String, val params: List<TLType>): TLType() {
  override fun toString() = name + (if (params.isNotEmpty()) params.joinToString(",", "[", "]") else "")
}
data class TLTCallable(val args: List<TLType>, val ret: TLType): TLType()

fun renameTVars(t: TLType, renames: Map<String,String>): TLType {
  return when(t) {
    is TLTVar -> t.copy(name = renames[t.name] ?: t.name)
    is TLTClass -> t.copy(params = t.params.map { renameTVars(it, renames) })
    is TLTCallable -> t.copy(args = t.args.map { renameTVars(it, renames) }, ret = renameTVars(t.ret, renames))
    is TLTConst -> t
  }
}
fun renameTVars(s: TLSig, renames: Map<String, String>): TLSig {
  val uninstVars = s.tParams.filterIsInstance<TLTVar>().map { it.name }
  val remap = uninstVars.map { it to (renames[it] ?: it) }.toMap()
  return s.copy(tParams = s.tParams.map { renameTVars(it, remap) },
        args = s.args.map { it.copy(second = renameTVars(it.second, remap)) },
        ret = renameTVars(s.ret, remap))
}

fun parsePkgDecl(pkg: String, classes: Collection<String>, funcs: Collection<String>, extAliases: Map<String, String>):
    Pair<List<TLClassHead>, List<TLFuncDecl>> {
  val cds = classes.map { parseClassDecl(it).first }
  val fds = funcs.map { parseFuncDecl(it) }
  return parsePkgDecl(pkg, cds, fds, extAliases)
}

fun extractEntityNames(mod: TLModule): Collection<String> = mod.declarations.map { it.name }

fun getShortName(n: String) = n.substring(n.lastIndexOf('.') + 1)

fun resolveAliasesInPkg(mod: TLModule, deps: Collection<TLModule>): TLModule {
  val aliases = mutableMapOf<String,String>()
  deps.forEach { dp ->
    extractEntityNames(dp).forEach { n ->
      val sn = getShortName(n)
      if (sn != n) {
        aliases[sn] = n
      }
    }
  }
  extractEntityNames(mod).forEach { n ->
    aliases[n] = mod.name + "." + n
  }
  return mod.copy(declarations = mod.declarations.map { resolveAliases(it, aliases) })
}

fun mkModule(mod: String, deps: Collection<String>, defs: Collection<TLModDef>, extern: Collection<TLModDecl> = emptyList()): TLModLoader {
  return TLModLoader(mod, deps) { deps ->
    val newEntries = importModule(mod, deps, defs.plus(extern))
    val entries = defs.flatMap {
      when(it) {
        is TLConstDef -> emptyList() //parseConstDecl(it)
        is TLClassDef -> listOf(parseClassDescr(it.cls, classParseCassInfo))
        is TLFuncDef -> listOf(parseFuncDecl(it.func, classParseCassInfo))
      }
    }.plus(extern)
    val newDecls = newEntries.filterIsInstance<TLModDecl>()
    resolveAliasesInPkg(TLModule(mod, entries.plus(newDecls), newEntries.filterIsInstance<TLModDef>()), deps)
  }
}
fun mkLibraryModule(pkg: String, deps: Collection<String>, cds: Collection<TLModDecl>) =
    TLModLoader(pkg, deps) { pkgs -> resolveAliasesInPkg(TLModule(pkg, cds), pkgs) }

fun parsePkgDecl(pkg: String, cds: List<TLClassHead>, fds: List<TLFuncDecl>, extAliases: Map<String, String>): Pair<List<TLClassHead>, List<TLFuncDecl>> {
  val intAliases = cds.map { it.name to pkg + "." + it.name }
      .plus(fds.map { it.name to pkg + "." + it.name })
  val aliases = extAliases.plus(intAliases)
  return cds.map { resolveAliases(it, aliases) } to fds.map { resolveAliases(it, aliases) }
}

fun resolveAliases(c: TLConstDef, aliases: Map<String, String>): TLConstDef {
  val name = aliases[c.name] ?: c.name
  val res = AliasResolver(aliases::get).transform(c.value.const.e, Unit)
  return c.copy(name = name, value = TLTConst(ConstExpr(res)))
}

fun resolveAliases(c: TLConstDecl, aliases: Map<String, String>): TLConstDecl {
  return c.copy(name = aliases[c.name] ?: c.name, type = resolveAliases(c.type, aliases) as TLTClass)
}
fun resolveAliases(c: TLClassDecl, aliases: Map<String, String>): TLClassDecl {
  return c.copy(
      head = resolveAliases(c.head, aliases),
      parent = c.parent?.let { resolveAliases(it, aliases) as TLTClass },
      attrs = c.attrs.mapValues { resolveAliases(it.value, aliases) })
}
fun resolveAliases(c: TLClassHead, aliases: Map<String, String>): TLClassHead = c.copy(name = aliases[c.name] ?: c.name)
fun resolveAliases(t: TLType, aliases: Map<String,String>): TLType = when(t) {
  is TLTClass -> t.copy(aliases[t.name] ?: t.name, t.params.map { resolveAliases(it, aliases) })
  is TLTCallable -> t.copy(t.args.map { resolveAliases(it, aliases) }, resolveAliases(t.ret, aliases))
  is TLTVar -> t
  is TLTConst -> t
}
fun resolveAliases(s: TLSig, aliases: Map<String, String>): TLSig =
    s.copy(args = s.args.map { it.first to resolveAliases(it.second, aliases) },
        ret = resolveAliases(s.ret, aliases))
fun resolveAliases(f: TLFuncDecl, aliases: Map<String, String>): TLFuncDecl =
    f.copy(name = aliases[f.name] ?: f.name, sigs = f.sigs.map { resolveAliases(it, aliases) })

fun resolveAliases(e: TLModDecl, aliases: Map<String, String>) = when(e) {
  is TLConstDecl -> onotole.typelib.resolveAliases(e, aliases)
  is TLClassDecl -> resolveAliases(e, aliases)
  is TLFuncDecl -> resolveAliases(e, aliases)
  //is TLConstDef -> resolveAliases(e, aliases)
}

fun parseAttrDecl(cd: TLClassHead, fd: String): TLType {
  return parseTypeDecl(getTypeExprParser(fd).type(), cd.tvars)
}
fun parseClassDescr(cn: String, vararg attrs: Pair<String,String>) = parseClassDescr(cn to attrs.toList().toMap())
fun parseClassDescr(cd: Pair<String,Map<String,String>>): TLClassDecl {
  val c = parseClassDecl(cd.first)
  val attrs = cd.second.map { it.key to parseAttrDecl(c.first, it.value) }.toMap()
  return TLClassDecl(c.first, c.second, attrs)
}

fun parseClassDecl(cd: String): Pair<TLClassHead,TLTClass?> {
  val cd = getTypeExprParser(cd).clsDecl()
  val clsHead = cd.clsHead()
  val n = clsHead.name.text
  val ps = clsHead.tparams.map { it.text }
  val parent = cd.base?.let { parseTypeDecl(it, ps) as TLTClass }
  return TLClassHead(n, ps) to parent
}



fun parseTypeDecl(t: TypeExprParser.TypeContext, tvars: List<String>): TLType {
  return if (t.clsName() != null) {
    val typeParamList = t.typeParamList()?.tparams
    if (typeParamList == null) {
      if (t.clsName().text in tvars) TLTVar(t.clsName().text)
      else TLTClass(t.clsName().text, emptyList())
    } else {
      TLTClass(t.clsName().text, typeParamList.map { parseTypeDecl(it, tvars) })
    }
  } else {
    val tparams = t.typeParamList()?.tparams ?: emptyList()
    TLTCallable(tparams.map { parseTypeDecl(it, tvars) }, parseTypeDecl(t.type(), tvars))
  }
}

fun parseConstDecl(cd: Assign) = TLConstDef((cd.target as Name).id, TLTConst(ConstExpr(cd.value)))

fun parseClassDescr(cd: ClassDef, clsInfo: Map<String, TLClassHead>): TLClassDecl {
  if (cd.bases.size != 1) TODO()
  val base = parseTypeDecl(cd.bases[0], clsInfo)
  val attrs = cd.body.filter { it !is Pass }.map { it as AnnAssign }.map { (it.target as Name).id to parseTypeDecl(it.annotation, clsInfo) }.toMap()
  return TLClassDecl(TLClassHead(cd.name), parent = base, attrs = attrs)
}
fun parseTypeDecl(t: TExpr, clsInfo: Map<String, TLClassHead>): TLTClass {
  return parseTypeDecl(t, clsInfo::get)
}
fun parseTypeDecl(t: TExpr, clsInfo: (String) -> TLClassHead?): TLTClass {
  fun getFullName(t: TExpr): String = when(t) {
    is Name -> t.id
    is Attribute -> getFullName(t.value) + "." + t.attr
    else -> TODO()
  }
  return when(t) {
    is NameConstant -> if (t.value == null) TLTClass("pylib.None", emptyList()) else fail()
    is Name -> TLTClass(getFullName(t), emptyList())
    is Attribute -> parseTypeDecl(mkName(getFullName(t)), clsInfo)
    is Subscript -> {
      val ind = (t.slice as Index).value
      val elts = if (ind is Tuple) ind.elts else listOf(ind)
      val name = getFullName(t.value)
      val clsHead = clsInfo.invoke(name)
      val className = clsHead?.name ?: name
      val kinds = clsHead?.tvars?.map { it[0].isUpperCase() } ?: elts.indices.map { true }
      if (className != "pylib.Tuple" && elts.size != kinds.size)
        fail()
      TLTClass(className, elts.zip(kinds).map { (e,k) -> if (k) parseTypeDecl(e, clsInfo) else TLTConst(ConstExpr(e)) })
    }
    is CTV -> if (t.v is ClassVal)
      parseTypeDecl(t.v.toTExpr(), clsInfo)
    else if (t.v is ConstExpr)
      parseTypeDecl(t.v.e, clsInfo)
    else
      TODO()
    else -> TODO()
  }
}

fun parseFuncDecl(fd: FunctionDef, clsInfo: Map<String, TLClassHead>): TLFuncDecl {
  val args = fd.args.args.map { it.arg to parseTypeDecl(it.annotation!!, clsInfo) }
  val sig = TLSig(emptyList(), args = args, ret = parseTypeDecl(fd.returns!!, clsInfo))
  return TLFuncDecl(fd.name, listOf(sig))
}

fun parseFuncDecl(fd: String): TLFuncDecl {
  val sigs = fd.split("\n")
  fun parseSig(s: String): Pair<String, TLSig> {
    val fd = getTypeExprParser(s).funDecl()
    val name = fd.name.text
    val tvars = fd.targs.map { it.text }
    val args = fd.args.mapIndexed { i, a -> (a.name?.text ?: "_$i") to parseTypeDecl(a.type(), tvars) }
    val retType = parseTypeDecl(fd.resType, tvars)
    return name to TLSig(tvars.map { TLTVar(it) }, args, retType)
  }
  val fSigs = sigs.map { parseSig(it) }
  val fNames = fSigs.map { it.first }.toSet()
  if (fNames.size != 1) fail()
  return TLFuncDecl(fNames.first(), fSigs.map { it.second })
}

private fun getTypeExprParser(s: String): TypeExprParser {
  val str = CharStreams.fromString(s)
  val lexer = TypeExprLexer(str)
  val ts = CommonTokenStream(lexer)
  return TypeExprParser(ts)
}


fun main() {
  val fd = "max[A,B](Seq[A], key: (A)->B) -> A\nmax[A](A,A) -> A" //, B <: Comp[B]
  val cls = "List[A,B] <: object"
  //val r = parseClassDescr(cls)
  //val r = parseFunDecl(fd)
  val sszCls = setOf(
      "uint8", "uint32", "uint64",
      "Bitlist[n]", "Bitvector[n]",
      "List[T,n]", "Vector[T,n]", "Bytes4", "Bytes32",
  )
  val sszFuncs = setOf(
      "hash(object)->ssz.Hash", "hash_tree_root(object)->Hash", "uint_to_bytes(uint)->bytes"
  )
  val r = parsePkgDecl("ssz", sszCls, sszFuncs, mapOf("object" to "pylib.object"))
  println()
}

