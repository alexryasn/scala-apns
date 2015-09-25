package ru.ryasale.apns.internal

import java.io.{EOFException, InputStream, IOException}
import java.net.{InetSocketAddress, Socket, Proxy}
import java.util
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ConcurrentLinkedQueue, ThreadFactory}
import javax.net.SocketFactory
import javax.net.ssl.SSLSocketFactory

import org.slf4j.LoggerFactory
import ru.ryasale.apns.exceptions.{ApnsDeliveryErrorException, NetworkIOException}
import ru.ryasale.apns._

/**
 * !Ready!
 * Created by ryasale on 22.09.15.
 */
class ApnsConnectionImpl(factory: SocketFactory, host: String, port: Int, proxy: Proxy, proxyUsername: String, proxyPassword: String,
                         reconnectPolicy: ReconnectPolicy, delegate: ApnsDelegate, errorDetection: Boolean, tf: ThreadFactory, var cacheLength: Int,
                         autoAdjustCacheLength: Boolean, readTimeout: Int, connectTimeout: Int) extends ApnsConnection {

  val logger = LoggerFactory.getLogger(getClass)

  private val threadFactory: ThreadFactory = null

  private val cachedNotifications, notificationsBuffer: ConcurrentLinkedQueue[ApnsNotification] = new ConcurrentLinkedQueue[ApnsNotification]
  private var socket: Socket = _
  private val threadId: AtomicInteger = new AtomicInteger(0)

  if (delegate == null) ApnsDelegate.EMPTY else delegate
  if (tf == null) defaultThreadFactory() else tf

  def this(factory: SocketFactory, host: String, port: Int, proxy: Proxy, proxyUsername: String, proxyPassword: String, reconnectPolicy: ReconnectPolicy, delegate: ApnsDelegate) = this(factory, host, port, proxy, proxyUsername, proxyPassword, reconnectPolicy, delegate, false, null, ApnsConnection.DEFAULT_CACHE_LENGTH, true, 0, 0)

  def this(factory: SocketFactory, host: String, port: Int, reconnectPolicy: ReconnectPolicy, delegate: ApnsDelegate) = this(factory, host, port, null, null, null, reconnectPolicy, delegate)

  def this(factory: SocketFactory, host: String, port: Int) = this(factory, host, port, new ReconnectPolicies.Never(), ApnsDelegate.EMPTY)


  def defaultThreadFactory(): ThreadFactory = {
    new ThreadFactory() {
      val wrapped = Executors.defaultThreadFactory()

      override def newThread(r: Runnable) = {
        val result = wrapped.newThread(r)
        result.setName("MonitoringThread-" + threadId.incrementAndGet())
        result.setDaemon(true)
        result
      }
    }
  }

  override def close() {
    this.synchronized {
      Utilities.close(socket)
    }
  }

  private def monitorSocket(socket: Socket) {
    logger.debug("Launching Monitoring Thread for socket {}", socket)

    val t: Thread = threadFactory.newThread(new Runnable() {
      val EXPECTED_SIZE = 6

      @SuppressWarnings(Array[String]("InfiniteLoopStatement"))
      override def run() {
        logger.debug("Started monitoring thread")
        try {
          var in: InputStream = null
          try {
            in = socket.getInputStream
          } catch {
            case ioe: IOException => in = null
          }

          val bytes = new Array[Byte](EXPECTED_SIZE)

          while (in != null && readPacket(in, bytes)) {
            logger.debug("Error-response packet {}", Utilities.encodeHex(bytes))
            // Quickly close socket, so we won't ever try to send push notifications
            // using the defective socket.
            Utilities.close(socket)

            val command = bytes(0) & 0xFF
            if (command != 8) {
              throw new IOException("Unexpected command byte " + command)
            }
            val statusCode = bytes(1) & 0xFF
            val e = DeliveryError.ofCode(statusCode)

            val id = Utilities.parseBytes(bytes(2), bytes(3), bytes(4), bytes(5))

            logger.debug("Closed connection cause={}; id={}", e, id)
            delegate.connectionClosed(e, id)

            //var tempCache: util.Queue[ApnsNotification] = new util.LinkedList[ApnsNotification]()
            var tempCache = new util.LinkedList[ApnsNotification]()
            var notification: ApnsNotification = null
            var foundNotification = false

            while (!cachedNotifications.isEmpty) {
              notification = cachedNotifications.poll()
              logger.debug("Candidate for removal, message id {}", notification.getIdentifier)

              if (notification.getIdentifier == id) {
                logger.debug("Bad message found {}", notification.getIdentifier)
                foundNotification = true
                return
              }
              tempCache.add(notification)
            }

            if (foundNotification) {
              logger.debug("delegate.messageSendFailed, message id {}", notification.getIdentifier)
              delegate.messageSendFailed(notification, new ApnsDeliveryErrorException(e))
            } else {
              cachedNotifications.addAll(tempCache)
              val resendSize = tempCache.size()
              logger.warn("Received error for message that wasn't in the cache...")
              if (autoAdjustCacheLength) {
                cacheLength = cacheLength + (resendSize / 2)
                delegate.cacheLengthExceeded(cacheLength)
              }
              logger.debug("delegate.messageSendFailed, unknown id")
              delegate.messageSendFailed(null, new ApnsDeliveryErrorException(e))
            }

            var resendSize = 0

            while (!cachedNotifications.isEmpty) {

              resendSize += 1
              val resendNotification: ApnsNotification = cachedNotifications.poll()
              logger.debug("Queuing for resend {}", resendNotification.getIdentifier)
              notificationsBuffer.add(resendNotification)
            }
            logger.debug("resending {} notifications", resendSize)
            delegate.notificationsResent(resendSize)
          }
          logger.debug("Monitoring input stream closed by EOF")
        } catch {
          case e: IOException =>
            // An exception when reading the error code is non-critical, it will cause another retry
            // sending the message. Other than providing a more stable network connection to the APNS
            // server we can't do much about it - so let's not spam the application's error log.
            logger.info("Exception while waiting for error code", e)
            delegate.connectionClosed(DeliveryError.UNKNOWN, -1)
        } finally {
          close()
          drainBuffer()
        }
      }

      @throws(classOf[IOException])
      private def readPacket(in: InputStream, bytes: Array[Byte]): Boolean = {
        val len = bytes.length
        var n = 0
        while (n < len) {
          try {
            val count = in.read(bytes, n, len - n)
            if (count < 0) {
              throw new EOFException("EOF after reading " + n + " bytes of new packet.")
            }
            n += count
          } catch {
            case ioe: IOException =>
              if (n == 0)
                return false
              throw new IOException("Error after reading " + n + " bytes of packet", ioe)
          }
        }
        true
      }
    })
    t.start()
  }

  @throws(classOf[NetworkIOException])
  private def getOrCreateSocket(resend: Boolean): Socket = this.synchronized {
    if (reconnectPolicy.shouldReconnect) {
      logger.debug("Reconnecting due to reconnectPolicy dictating it")
      Utilities.close(socket)
      socket = null
    }
    if (socket == null || socket.isClosed) {
      try {
        if (proxy == null) {
          socket = factory.createSocket(host, port)
          logger.debug("Connected new socket {}", socket)
        } else if (proxy.`type`() == Proxy.Type.HTTP) {
          val tunnelBuilder = new TlsTunnelBuilder()
          socket = tunnelBuilder.build(factory.asInstanceOf[SSLSocketFactory], proxy, proxyUsername, proxyPassword, host, port)
          logger.debug("Connected new socket through http tunnel {}", socket)
        } else {
          var success = false
          var proxySocket: Socket = null
          try {
            proxySocket = new Socket(proxy)
            proxySocket.connect(new InetSocketAddress(host, port), connectTimeout)
            socket = factory.asInstanceOf[SSLSocketFactory].createSocket(proxySocket, host, port, false)
            success = true
          } finally {
            if (!success) {
              Utilities.close(proxySocket)
            }
          }
          logger.debug("Connected new socket through socks tunnel {}", socket)
        }

        socket.setSoTimeout(readTimeout)
        socket.setKeepAlive(true)

        if (errorDetection) {
          monitorSocket(socket)
        }

        reconnectPolicy.reconnected()
        logger.debug("Made a new connection to APNS")
      } catch {
        case e: IOException =>
          logger.error("Couldn't connect to APNS server", e)
          // indicate to clients whether this is a resend or initial send
          throw new NetworkIOException(e, resend)
      }
    }
    return socket
  }

  val DELAY_IN_MS = 1000
  val RETRIES = 3

  @throws(classOf[NetworkIOException])
  override def sendMessage(m: ApnsNotification) {
    sendMessage(m, fromBuffer = false)
    drainBuffer()
  }

  @throws(classOf[NetworkIOException])
  private def sendMessage(m: ApnsNotification, fromBuffer: Boolean): Unit = this.synchronized {
    logger.debug("sendMessage {} fromBuffer: {}", m, fromBuffer)

    delegate match {
      case delegate1: StartSendingApnsDelegate =>
        delegate1.startSending(m, fromBuffer)
      case _ =>
    }

    var attempts = 0
    while (true) {
      try {
        attempts += 1
        val socket: Socket = getOrCreateSocket(fromBuffer)
        socket.getOutputStream.write(m.getMarshall)
        socket.getOutputStream.flush()
        cacheNotification(m)

        delegate.messageSent(m, fromBuffer)

        //logger.debug("Message \"{}\" sent", m)
        attempts = 0
        return
      } catch {
        case e: IOException =>
          Utilities.close(socket)
          if (attempts >= RETRIES) {
            logger.error("Couldn't send message after " + RETRIES + " retries." + m, e)
            delegate.messageSendFailed(m, e)
            Utilities.wrapAndThrowAsRuntimeException(e)
          }
          // The first failure might be due to closed connection (which in turn might be caused by
          // a message containing a bad token), so don't delay for the first retry.
          //
          // Additionally we don't want to spam the log file in this case, only after the second retry
          // which uses the delay.

          if (attempts != 1) {
            logger.info("Failed to send message " + m + "... trying again after delay", e)
            Utilities.sleep(DELAY_IN_MS)
          }
      }
    }
  }

  private def drainBuffer() {
    this.synchronized {
      logger.debug("draining buffer")
      while (!notificationsBuffer.isEmpty) {
        val notification: ApnsNotification = notificationsBuffer.poll()
        try {
          sendMessage(notification, fromBuffer = true)
        }
        catch {
          // at this point we are retrying the submission of messages but failing to connect to APNS, therefore
          // notify the client of this
          case ex: NetworkIOException => delegate.messageSendFailed(notification, ex)
        }
      }
    }
  }


  private def cacheNotification(notification: ApnsNotification) {
    cachedNotifications.add(notification)
    while (cachedNotifications.size() > cacheLength) {
      cachedNotifications.poll()
      logger.debug("Removing notification from cache " + notification)
    }
  }

  override def copy(): ApnsConnection = new ApnsConnectionImpl(factory, host, port, proxy, proxyUsername, proxyPassword, reconnectPolicy.copy, delegate,
    errorDetection, threadFactory, cacheLength, autoAdjustCacheLength, readTimeout, connectTimeout)

  @throws(classOf[NetworkIOException])
  override def testConnection() {
    var testConnection: ApnsConnectionImpl = null
    try {
      testConnection = new ApnsConnectionImpl(factory, host, port, proxy, proxyUsername, proxyPassword, reconnectPolicy.copy, delegate)
      val notification: ApnsNotification = new EnhancedApnsNotification(0, 0, Array[Byte](0), Array[Byte](0))
      testConnection.sendMessage(notification)
    } finally {
      if (testConnection != null) {
        testConnection.close()
      }
    }
  }

  // I think should using internal cacheLength() method
  override def setCacheLength(cacheLength: Integer) {
    this.cacheLength = cacheLength
  }

  // I think should using internal cacheLength_() method
  override def getCacheLength(): Int = {
    this.cacheLength
  }

}
