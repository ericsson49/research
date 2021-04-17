import java.nio.file.Files
import java.nio.file.Paths

fun findDeps(c: ClassDef): Collection<String> {
  val fieldTypes = c.body.filter { if (it is Pass) false else if (it is AnnAssign) true else TODO() }
          .map { (it as AnnAssign).annotation }
  return c.bases.plus(fieldTypes).flatMap { extractNamesFromTypeExpr(it) }.toSet()
}

fun findDeps(f: FunctionDef): Collection<String> {
  return extractNamesFromFuncBody(f)
}

fun combine(d1: List<TopLevelDef>, d2: List<TopLevelDef>): Pair<Set<String>, List<TopLevelDef>> {
  val prevCDefs = d1.filterIsInstance<ClassTLDef>().map { it.name to it }.toMap()
  val currCDefs = d2.filterIsInstance<ClassTLDef>().map { it.name to it }.toMap()

  val newClasses = currCDefs.filter { it.key !in prevCDefs || prevCDefs[it.key] != it.value}.keys
  val deps = currCDefs.mapValues { d -> findDeps(d.value.clazz) }
  val revDeps = reverse(deps)
  val depsPrev = prevCDefs.mapValues { d -> findDeps(d.value.clazz) }
  val revDepsPrev = reverse(depsPrev)

  val toCopy = transClosure(newClasses) { revDeps[it] ?: revDepsPrev[it] ?: emptyList() }.minus(newClasses)
  val toImport = prevCDefs.keys.minus(newClasses.plus(toCopy))
  val copiedCDefs = toCopy.map { ClassTLDef(prevCDefs[it]!!.clazz.copy()) }

  val changedClasses = toCopy + newClasses.filter { it in prevCDefs }

  val (funcsTOImport, copiedFDefs) = findFuncsToCopy(d1, d2, changedClasses)

  return toImport.plus(funcsTOImport) to d2.plus(copiedCDefs).plus(copiedFDefs)
}

fun findFuncsToCopy(d1: List<TopLevelDef>, d2: List<TopLevelDef>, changedClasses: Set<String>): Pair<Set<String>, List<FuncTLDef>> {
  val prevFDefs = d1.filterIsInstance<FuncTLDef>().map { it.name to it }.toMap()
  val currFDefs = d2.filterIsInstance<FuncTLDef>().map { it.name to it }.toMap()
  val initF = prevFDefs.keys.minus(currFDefs.keys).filter { n ->
    val f = prevFDefs[n]!!.func
    val names = extractNamesFromArguments(f.args)
    names.intersect(changedClasses).isNotEmpty()
  }

  val funcNames = prevFDefs.keys + currFDefs.keys
  val prevFuncDeps = prevFDefs.mapValues { d -> findDeps(d.value.func).intersect(funcNames) }
  val revPrevFuncDefs = reverse(prevFuncDeps)
  val currFuncDeps = currFDefs.mapValues { d -> findDeps(d.value.func).intersect(funcNames) }
  val revCurrFuncDefs = reverse(currFuncDeps)

  val newFuncs = currFDefs.filter { it.key !in prevFDefs || prevFDefs[it.key]!! != it.value }.keys
  val usedFuncsInit = currFDefs.values.flatMap { d -> findDeps(d.func) }.toSet().intersect(funcNames)

  val usedFuncs = transClosure(usedFuncsInit) { currFuncDeps[it] ?: prevFuncDeps[it] ?: emptySet() }

  val funcsToCopy = transClosure(initF) {
    revCurrFuncDefs[it] ?: revPrevFuncDefs[it] ?: emptyList()
  }.minus(newFuncs).intersect(usedFuncs)
  val funcsTOImport = prevFDefs.keys.minus(newFuncs + funcsToCopy)
  val copiedFDefs = funcsToCopy.map { FuncTLDef(prevFDefs[it]!!.func.copy()) }
  return Pair(funcsTOImport, copiedFDefs)
}

fun <T> transClosure(init: Collection<T>, trans: (T) -> Collection<T>): Set<T> {
  var queue = init
  val res = mutableSetOf<T>()
  while (queue.isNotEmpty()) {
    res.addAll(queue)
    queue = res.flatMap(trans).minus(res)
  }
  return res
}

fun parseDefs(s: String): List<Stmt> {
  return Files.readAllLines(Paths.get(s)).map(ItemsParser2::parseToEnd).map(::toStmt)
}

fun main() {
  val phase0 = parseSpecFile(Paths.get("../eth2.0-specs/tests/fork_choice/defs_phase0_dev.txt"))
  val altair = parseSpecFile(Paths.get("../eth2.0-specs/tests/fork_choice/defs_altair_dev.txt"))
  combine(phase0, altair)
}
