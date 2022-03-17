package onotole

import kotlin.Number

typealias identifier = String
typealias int = Int
typealias string = String
typealias _object = Any
typealias constant = Any?

val None = null
val True = true
val False = false

sealed class Alias
sealed class WithItem
data class ExceptHandler(val typ: TExpr, val name: identifier?, val body: List<Stmt>)

data class Keyword(val arg: identifier?, val value: TExpr)
data class Arg(val arg: identifier, val annotation: TExpr? = null)
data class Arguments(
    val posonlyargs: List<Arg> = listOf(),
    val args: List<Arg> = listOf(),
    val vararg: Arg? = null,
    val kwonlyargs: List<Arg> = listOf(),
    val kw_defaults:List<TExpr> = listOf(),
    val kwarg: Arg? = null,
    val defaults: List<TExpr> = listOf())

data class Comprehension(val target: TExpr, val iter: TExpr, val ifs: List<TExpr>, val is_async: int)

enum class ECmpOp {
  Eq, NotEq, Lt, LtE, Gt, GtE, Is, IsNot, In, NotIn
}

enum class EUnaryOp {
  Invert, Not, UAdd, USub
}

enum class EBinOp {
  Add, Sub, Mult, MatMult, Div, Mod, Pow, LShift, RShift, BitOr, BitXor, BitAnd, FloorDiv
}

enum class EBoolOp {
  And, Or
}

sealed class TSlice
data class Slice(val lower: TExpr? = null, val upper: TExpr? = null, val step: TExpr? = null): TSlice()
class ExtSlice(dims: List<TSlice> = listOf()): TSlice()
data class Index(val value: TExpr): TSlice()

enum class ExprContext {
  Load, Store, Del, AugLoad, AugStore, Param
}

sealed class TExpr
data class CTValue(val vtype: VType): TExpr()
data class CTV(val v: CTVal): TExpr()
data class Let(val bindings: List<Keyword>, val value: TExpr): TExpr()
data class BoolOp(val op: EBoolOp, val values: List<TExpr>): TExpr()
//class NamedExpr(target: TExpr, value: TExpr): TExpr()
data class BinOp(val left: TExpr, val op: EBinOp, val right: TExpr): TExpr()
data class UnaryOp(val op: EUnaryOp, val operand: TExpr): TExpr()
data class Lambda(val args: Arguments, val body: TExpr, val returns: TExpr? = null): TExpr()
data class IfExp(val test: TExpr, val body: TExpr, val orelse: TExpr): TExpr()
data class PyDict(val keys: List<TExpr>, val values: List<TExpr>): TExpr()
data class PySet(val elts: List<TExpr>): TExpr()
data class ListComp(val elt: TExpr, val generators: List<Comprehension>): TExpr()
data class SetComp(val elt: TExpr, val generators: List<Comprehension>): TExpr()
data class DictComp(val key: TExpr, val value: TExpr, val generators: List<Comprehension>): TExpr()
data class GeneratorExp(val elt: TExpr, val generators: List<Comprehension>): TExpr()
//class Await(value: TExpr): TExpr()
//class Yield(value: TExpr? = null): TExpr()
//class YieldFrom(value: TExpr): TExpr()
data class Compare(val left: TExpr, val ops: List<ECmpOp>, val comparators: List<TExpr>): TExpr()
data class Call(val func: TExpr, val args: List<TExpr>, val keywords: List<Keyword>): TExpr()
data class FormattedValue(val value: TExpr, val conversion: int? = null, val format_spec: TExpr? = null): TExpr()
data class JoinedStr(val values: List<TExpr>): TExpr()
sealed class Constant(val value: constant, kind: string? = null): TExpr()
data class Attribute(val value: TExpr, val attr: identifier, val ctx: ExprContext): TExpr()
data class Subscript(val value: TExpr, val slice: TSlice, val ctx: ExprContext): TExpr()
data class Starred(val value: TExpr, val ctx: ExprContext): TExpr()
data class Name(val id: identifier, val ctx: ExprContext): TExpr()
data class PyList(val elts: List<TExpr>, val ctx: ExprContext): TExpr()
data class Tuple(val elts: List<TExpr>, val ctx: ExprContext): TExpr()

data class Str(val s: String): Constant(s, "str")
data class Num(val n: Number): Constant(n, "num")
data class Bytes(val s: String): Constant(s, "bytes")
data class NameConstant(val _value: constant): Constant(_value, "name")

sealed class Stmt
data class FunctionDef(val name: identifier, val args: Arguments,
                       val body: List<Stmt> = listOf(), val decorator_list: List<TExpr> = listOf(),
                       val returns: TExpr? = null,
                       val type_comment: string? = null): Stmt()
//class AsyncFunctionDef(name: identifier, args: Arguments,
//                       body: List<Stmt> = listOf(), decorator_list: List<TExpr> = listOf(), returns: TExpr? = null,
//                       type_comment: string? = null): Stmt()

data class ClassDef(val name: identifier,
                    val bases: List<TExpr> = listOf(),
                    val keywords: List<Keyword> = listOf(),
                    val body: List<Stmt> = listOf(),
                    val decorator_list: List<TExpr> = listOf()): Stmt()
data class Return(val value: TExpr? = null): Stmt()

//class Delete(targets: List<TExpr> = listOf()): Stmt()
data class Assign(val target: TExpr, val value: TExpr/*, val type_comment: string? = null*/): Stmt()
data class AugAssign(val target: TExpr, val op: EBinOp, val value: TExpr): Stmt()
data class AnnAssign(val target: TExpr, val annotation: TExpr, val value: TExpr? = null/*, val simple: int*/): Stmt()
data class For(val target: TExpr, val iter: TExpr, val body: List<Stmt> = listOf()/*, val orelse: List<Stmt> = listOf(), val type_comment: string? = null*/): Stmt()
//class AsyncFor(target: TExpr, iter: TExpr, body: List<Stmt> = listOf(), orelse: List<Stmt> = listOf(), type_comment: string? = null): Stmt()
data class While(val test: TExpr, val body: List<Stmt> = listOf()/*, val orelse: List<Stmt> = listOf()*/): Stmt()
data class If(val test: TExpr, val body: List<Stmt> = listOf(), val orelse: List<Stmt> = listOf()): Stmt()
//class With(items: List<WithItem> = listOf(), body: List<Stmt> = listOf(), type_comment: string? = null): Stmt()
//class AsyncWith(items: List<WithItem> = listOf(), body: List<Stmt> = listOf(), type_comment: string? = null): Stmt()

class Raise(exc: TExpr? = null, cause: TExpr? = null): Stmt()
data class Try(val body: List<Stmt> = listOf(), val handlers: List<ExceptHandler> = listOf(), val orelse: List<Stmt> = listOf(), val finalbody: List<Stmt> = listOf()): Stmt()
data class Assert(val test: TExpr, val msg: TExpr? = null): Stmt()

//class Import(names: List<Alias> = listOf()): Stmt()
//class ImportFrom(module: identifier? = null, names: List<Alias> = listOf(), level: int? = null): Stmt()

//class Global(names: List<identifier> = listOf()): Stmt()
class Nonlocal(names: List<identifier> = listOf()): Stmt()
data class Expr(val value: TExpr): Stmt()
class Pass: Stmt()
class Break: Stmt()
class Continue: Stmt()
