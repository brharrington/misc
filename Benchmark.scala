package foo

import java.util.Arrays

trait Benchmark {
  /**
   * Time the execution of a function.
   *
   * @param name  name of the test to show
   * @param w     number of warmup runs to execute before starting to time the function
   * @param n     number of test runs to execute
   * @apram v     expected result value from the function
   * @param f     function to test
   */
  def time(name: String, w: Int, n: Int, v: Int)(f: => Int) {
    System.gc()
    val warmupNumOk = (0 until w).count { i => v == f }

    var numOk = 0
    val times = new Array[Long](n)
    (0 until n).foreach { i =>
      val start = System.nanoTime
      if (v == f) numOk += 1
      val end = System.nanoTime
      times(i) = end - start
    }

    Arrays.sort(times)
    val timesMillis = times.map(_ / 1e6)
    printf(" warmup ok: %10.3f%%%n", 100.0 * warmupNumOk.toDouble / w)
    printf("   success: %10.3f%%%n", 100.0 * numOk.toDouble / n)
    printf("     total: %10.3f%n", timesMillis.sum)
    printf("       avg: %10.3f%n", timesMillis.sum / n)
    printf("       min: %10.3f%n", timesMillis.min)
    printf("       max: %10.3f%n", timesMillis.max)
    printf(" 50 %%-tile: %10.3f%n", timesMillis((n * 0.5).toInt))
    printf(" 90 %%-tile: %10.3f%n", timesMillis((n * 0.9).toInt))
    printf(" 95 %%-tile: %10.3f%n", timesMillis((n * 0.95).toInt))
    printf(" 99 %%-tile: %10.3f%n", timesMillis((n * 0.99).toInt))
    println("")
  }
}
