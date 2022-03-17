package onotole.typelib

import onotole.AliasResolver
import onotole.AnnAssign
import onotole.Attribute
import onotole.CTV
import onotole.CTVal
import onotole.ClassTemplate
import onotole.ClassVal
import onotole.CompileTimeCalc
import onotole.ConstExpr
import onotole.ExprContext
import onotole.ExprTransformer
import onotole.FuncTempl
import onotole.FunctionDef
import onotole.Index
import onotole.Name
import onotole.NameConstant
import onotole.Pass
import onotole.PkgVal
import onotole.StaticNameResolver
import onotole.Stmt
import onotole.Subscript
import onotole.TExpr
import onotole.Tuple
import onotole.TypeResolver
import onotole.allArgs
import onotole.fail
import onotole.getFunArgs
import onotole.mkName

class ModuleNames(val mod: String, _entries: Collection<TLModEntry>, deps: Collection<TLModule>) {
  val pkgs = deps.map { it.name }.plus(mod)
  val imported = deps.flatMap { (it.declarations + it.constantDefs).map { it.name to it } }.toMap()
  val entries = _entries.map { "$mod.${it.name}" to it }.toMap()
  val aliases = (imported.keys + entries.keys).map { getShortName(it) to it }.toMap()
  val cache = mutableMapOf<String,CTVal>()

  fun getClassHead(n: String): TLClassHead {
    return when(val nk = resolveName(n)) {
      is ClassTemplate -> nk.clsTempl
      is ClassVal -> TLClassHead(nk.name)
      else -> fail()
    }
  }

  fun toCTVal(cls: TLClassHead): CTVal {
    return if (cls.name != "pylib.Tuple" && cls.noTParams + cls.noEParams == 0)
      ClassVal(cls.name, emptyList(), emptyList())
    else ClassTemplate(cls)
  }

  fun resolveName(n: String): CTVal {
    val n = aliases[n] ?: n
    return cache.getOrPut(n) {
      val entry = entries[n]
      if (entry != null) {
        when (entry) {
          is TLConstDecl -> ConstExpr(mkName(n))
          is TLClassDecl -> toCTVal(resolveAliases(entry.head, aliases))
          is TLFuncDecl -> FuncTempl(resolveAliases(entry, aliases))
          is TLConstDef -> ConstExpr(mkName(n))
          is TLClassDef -> toCTVal(resolveAliases(TLClassHead(entry.name), aliases))
          is TLFuncDef -> {
            val sig = TLSig(emptyList(), entry.func.allArgs.map {
              it.arg to parseTypeDecl(it.annotation!!, ::getClassHead)
            }, parseTypeDecl(entry.func.returns!!, ::getClassHead))
            val fd = TLFuncDecl(entry.name, listOf(sig))
            FuncTempl(resolveAliases(fd, aliases))
          }
        }
      } else {
        val impEntry = imported[n]
        if (impEntry != null) {
          when (impEntry) {
            is TLConstDef -> ConstExpr(mkName(n))
            is TLClassDecl -> toCTVal(impEntry.head)
            is TLFuncDecl -> FuncTempl(impEntry)
            else -> TODO()
          }
        } else if (n in pkgs) {
          PkgVal(n)
        } else
          fail("Unknown $n")
      }
    }
  }
}

fun resolveAliasesInType(t: TExpr, aliases: Map<String, String>): TExpr {
  return when (t) {
    is NameConstant -> t
    is Name -> aliases[t.id]?.let { t.copy(id = it) } ?: t
    is Attribute -> t.copy(value = resolveAliasesInType(t.value, aliases))
    is Subscript -> {
      if (t.slice is Index) {
        val ind = t.slice.value
        val indValue = if (ind is Tuple)
          ind.copy(elts = ind.elts.map { resolveAliasesInType(it, aliases) })
        else
          resolveAliasesInType(ind, aliases)
        t.copy(value = resolveAliasesInType(t.value, aliases), slice = t.slice.copy(value = indValue))
      } else fail()
    }
    else -> TODO()
  }
}

