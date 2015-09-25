package ru.ryasale.apns.exceptions

import java.io.IOException

/**
 * !Ready!
 * Created by ryasale on 22.09.15.
 *
 * Thrown to indicate that that a network operation has failed:
 * (e.g. connectivity problems, domain cannot be found, network
 * dropped).
 */
class NetworkIOException(m: String, cause: IOException, resend: Boolean) extends ApnsException(m, cause) {
  val serialVersionUID: Long = 3353516625486306533L

  def this() = this(null, null, false)

  def this(message: String) = this(message, null, false)

  def this(cause: IOException) = this(null, cause, false)

  def this(m: String, c: IOException) = this(m, c, false)

  def this(cause: IOException, resend: Boolean) = this(cause)

  /**
   * Identifies whether an exception was thrown during a resend of a
   * message or not.  In this case a resend refers to whether the
   * message is being resent from the buffer of messages internal.
   * This would occur if we sent 5 messages quickly to APNS:
   * 1,2,3,4,5 and the 3 message was rejected.  We would
   * then need to resend 4 and 5.  If a network exception was
   * triggered when doing this, then the resend flag will be
   * {@code true}.
   * @return { @code true} for an exception trigger during a resend, otherwise { @code false}.
   */
  def isResend: Boolean = {
    resend
  }

}
