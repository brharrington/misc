
object Macro {
  import scala.language.experimental.macros
  import scala.reflect.macros.blackbox.Context

  case class Timer(name: String) {
    def record(s: Long, e: Long): Unit = println(s"$name => $e - $s = ${e - s}")
  }

  implicit class RichTimer(val t: Timer) {
    def time[T](expr: T): T = macro timeImpl[T]
  }

  def timeImpl[T](c: Context)(expr: c.Tree): c.Tree = {
    import c.universe._
    q"""
      val s = System.nanoTime()
      try $expr finally ${c.prefix.tree}.t.record(s, System.nanoTime())
    """
  }
}

val t1 = Macro.Timer("a")
val t2 = Macro.Timer("b")
val v = t1 time { Thread.sleep(1000); t2 time { Thread.sleep(1000); 42 } }

