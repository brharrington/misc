import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy

import scala.util.control.Exception


object ProxyHelper {

  /**
   * Creates a default implementation for an interface where all methods other than those on the
   * overrides object get sent to a wrapped delegate.
   */
  def wrapper[T](ctype: Class[T], delegate: T, overrides: AnyRef): T = {
    val handler = new InvocationHandler {
      def invoke(proxy: AnyRef, method: Method, args: Array[AnyRef]): AnyRef = {
        val c = overrides.getClass
        Exception.unwrapping(classOf[InvocationTargetException]) {
          try {
            val m = c.getMethod(method.getName, method.getParameterTypes: _*)
            m.invoke(overrides, args: _*)
          } catch {
            case _: NoSuchMethodException =>
              method.invoke(delegate, args: _*)
          }
        }
      }
    }

    val classLoader = ctype.getClassLoader
    val classes = Array[Class[_]](ctype)
    val config = Proxy.newProxyInstance(classLoader, classes, handler)
    config.asInstanceOf[T]
  }

  /**
   * Creates a default implementation for an interface where all methods return
   * an UnsupportedOperationException.
   */
  def unsupported[T](ctype: Class[T]): T = {
    val handler = new InvocationHandler {
      def invoke(proxy: AnyRef, method: Method, args: Array[AnyRef]): AnyRef = {
        throw new UnsupportedOperationException(s"${ctype.getName}.${method.getName}")
      }
    }

    val classLoader = ctype.getClassLoader
    val classes = Array[Class[_]](ctype)
    val config = Proxy.newProxyInstance(classLoader, classes, handler)
    config.asInstanceOf[T]
  }

  /**
   * Creates a default implementation for an interface where all methods return
   * an UnsupportedOperationException.
   */
  def unsupported[T](ctype: Class[T], overrides: AnyRef): T = {
    val handler = new InvocationHandler {
      def invoke(proxy: AnyRef, method: Method, args: Array[AnyRef]): AnyRef = {
        val c = overrides.getClass
        Exception.unwrapping(classOf[InvocationTargetException]) {
          try {
            val m = c.getMethod(method.getName, method.getParameterTypes: _*)
            m.invoke(overrides, args: _*)
          } catch {
            case _: NoSuchMethodException =>
              throw new UnsupportedOperationException(s"${ctype.getName}.${method.getName}")
          }
        }
      }
    }

    val classLoader = ctype.getClassLoader
    val classes = Array[Class[_]](ctype)
    val config = Proxy.newProxyInstance(classLoader, classes, handler)
    config.asInstanceOf[T]
  }
}
