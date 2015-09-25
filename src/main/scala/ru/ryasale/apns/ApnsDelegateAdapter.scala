package ru.ryasale.apns

import DeliveryError.DeliveryError

/**
 * !Ready!
 * Created by ryasale on 22.09.15.
 *
 * A no operation delegate that does nothing!
 */
class ApnsDelegateAdapter extends ApnsDelegate {
  override def messageSent(message: ApnsNotification, resent: Boolean) {}

  override def messageSendFailed(message: ApnsNotification, e: Throwable) {}

  override def connectionClosed(e: DeliveryError, messageIdentifier: Int) {}

  override def cacheLengthExceeded(newCacheLength: Int) {}

  override def notificationsResent(resendCount: Int) {}
}
