
import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicReference
import javax.management.Attribute
import javax.management.AttributeList
import javax.management.AttributeNotFoundException
import javax.management.DynamicMBean
import javax.management.MBeanAttributeInfo
import javax.management.MBeanInfo
import javax.management.ObjectName
import javax.management.openmbean.CompositeData
import javax.management.openmbean.TabularData
import org.slf4j.LoggerFactory


object Jmx {

  import scala.collection.JavaConversions._

  private val logger = LoggerFactory.getLogger(getClass)

  private val mbeanServer = ManagementFactory.getPlatformMBeanServer

  private val beans = scala.collection.mutable.HashMap.empty[JmxId, MutableJmxBean]

  def addOrUpdate(bean: JmxBean) {
    def register: MutableJmxBean = {
      val mb = new MutableJmxBean(bean)
      if (mbeanServer.isRegistered(bean.id.objectName)) remove(bean.id)
      mbeanServer.registerMBean(mb, bean.id.objectName)
      mb
    }
    val b = beans.getOrElseUpdate(bean.id, register)
    b.update(bean)
  }

  def remove(id: JmxId) {
    mbeanServer.unregisterMBean(id.objectName)
  }

  def list(query: String): List[JmxBean] = {
    val matches = mbeanServer.queryNames(new ObjectName(query), null).toList
    matches.flatMap { case name: ObjectName =>
      val mbeanInfo = mbeanServer.getMBeanInfo(name)
      val attrNames = mbeanInfo.getAttributes.map(_.getName)
      try {
        val attrs = mbeanServer.getAttributes(name, attrNames)
        val tuples = attrs.map { case attr: Attribute => attr.getName -> fixValue(attr.getValue) }
        Some(JmxBean(JmxId(name), tuples.toMap))
      } catch {
        case e: Exception =>
          logger.warn(s"could not query bean ${name}", e)
          None
      }
    }
  }

  private def isArray(v: Any): Boolean = {
    v != null && v.getClass.isArray
  }

  private def fixValue(value: Any): AnyRef = value match {
    case c: CompositeData =>
      val keys = c.getCompositeType.keySet
      keys.map(k => k -> fixValue(c.get(k))).toMap
    case t: TabularData =>
      t.values.map(v => fixValue(v)).toList
    case v if isArray(v) =>
      v.asInstanceOf[Array[_]].map(fixValue).toList
    case v: Number => v
    case v => v.toString
  }
}

object JmxId {
  def apply(name: String): JmxId = {
    apply(new ObjectName(name))
  }

  def apply(name: ObjectName): JmxId = {
    import scala.collection.JavaConversions._
    val props = name.getKeyPropertyList.asInstanceOf[java.util.Map[String, String]].toMap
    JmxId(name.getDomain, props)
  }
}

case class JmxId(domain: String, props: Map[String, String]) {
  // Cached as private val so it doesn't get included in automatically serialized output with the
  // json lib
  private[this] val objName: ObjectName = {
    val htable = new java.util.Hashtable[String, String]
    props.foreach { t => htable.put(t._1, t._2) }
    new ObjectName(domain, htable)
  }

  def objectName: ObjectName = objName
}

case class JmxBean(id: JmxId, attributes: Map[String, AnyRef]) extends DynamicMBean {

  private[this] val mbeanInfo = {
    new MBeanInfo(getClass.getName, "???", mbeanAttributes, null, null, null)
  }

  private def mbeanAttributes: Array[MBeanAttributeInfo] = {
    val attrInfos = attributes.map { case (name, value) => createAttributeInfo(name, value) }
    attrInfos.toArray
  }

  private def createAttributeInfo(name: String, value: AnyRef): MBeanAttributeInfo = {
    val typeName = value match {
      case _: Number => classOf[Number].getName
      case _         => classOf[String].getName
    }
    val isReadable = true
    val isWritable = false
    val isIs = false
    new MBeanAttributeInfo(name, typeName, "???", isReadable, isWritable, isIs)
  }

  def getAttribute(name: String): AnyRef = attributes.get(name).getOrElse {
    throw new AttributeNotFoundException(s"no attribute '${name}' for jmx bean '${id}'")
  }

  def getAttributes(names: Array[String]): AttributeList = {
    val list = new AttributeList
    names.foreach { name => list.add(new Attribute(name, getAttribute(name))) }
    list
  }

  def getMBeanInfo: MBeanInfo = mbeanInfo

  def invoke(name: String, params: Array[AnyRef], signature: Array[String]): AnyRef = {
    throw new UnsupportedOperationException(s"mbean '${id}' is read-only")
  }

  def setAttribute(attr: Attribute) {
    throw new UnsupportedOperationException(s"mbean '${id}' is read-only")
  }

  def setAttributes(attrs: AttributeList): AttributeList = {
    throw new UnsupportedOperationException(s"mbean '${id}' is read-only")
  }
  
}

class MutableJmxBean(initial: JmxBean) extends DynamicMBean {

  private val beanRef = new AtomicReference[JmxBean](initial)

  def update(bean: JmxBean) {
    beanRef.set(bean)
  }

  def getAttribute(name: String): AnyRef = beanRef.get.getAttribute(name)

  def getAttributes(names: Array[String]): AttributeList = beanRef.get.getAttributes(names)

  def getMBeanInfo: MBeanInfo = beanRef.get.getMBeanInfo

  def invoke(name: String, params: Array[AnyRef], signature: Array[String]): AnyRef = {
    beanRef.get.invoke(name, params, signature)
  }

  def setAttribute(attr: Attribute) {
    beanRef.get.setAttribute(attr)
  }

  def setAttributes(attrs: AttributeList): AttributeList = {
    beanRef.get.setAttributes(attrs)
  }
  
}
