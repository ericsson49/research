package onotole

import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

typealias CPoint = Int

sealed class CtrlInfo(val entry: CPoint, val exit: CPoint)
class WhileInfo(val head: CPoint, val test: CPoint, val body: CPoint, exit: CPoint): CtrlInfo(head, exit)
class IfInfo(val head: CPoint, val test: CPoint, val body: CPoint, val orelse: CPoint, exit: CPoint): CtrlInfo(head, exit)

typealias CPoint2 = Pair<CPoint, Int>

sealed class CtrlInfo2(val entry: CPoint2, val exit: CPoint)
class WhileInfo2(val head: CPoint, val test: CPoint, val body: CPoint, exit: CPoint): CtrlInfo2(head to 0, exit)
class IfInfo2(val head: CPoint2, val test: CPoint, val body: CPoint, val orelse: CPoint, exit: CPoint): CtrlInfo2(head, exit)

class StmtToNode {
  val instrs = linkedMapOf<CPoint,Pair<Instr2,List<CPoint>>>()
  val loopInfo = mutableListOf<WhileInfo>()
  val ifInfo = mutableListOf<IfInfo>()

  val tmps = FreshNames()

  fun freshVar(): String = tmps.fresh()

  var counter = 0
  fun newPoint(): CPoint {
    return counter++
  }
  fun addInstr(at: CPoint, i: Instr2, next: CPoint?): CPoint {
    val nexts = when(i) {
      is StmtInstr -> if (next == null) fail() else listOf(next)
      is BranchInstr -> if (next == null) fail() else listOf(next, i.orelse)
      is ExitInstr -> if (next != null) fail() else emptyList()
    }
    instrs[at] = i to nexts
    return at
  }
  fun mkBranch(v: String, bodyL: CPoint, orelse: CPoint): CPoint {
    return addInstr(newPoint(), BranchInstr(v, orelse), bodyL)
  }
  fun mkAssgnInstr(v: String, e: TExpr, t: TExpr?, next: CPoint): CPoint {
    return addInstr(newPoint(), StmtInstr(VarLVal(v, t), e), next)
  }
  fun mkExprInstr(e: TExpr, next: CPoint): CPoint {
    return addInstr(newPoint(), StmtInstr(EmptyLVal, e), next)
  }
  fun mkAttrAssgnInstr(r: TExpr, f: identifier, e: TExpr, next: CPoint): CPoint {
    return addInstr(newPoint(), StmtInstr(FieldLVal(r, f), e), next)
  }
  fun mkSubscrAssgnInstr(r: TExpr, i: TSlice, e: TExpr, next: CPoint): CPoint {
    return addInstr(newPoint(), StmtInstr(SubscriptLVal(r, i), e), next)
  }
  fun mkExit(): CPoint {
    return addInstr(newPoint(), ExitInstr(), null)
  }

  fun convert(b: List<Stmt>, next: CPoint, ret: CPoint, loops: List<Pair<CPoint, CPoint>>): CPoint =
      if (b.isEmpty()) next
      else {
        val res = convert(b[b.size - 1], next, ret, loops)
        convert(b.subList(0, b.size - 1), res, ret, loops)
      }

