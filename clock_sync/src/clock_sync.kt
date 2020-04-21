import kotlin.random.Random

typealias ClockId = Int
typealias Time = Double

data class Interval(val a: Double, val b: Double, val mid: Double = (a+b)/2) {
  constructor(a: Int, b: Int): this(a.toDouble(), b.toDouble())
}

operator fun Number.plus(b: Interval) = b.plus(this)
operator fun Interval.plus(o: Number) = Interval(a + o.toDouble(), b + o.toDouble())
operator fun Interval.times(o: Number) = Interval(a * o.toDouble(), b * o.toDouble())
operator fun Interval.plus(o: Interval) = Interval( a + o.a, b + o.b)
operator fun Interval.minus(o: Number) = Interval(a - o.toDouble(), b + o.toDouble())

fun marzullo(data: List<Interval>, k: Int): Interval {
  val lows = data.map { it.a }.sorted()
  val highs = data.map { it.b }.sorted()
  return Interval(lows[data.size-k-1], highs[k])
}

const val N = 1000
const val rho = 0.0
const val minD = 0
const val maxD = 2000
const val refClockAccuracy = 1000

val REF_CLOCK_ACCURACY = Interval(-refClockAccuracy/2,refClockAccuracy/2)
val NETWORK_DELAY = Interval(minD, maxD)
val DRIFT_FACTOR = Interval(1/(1+rho), 1+rho)
val ZERO_INTERVAL = Interval(0, 0)
val FAR_IN_FUTURE = Double.MAX_VALUE + ZERO_INTERVAL

data class TimeEstimate(val estimate: Interval, val localTimestamp: Time)

class TimeEstimator(val N: Int, val aggregateMethod: (List<Interval>, Int)->Interval) {
  val estimates = hashMapOf<ClockId,TimeEstimate>()
  var refClockEstimate: TimeEstimate? = null

  val localEstimate: TimeEstimate
      get() = refClockEstimate ?: TimeEstimate(FAR_IN_FUTURE, FAR_IN_FUTURE.mid.toDouble())
  val remoteEstimates: Collection<TimeEstimate>
      get() = estimates.values

  fun onRemoteClock(q: ClockId, clockTime: Time, localTime: Time) {
    estimates[q] = TimeEstimate(clockTime + REF_CLOCK_ACCURACY + NETWORK_DELAY, localTime)
  }
  fun onRefClock(clockTime: Time, localTime: Time) {
    refClockEstimate = TimeEstimate(clockTime + REF_CLOCK_ACCURACY, localTime)
  }

  fun updatedEstimates(localTime: Time) = run {
    val estimates = (remoteEstimates + localEstimate)
        .map { it.estimate + DRIFT_FACTOR * (localTime - it.localTimestamp) }
    val ests = (estimates + List(N-estimates.size) { FAR_IN_FUTURE })
    aggregateMethod(ests, (N-1)/2)
  }
}

fun main() {
  val rnd = Random
  val nodes = List(N) { _ -> TimeEstimator(N, ::marzullo)}
  val localClockOffsets = List(N) {_ -> 0*rnd.nextDouble(-1000.0,1000.0)}

  val badGuysAmount = 0 * (N - 1) / 2
  val twoSidedAttack = false

  val badGuys = nodes.indices.toList().subList(0, badGuysAmount).toSet()

  val ticks = 100

  val trueTimeStart = 0
  for (t in 0 until ticks) {
    val trueTime = trueTimeStart + t*10000
    println("tt $trueTime")
    for (i in nodes.indices) {
      val refClockOffset = if (i in badGuys) {
        val baseOffset = 10000.0 //rnd.nextDouble(10000.0,10000.0)
        if (twoSidedAttack) (2*(i%2)-1)*baseOffset else baseOffset
      } else 0.0

      val refClock = trueTime + rnd.nextDouble(REF_CLOCK_ACCURACY.a, REF_CLOCK_ACCURACY.b) + refClockOffset

      val p = nodes[i]
      val localTime = trueTime + localClockOffsets[i]
      p.onRefClock(refClock, localTime)
      val est = if (true) p.localEstimate.estimate else p.updatedEstimates(localTime)
      // broadcast
      for (j in nodes.indices) {
        if (i != j) {
          val q = nodes[j]
          val delay = rnd.nextDouble(NETWORK_DELAY.a, NETWORK_DELAY.b)
          val locTime = trueTime + localClockOffsets[j] + delay
          q.onRemoteClock(i, est.mid, locTime)
        }
      }
    }
    val ests = nodes.indices.map {
      val e = nodes[it].updatedEstimates(trueTime + localClockOffsets[it])
      e + -trueTime
    }
    val lows = ests.map { it.a }
    val mids = ests.map { it.mid }
    val highs = ests.map { it.b }
    val widths = ests.map {it.b-it.a}

    println("lows   " + lows.min() + " " + lows.max())
    println("mids   " + mids.min() + " " + mids.max() + " " + (mids.max()!! - mids.min()!!))
    println("highs  " + highs.min() + " " + highs.max())
    println("widths " + widths.min() + " " + widths.max())
  }
}
