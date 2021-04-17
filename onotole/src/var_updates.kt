

abstract class BackwardAnalysis<T> {
  val before = StmtAnnoMap<T>()
  val after = StmtAnnoMap<T>()
  abstract fun procStmt(s: Stmt, out: T): T
  private fun analyzeStmt(s: Stmt, out: T): T {
    after[s] = out
    val res = procStmt(s, out)
    before[s] = res
    return res
  }
  fun analyze(c: List<Stmt>, out: T): T = c.foldRight(out, { s, acc -> analyzeStmt(s, acc)})
}


//typealias VW = Map<String,List<Stmt>>

/*class VarUpdsAnalysis: BackwardAnalysis<VW>() {
  private fun updateVars(out: VW, s: Stmt, target: TExpr): VW {
    val res = out.toMutableMap()
    getVarNamesInStoreCtx(target).forEach {
      res[it] = listOf(s)
    }
    return res
  }
  override fun procStmt(s: Stmt, out: VW): VW = when(s) {
    is Assign -> {
      if (s.targets.size != 1)
        fail("unsupported")
      updateVars(out, s, s.targets[0])
    }
    is AnnAssign -> {
      updateVars(out, s, s.target)
    }
    is AugAssign -> {
      updateVars(out, s, s.target)
    }
    is If -> {
      val bodyVarUpds = analyze(s.body, out.toMap())
      val elseVarUpds = analyze(s.orelse, out.toMap())

      val res = mutableMapOf<String,List<Stmt>>()
      for(v in bodyVarUpds.keys.union(elseVarUpds.keys)) {
        res[v] = (bodyVarUpds[v] ?: emptyList()).plus(elseVarUpds[v] ?: emptyList())
      }
      res.toMap()
    }
    is While -> {
      val bodyVarUpds = analyze(s.body, out.toMap())
      val res = mutableMapOf<String,List<Stmt>>()
      for(v in bodyVarUpds.keys.union(out.keys)) {
        res[v] = (bodyVarUpds[v] ?: emptyList()).plus(out[v] ?: emptyList())
      }
      res.toMap()
    }
    is For -> {
      val bodyVarUpds = analyze(s.body, out.toMap())
      val res = mutableMapOf<String,List<Stmt>>()
      for(v in bodyVarUpds.keys.union(out.keys)) {
        res[v] = (bodyVarUpds[v] ?: emptyList()).plus(out[v] ?: emptyList())
      }
      res.toMap()
    }
    is Return -> emptyMap()
    else -> fail("unsupported $s")
  }
}*/
/*fun varUpdatesAnalysis(f: FunctionDef): Pair<StmtAnnoMap<VW>,StmtAnnoMap<VW>> {
  val analysis = VarUpdsAnalysis()
  analysis.analyze(f.body, emptyMap())
  T(analysis.before, analysis.after).prStmts(f.body, f.args.args.map { it.arg to ParamVarDef(it.arg) }.toMap())
  return Pair(analysis.before, analysis.after)
}*/
/*class T(val before: StmtAnnoMap<VW>, val after: StmtAnnoMap<VW>) {
  fun prStmts(c: List<Stmt>, vars: Map<String, VarDef>) {
    c.forEach { prStmt(it, vars) }
  }
  fun prStmt(s: Stmt, vars: Map<String, VarDef>) {
    val bf = before[s]
    val af = after[s]
    when(s) {
      is Assign -> {
        val varDefs = getVarNamesInStoreCtx(s.targets[0])
        varDefs.forEach {
          val vd = vars[it]
          if (vd != null) {
            if (vd is ParamVarDef)
              fail("updating readonly")
          }
        }
      }
    }
  }
}*/
