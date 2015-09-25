package ru.ryasale.apns.exceptions

/**
 * !Ready!
 * Created by ryasale on 16.09.15.
 *
 * Base class for all the exceptions thrown in Apns Library
 */
abstract class ApnsException(message: String, cause: Throwable) extends RuntimeException(message, cause) {
  val SerialVersionUID: Long = -4756693306121825229L

  def this() = this(null, null)

  def this(message: String) = this(message, null)

  def this(cause: Throwable) = this(null, cause)
}