class TestResolver(val globals: ModuleNames, val locals: Set<String>) {
  fun get(n: String) = globals.resolveName(n)
  fun isSpecialFunc(n: String) = n in TypeResolver.specialFuncNames
  fun isLocal(n: String) = n in locals
  fun getVal(n: String, store: Boolean): TExpr {
    return if (isSpecialFunc(n) || isLocal(n))
      Name(n, ctx = if (store) ExprContext.Store else ExprContext.Load)
    else CTV(get(n))
  }
  fun resolveAlias(n: String) = if (n !in locals) globals.aliases[n] else null
  fun updated(ns: Collection<String>) = TestResolver(globals, locals.plus(ns))
}

class ModuleTransformer(globals: ModuleNames) {
  val snResolver = StaticNameResolver()
  val ctCalc = CompileTimeCalc()
  val globalCtx = TestResolver(globals, emptySet())
  val aliasResolver = AliasResolver(globalCtx::resolveAlias)

  fun processStmts(c: List<Stmt>, resolver: TestResolver): List<Stmt> {
    val stmts = aliasResolver.procStmts(c, Unit).first
    val stmts2 = snResolver.procStmts(stmts, resolver).first
    return ctCalc.procStmts(stmts2, resolver).first
  }
  fun transform(e: TExpr, resolver: TestResolver, store: Boolean = false): TExpr {
    val e1 = aliasResolver.transform(e, Unit, store)
    val e2 = snResolver.transform(e1, resolver, store)
    return ctCalc.transform(e2, resolver, store)
  }

  fun transformType(t: TExpr) = transform(t, globalCtx, false)

  fun transformName(n: String) = aliasResolver.resolveAlias(n) ?: n

  fun transform(fd: FunctionDef): FunctionDef {
    val stmts = processStmts(fd.body, globalCtx.updated(getFunArgs(fd).map { it.first.arg }))
    val ret = fd.returns?.let { transformType(it) }
    val args = fd.args.copy(
        args = fd.args.args.map {
          if (it.annotation == null) it
          else it.copy(annotation = transformType(it.annotation))
        },
        defaults = fd.args.defaults.map { transform(it, globalCtx) }
    )
    return fd.copy(name = transformName(fd.name), body = stmts, args = args, returns = ret)
  }
}


fun <C> transformModEntry(t: ExprTransformer<C>, c: C, s: TLModDef): TLModDef = when(s) {
  is TLConstDef -> {
    s.copy(value = TLTConst(ConstExpr(t.transform(s.value.const.e, c))))
  }
  is TLClassDef -> {
    val cls = s.cls
    TLClassDef(cls.copy(bases = cls.bases.map { t.transform(it, c) }, body = cls.body.map { t.procStmt(it, c).first }))
  }
  is TLFuncDef -> {
    val f = s.func
    val stmts = t.procStmts(f.body, c).first
    val ret = f.returns?.let { t.transform(it, c) }
    val args = f.args.copy(
        args = f.args.args.map {
          if (it.annotation == null) it
          else it.copy(annotation = t.transform(it.annotation, c))
        },
        defaults = f.args.defaults.map { t.transform(it, c) }
    )
    TLFuncDef(f.copy(body = stmts, args = args, returns = ret))
  }
}


fun importModule(modName: String, deps: Collection<TLModule>, entries: Collection<TLModEntry>): List<TLModEntry> {
  val modNames = ModuleNames(modName, entries, deps)
  val transformer = ModuleTransformer(modNames)
  return entries.map { s ->
    when (s) {
      is TLConstDecl -> resolveAliases(s, modNames.aliases)
      is TLClassDecl -> resolveAliases(s, modNames.aliases)
      is TLFuncDecl -> (modNames.resolveName(s.name) as FuncTempl).func
      is TLConstDef -> {
        val e = transformer.transform(s.value.const.e, transformer.globalCtx)
        s.copy(name = transformer.transformName(s.name), value = TLTConst(ConstExpr(e)))
      }
      is TLClassDef -> {
        val cls = s.cls
        TLClassDef(cls.copy(
            name = transformer.transformName(cls.name),
            bases = cls.bases.map { transformer.transformType(it) },
            body = cls.body.map {
              when (it) {
                is AnnAssign -> it.copy(annotation = transformer.transformType(it.annotation))
                is Pass -> it
                else -> TODO()
              }
            }))
      }
      is TLFuncDef -> TLFuncDef(transformer.transform(s.func))
    }
  }
}
