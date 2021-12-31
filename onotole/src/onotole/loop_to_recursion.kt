package onotole

import onotole.lib_defs.Additional
import onotole.lib_defs.BLS
import onotole.lib_defs.PyLib
import onotole.lib_defs.SSZLib
import java.nio.file.Files
import java.nio.file.Paths

fun main() {
  PyLib.init()
  SSZLib.init()
  BLS.init()
  val specVersion = "phase0"
  Additional.init(specVersion)
  val tlDefs = loadSpecDefs(specVersion)
  PhaseInfo.getPkgDeps(specVersion).forEach {
    TypeResolver.importFromPackage(it)
  }
  TypeResolver.importFromPackage(specVersion)




  val path = Paths.get("../eth2.0-specs/tests/fork_choice/defs_phase0_dev.txt")
  val parsed = Files.readAllLines(path).map { ItemsParser2.parseToEnd(it) }
  val defs = parsed.map { toStmt(it) }

  val fDefs = defs.filterIsInstance<FunctionDef>()
  val descrs = phase0PDs()

  fDefs.forEach { fd ->
    val fd2 = desugarExprs(transformForEnumerate(transformForOps(fd)))
    pyPrintFunc(fd2)
    val fs = transformLoopsToFuncs(fd2)
    println()
    fs.forEach { pyPrintFunc(it) }
    println()
  }
}