  fun convert(s: Stmt, next: CPoint, ret: CPoint, loops: List<Pair<CPoint, CPoint>>): CPoint = when (s) {
    is Return ->
      convert(mkAssign(mkName("<return>", true), s.value ?: NameConstant(null)), ret, ret, loops)
    is If -> {
      val bodyL = convert(s.body, next, ret, loops)
      val elseL = convert(s.orelse, next, ret, loops)
      val testV = freshVar()
      val testL = mkBranch(testV, bodyL, elseL)
      val headL = mkAssgnInstr(testV, s.test, null, testL)
      ifInfo.add(IfInfo(headL, testL, bodyL, elseL, next))
      headL
    }
    is While -> {
      val headL = newPoint()
      val bodyL = convert(s.body, headL, ret, loops.plus(headL to next))
      val testV = freshVar()
      val testL = mkBranch(testV, bodyL, next)
      loopInfo.add(WhileInfo(headL, testL, bodyL, next))
      addInstr(headL, StmtInstr(VarLVal(testV), s.test), testL)
    }
    is For -> {
      val iter = mkName(freshVar())
      val headL = convert(
          While(mkCall("has_next", listOf(iter)),
              listOf(mkAssign(s.target, mkCall("next", listOf(iter)))).plus(s.body)),
          next, ret, loops
      )
      convert(mkAssign(iter, mkCall("iter", listOf(s.iter))), headL, ret, loops)
    }
    is Continue -> loops[loops.size - 1].first
    is Break -> loops[loops.size - 1].second
    is Pass -> next

    is Assign -> {
      when(s.target) {
        is Name -> mkAssgnInstr(s.target.id, s.value, null, next)
        is Tuple -> {
          //mkAssgnInstr(s.target.elts.map { (it as Name).id }, s.value, next)
          val t = freshVar()
          val assigns = s.target.elts.mapIndexed { i, a -> Assign((a as Name), mkSubscript(mkName(t), Index(Num(i)))) }
          listOf(mkAssign(mkName(t, true), s.value)).plus(assigns)
              .foldRight(next) { s, next -> convert(s, next, ret, loops) }
        }
        is Attribute -> mkAttrAssgnInstr(s.target.value, s.target.attr, s.value, next)
        is Subscript -> mkSubscrAssgnInstr(s.target.value, s.target.slice, s.value, next)
        else -> TODO("${s.target}")
      }
    }
    is AnnAssign -> if (s.value == null)
      TODO()
    else
      mkAssgnInstr((s.target as Name).id, s.value, s.annotation, next)
    is AugAssign -> convert(mkAssign(s.target, BinOp(s.target, s.op, s.value)), next, ret, loops)
    is Expr -> mkExprInstr(s.value, next)

    is Assert -> convert(
        Expr(mkCall(
            "<assert>",
            listOf(s.test).plus(s.msg?.let { listOf(it) } ?: emptyList()))),
        next, ret, loops
    )

    is ClassDef, is FunctionDef, is Nonlocal, is Try, is Raise -> TODO("$s")
  }
}

val convertToCFG = MemoizingFunc(::makeCFGFromFunctionDef)

private fun makeCFGFromFunctionDef(f: FunctionDef): CFGraphImpl {
  val convertor = StmtToNode()
  val exit = convertor.mkExit()
  val implicitReturn = if (f.returns == null || f.returns == NameConstant(null))
    listOf(Return())
  else
    emptyList()
  val funcBody = convertor.convert(f.body.plus(implicitReturn), exit, exit, emptyList())
  val argStmts = f.args.args.mapIndexed { i, a -> AnnAssign(mkName(a.arg), a.annotation!!, mkCall("<Parameter>", listOf(Num(i)))) }
  val entry = argStmts.foldRight(funcBody) { a, next -> convertor.convert(a, next, exit, emptyList()) }
  val invMap = IdentityHashMap<Instr2, CPoint>()
  convertor.instrs.forEach { at, (i,_) -> invMap[i] = at }

  val targets = reverse(convertor.instrs.mapValues { it.value.second })
      .filterValues { it.size >= 2 }.keys.plus(funcBody)

  val renames = mutableMapOf<CPoint,CPoint2>()
  val queue = mutableListOf<CPoint>(entry)
  val visited = mutableSetOf<CPoint>()
  //val sortedInstrs = mutableListOf<Pair<Instr2,List<CPoint>>>()
  val blocks = mutableListOf<BasicBlock>()

  fun getBlock(start: CPoint): List<CPoint> {
    val block = mutableListOf<CPoint>()
    var curr = start
    while (true) {
      block.add(curr)
      val next = convertor.instrs[curr]!!.second
      if (next.size == 0)
        break
      else if (next.size == 2 || next[0] in targets)
        break
      else
        curr = next[0]
    }
    return block
  }
  while (queue.isNotEmpty()) {
    val curr = queue.removeAt(0)
    if (curr in visited)
      continue
    visited.add(curr)

    val blockCPoints = getBlock(curr)
    val block = blockCPoints.map { convertor.instrs[it]!! }
    val (lastInstr, nxts) = block[block.size - 1]
    val blck = if (lastInstr is BranchInstr) {
      BasicBlock(block.subList(0, block.size-1).map { it.first as StmtInstr }, Branch(lastInstr.t, nxts))
    } else if (lastInstr is StmtInstr) {
      BasicBlock(block.map { it.first as StmtInstr }, Branch(null, nxts))
    } else if (lastInstr is ExitInstr) {
      BasicBlock(block.subList(0, block.size-1).map { it.first as StmtInstr }, Branch("<return>", nxts))
    } else fail()

    val newIdx = blocks.size
    blocks.add(blck)
    blockCPoints.forEachIndexed { offset, cp ->
      renames[cp] = newIdx to offset
    }
    queue.addAll(nxts)
  }
  // fix instruction references
  val res = blocks.mapIndexed { at, b ->
    at to BasicBlock(b.stmts, b.branch.copy(next = b.branch.next.map { renames[it]!!.first }))
  }
  val loopInfo = convertor.loopInfo.map {
    if (renames[it.head]!!.second != 0) fail()
    if (renames[it.body]!!.second != 0) fail()
    if (renames[it.exit]!!.second != 0) fail()
    WhileInfo2(renames[it.head]!!.first, renames[it.test]!!.first, renames[it.body]!!.first, renames[it.exit]!!.first)
  }
  val ifInfo = convertor.ifInfo.map {
    if (renames[it.body]!!.second != 0) fail()
    if (renames[it.orelse]!!.second != 0) fail()
    if (renames[it.exit]!!.second != 0) fail()
    IfInfo2(renames[it.head]!!, renames[it.test]!!.first, renames[it.body]!!.first, renames[it.orelse]!!.first, renames[it.exit]!!.first)
  }

  return CFGraphImpl(res, loopInfo, ifInfo)
  //return CFGImpl(res, renames[exit]!!, loopInfo, ifInfo)
}

