package onotole

import java.awt.Frame

sealed class CExpr
data class CVar(val v: String): CExpr()
data class CApp(val func: CExpr, val arg: CExpr): CExpr()
data class CAbs(val p: String, val body: CExpr): CExpr()
sealed class CValue: CExpr()
data class WConst(val n: Int): CValue()
data class WClosure(val lam: CAbs, val env: CEnv): CValue()

typealias CEnv = Map<String, CValue>

sealed class CFrame
data class FArgHole(val func: WClosure): CFrame()
data class FAppHole(val arg: CExpr, val env: CEnv): CFrame()

typealias Kont = List<CFrame>
fun eval(c: CExpr, env: CEnv, cont: Kont): Triple<CExpr, CEnv, Kont> {
  return when (c) {
    is CVar -> Triple(env[c.v] ?: fail(), env, cont)
    is CApp -> Triple(c.func, env, cont.plus(FAppHole(c.arg, env)))
    is CAbs -> Triple(WClosure(c, env), env, cont)
    is CValue -> {
      if (cont.isEmpty()) {
        Triple(c, env, cont)
      } else {
        val top = cont.last()
        val rest = cont.subList(0, cont.size-1)
        when(top) {
          is FAppHole -> Triple(top.arg, top.env, rest.plus(FArgHole(if (c is WClosure) c else fail())))
          is FArgHole -> Triple(top.func.lam.body, env.plus(top.func.lam.p to c), rest)
        }
      }
    }
  }
}
fun eval(a: Triple<CExpr, CEnv, Kont>) = eval(a.first, a.second, a.third)

fun main() {

  val s1 = eval(CApp(CAbs("x", CVar("x")), WConst(2)), emptyMap(), emptyList())
  val s2 = eval(s1)
  val s3 = eval(s2)
  val s4 = eval(s3)
  val s5 = eval(s4)
  println(s5)
}