package onotole

private val funcInfoCache = mutableMapOf<Pair<String,String>, FuncInfo>()
fun registerFuncInfo(phase: String, fd: FunctionDef) {
  funcInfoCache[phase to fd.name] = FuncInfo(fd)
}
fun getFuncInfo(phase: String, name: String): FuncInfo {
  return funcInfoCache[phase to name]!!
}

fun getFuncDefInfo(phase: String, name: String) = getFuncInfo(phase, name).desugaredDefInfo

class FuncInfo(val original: FunctionDef) {
  val desugared: FunctionDef by lazy {
    desugarExprs(transformForEnumerate(transformForOps(original)))
  }

  val desugaredDefInfo: FuncDefInfo by lazy { FuncDefInfo(desugared) }
}

class FuncDefInfo(val fd: FunctionDef) {
  val destructedWithCopies: FunctionDef by lazy { convertToAndOutOfSSA(fd) }

  val cfg: CFGraphImpl by lazy { convertToCFG(fd) }
  val ssa: CFGraphImpl by lazy { convertToSSA(cfg) }
  private val destructedSSAandRenames: Pair<CFGraphImpl, ExprRenamer> by lazy {
    destructSSA(ssa, getFuncAnalyses(ssa).cfgAnalyses.dom)
  }
  val destructed get() = destructedSSAandRenames.first
  val ssaDestructRenames get() = destructedSSAandRenames.second
  val varTypes: Map<String, RTType> by lazy {
    _inferTypes_CFG(ssa)
  }
}