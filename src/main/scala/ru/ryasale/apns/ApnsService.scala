package ru.ryasale.apns

import java.util
import java.util.Date
import ru.ryasale.apns.exceptions.NetworkIOException

/**
 * !Ready!
 * Created by ryasale on 16.09.15.
 *
 * Represents the connection and interface to the Apple APNS servers.
 *
 * The service is created by {@link ApnsServiceBuilder} like:
 *
 * <pre>
 * ApnsService = APNS.newService()
 * .withCert("/path/to/certificate.p12", "MyCertPassword")
 * .withSandboxDestination()
 * .build()
 * </pre>
 */
trait ApnsService {

  /**
   * Sends a push notification with the provided {@code payload} to the
   * iPhone of {@code deviceToken}.
   *
   * The payload needs to be a valid JSON object, otherwise it may fail
   * silently.  It is recommended to use {@link PayloadBuilder} to create
   * one.
   *
   * @param deviceToken   the destination iPhone device token
   * @param payload       The payload message
   *                      //@throws NetworkIOException if a network error occurred while
   *                      attempting to send the message
   */
  @throws(classOf[NetworkIOException])
  def push(deviceToken: String, payload: String): ApnsNotification

  @throws(classOf[NetworkIOException])
  def push(deviceToken: String, payload: String, expiry: Date): EnhancedApnsNotification

  /**
   * Sends a push notification with the provided {@code payload} to the
   * iPhone of {@code deviceToken}.
   *
   * The payload needs to be a valid JSON object, otherwise it may fail
   * silently.  It is recommended to use {@link PayloadBuilder} to create
   * one.
   *
   * @param deviceToken   the destination iPhone device token
   * @param payload       The payload message
   * @throws NetworkIOException if a network error occurred while
   *                            attempting to send the message
   */
  @throws(classOf[NetworkIOException])
  def push(deviceToken: Array[Byte], payload: Array[Byte]): ApnsNotification

  @throws(classOf[NetworkIOException])
  def push(deviceToken: Array[Byte], payload: Array[Byte], expiry: Int): EnhancedApnsNotification

  /**
   * Sends a bulk push notification with the provided
   * {@code payload} to iPhone of {@code deviceToken}s set.
   *
   * The payload needs to be a valid JSON object, otherwise it may fail
   * silently.  It is recommended to use {@link PayloadBuilder} to create
   * one.
   *
   * @param deviceTokens   the destination iPhone device tokens
   * @param payload       The payload message
   * @throws NetworkIOException if a network error occurred while
   *                            attempting to send the message
   */
  @throws(classOf[NetworkIOException])
  def push(deviceTokens: util.Collection[String], payload: String): util.Collection[_ <: ApnsNotification]

  @throws(classOf[NetworkIOException])
  def push(deviceTokens: util.Collection[String], payload: String, expiry: Date): util.Collection[_ <: EnhancedApnsNotification]

  /**
   * Sends a bulk push notification with the provided
   * {@code payload} to iPhone of {@code deviceToken}s set.
   *
   * The payload needs to be a valid JSON object, otherwise it may fail
   * silently.  It is recommended to use {@link PayloadBuilder} to create
   * one.
   *
   * @param deviceTokens   the destination iPhone device tokens
   * @param payload       The payload message
   * @throws NetworkIOException if a network error occurred while
   *                            attempting to send the message
   */
  @throws(classOf[NetworkIOException])
  def push(deviceTokens: util.Collection[Array[Byte]], payload: Array[Byte]): util.Collection[_ <: ApnsNotification]

  @throws(classOf[NetworkIOException])
  def push(deviceTokens: util.Collection[Array[Byte]], payload: Array[Byte], expiry: Int): util.Collection[_ <: EnhancedApnsNotification]

  /**
   * Sends the provided notification {@code message} to the desired
   * destination.
   * @throws NetworkIOException if a network error occurred while
   *                            attempting to send the message
   */

  def push(message: ApnsNotification)

  /**
   * Starts the service.
   *
   * The underlying implementation may prepare its connections or
   * data structures to be able to send the messages.
   *
   * This method is a blocking call, even if the service represents
   * a Non-blocking push service.  Once the service is returned, it is ready
   * to accept push requests.
   *
   * @throws NetworkIOException if a network error occurred while
   *                            starting the service
   */
  def start()

  /**
   * Stops the service and frees any allocated resources it created for this
   * service.
   *
   * The underlying implementation should close all connections it created,
   * and possibly stop any threads as well.
   */
  def stop()

  /**
   * Returns the list of devices that reported failed-delivery
   * attempts to the Apple Feedback services.
   *
   * The result is map, mapping the device tokens as Hex Strings
   * mapped to the timestamp when APNs determined that the
   * application no longer exists on the device.
   * @throws NetworkIOException if a network error occurred
   *                            while retrieving invalid device connection
   */
  @throws(classOf[NetworkIOException])
  def getInactiveDevices: Map[String, Date]

  /**
   * Test that the service is setup properly and the Apple servers
   * are reachable.
   *
   * @throws NetworkIOException   if the Apple servers aren't reachable
   *      or the service cannot send notifications for now
   */
  @throws(classOf[NetworkIOException])
  def testConnection()

}
