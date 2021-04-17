data class VarVersion(val name: String, val version: Int, val phi: Boolean = false) {
  override fun toString() = name + "_" + version
}

fun <T> diff(a: VarCtx<T>, b: VarCtx<T>): Collection<Pair<String,Pair<T?,T?>>> {
  val res = mutableSetOf<Pair<String,Pair<T?,T?>>>()
  for(k in a.keys.union(b.keys)) {
    val v1 = a[k]
    val v2 = b[k]
    if (v1 != v2) {
      res.add(Pair(k,Pair(v1,v2)))
    }
  }
  return res
}

fun gatherVarVersions(stmts: List<Stmt>, vc: MutableMap<String,Int>, ctx: VarCtx<VarVersion>): VarCtx<VarVersion> {
  return stmts.fold(ctx, { acc, s -> gatherVarVersions(s, vc, acc)})
}

fun gatherVarVersions(s: Stmt, vc: MutableMap<String,Int>, ctx: VarCtx<VarVersion>): VarCtx<VarVersion> {
  val r = when(s) {
    is Assign -> {
      val newNames = getVarNamesInStoreCtx(s.target)
      VarCtx(ctx, newNames.map { it to VarVersion(it, vc.incrAndGet(it)) }.toMap().toMutableMap())
    }
    is If -> {
      val diffs = diff(gatherVarVersions(s.body, vc, ctx), gatherVarVersions(s.orelse, vc, ctx))
      VarCtx(ctx, diffs.map { (n,_) -> n to VarVersion(n, vc.incrAndGet(n), true) }.toMap().toMutableMap())
    }
    else -> fail("not supported $s")
  }
  return r
}

fun gatherVarUpdates(stmts: List<Stmt>): Set<String> = stmts.flatMap(::gatherVarUpdates).toSet()

fun gatherVarUpdates(s: Stmt): Set<String> {
  val r = when(s) {
    is Assign -> {
      getVarNamesInStoreCtx(s.target).toSet()
    }
    is If -> {
      gatherVarUpdates(s.body).union(gatherVarUpdates(s.orelse))
    }
    else -> fail("not supported $s")
  }
  return r
}


fun renameVars(stmts: List<Stmt>, vc: MutableMap<String,Int>, ctx: VarCtx<VarVersion>): VarCtx<VarVersion> {
  return stmts.fold(ctx, { acc, s -> renameVars(s, vc, acc)})
}

fun MutableMap<String,Int>.incrAndGet(n: String): Int {
  val nv = this.getOrDefault(n,-1)+1
  this[n] = nv
  return nv
}

fun MutableMap<String,Int>.copy() = HashMap(this)

fun renameVars(s: Stmt, vc: MutableMap<String,Int>, ctx: VarCtx<VarVersion>): VarCtx<VarVersion> {
  val r = when(s) {
    is Assign -> {
      val vv = gatherVarUpdates(s)
      println("val ${vv.joinToString(", ")} = ${s.value}")
      gatherVarVersions(s, vc, ctx)
    }
    is If -> {
      val vcc = vc.copy()
      val vv = gatherVarUpdates(s)

      vv.forEach {
        println("val $it = ")
      }

      println("if (${s.test}) {")
      val b1 = gatherVarUpdates(s.body)
      vv.forEach {
        println(it)
      }
      println("} else {")
      val b2 = gatherVarUpdates(s.orelse)
      vv.forEach {
        println(it)
      }
      println("}")
      gatherVarVersions(s, vcc, ctx)
    }
    is While -> {
      println("while (${s.test})")
      val b1 = renameVars(s.body, vc, ctx)
      val diffs = diff(b1, ctx)
      val newCtx = ctx.copy(
      diffs.map { (n, vv) ->
        val nv = vc.incrAndGet(n)
        println("${VarVersion(n,nv)} = Phi(${vv.first}, ${vv.second})")
        n to VarVersion(n, nv)
      })
      println("elihw")
      newCtx
    }
    is For -> {
      val newNames = getVarNamesInStoreCtx(s.target)
      val ctxUpd = ctx.copy(newNames.map { it to VarVersion(it, vc.incrAndGet(it)) })
      println("it'x = iter(${s.iter})")
      println("while (it'x.hasNext)")
      println("${ctxUpd.localVars.values} = next(it'x)")

      val b1 = renameVars(s.body, vc, ctx)
      b1

    }
    is Return -> {
      println("return ${s.value}")
      ctx
    }
    else -> fail("unsupported $s")
  }
  return r
}


typealias VEnv = Map<String,String>

fun renameVars(e: TExpr, env: VEnv): TExpr {
  return when(e) {
    is Num -> e
    is Name -> if (e.id !in env || env[e.id] == e.id) e else Name(env[e.id]!!, e.ctx)
    is NameConstant -> e
    is Compare -> Compare(renameVars(e.left, env), e.ops, e.comparators.map { renameVars(it, env) })
    else -> fail("$e")
  }
}

