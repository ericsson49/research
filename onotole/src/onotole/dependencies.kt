package onotole

fun <T> flatten(vararg t: T?): List<T> {
  val res = mutableListOf<T>()
  for (e in t) {
    if (e != null)
      res.add(e)
  }
  return res.toList()
}
fun extractNamesFromTypeExpr(e: TExpr): Set<String> {
  fun processAttr(e: TExpr): String = when(e) {
    is Name -> e.id
    is Attribute -> processAttr(e.value) + "." + e.attr
    else -> TODO()
  }
  fun processSliceParam(e: TExpr): Set<String> = when (e) {
      is NameConstant, is Name, is Subscript, is Attribute -> extractNamesFromTypeExpr(e)
    else -> liveVarAnalysis(e)
  }
  return when(e) {
    is NameConstant -> emptySet()
    is Name -> setOf(e.id)
    is Subscript -> extractNamesFromTypeExpr(e.value) + when(e.slice) {
      is Index -> processSliceParam(e.slice.value)
      is Slice -> flatten(e.slice.lower, e.slice.upper, e.slice.step).flatMap(::processSliceParam)
      else -> fail()
    }
    is Attribute -> setOf(processAttr(e))
    is CTV -> when(e.v) {
      is ClassVal -> extractNamesFromTypeExpr(e.v.toTExpr())
      else -> TODO()
    }
    else -> fail()
  }
}
fun extractNamesFromArg(a: Arg) = a.annotation?.let(::extractNamesFromTypeExpr) ?: emptySet()
fun extractNamesFromArguments(args: Arguments): Set<String> {
  val allArgs = args.posonlyargs.plus(args.args).plus(args.kwonlyargs).plus(flatten(args.vararg, args.kwarg))
  val argNames = allArgs.flatMap(::extractNamesFromArg)
  val exprNames = args.defaults.plus(args.kw_defaults).flatMap(::liveVarAnalysis)
  return argNames.plus(exprNames).toSet()
}

fun extractNamesFromFuncBody(f: FunctionDef): Set<String> {
  val liveVars = liveVarAnalysis(f).first[f.body[0]]!!
  return liveVars.minus(f.allArgs.map { it.arg })
}