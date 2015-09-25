package ru.ryasale.apns.exceptions

import ru.ryasale.apns.DeliveryError
import DeliveryError.DeliveryError

/**
 * !Ready!
 * Created by ryasale on 24.09.15.
 */
class ApnsDeliveryErrorException(deliveryError: DeliveryError) extends ApnsException {
  override def getMessage: String = {
    "Failed to deliver notification with error code " + deliveryError.id
  }

  // getDeliveryError() not moved from Java, because in Scala method deliveryError() exists by default
}
