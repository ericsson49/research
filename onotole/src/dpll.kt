typealias Lit = String
typealias Clause = Set<Lit>
typealias Formula = Set<Clause>
typealias Solution = Set<Lit>
fun p(l:String) = l
fun n(l:String) = "!$l"
fun or(vararg ls: Lit): Clause = ls.toSet()
fun and(vararg cls: Clause): Formula = cls.toSet()
val Lit.positive get() = !this.startsWith("!")
val Lit.name get() = if (!this.positive) this.substring(1) else this
operator fun Lit.not() = if (this.positive) n(this) else this.name
infix fun Clause.or(l: Lit) = this.plusElement(l)
infix fun Lit.or(l: Lit): Clause = setOf(this, l)
infix fun Lit.and(c: Clause): Formula = setOf(setOf(this)).plusElement(c)
infix fun Clause.and(c: Clause): Formula = setOf(c).plusElement(c)
infix fun Formula.and(l: Lit): Formula = this.plusElement(setOf(l))
fun Solution.applyToC(c: Clause): Set<Clause> = when {
    c.any { l -> l in this } -> emptySet()
    else                     -> setOf(c.filter { l -> l.not() !in this }.toSet())
}
fun Solution.applyToF(f: Formula): Formula = f.flatMap(this::applyToC).toSet()
fun sat(origF: Formula): Solution? {
    var f = origF
    val solution = mutableSetOf<Lit>()
    while (true) {
        if (f.isEmpty()) return solution
        if (f.any { it.isEmpty() }) return null
        if (!solution.addAll(f.filter { it.size == 1 }.flatten()))
            break
        f = solution.applyToF(f)
    }
    val splitL = f.first().first()
    return (sat(f and splitL) ?: sat(f and !splitL))?.union(solution)
}
fun main() {
    println(sat("a" and (!"a" or !"b" or "c") and "b"))
}