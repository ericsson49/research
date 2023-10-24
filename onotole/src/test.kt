
@JvmInline
value class TTA<TA,TB,TC>(val ta: IfcA<TA,TB,TC>)
@JvmInline
value class TTB<TA,TB,TC>(val tb: IfcB<TA,TB,TC>)
@JvmInline
value class TTC<TA,TD>(val tc: IfcC<TA,TD>)
@JvmInline
value class TTD<TA>(val td: IfcD<TA>)

interface IfcA<TA, TB, TC> {
  fun copy(): TA
  fun calcSmth(b: TB, c: TC): TC
}
interface IfcB<TA, TB, TC> {
  fun copy(): TB
  fun f(a: TA) : TC
}
interface IfcC<TA, TD> {
  fun getA(): TA
  fun getD(): TD
}
interface IfcD<TA> {
  fun getA(): TA
}

interface TTT<TA: IfcA<TA,TB,TC>, TB: IfcB<TA,TB,TC>,TC:IfcC<TA,TD>,TD: IfcD<TA>> {
  fun t(b: TB, c: TC, d: TD): TC {
    return d.getA().copy().calcSmth(b, c)
  }
  fun t2(b: TB, c: TC): TD {
    return c.getD().getA().copy().calcSmth(b, c).getD()
  }
}

class TAImpl: IfcA<TAImpl,TBImpl,TCImpl> {
  override fun copy(): TAImpl = this
  override fun calcSmth(b: TBImpl, c: TCImpl): TCImpl {
    TODO("Not yet implemented")
  }
}
class TBImpl: IfcB<TAImpl,TBImpl,TCImpl> {
  override fun copy(): TBImpl = this
  override fun f(a: TAImpl): TCImpl {
    TODO("Not yet implemented")
  }
}
class TCImpl: IfcC<TAImpl,TDImpl> {
  override fun getA(): TAImpl {
    TODO("Not yet implemented")
  }
  override fun getD(): TDImpl {
    TODO("Not yet implemented")
  }
}
class TDImpl: IfcD<TAImpl> {
  override fun getA(): TAImpl {
    TODO("Not yet implemented")
  }
}
object TTTImpl: TTT<TAImpl,TBImpl,TCImpl,TDImpl>

fun main() {
  val b = TBImpl()
  val c = TCImpl()
  val d = TDImpl()
  val c2 = TTTImpl.t(b,c,d)
  TTTImpl.t2(b, c2)
}