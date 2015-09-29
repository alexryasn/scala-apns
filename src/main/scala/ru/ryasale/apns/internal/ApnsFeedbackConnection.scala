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
 * Created by ryasale on 22.09.15.
 *
 * Connection that used for getting feedback from APNS service.
 */
class ApnsFeedbackConnection(factory: SocketFactory, host: String, port: Int,
                             proxy: Proxy, readTimeout: Int, connectTimeout: Int,
                             proxyUsername: String, proxyPassword: String) {
  val logger = LoggerFactory.getLogger(getClass)

  def this(factory: SocketFactory, host: String, port: Int) = this(factory, host, port, null, 0, 0, null, null)

  val DELAY_IN_MS = 1000
  val RETRIES = 3

  @throws(classOf[NetworkIOException])
  def getInactiveDevices = {
    var attempts = 0
    var result: Map[String, Date] = Map()
    var continue = true
    while (continue) {
      attempts += 1
      if (getInactiveDevicesImpl.nonEmpty) {
        result = getInactiveDevicesImpl
        continue = false
      } else {
        if (attempts >= RETRIES) {
          logger.warn("Couldn't get feedback connection")
          continue = false
        }
        Utilities.sleep(DELAY_IN_MS)
      }
    }
    result
  }

  @throws(classOf[IOException])
  def getInactiveDevicesImpl = {
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
