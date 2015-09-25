package ru.ryasale.apns.exceptions

import java.io.IOException

import org.slf4j.LoggerFactory

/**
 * !Ready!
 * Created by ryasale on 16.09.15.
 *
 * Signals that an I/O exception of some sort has occurred. This
 * class is the general class of exceptions produced by failed or
 * interrupted I/O operations.
 *
 * This is a RuntimeException, unlike the java.io.IOException
 */
class RuntimeIOException(message: String, cause: IOException) extends ApnsException(message, cause) {
  override val SerialVersionUID: Long = 8665285084049041306L

  val logger = LoggerFactory.getLogger(getClass)

  def this() = this(null, null)

  def this(message: String) = this(message, null)

  def this(cause: IOException) = {
    this(null, cause)
    logger.error("{}", cause)
  }
}
