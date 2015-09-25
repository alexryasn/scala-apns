package ru.ryasale.apns

import ru.ryasale.apns.internal.ReconnectPolicies
import ReconnectPolicies._

/**
 * !Ready!
 * Created by ryasale on 22.09.15.
 *
 * Represents the reconnection policy for the library.
 *
 * Each object should be used exclusively for one
 * {@code ApnsService} only.
 */
object ReconnectPolicy {

  /**
   * Types of the library provided reconnection policies.
   *
   * This should capture most of the commonly used cases.
   */
  object Provided extends Enumeration {
    type Provided = Value
    //val NEVER, EVERY_HALF_HOUR, EVERY_NOTIFICATION = Value

    /**
     * Only reconnect if absolutely needed, e.g. when the connection is dropped.
     * <p>
     * Apple recommends using a persistent connection.  This improves the latency of sending push notification messages.
     * <p>
     * The down-side is that once the connection is closed ungracefully (e.g. because Apple server drops it), the library wouldn't
     * detect such failure and not warn against the messages sent after the drop before the detection.
     */
    val NEVER = new Never // return new ReconnectPolicies.Never

    /**
     * Makes a new connection if the current connection has lasted for more than half an hour.
     * <p>
     * This is the recommended mode.
     * <p>
     * This is the sweat-spot in my experiments between dropped connections while minimizing latency.
     */
    val EVERY_HALF_HOUR = new EveryHalfHour // return new ReconnectPolicies.EveryHalfHour

    /**
     * Makes a new connection for every message being sent.
     *
     * This option ensures that each message is actually
     * delivered to Apple.
     *
     * If you send <strong>a lot</strong> of messages though,
     * Apple may consider your requests to be a DoS attack.
     */
    val EVERY_NOTIFICATION = new Always // return new ReconnectPolicies.Always
  }

  abstract class Provided {
    def newObject: ReconnectPolicy
  }

}

trait ReconnectPolicy {
  /**
   * Returns {@code true} if the library should initiate a new
   * connection for sending the message.
   *
   * The library calls this method at every message push.
   *
   * @return true if the library should be reconnected
   */
  def shouldReconnect: Boolean

  /**
   * Callback method to be called whenever the library
   * makes a new connection
   */
  def reconnected()

  /**
   * Returns a deep copy of this reconnection policy, if needed.
   *
   * Subclasses may return this instance if the object is immutable.
   */
  def copy: ReconnectPolicy
}
