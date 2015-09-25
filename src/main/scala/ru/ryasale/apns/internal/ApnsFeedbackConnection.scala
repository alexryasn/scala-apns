package ru.ryasale.apns.internal

import java.io.IOException
import java.io.InputStream
import java.net.{InetSocketAddress, Socket, Proxy}
import java.util.Date
import javax.net.SocketFactory
import javax.net.ssl.SSLSocketFactory
import org.slf4j.LoggerFactory
import ru.ryasale.apns.exceptions.NetworkIOException

/**
 * !Ready!
 * Created by ryasale on 22.09.15.
 */
class ApnsFeedbackConnection(factory: SocketFactory, host: String, port: Int,
                             proxy: Proxy, readTimeout: Int, connectTimeout: Int,
                             proxyUsername: String, proxyPassword: String) {
  val logger = LoggerFactory.getLogger(getClass)

  def this(factory: SocketFactory, host: String, port: Int) = this(factory, host, port, null, 0, 0, null, null)

  val DELAY_IN_MS = 1000
  val RETRIES = 3

  @throws(classOf[NetworkIOException])
  def getInactiveDevices: Map[String, Date] = {
    var attempts: Int = 0
    val result: Map[String, Date] = null
    while (true) {
      try {
        attempts += 1
        val result: Map[String, Date] = getInactiveDevicesImpl
        attempts = 0
      } catch {
        case e: Exception =>
          logger.warn("Failed to retrieve invalid devices", e)
          if (attempts >= RETRIES) {
            logger.error("Couldn't get feedback connection", e)
            Utilities.wrapAndThrowAsRuntimeException(e)
          }
          Utilities.sleep(DELAY_IN_MS)
      }
    }
    result
  }

  @throws(classOf[IOException])
  def getInactiveDevicesImpl: Map[String, Date] = {
    var proxySocket: Socket = null
    var socket: Socket = null
    try {
      if (proxy == null) {
        socket = factory.createSocket(host, port)
      } else if (proxy.`type`() == Proxy.Type.HTTP) {
      val tunnelBuilder: TlsTunnelBuilder = new TlsTunnelBuilder()
      socket = tunnelBuilder.build(factory.asInstanceOf[SSLSocketFactory], proxy, proxyUsername, proxyPassword, host, port)
    } else {
      proxySocket = new Socket(proxy)
      proxySocket.connect(new InetSocketAddress(host, port), connectTimeout)
      socket = factory.asInstanceOf[SSLSocketFactory].createSocket(proxySocket, host, port, false)
    }
    socket.setSoTimeout(readTimeout)
    socket.setKeepAlive(true)
    val stream: InputStream = socket.getInputStream
    Utilities.parseFeedbackStream(stream)
    } finally {
      Utilities.close(socket)
      Utilities.close(proxySocket)
    }
  }

}
