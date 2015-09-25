package ru.ryasale.apns.internal

import java.util
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import ru.ryasale.apns.{EnhancedApnsNotification, ApnsService, ApnsNotification}
import ru.ryasale.apns.exceptions.NetworkIOException

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import scala.collection.JavaConversions._

/**
 * !Ready!
 * Created by ryasale on 22.09.15.
 */
abstract class AbstractApnsService(feedback: ApnsFeedbackConnection) extends ApnsService {

  private val c: AtomicInteger = new AtomicInteger()

  @throws(classOf[NetworkIOException])
  override def push(deviceToken: String, payload: String): EnhancedApnsNotification = {
    val notification: EnhancedApnsNotification =
      new EnhancedApnsNotification(c.incrementAndGet(), EnhancedApnsNotification.MAXIMUM_EXPIRY, deviceToken, payload)
    push(notification)
    notification
  }

  @throws(classOf[NetworkIOException])
  override def push(deviceToken: String, payload: String, expiry: Date): EnhancedApnsNotification = {
    val notification: EnhancedApnsNotification =
      new EnhancedApnsNotification(c.incrementAndGet(), (expiry.getTime / 1000).toInt, deviceToken, payload)
    push(notification)
    notification
  }

  @throws(classOf[NetworkIOException])
  def push(deviceToken: Array[Byte], payload: Array[Byte]): EnhancedApnsNotification = {
    val notification: EnhancedApnsNotification =
      new EnhancedApnsNotification(c.incrementAndGet(), EnhancedApnsNotification.MAXIMUM_EXPIRY, deviceToken, payload)
    push(notification)
    notification
  }

  @throws(classOf[NetworkIOException])
  def push(deviceToken: Array[Byte], payload: Array[Byte], expiry: Int): EnhancedApnsNotification = {
    val notification: EnhancedApnsNotification =
      new EnhancedApnsNotification(c.incrementAndGet(), expiry, deviceToken, payload)
    push(notification)
    notification
  }


  @throws(classOf[NetworkIOException])
  def push(deviceTokens: util.Collection[String], payload: String): util.Collection[EnhancedApnsNotification] = {
    val messageBytes: Array[Byte] = Utilities.toUTF8Bytes(payload)
    val notifications: mutable.Buffer[EnhancedApnsNotification] = new ArrayBuffer[EnhancedApnsNotification](deviceTokens.size())
    for (deviceToken <- deviceTokens) {
      val dtBytes: Array[Byte] = Utilities.decodeHex(deviceToken)
      val notification: EnhancedApnsNotification =
        new EnhancedApnsNotification(c.incrementAndGet(), EnhancedApnsNotification.MAXIMUM_EXPIRY, dtBytes, messageBytes)
      notifications += notification
      push(notification)
    }
    asJavaCollection(notifications)
  }

  @throws(classOf[NetworkIOException])
  def push(deviceTokens: util.Collection[String], payload: String, expiry: Date): util.Collection[EnhancedApnsNotification] = {
    val messageBytes: Array[Byte] = Utilities.toUTF8Bytes(payload)
    val notifications: mutable.Buffer[EnhancedApnsNotification] = new ArrayBuffer[EnhancedApnsNotification](deviceTokens.size())
    for (deviceToken <- deviceTokens) {
      val dtBytes: Array[Byte] = Utilities.decodeHex(deviceToken)
      val notification: EnhancedApnsNotification =
        new EnhancedApnsNotification(c.incrementAndGet(), (expiry.getTime / 1000).toInt, dtBytes, messageBytes)
      notifications += notification
      push(notification)
    }
    asJavaCollection(notifications)
  }

  @throws(classOf[NetworkIOException])
  def push(deviceTokens: util.Collection[Array[Byte]], payload: Array[Byte]): util.Collection[EnhancedApnsNotification] = {
    val notifications: mutable.Buffer[EnhancedApnsNotification] = new ArrayBuffer[EnhancedApnsNotification](deviceTokens.size())
    for (deviceToken <- deviceTokens) {
      val notification: EnhancedApnsNotification =
        new EnhancedApnsNotification(c.incrementAndGet(), EnhancedApnsNotification.MAXIMUM_EXPIRY, deviceToken, payload)
      notifications += notification
      push(notification)
    }
    asJavaCollection(notifications)
  }

  @throws(classOf[NetworkIOException])
  def push(deviceTokens: util.Collection[Array[Byte]], payload: Array[Byte], expiry: Int): util.Collection[EnhancedApnsNotification] = {
    val notifications: mutable.Buffer[EnhancedApnsNotification] = new ArrayBuffer[EnhancedApnsNotification](deviceTokens.size())
    for (deviceToken <- deviceTokens) {
      val notification: EnhancedApnsNotification =
        new EnhancedApnsNotification(c.incrementAndGet(), expiry, deviceToken, payload)
      notifications += notification
      push(notification)
    }
    asJavaCollection(notifications)
  }

  @throws(classOf[NetworkIOException])
  def push(message: ApnsNotification) {}

  @throws(classOf[NetworkIOException])
  def getInactiveDevices: Map[String, Date] = {
    feedback.getInactiveDevices
  }

}
