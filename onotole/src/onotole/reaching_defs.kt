package onotole

fun calcReachingDefs(f: FunctionDef): StmtAnnoMap<List<Stmt>> {
  fun getNames(t: TExpr): List<String> = when(t) {
    is Name -> listOf(t.id)
    is Tuple -> fail("tuple assignments should be destructed")
    else -> emptyList()
  }
  val v = object : StmtVisitor<MutableSet<Pair<String,Stmt>>>() {
    override fun visitStmt(s: Stmt, ctx: MutableSet<Pair<String,Stmt>>) {
      val names = when (s) {
        is Assign -> getNames(s.target)
        is AnnAssign -> getNames(s.target)
        is AugAssign -> getNames(s.target)
        is For -> getNames(s.target)
        else -> emptyList()
      }
      ctx.addAll(names.map { it to s })
    }
  }
  val res = mutableSetOf<Pair<String,Stmt>>()
  v.procStmts(f.body, res)
  val stmts = res.groupBy { it.first }.mapValues { it.value.map { it.second } }
  val a = object : ForwardAnalysis<Set<Stmt>>() {
    override fun procAssign(t: TExpr, v: TExpr, p: Stmt, ins: Set<Stmt>): Set<Stmt> {
      val names = getNames(t)
      val gen = setOf(p)
      val kill = names.flatMap { stmts[it] ?: emptyList() }.minus(p)
      return ins.minus(kill).plus(gen)
    }

    override fun merge(a: Set<Stmt>, b: Set<Stmt>): Set<Stmt> = a.union(b)
    override val bottom: Set<Stmt> get() = emptySet()
  }
  val params = getFunArgs(f).map { AnnAssign(mkName(it.first.arg, true), annotation = it.first.annotation!!) }
  a.analyze(f.body, params.toSet())
  TODO()
}

