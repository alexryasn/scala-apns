package ru.ryasale.apns

import DeliveryError.DeliveryError

/**
 * !Ready!
 * Created by ryasale on 22.09.15.
 *
 * A delegate that gets notified of the status of notification delivery to the
 * Apple Server.
 *
 * The delegate doesn't get notified when the notification actually arrives at
 * the phone.
 */
trait ApnsDelegate {

  /**
   * Called when message was successfully sent to the Apple servers
   *
   * @param message the notification that was sent
   * @param resent whether the notification was resent after an error
   */
  def messageSent(message: ApnsNotification, resent: Boolean)

  /**
   * Called when the delivery of the message failed for any reason
   *
   * If message is null, then your notification has been rejected by Apple but
   * it has been removed from the cache so it is not possible to identify
   * which notification caused the error. In this case subsequent
   * notifications may be lost. If this happens you should consider increasing
   * your cacheLength value to prevent data loss.
   *
   * @param message the notification that was attempted to be sent
   * @param e the cause and description of the failure
   */
  def messageSendFailed(message: ApnsNotification, e: Throwable)

  /**
   * The connection was closed and/or an error packet was received while
   * monitoring was turned on.
   *
   * @param e the delivery error
   * @param messageIdentifier  id of the message that failed
   */
  def connectionClosed(e: DeliveryError, messageIdentifier: Int)

  /**
   * The resend cache needed a bigger size (while resending messages)
   *
   * @param newCacheLength new size of the resend cache.
   */
  def cacheLengthExceeded(newCacheLength: Int)

  /**
   * A number of notifications has been queued for resending due to a error-response
   * packet being received.
   *
   * @param resendCount the number of messages being queued for resend
   */
  def notificationsResent(resendCount: Int)

}

/**
 * A no operation delegate that does nothing!
 */
object ApnsDelegate {
  val EMPTY = new ApnsDelegateAdapter()
}