package ru.ryasale.apns.internal

import java.io._
import java.net.Socket
import java.security.{GeneralSecurityException, KeyStore}
import java.util.Date
import java.util.regex.Pattern
import javax.net.ssl.{SSLSocketFactory, SSLContext, KeyManagerFactory, TrustManagerFactory}

import org.slf4j.LoggerFactory
import ru.ryasale.apns.exceptions.{InvalidSSLConfig, NetworkIOException}

import scala.collection.immutable.HashMap
import scala.collection.mutable

/**
 * !Ready!
 * Created by ryasale on 16.09.15.
 */
//class Utilities {
//def this() = throw new AssertionError("Uninstantiable class")
//}

object Utilities {

  val logger = LoggerFactory.getLogger(getClass)

  val SANDBOX_GATEWAY_HOST = "gateway.sandbox.push.apple.com"
  val SANDBOX_GATEWAY_PORT = 2195

  val SANDBOX_FEEDBACK_HOST = "feedback.sandbox.push.apple.com"
  val SANDBOX_FEEDBACK_PORT = 2196

  val PRODUCTION_GATEWAY_HOST = "gateway.sandbox.push.apple.com"
  val PRODUCTION_GATEWAY_PORT = 2195

  val PRODUCTION_FEEDBACK_HOST = "feedback.sandbox.push.apple.com"
  val PRODUCTION_FEEDBACK_PORT = 2196

  val MAX_PAYLOAD_LENGTH = 2048

  @throws(classOf[InvalidSSLConfig])
  def newSSLSocketFactory(cert: InputStream, password: String,
                          ksType: String, ksAlgorithm: String) = {
    val context = newSSLContext(cert, password, ksType, ksAlgorithm)
    context.getSocketFactory
  }

  @throws(classOf[InvalidSSLConfig])
  def newSSLContext(cert: InputStream, password: String,
                    ksType: String, ksAlgorithm: String): SSLContext = {
    try {
      val ks: KeyStore = KeyStore.getInstance(ksType)
      logger.info("try load KeyStore")
      ks.load(cert, password.toCharArray)
      logger.info("ks was succesfully load")
      newSSLContext(ks, password, ksAlgorithm);
    } catch {
      case e: Exception => throw new InvalidSSLConfig(e)
    }
  }

  @throws(classOf[InvalidSSLConfig])
  def newSSLContext(ks: KeyStore, password: String,
                    ksAlgorithm: String): SSLContext = {
    try {
      // Get a KeyManager and initialize it
      val kmf: KeyManagerFactory = KeyManagerFactory.getInstance(ksAlgorithm)
      kmf.init(ks, password.toCharArray)

      // Get a TrustManagerFactory with the DEFAULT KEYSTORE, so we have all
      // the certificates in cacerts trusted
      val tmf: TrustManagerFactory = TrustManagerFactory.getInstance(ksAlgorithm)
      tmf.init(null.asInstanceOf[KeyStore])

      // Get the SSLContext to help create SSLSocketFactory
      val sslContext: SSLContext = SSLContext.getInstance("TLS")
      sslContext.init(kmf.getKeyManagers, tmf.getTrustManagers, null)
      sslContext
    }
    catch {
      case e: GeneralSecurityException => throw new InvalidSSLConfig(e)
    }
  }

  private val pattern: Pattern = Pattern.compile("[ -]")

  def decodeHex(deviceToken: String) = {
    val hex: String = pattern.matcher(deviceToken).replaceAll("")

    val bts: Array[Byte] = new Array[Byte](hex.length() / 2)
    for (i <- bts.indices) {
      bts(i) = (hex.charAt(2 * i).toInt * 16 + hex.charAt(2 * i + 1).toInt).asInstanceOf[Byte]
    }
    bts
  }

  def charVal(a: Char): Int = {
    if ('0' <= a && a <= '9') {
      a - '0'
    } else if ('a' <= a && a <= 'f') {
      (a - 'a') + 10
    } else if ('A' <= a && a <= 'F') {
      (a - 'A') + 10
    } else {
      throw new RuntimeException("Invalid hex character: " + a)
    }
  }

  val base: Array[Char] = "0123456789ABCDEF".toArray

  def encodeHex(bytes: Array[Byte]) = {
    val chars: Array[Char] = new Array[Char](bytes.length * 2)
    for (i <- 1 to (bytes.length - 1)) {
      val b = bytes(i) & 0xFF
      chars(2 * i) = base(b >>> 4)
      chars(2 * i + 1) = base(b & 0xF)
    }
    new String(chars)
  }

  @throws(classOf[RuntimeException])
  def toUTF8Bytes(s: String): Array[Byte] = {
    try {
      s.getBytes("UTF-8")
    } catch {
      case e: UnsupportedEncodingException => throw new RuntimeException
    }
  }

