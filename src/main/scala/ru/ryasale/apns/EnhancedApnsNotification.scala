package ru.ryasale.apns

import java.util
import java.util.concurrent.atomic.AtomicInteger

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import ru.ryasale.apns.internal.Utilities

/**
 * !Ready!
 * Created by ryasale on 22.09.15.
 *
 * Represents an APNS notification to be sent to Apple service.
 */
class EnhancedApnsNotification(identifier: Int, expiry: Int) extends ApnsNotification {

  private val COMMAND: Byte = 1
  var deviceToken: Array[Byte] = _
  var payload: Array[Byte] = _

  /**
   * Constructs an instance of {@code ApnsNotification}.
   *
   * The message encodes the payload with a {@code UTF-8} encoding.
   *
   * @param dtoken    The Hex of the device token of the destination phone
   * @param payloadS  The payload message to be sent
   */
  def this(identifier: Int, expiry: Int, dtoken: String, payloadS: String) = {
    this(identifier, expiry)
    deviceToken = Utilities.decodeHex(dtoken)
    payload = Utilities.toUTF8Bytes(payloadS)
  }

  /**
   * Constructs an instance of {@code ApnsNotification}.
   *
   * @param dtoken    The binary representation of the destination device token
   * @param payloadB   The binary representation of the payload to be sent
   */
  def this(identifier: Int, expiry: Int, dtoken: Array[Byte], payloadB: Array[Byte]) = {
    this(identifier, expiry)
    deviceToken = Utilities.copyOf(dtoken)
    payload = Utilities.copyOf(payloadB)
  }

  /**
   * Returns the binary representation of the device token.
   */
  override def getDeviceToken: Array[Byte] = Utilities.copyOf(deviceToken)

  /**
   * Returns the binary representation of the payload.
   *
   */
  override def getPayload: Array[Byte] = Utilities.copyOf(payload)

  override def getIdentifier: Int = identifier

  override def getExpiry: Int = expiry

  private var marshall: Array[Byte] = _

  /**
   * Returns the binary representation of the message as expected by the
   * APNS server.
   *
   * The returned array can be used to sent directly to the APNS server
   * (on the wire/socket) without any modification.
   */
  override def getMarshall = {
    if (marshall == null) {
      marshall = Utilities.marshallEnhanced(COMMAND, identifier,
        expiry, deviceToken, payload)
    }
    marshall.clone()
  }

  /**
   * Returns the length of the message in bytes as it is encoded on the wire.
   *
   * Apple require the message to be of length 255 bytes or less.
   *
   * @return length of encoded message in bytes
   */
  def length() = {
    val length: Int = 1 + 4 + 4 + 2 + deviceToken.length + 2 + payload.length
    val marshalledLength = getMarshall.length
    assert(marshalledLength == length)
    length
  }

  override def hashCode() = {
    (21
      + 31 * identifier
      + 31 * expiry
      + 31 * util.Arrays.hashCode(deviceToken)
      + 31 * util.Arrays.hashCode(payload)
      )
  }

  override def equals(that: Any): Boolean = {
    that match {
      case that: EnhancedApnsNotification =>
        that.isInstanceOf[EnhancedApnsNotification]
        true
      case _ =>
        val o: EnhancedApnsNotification = that.asInstanceOf[EnhancedApnsNotification]
        (this.identifier == o.getIdentifier && expiry == o.getExpiry
          && util.Arrays.equals(this.deviceToken, o.deviceToken)
          && util.Arrays.equals(this.payload, o.payload))
    }
  }


  @SuppressFBWarnings(value = Array[String]("DE_MIGHT_IGNORE"))
  override def toString = {
    var payloadString: String = null
    try {
      payloadString = new String(payload, "UTF-8")
    } catch {
      case ex: Exception => payloadString = "???"
    }
    "Message(Id=" + identifier + "; Token=" + Utilities.encodeHex(deviceToken) + "; Payload=" + payloadString + ")"
  }

}

object EnhancedApnsNotification {
  /**
   * The infinite future for the purposes of Apple expiry date
   */
  val MAXIMUM_EXPIRY: Int = Integer.MAX_VALUE

  private val nextId: AtomicInteger = new AtomicInteger(0)

  def INCREMENT_ID(): Int = {
    nextId.incrementAndGet()
  }

}
