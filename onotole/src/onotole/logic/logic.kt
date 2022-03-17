package onotole.logic

data class Term(val n: String, val args: List<Term> = emptyList())
sealed class Formula
sealed class Constraint: Formula()
data class SubType(val a: Term, val b: Term): Constraint()
data class EqType(val a: Term, val b: Term): Constraint()
data class Rule(val pre: List<Constraint>, val post: List<Constraint>)
data class RuleSet(val rules: List<Rule>): Formula()

interface Solver {
  fun add(c: Constraint)
  fun add(cs: Collection<Constraint>) {
    cs.forEach(::add)
  }
  fun checkSat(cs: List<Constraint>): Boolean = TODO()

  fun addRuleSet(rs: RuleSet): Boolean {
    for(r in rs.rules) {
      if (checkSat(r.pre)) {
        add(r.post)
        return true
      }
    }
    return false
  }
}