package onotole

class VarDeclTransform(f: FunctionDef, exprTyper: ExprTyper): SimpleStmtTransformer() {
  val varInfo = varSlotAnalysis(f)
  val analyses = inferVarTypes(exprTyper, f)
  val newAnalyses = analyses.deepCopy()

  override fun transform(s: Stmt): List<Stmt> {
    val res = super.transform(s)
    if (res.size != 1 || res[0] != s) {
      res.forEach {
        if (it != s) {
          newAnalyses.varTypings[it] = newAnalyses.varTypings[s]!!
          newAnalyses.varTypingsAfter[it] = newAnalyses.varTypingsAfter[s]!!
        }
      }
    }
    return res
  }
  override fun doTransform(s: Stmt): List<Stmt>? {
    fun getVarDecl(vs: VarSlot, s: Stmt): Pair<Boolean?, String> {
      val decl: Boolean?
      val name: String
      when(vs.def) {
        is StmtVarDef -> {
          val defStmt = vs.def.s
          if (defStmt === s) {
            decl = vs.refs != null && vs.refs.isNotEmpty()
            name = vs.varName.toString()
          } else {
            val tt = varInfo.newSlots[defStmt]!![vs.name]!!
            decl = null
            name = tt.varName.toString()
          }
        }
        is PhiVarDef -> {
          decl = null
          name = vs.varName.toString()
        }
        else -> fail("gggg")
      }
      return Pair(decl, name)
    }

    fun mkVarDecl(vs: VarSlot, loop: Boolean): VarDeclaration {
      val phiVar = vs.def as PhiVarDef
      val phiWeb = varInfo.phiWebMap[phiVar]!!
      val defS = (phiWeb.defStmt!! as StmtVarDef).s
      val varAssignment: Stmt
      if (defS == s) {
        if (loop) {
          val info = varInfo.ifAnnoMap[s]!![vs.name]!!
          val phiWeb = varInfo.phiWebMap[info.target.def as PhiVarDef]!!
          val targetName = phiWeb.lastDef!!.varName
          varAssignment = VarDeclaration(true, listOf(mkName(targetName.toString(), true)), null, mkName(info.elseCopy!!.varName.toString()))
        } else {
          val types = phiWeb.getNonPhiDefs().map { vd ->
            when (vd) {
              is StmtVarDef -> analyses.varTypingsAfter[vd.s]!![vs.name]!!.asType()
              is ParamVarDef -> analyses.funcArgs.find { it.name == vs.name }!!.type
              else -> fail("shouldn't happen")
            }
          }
          val phiVarType = getCommonSuperType(types)
          varAssignment = VarDeclaration(false, listOf(mkName(vs.varName.toString(), true)), phiVarType.toTExpr(), null)
        }
        return varAssignment
      } else {
        TODO()
      }
    }

    return when (s) {
      is Assign -> {
        when(val target = s.target) {
          is Name -> {
            val slots = varInfo.newSlots[s]?.values
            if (slots == null || slots.size != 1) fail()
            slots.first().let {
              val (isVar, name) = getVarDecl(it, s)
              if (isVar != null)
                listOf(VarDeclaration(isVar, listOf(mkName(name, true)), null, s.value))
              else
                null
            }
          }
          is Subscript, is Attribute -> null
          is Tuple -> if (target.elts.all { it is Name }) {
            listOf(VarDeclaration(false, target.elts.map { it as Name }, null, s.value))
          } else
            TODO()
          else -> {
            TODO()
            /*val destructInfo = genLValExpr(target, exprTypes)
            val vh = destructInfo.exprHolder
            val exprType = getExprType(s.value)
            var expr: RExpr = genExpr(s.value)
            if (destructInfo.type != null && destructInfo.type != exprType) {
              expr = coerceExprToType(s.value, destructInfo.type, exprTypes)
            }
            if (!destructLValTuples) {
              res.add(genVarAssignment(false, vh, null, render(expr)))
            } else {
              res.add(genVarAssignment(false, vh, null, render(expr)))
              res.addAll(genDestructors(destructInfo))
            }*/
          }
        }
      }
      is AnnAssign -> {
        val slots = varInfo.newSlots[s]?.values
        if (slots == null || slots.size != 1) fail()
        val (isVar, name) = getVarDecl(slots.first(), s)
        if (isVar != null)
          listOf(VarDeclaration(isVar, listOf(mkName(name, true)), s.annotation, s.value))
        else
          fail()
      }
      is If -> {
        val res = (varInfo.newSlots[s] ?: emptyMap()).values.map { vs ->
          mkVarDecl(vs, false)
        }
        return res.plus(defaultTransform(s))
      }
      else -> null
    }
  }
}