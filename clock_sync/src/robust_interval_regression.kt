import kotlin.random.Random

fun <T: Comparable<T>> checkSorted(c: Iterable<T>) {
  var prev: T? = null
  for(e in c) {
    if (prev != null && e < prev) {
        throw IllegalArgumentException("Not sorted")
    }
    prev = e
  }
}

fun <A,B> Iterable<Pair<A,B>>.split(): Pair<Collection<A>,Collection<B>> {
  val aList = mutableListOf<A>()
  val bList = mutableListOf<B>()
  for (e in this) {
    aList.add(e.first)
    bList.add(e.second)
  }
  return Pair(aList, bList)
}

fun incompleteTheilSenInterval(es: List<TimeEstimate>, k: Int): Pair<Interval, Interval> {
  checkSorted(es.map { it.localTimestamp })

  val offset = if (es.size % 2 == 0) {
    // even
    es.size / 2
  } else {
    // odd
    (es.size + 1) / 2
  }

  val (intercepts, slopes) = (0 until (es.size-offset))
      .map { calcInterceptAndSlope(es[it], es[it+offset]) }
      .split()

  return Pair(marzullo(intercepts, k), marzullo(slopes, k))
}

fun theilSenInterval(es: List<TimeEstimate>, k: Int): Pair<Interval, Interval> {
  val interceptsAndSlopes = mutableListOf<Pair<Interval, Interval>>()
  for(i in 0 until es.size) {
    for(j in (i+1) until es.size) {
      interceptsAndSlopes.add(calcInterceptAndSlope(es[i], es[j]))
    }
  }
  val (intercepts, slopes) = interceptsAndSlopes.split()
  return Pair(marzullo(intercepts, k), marzullo(slopes, k))
}

fun repeatedMedianInterval(es: List<TimeEstimate>): Pair<Interval, Interval> {
  val slopEsts = mutableListOf<Interval>()
  val interceptEsts = mutableListOf<Interval>()
  for(i in es.indices) {
    val slopes = mutableListOf<Interval>()
    val intercepts = mutableListOf<Interval>()
    for (j in es.indices) {
      if (i != j) {
        val (i1,i2) = if (i > j) Pair(j, i) else Pair(i, j)
        val e1 = es[i1]
        val e2 = es[i2]

        val (intercept, slope) = calcInterceptAndSlope(e2, e1)
        intercepts.add(intercept)
        slopes.add(slope)
      }
    }
    slopEsts.add(marzullo(slopes, (slopes.size - 1) / 2))
    interceptEsts.add(marzullo(intercepts, (intercepts.size - 1) / 2))
  }
  val slopeEst = marzullo(slopEsts, (es.size - 1) / 2)
  val interceptEst = marzullo(interceptEsts, (es.size - 1) / 2)
  return Pair(interceptEst, slopeEst)
}

private fun calcInterceptAndSlope(e1: TimeEstimate, e2: TimeEstimate): Pair<Interval, Interval> {
  val dx = e2.localTimestamp - e1.localTimestamp
  val dyMax = e2.estimate.b - e1.estimate.a
  val dyMin = e2.estimate.a - e1.estimate.b
  val slopeMax = dyMax / dx
  val slopeMin = dyMin / dx
  val interceptMax = (e2.estimate.b * e1.localTimestamp - e1.estimate.a * e2.localTimestamp) / dx
  val interceptMin = (e2.estimate.a * e1.localTimestamp - e1.estimate.b * e2.localTimestamp) / dx

  if (interceptMax < interceptMin) {
    throw IllegalArgumentException()
  }
  val intercept = Interval(-interceptMax, -interceptMin)

  if (slopeMax < slopeMin) {
    throw IllegalArgumentException()
  }
  val slope = Interval(slopeMin, slopeMax)

  return Pair(intercept, slope)
}

fun main() {
  val rnd = Random(System.currentTimeMillis())
  val points = 0 .. 10000 step 100
  val cnt = points.toList().size
  val errCnt = 48
  val errs = List(errCnt) { true }.plus(List(cnt - errCnt) {false}).shuffled(rnd)
  val timeEsts = points.zip(errs).map { (it, err) ->
    val intrcpt = 100.0
    val sl = if (err) 3.0 else 1.0
    val te = TimeEstimate(Interval(intrcpt + it*sl - 10, intrcpt + it*sl + 10), it.toDouble())
    te
  }
  val (intEst, slEst) = repeatedMedianInterval(timeEsts)
  println("" + intEst + " " +  slEst)
}