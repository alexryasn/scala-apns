package ru.ryasale.apns.internal

import java.io.Closeable
import ru.ryasale.apns.ApnsNotification
import ru.ryasale.apns.exceptions.NetworkIOException

/**
 * !Ready!
 * Created by ryasale on 22.09.15.
 */
trait ApnsConnection extends Closeable {

  @throws(classOf[NetworkIOException])
  def sendMessage(m: ApnsNotification)

  @throws(classOf[NetworkIOException])
  def testConnection()

  def copy(): ApnsConnection

  def setCacheLength(cacheLength: Integer)

  def getCacheLength(): Int
}

object ApnsConnection {
  //Default number of notifications to keep for error purposes
  val DEFAULT_CACHE_LENGTH: Int = 100
}