class CFGraphImpl(_blocks: List<Pair<CPoint,BasicBlock>>, val loops: List<WhileInfo2>, val ifs: List<IfInfo2>): CFGraph<CPoint, BasicBlock> {
  val blocks = _blocks.toMap()
  override val transitions = blocks.mapValues { it.value.branch.next }
  override val reverse = reverse(transitions)
  override fun get(l: CPoint): BasicBlock = blocks[l]!!

  val entry = ensureSingle(transitions.keys.minus(reverse.keys))
}


data class Branch(val discrVar: String?, val next: List<CPoint>)
class BasicBlock(val stmts: List<StmtInstr>, val branch: Branch): IInstr {
  override val lNames: Set<String>
  override val rNames: Set<String>

  init {
    var gen: VarSet = branch.discrVar?.let { setOf(it) } ?: emptySet()
    var kill: VarSet = emptySet()
    stmts.reversed().forEach {
      gen = (gen - it.lNames) + it.rNames
      kill = kill + it.lNames
    }
    lNames = kill
    rNames = gen
  }
}

sealed class Instr2: IInstr
class StmtInstr(val lval: LVal, val rval: TExpr): Instr2() {
  override val lNames = lval.lNames
  override val rNames = lval.rNames.union(liveVarAnalysis(rval))
  override fun toString() = when {
    lval is EmptyLVal -> pyPrint(rval)
    else -> lval.toString() + " = " + pyPrint(rval)
  }
}
class BranchInstr(val t: String, val orelse: CPoint): Instr2() {
  override val lNames = emptySet<String>()
  override val rNames = setOf(t)
}
class ExitInstr(): Instr2() {
  override val lNames = emptySet<String>()
  override val rNames = setOf("<return>")
}

sealed class LVal {
  open val lNames: Set<String> = emptySet()
  open val rNames: Set<String> = emptySet()
}
object EmptyLVal: LVal()
class VarLVal(val v: String, val t: TExpr? = null): LVal() {
  override val lNames = setOf(v)
  override fun toString() = v
}
class FieldLVal(val r: TExpr, val f: String): LVal() {
  override val rNames = liveVarAnalysis(r)
  override fun toString() = pyPrint(r) + "." + f
}
class SubscriptLVal(val r: TExpr, val i: TSlice): LVal() {
  override val rNames = liveVarAnalysis(Subscript(r, i, ExprContext.Store))
  override fun toString() = pyPrint(r) + "[" + pyPrint(i) + "]"
}


class CFGImpl(val instrs: List<Pair<Instr2,List<CPoint>>>, exitL: CPoint, val loops: List<WhileInfo>, val ifs: List<IfInfo>): Graph<Instr2> {
  val revMap = instrs.mapIndexed { at: Int, (instr, _) -> instr to at }.toMap()
  fun index(n: Instr2): CPoint = revMap[n]!!
  val entry = instrs[0]
  val exit = instrs[exitL].first
  fun next(n: Instr2): List<Instr2> = instrs[revMap[n]!!].second.map { instrs[it].first }

  override val transitions = instrs.map { it.first to next(it.first) }.toMap()
  override val reverse = reverse(transitions)
}

