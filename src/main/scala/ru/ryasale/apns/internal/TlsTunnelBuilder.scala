package ru.ryasale.apns.internal

import java.io.IOException
import java.net.{ProtocolException, InetSocketAddress, Socket, Proxy}
import javax.net.ssl.SSLSocketFactory
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.{UsernamePasswordCredentials, NTCredentials, ConnectMethod, ProxyClient}

import org.slf4j.LoggerFactory

/**
 * !Ready!
 * Created by ryasale on 23.09.15.
 *
 * Establishes a TLS connection using an HTTP proxy. See <a
 * href="http://www.ietf.org/rfc/rfc2817.txt">RFC 2817 5.2</a>. This class does
 * not support proxies requiring a "Proxy-Authorization" header.
 */
class TlsTunnelBuilder {

  val logger = LoggerFactory.getLogger(getClass)

  @throws(classOf[IOException])
  def build(factory: SSLSocketFactory, proxy: Proxy, proxyUsername: String, proxyPassword: String, host: String, port: Int): Socket = {
    var success: Boolean = false
    var proxySocket: Socket = null
    try {
      logger.debug("Attempting to use proxy : " + proxy)
      val proxyAddress: InetSocketAddress = proxy.address().asInstanceOf[InetSocketAddress]
      proxySocket = makeTunnel(host, port, proxyUsername, proxyPassword, proxyAddress)

      // Handshake with the origin server.
      if (proxySocket == null) {
        throw new ProtocolException("Unable to create tunnel through proxy server.")
      }
      val socket: Socket = factory.createSocket(proxySocket, host, port, true /* auto close */)
      success = true
      socket
    } finally {
      if (!success) {
        Utilities.close(proxySocket)
      }
    }
  }

  @SuppressFBWarnings(value = Array("VA_FORMAT_STRING_USES_NEWLINE"), justification = "use <CR><LF> as according to RFC, not platform-linefeed")
  @throws(classOf[IOException])
  def makeTunnel(host: String, port: Int, proxyUsername: String, proxyPassword: String, proxyAddress: InetSocketAddress): Socket = {
    if (host == null || port < 0 || host.isEmpty || proxyAddress == null) {
      throw new ProtocolException("Incorrect parameters to build tunnel.")
    }
    logger.debug("Creating socket for Proxy : " + proxyAddress.getAddress + ":" + proxyAddress.getPort)
    var socket: Socket = null
    try {
      val client = new ProxyClient()
      client.getParams.setParameter("http.useragent", "java-apns")
      client.getHostConfiguration.setHost(host, port)
      val proxyHost = proxyAddress.getAddress.toString.substring(0, proxyAddress.getAddress.toString.indexOf("/"))
      client.getHostConfiguration.setProxy(proxyHost, proxyAddress.getPort)

      val response = client.connect()
      socket = response.getSocket
      if (socket == null) {
        val method = response.getConnectMethod
        // Read the proxy's HTTP response.
        if (method.getStatusLine.getStatusCode == 407) {
          // Proxy server returned 407. We will now try to connect with auth Header
          if (proxyUsername != null && proxyPassword != null) {
            socket = AuthenticateProxy(method, client, proxyHost, proxyAddress.getPort,
              proxyUsername, proxyPassword)
          } else {
            throw new ProtocolException("Socket not created: " + method.getStatusLine)
          }
        }
      }

    } catch {
      case e: Exception =>
        throw new ProtocolException("Error occurred while creating proxy socket : " + e.toString)
    }
    if (socket != null) {
      logger.debug("Socket for proxy created successfully : " + socket.getRemoteSocketAddress.toString)
    }
    socket
  }

  @throws(classOf[IOException])
  private def AuthenticateProxy(method: ConnectMethod, client: ProxyClient,
                                proxyHost: String, proxyPort: Int,
                                proxyUsername: String, proxyPassword: String): Socket = {
    if (method.getProxyAuthState.getAuthScheme.getSchemeName.equalsIgnoreCase("ntlm")) {
      // If Auth scheme is NTLM, set NT credentials with blank host and domain name
      client.getState.setProxyCredentials(new AuthScope(proxyHost, proxyPort),
        new NTCredentials(proxyUsername, proxyPassword, "", ""))
    } else {
      // If Auth scheme is Basic/Digest, set regular Credentials
      client.getState.setProxyCredentials(new AuthScope(proxyHost, proxyPort),
        new UsernamePasswordCredentials(proxyUsername, proxyPassword))
    }

    val response = client.connect()
    val socket = response.getSocket

    if (socket == null) {
      val method = response.getConnectMethod
      throw new ProtocolException("Proxy Authentication failed. Socket not created: "
        + method.getStatusLine)
    }
    socket
  }

}
