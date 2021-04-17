import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.parser.Parser

import kotlin.coroutines.coroutineContext

data class Ar(val n: String?, val t: Tp) {
  override fun toString() = (if (n == null) "" else "$n:") + t
}
data class Tp(val n: String, val ps: List<Tp>) {
  override fun toString() = if (ps.isEmpty()) n else "$n[${ps.joinToString(",")}]"
}
data class Tv(val n: String, val varAnno: String, val bound: Tp?) {
  override fun toString() = "$varAnno$n" + (bound?.let { "<:$it" } ?: "")
}
data class Fd(val n: String, val tps: List<Tv>, val ps: List<Ar>, val ret: Tp) {
  override fun toString() = "$n[${tps.joinToString(",")}](${ps.joinToString(",")}):$ret"
}

object FuncDeclParser : Grammar<Fd>() {
  val TPARAM by token("[A-Z_][A-Z_a-z0-9]*")
  val WORD by token("[A-Z_a-z][A-Z_a-z0-9]*")
  val COMMA by token(",")
  val LBR by token("\\[")
  val RBR by token("\\]")
  val LPAR by token("\\(")
  val RPAR by token("\\)")
  val ARR by token("->")
  val EQ by token("=")
  val ST by token("<:")
  val COL by token(":")
  val ASTER by token("\\*")
  val MOD by token("[+-?]")

  //val typeDecl: Parser<Pair<TokenMatch>> = WORD * optional(-LBR * separatedTerms(typeDecl, COMMA) * -RBR)
  //val varAnno = optional(token("[+\\-?]"))
  val tp = optional(MOD) * TPARAM * optional(-ST * parser { type }) map { Tv(it.t2.text, it.t1?.text ?: "", it.t3) }
  val tps = -LBR * separatedTerms(tp, COMMA) * -RBR
  //val argDecl = optional(token("\\*")) * optional(WORD * -token(":")) * WORD use { it -> }
  //val funDef = WORD * /*optional(typeParams) **/ -LPAR * separatedTerms(argDecl, COMMA, true) * -RPAR * ARR

  val tpList: Parser<List<Tp>> = separatedTerms(parser { type }, COMMA)
  val type: Parser<Tp> = ((WORD or TPARAM) * optional(-LBR * tpList * -RBR)) map  { Tp(it.t1.text, it.t2 ?: emptyList()) }
  val arg = optional((WORD or ASTER) * -COL) * type map { Ar(it.t1?.text, it.t2) }
  val funDef = WORD * optional(tps) * -LPAR * separatedTerms(arg, COMMA, true) * -RPAR * -ARR * type map {
      Fd(it.t1.text, it.t2 ?: emptyList(), it.t3, it.t4) }


  override val rootParser = funDef

}

fun main() {
  println(FuncDeclParser.parseToEnd("max[T,R<:comp[R]](*:T,key:call[T,R],default:T)->T"))
  println(FuncDeclParser.parseToEnd("map[A,B](call[A,B],iter[A])->iter[B]"))
}