class CfgToStmtConvertor(val cfg: CFGImpl) {
  val exit = cfg.index(cfg.exit)
  val ctrls = mutableListOf<CtrlInfo>()

  fun exitsLastCtrl(p: CPoint) = ctrls.isNotEmpty() && ctrls[ctrls.size-1].exit == p
  fun continuesLastCtrl(p: CPoint) = ctrls.isNotEmpty() && ctrls[ctrls.size-1].entry == p
  fun lastCtrlIsIf(p: CPoint) = ctrls.isNotEmpty() && ctrls[ctrls.size-1] is IfInfo
  fun lastCtrlIsWhile(p: CPoint) = ctrls.isNotEmpty() && ctrls[ctrls.size-1] is WhileInfo
  fun getLoops() = ctrls.filterIsInstance<WhileInfo>()
  fun exitsLastLoop(p: CPoint): Boolean {
    val loops = getLoops()
    return loops.isNotEmpty() && loops[loops.size-1].exit == p
  }
  fun continuesLastLoop(p: CPoint): Boolean {
    val loops = getLoops()
    return loops.isNotEmpty() && loops[loops.size-1].entry == p
  }
  fun continuesLoop(p: CPoint): Boolean {
    return ctrls.find { it is WhileInfo && it.entry == p } != null
  }

  fun process(p: CPoint): List<Stmt> {
    if (exitsLastCtrl(p) && lastCtrlIsIf(p)) return emptyList()
    if (continuesLastCtrl(p) && lastCtrlIsWhile(p)) return emptyList()
    if (p == exit) return listOf(Return())
    if (exitsLastLoop(p)) return listOf(Break())
    if (continuesLastLoop(p)) return listOf(Continue())

    val ctrl = (cfg.ifs+cfg.loops).find { it.entry == p }
    val (stmts, next) = if (ctrl != null) {
      when(ctrl) {
        is WhileInfo -> {
          processWhile(p, ctrl)
        }
        is IfInfo -> {
          processIf(p, ctrl)
        }
      } to ctrl.exit
    } else {
      val (i, nxt) = cfg.instrs[p]
      processAssign(i as StmtInstr) to nxt[0]
    }
    return stmts.plus(process(next))
  }

  fun processAssign(i: StmtInstr): List<Stmt> {
    val s = when(i.lval) {
      is EmptyLVal -> Expr(i.rval)
      is VarLVal -> Assign(mkName(i.lval.v, true), i.rval)
      is FieldLVal -> Assign(Attribute(i.lval.r, i.lval.f, ExprContext.Store), i.rval)
      is SubscriptLVal -> Assign(Subscript(i.lval.r, i.lval.i, ExprContext.Store), i.rval)
    }
    return listOf(s)
  }

  fun getTestBlock(p: CPoint, testL: CPoint): List<Stmt> {
    if (p == testL) return emptyList()
    val (i, nxt) = cfg.instrs[p]
    return processAssign(i as StmtInstr).plus(getTestBlock(nxt[0], testL))
  }
  fun getTest(p: CPoint, testL: CPoint): Pair<List<Stmt>, TExpr> {
    val stmts = getTestBlock(p, testL)
    val testI = cfg.instrs[testL].first as BranchInstr
    if (stmts.size == 1) {
      val s = stmts[0]
      if (s is Assign && s.target is Name && s.target.id == testI.t) {
        return emptyList<Stmt>() to s.value
      }
    }
    return stmts to mkName(testI.t)
  }

  fun getBlock(p: CPoint): List<Stmt> = process(p)
  fun processWhile(p: CPoint, wi: WhileInfo): List<Stmt> {
    ctrls.add(wi)
    val (testInstrs, testExpr) = getTest(p, wi.test)
    val body = getBlock(wi.body)
    val wh = if (testInstrs.isEmpty()) {
      While(testExpr, body)
    } else {
      While(test = NameConstant(true),
          body = testInstrs
              .plus(If(UnaryOp(EUnaryOp.Not, testExpr), listOf(Break())))
              .plus(body))
    }
    ctrls.remove(wi)
    return listOf(wh)
  }
  fun processIf(p: CPoint, ifi: IfInfo): List<Stmt> {
    ctrls.add(ifi)
    val (testInstrs, testExpr) = getTest(p, ifi.test)
    val res = testInstrs.plus(If(
        test = testExpr,
        body = getBlock(ifi.body),
        orelse = getBlock(ifi.orelse)
    ))
    ctrls.remove(ifi)
    return res
  }

}

