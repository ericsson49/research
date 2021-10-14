package onotole

sealed class RExpr
data class RLit(val expr: String): RExpr()
data class RName(val name: String): RExpr()
data class RInfix(val op: String, val args: List<RExpr>): RExpr() {
  constructor(op: String, l: RExpr, r: RExpr): this(op, listOf(l, r))
}
data class RPrefix(val op: String, val e: RExpr): RExpr()
data class RSeq(val open: String, val close: String, val delim: String, val args: List<RExpr>): RExpr()
data class RPExpr(val prio: Int, val expr: String): RExpr()

data class PExpr(val prio: Int, val expr: String) {
  override fun toString() = expr
}

const val MAX_PRIO = 15
const val MIN_PRIO = 0

fun get_op_prio(op: String): Int = when(op) {
  "" -> 15 // concatenation
  "()", "[]", "." -> 15
  "unary_+", "unary_-", "unary_~", "unary_!" -> 14
  "as", "as?" -> 13
  "*", "/", "%" -> 12
  "+", "-" -> 11
  "to", "until", "shr", "shl", "and", "or", "xor" -> 9
  "^", ">>", "<<", "&", "|" -> 9
  "in", "!in", "is", "!is" -> 7
  "<", ">", "<=", ">=" -> 6
  "==", "!=" -> 5
  "&&" -> 4
  "||" -> 3
  "unary_*" -> 2
  "=" -> 1
  else -> fail("unsupported $op")
}

fun format_commutative_op_expr(args: List<PExpr>, op: String): PExpr {
  val prio = get_op_prio(op)
  return when {
    args.size == 1 -> args[0]
    args.size > 1 -> PExpr(prio, args.joinToString(" $op ") { if (/*it.prio <= prio*/it.prio < 14) "(${it.expr})" else it.expr })
    else -> fail("shouldn't happen")
  }
}

fun format_binop_expr(a: PExpr, b: PExpr, op: String): PExpr {
  val prio = get_op_prio(op)
  val wa = if (/*a.prio < prio*/ a.prio != prio && a.prio < 14) "(${a.expr})" else a.expr
  val wb = if (/*b.prio <= prio*/b.prio == prio || b.prio < 14) "(${b.expr})" else b.expr
  return PExpr(prio, "$wa $op $wb")
}

fun format_unop_expr(a: PExpr, op: String): PExpr {
  val prio = get_op_prio("unary_" + op)
  val wa = if (/*a.prio < prio*/a.prio < 14) "(${a.expr})" else a.expr
  return PExpr(prio, "$op$wa")
}