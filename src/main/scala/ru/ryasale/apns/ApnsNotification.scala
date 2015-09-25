package ru.ryasale.apns

/**
 * !Ready!
 * Created by ryasale on 16.09.15.
 *
 * Represents an APNS notification to be sent to Apple service.
 */
trait ApnsNotification {

  /**
   * Returns the binary representation of the device token.
   */
  def getDeviceToken: Array[Byte]

  /**
   * Returns the binary representation of the payload.
   *
   */
  def getPayload: Array[Byte]

  /**
   * Returns the identifier of the current message.  The
   * identifier is an application generated identifier.
   *
   * @return the notification identifier
   */
  def getIdentifier: Int

  /**
   * Returns the expiry date of the notification, a fixed UNIX
   * epoch date expressed in seconds
   *
   * @return the expiry date of the notification
   */
  def getExpiry: Int

  /**
   * Returns the binary representation of the message as expected by the
   * APNS server.
   *
   * The returned array can be used to sent directly to the APNS server
   * (on the wire/socket) without any modification.
   */
  def getMarshall: Array[Byte]

}