fun cfgToStmts(cfg: CFGImpl): List<Stmt> {
  val convertor = CfgToStmtConvertor(cfg)
  return convertor.process(cfg.index(cfg.entry.first))
}

class CfgToStmtConvertor2(val fd: FunctionDef, val cfg: CFGraphImpl) {
  //val exit = cfg.index(cfg.exit)
  val ctrls = mutableListOf<CtrlInfo2>()

  fun exitsLastCtrl(p: CPoint2) = ctrls.isNotEmpty() && (ctrls[ctrls.size-1].exit to 0) == p
  fun continuesLastCtrl(p: CPoint2) = ctrls.isNotEmpty() && ctrls[ctrls.size-1].entry == p
  fun lastCtrlIsIf(p: CPoint2) = ctrls.isNotEmpty() && ctrls[ctrls.size-1] is IfInfo2
  fun lastCtrlIsWhile(p: CPoint2) = ctrls.isNotEmpty() && ctrls[ctrls.size-1] is WhileInfo2
  fun getLoops() = ctrls.filterIsInstance<WhileInfo2>()
  fun exitsLastLoop(p: CPoint2): Boolean {
    val loops = getLoops()
    return loops.isNotEmpty() && (loops[loops.size-1].exit to 0) == p
  }
  fun continuesLastLoop(p: CPoint2): Boolean {
    val loops = getLoops()
    return loops.isNotEmpty() && loops[loops.size-1].entry == p
  }
  fun continuesLoop(p: CPoint2): Boolean {
    return ctrls.find { it is WhileInfo2 && it.entry == p } != null
  }

  fun extract(p: CPoint2): Pair<Instr2, List<CPoint2>> {
    val bb = cfg.get(p.first)
    if (p.second < bb.stmts.size) {
      val nxt: CPoint2 = if (p.second + 1 == bb.stmts.size && bb.branch.next.size == 1)
        bb.branch.next[0] to 0
      else
        p.first to p.second+1
      return bb.stmts[p.second] to listOf(nxt)
    } else {
      if (bb.branch.next.isEmpty())
        return ExitInstr() to emptyList()
      else if (bb.branch.next.size == 1)
        fail()
      else
        return BranchInstr(bb.branch.discrVar!!, bb.branch.next[1]) to bb.branch.next.map { it to 0 }
    }
  }
  fun process(p: CPoint2): List<Stmt> {
    if (exitsLastCtrl(p) && lastCtrlIsIf(p)) return emptyList()
    if (continuesLastCtrl(p) && lastCtrlIsWhile(p)) return emptyList()
    if (extract(p).first is ExitInstr) fail() //listOf(Return())
    if (exitsLastLoop(p)) return listOf(Break())
    if (continuesLastLoop(p)) return listOf(Continue())

    val ctrl = (cfg.ifs+cfg.loops).find { it.entry == p }
    val (stmts, next) = if (ctrl != null) {
      when(ctrl) {
        is WhileInfo2 -> {
          processWhile(p, ctrl)
        }
        is IfInfo2 -> {
          processIf(p, ctrl)
        }
      } to (ctrl.exit to 0)
    } else {
      val (_i, nxt) = extract(p)
      val i = _i as StmtInstr

      if (extract(nxt[0]).first is ExitInstr) {
        val retName = (i.lval as VarLVal).v
        if (retName != "<return>") fail()
        val retVal = if (fd.returns == null || fd.returns == NameConstant(null)) {
          if (i.rval != NameConstant(null)) fail()
          null
        } else i.rval
        return listOf(Return(retVal))
      }
      processAssign(i) to nxt[0]
    }
    return stmts.plus(process(next))
  }

  fun processAssign(i: StmtInstr): List<Stmt> {
    val s = when(i.lval) {
      is EmptyLVal -> Expr(i.rval)
      is VarLVal ->
        Assign(mkName(i.lval.v, true), i.rval)
      is FieldLVal -> Assign(Attribute(i.lval.r, i.lval.f, ExprContext.Store), i.rval)
      is SubscriptLVal -> Assign(Subscript(i.lval.r, i.lval.i, ExprContext.Store), i.rval)
    }
    return listOf(s)
  }

