import org.scalatest.FunSuite

class ProxyHelperSuite extends FunSuite {

  test("wrapper with override") {
    val impl = new HelperTrait {
        def foo: Int = 1
        def foo2: Int = 2
        def bar(v: Int, s: String): String = s"not defined"
    }
    val p = ProxyHelper.wrapper(classOf[HelperTrait], impl, new OverrideClass)
    assert(p.foo === 42)
    assert(p.foo2 === 2)
    assert(p.bar(42, "forty-two") === "bar(42, forty-two)")
  }

  test("unsupported") {
    val p = ProxyHelper.unsupported(classOf[HelperTrait])
    intercept[UnsupportedOperationException] { p.foo }
    intercept[UnsupportedOperationException] { p.foo2 }
    intercept[UnsupportedOperationException] { p.bar(42, "forty-two") }
  }

  test("unsupported with override") {
    val p = ProxyHelper.unsupported(classOf[HelperTrait], new OverrideClass)
    assert(p.foo === 42)
    intercept[UnsupportedOperationException] { p.foo2 }
    assert(p.bar(42, "forty-two") === "bar(42, forty-two)")
  }
}

trait HelperTrait {
  def foo: Int
  def foo2: Int
  def bar(v: Int, s: String): String
}

class OverrideClass {
  def foo: Int = 42
  def bar(v: Int, s: String): String = s"bar($v, $s)"
}