  def marshall(command: Byte, deviceToken: Array[Byte], payload: Array[Byte]) = {
    val boas = new ByteArrayOutputStream()
    val dos = new DataOutputStream(boas)

    try {
      dos.writeByte(command)
      dos.writeShort(deviceToken.length)
      dos.write(deviceToken)
      dos.writeShort(payload.length)
      dos.write(payload)
      boas.toByteArray
    } catch {
      case e: IOException =>
        throw new AssertionError()
    }
  }

  @throws(classOf[AssertionError])
  def marshallEnhanced(command: Byte, identifier: Int,
                       expiryTime: Int, deviceToken: Array[Byte], payload: Array[Byte]) = {
    val boas: ByteArrayOutputStream = new ByteArrayOutputStream()
    val dos: DataOutputStream = new DataOutputStream(boas)

    try {
      dos.writeByte(command)
      dos.writeInt(identifier)
      dos.writeInt(expiryTime)
      dos.writeShort(deviceToken.length)
      dos.write(deviceToken)
      dos.writeShort(payload.length)
      dos.write(payload)
      boas.toByteArray
    } catch {
      case e: IOException => throw new AssertionError()
    }
  }

  def parseFeedbackStreamRaw(in: InputStream) = {
    var result: Map[Array[Byte], Integer] = new HashMap[Array[Byte], Integer]
    val data: DataInputStream = new DataInputStream(in)

    while (true) {
      try {
        val time: Int = data.readInt()
        val dtLength: Int = data.readUnsignedShort()
        val deviceToken: Array[Byte] = new Array[Byte](dtLength)
        data.readFully(deviceToken)

        result += deviceToken -> time
      } catch {
        // case eofe: EOFException => return // не компилится
        case ioe: IOException => throw new RuntimeException(ioe)
      }
    }
    result
  }

  def parseFeedbackStream(in: InputStream) = {
    var result: Map[String, Date] = new HashMap[String, Date]
    val raw: Map[Array[Byte], Integer] = parseFeedbackStreamRaw(in)
    raw foreach {
      case (dtArray, time) =>
        val date: Date = new Date(time * 1000L)
        val dtString: String = encodeHex(dtArray)
        result += dtString -> date
    }
    result
  }

  def close(closeable: Closeable) {
    logger.debug("close {}", closeable)

    try {
      if (closeable != null) {
        closeable.close()
      }
    } catch {
      case e: IOException => logger.debug("error while closing resource", e)
    }
  }

  def close(closeable: Socket) = {
    logger.debug("close {}", closeable)
    try {
      if (closeable != null) {
        closeable.close()
      }
    } catch {
      case e: IOException => logger.debug("error while closing socket", e)
    }
  }

  def sleep(delay: Int) {
    try {
      Thread.sleep(delay)
    } catch {
      case e: InterruptedException => Thread.currentThread().interrupt()
    }
  }

  def copyOf(bytes: Array[Byte]) = {
    val copy: Array[Byte] = new Array[Byte](bytes.length)
    System.arraycopy(bytes, 0, copy, 0, bytes.length)
    copy
  }

  def copyOfRange(original: Array[Byte], from: Int, to: Int) = {
    val newLength = to - from
    if (newLength < 0) {
      throw new IllegalArgumentException(from + " > " + to)
    }
    val copy = new Array[Byte](newLength)
    System.arraycopy(original, from, copy, 0,
      Math.min(original.length - from, newLength))
    copy
  }

  @throws(classOf[NetworkIOException])
  def wrapAndThrowAsRuntimeException(e: Exception) = {
    e match {
      case e: IOException => throw new NetworkIOException(e.asInstanceOf[IOException])
      case e: NetworkIOException => throw e
      case e: RuntimeException => throw e.asInstanceOf[RuntimeException]
      case _ => throw new RuntimeException(e)
    }
  }

  @SuppressWarnings(value = Array("PointlessArithmeticExpression", "PointlessBitwiseExpression"))
  def parseBytes(b1: Int, b2: Int, b3: Int, b4: Int): Int = {
    (b1 << 3 * 8) & 0xFF000000 | ((b2 << 2 * 8) & 0x00FF0000) | ((b3 << 1 * 8) & 0x0000FF00) | ((b4 << 0 * 8) & 0x000000FF)
  }

  // @see http://stackoverflow.com/questions/119328/how-do-i-truncate-a-java-string-to-fit-in-a-given-number-of-bytes-once-utf-8-enc
  def truncateWhenUTF8(s: String, maxBytes: Int): String = {
    var b = 0
    var i = 0
    for (c <- s.toCharArray) {
      // ranges from http://en.wikipedia.org/wiki/UTF-8
      var skip = 0
      var more = 0
      if (c <= 0x007f) {
        more = 1
      }
      else if (c <= 0x07FF) {
        more = 2
      } else if (c <= 0xd7ff) {
        more = 3
      } else if (c <= 0xDFFF) {
        // surrogate area, consume next char as well
        more = 4
        skip = 1
      } else {
        more = 3
      }

      if (b + more > maxBytes) {
        return s.substring(0, i)
      }
      b += more
      i += skip
    }
    s
  }

}