package ru.ryasale.apns

/**
 * !Ready!
 * Created by ryasale on 22.09.15.
 *
 * Errors in delivery that may get reported by Apple APN servers
 */
object DeliveryError extends Enumeration {

  type DeliveryError = Value

  /**
   * Connection closed without any error.
   *
   * This may occur if the APN service faces an invalid simple
   * APNS notification while running in enhanced mode
   */
  val NO_ERROR = Value(0)
  val PROCESSING_ERROR = Value(1)
  val MISSING_DEVICE_TOKEN = Value(2)
  val MISSING_TOPIC = Value(3)
  val MISSING_PAYLOAD = Value(4)
  val INVALID_TOKEN_SIZE = Value(5)
  val INVALID_TOPIC_SIZE = Value(6)
  val INVALID_PAYLOAD_SIZE = Value(7)
  val INVALID_TOKEN = Value(8)

  val NONE = Value(255)
  val UNKNOWN = Value(254)

  /** The status code as specified by Apple */
  def code(): Int = {
    Value.id
  }

  /**
   * Returns the appropriate {@code DeliveryError} enum
   * corresponding to the Apple provided status code
   *
   * @param code  status code provided by Apple
   * @return  the appropriate DeliveryError
   */
  def ofCode(code: Int): DeliveryError = {
    for (e <- DeliveryError.values) {
      if (e.id == code)
        e
    }
    UNKNOWN
  }

}