  fun getTestBlock(p: CPoint2, testL: CPoint): List<Stmt> {
    if (p.first == testL && p.second == cfg.get(testL).stmts.size) return emptyList()
    val (i, nxt) = extract(p)
    return processAssign(i as StmtInstr).plus(getTestBlock(nxt[0], testL))
  }
  fun getTest(p: CPoint2, testL: CPoint): Pair<List<Stmt>, TExpr> {
    val stmts = getTestBlock(p, testL)
    val testI = extract(testL to cfg.get(testL).stmts.size).first as BranchInstr
    if (stmts.size == 1) {
      val s = stmts[0]
      if (s is Assign && s.target is Name && s.target.id == testI.t) {
        return emptyList<Stmt>() to s.value
      }
    }
    return stmts to mkName(testI.t)
  }

  fun getBlock(p: CPoint): List<Stmt> = process(p to 0)
  fun processWhile(p: CPoint2, wi: WhileInfo2): List<Stmt> {
    ctrls.add(wi)
    val (testInstrs, testExpr) = getTest(p, wi.test)
    val body = getBlock(wi.body)
    val wh = if (testInstrs.isEmpty()) {
      While(testExpr, body)
    } else {
      While(test = NameConstant(true),
          body = testInstrs
              .plus(If(UnaryOp(EUnaryOp.Not, testExpr), listOf(Break())))
              .plus(body))
    }
    ctrls.remove(wi)
    return listOf(wh)
  }
  fun processIf(p: CPoint2, ifi: IfInfo2): List<Stmt> {
    ctrls.add(ifi)
    val (testInstrs, testExpr) = getTest(p, ifi.test)
    val res = testInstrs.plus(If(
        test = testExpr,
        body = getBlock(ifi.body),
        orelse = getBlock(ifi.orelse)
    ))
    ctrls.remove(ifi)
    return res
  }

}

fun isParamCall(e: TExpr) = e is Call && e.func == mkName("<Parameter>")
fun isParamDef(s: Stmt): Boolean = s is Assign && s.target is Name && isParamCall(s.value)
fun isParamDef(s: StmtInstr): Boolean = s.lval is VarLVal && isParamCall(s.rval)

fun cfgToStmts2(fd: FunctionDef, cfg: CFGraphImpl): List<Stmt> {
  val convertor = CfgToStmtConvertor2(fd, cfg)
  val res = convertor.process(0 to 0)
  val lastParamDef = res.findLast { isParamDef(it) }
  val stmts = if (lastParamDef != null) {
    val lastParamIndex = res.indexOf(lastParamDef)
    res.subList(lastParamIndex + 1, res.size)
  } else res
  return stmts
}

fun reconstructFuncDef(orig: FunctionDef, cfg: CFGraphImpl): FunctionDef {
  return orig.copy(body = cfgToStmts2(orig, cfg))
}

fun printCFG(cfg: CFGraphImpl) {
  cfg.blocks.forEach { (l, b) ->
    println("L$l:")
    b.stmts.forEach { i ->
      val st = when(i.lval) {
        is EmptyLVal -> Expr(i.rval)
        is VarLVal -> Assign(mkName(i.lval.v, true), i.rval)
        is FieldLVal -> Assign(Attribute(i.lval.r, i.lval.f, ExprContext.Store), i.rval)
        is SubscriptLVal -> Assign(Subscript(i.lval.r, i.lval.i, ExprContext.Store), i.rval)
      }
      pyPrintStmt(st).forEach { println("  $it") }
    }
    when(b.branch.next.size) {
      0 -> println("  exit")
      1 -> println("  goto L${b.branch.next[0]}")
      2 -> println("  if (${b.branch.discrVar!!}) goto L${b.branch.next[0]} else goto L${b.branch.next[1]}")
    }
  }

}

fun main() {
  val path = Paths.get("../eth2.0-specs/tests/fork_choice/defs_phase0_dev.txt")
  val parsed = Files.readAllLines(path).map { ItemsParser2.parseToEnd(it) }
  val defs = parsed.map { toStmt(it) }
  val fDefs = defs.filterIsInstance<FunctionDef>()

  val cfgs = mutableListOf<Pair<FunctionDef, CFGraphImpl>>()
  val pds = phase0PDs()
  fDefs.forEach {
    println(it.name)
    println()
    val f = desugar(transformForEnumerate(transformForOps(it)))

    //checkPurityConstraints(f, impure)
    val cfg = convertToCFG(f)
    val ssa = convertToSSA(cfg)

    cfgs.add(f to ssa)

    val mutRefs = findMutableRefs(f, ssa, pds)
    println()

    //printCFG(ssa)
    //println()

    //pyPrintFunc(f.copy(body = cfgToStmts2(ssa)))
    //println()


    println()

  }
}