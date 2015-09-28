package ru.ryasale.apns

import java.io.{InputStream, FileNotFoundException, FileInputStream}
import java.net.{Socket, InetSocketAddress, Proxy}
import java.security.KeyStore
import java.util.concurrent.{Executors, ExecutorService, ThreadFactory}
import javax.net.ssl.{SSLSocketFactory, SSLContext}

import org.slf4j.LoggerFactory
import ru.ryasale.apns.exceptions.{InvalidSSLConfig, RuntimeIOException}
import ru.ryasale.apns.internal._
import ru.ryasale.apns.internal.Utilities._

/**
 * !Ready!
 * Created by ryasale on 16.09.15.
 * The class is used to create instances of {@link ApnsService}.
 *
 * Note that this class is not synchronized.  If multiple threads access a
 * {@code ApnsServiceBuilder} instance concurrently, and at least on of the
 * threads modifies one of the attributes structurally, it must be
 * synchronized externally.
 *
 * Starting a new {@code ApnsService} is easy:
 *
 * <pre>
 * ApnsService = APNS.newService()
 * .withCert("/path/to/certificate.p12", "MyCertPassword")
 * .withSandboxDestination()
 * .build()
 * </pre>
 */
class ApnsServiceBuilder {

  val logger = LoggerFactory.getLogger(getClass)

  val KEYSTORE_TYPE: String = "PKCS12"
  val KEY_ALGORITHM: String =
    if (java.security.Security.getProperty("ssl.KeyManagerFactory.algorithm") == null) "sunx509" else java.security.Security.getProperty("ssl.KeyManagerFactory.algorithm")

  /**
   * Constructs a new instance of {@code ApnsServiceBuilder}
   */
  var sslContext: SSLContext = _

  var readTimeout: Int = 0
  var connectTimeout: Int = 0

  // TODO see for _ this link http://stackoverflow.com/questions/18453406/error-class-animal-needs-to-be-abstract-since-it-has-5-unimplemented-members
  var gatewayHost: String = _
  var gatewayPort: Int = -1

  var feedbackHost: String = _
  var feedbackPort: Int = _
  private var pooledMax: Int = 1
  private var cacheLength: Int = ApnsConnection.DEFAULT_CACHE_LENGTH
  var autoAdjustCacheLength: Boolean = true
  private var executor: ExecutorService = null

  private var reconnectPolicy: ReconnectPolicy = ReconnectPolicy.Provided.EVERY_HALF_HOUR
  private var isQueued = false
  private var queueThreadFactory: ThreadFactory = null

  private var isBatched: Boolean = _
  private var batchWaitTimeInSec: Int = _
  private var batchMaxWaitTimeInSec: Int = _
  private var batchThreadFactory: ThreadFactory = _

  private var delegate: ApnsDelegate = ApnsDelegate.EMPTY
  var proxy: Proxy = _
  var proxyUsername: String = _
  var proxyPassword: String = _
  private var errorDetection: Boolean = true
  private var errorDetectionThreadFactory: ThreadFactory = null

  /**
   * Specify the certificate used to connect to Apple APNS
   * servers.  This relies on the path (absolute or relative to
   * working path) to the keystore (*.p12) containing the
   * certificate, along with the given password.
   *
   * The keystore needs to be of PKCS12 and the keystore
   * needs to be encrypted using the SunX509 algorithm.  Both
   * of these settings are the default.
   *
   * This library does not support password-less p12 certificates, due to a
   * Oracle Java library <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6415637">
   * Bug 6415637</a>.  There are three workarounds: use a password-protected
   * certificate, use a different boot Java SDK implementation, or construct
   * the `SSLContext` yourself!  Needless to say, the password-protected
   * certificate is most recommended option.
   *
   * @param fileName  the path to the certificate
   * @param password  the password of the keystore
   * @return  this
   * @throws RuntimeIOException if it { @code fileName} cannot be
   *                            found or read
   * @throws InvalidSSLConfig if fileName is invalid Keystore
   *                          or the password is invalid
   */
  @throws(classOf[RuntimeException])
  @throws(classOf[InvalidSSLConfig])
  def withSert(fileName: String, password: String): ApnsServiceBuilder = {
    var stream: FileInputStream = null
    try {
      stream = new FileInputStream(fileName)
      withSert(stream, password)
    } catch {
      case nfe: FileNotFoundException => throw new RuntimeIOException(nfe)
    } finally {
      close(stream)
    }
  }

  /**
   * Specify the certificate used to connect to Apple APNS
   * servers.  This relies on the stream of keystore (*.p12)
   * containing the certificate, along with the given password.
   *
   * The keystore needs to be of PKCS12 and the keystore
   * needs to be encrypted using the SunX509 algorithm.  Both
   * of these settings are the default.
   *
   * This library does not support password-less p12 certificates, due to a
   * Oracle Java library <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6415637">
   * Bug 6415637</a>.  There are three workarounds: use a password-protected
   * certificate, use a different boot Java SDK implementation, or constract
   * the `SSLContext` yourself!  Needless to say, the password-protected
   * certificate is most recommended option.
   *
   * @param stream    the keystore represented as input stream
   * @param password  the password of the keystore
   * @return  this
   * @throws InvalidSSLConfig if stream is invalid Keystore
   *                          or the password is invalid
   */
  @throws(classOf[InvalidSSLConfig])
  @throws(classOf[IllegalArgumentException])
  def withSert(stream: InputStream, password: String) = {
    assertPasswordNotEmpty(password)
    withSSLContext(
      newSSLContext(stream, password,
        KEYSTORE_TYPE, KEY_ALGORITHM))
  }

  /**
   * Specify the certificate used to connect to Apple APNS
   * servers.  This relies on a keystore (*.p12)
   * containing the certificate, along with the given password.
   *
   * This library does not support password-less p12 certificates, due to a
   * Oracle Java library <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6415637">
   * Bug 6415637</a>.  There are three workarounds: use a password-protected
   * certificate, use a different boot Java SDK implementation, or construct
   * the `SSLContext` yourself!  Needless to say, the password-protected
   * certificate is most recommended option.
   *
   * @param keyStore  the keystore
   * @param password  the password of the keystore
   * @return  this
   * @throws InvalidSSLConfig if stream is invalid Keystore
   *                          or the password is invalid
   */
  @throws(classOf[InvalidSSLConfig])
  def withCert(keyStore: KeyStore, password: String) = {
    assertPasswordNotEmpty(password)
    withSSLContext(
      newSSLContext(keyStore, password, KEY_ALGORITHM))
  }

  def assertPasswordNotEmpty(password: String) {
    assert(password != null && password.length != 0,
      new IllegalArgumentException("Passwords must be specified. Oracle Java SDK does not support passwordless p12 certificates"))
  }

  /**
   * Specify the SSLContext that should be used to initiate the
   * connection to Apple Server.
   *
   * Most clients would use {@link #withCert(InputStream, String)}
   * or {@link #withCert(String, String)} instead.  But some
   * clients may need to represent the Keystore in a different
   * format than supported.
   *
   * @param sslContext    Context to be used to create secure connections
   * @return  this
   */
  def withSSLContext(sslContext: SSLContext) = {
    this.sslContext = sslContext
    this
  }

  /**
   * Specify the timeout value to be set in new setSoTimeout in created
   * sockets, for both feedback and push connections, in milliseconds.
   * @param readTimeout timeout value to be set in new setSoTimeout
   * @return this
   */
  def withReadTimeout(readTimeout: Int) = {
    this.readTimeout = readTimeout
    this
  }

  /**
   * Specify the timeout value to use for connectionTimeout in created
   * sockets, for both feedback and push connections, in milliseconds.
   * @param connectTimeout timeout value to use for connectionTimeout
   * @return this
   */
  def withConnectTimeout(connectTimeout: Int) = {
    this.connectTimeout = connectTimeout
    this
  }

  /**
   * Specify the gateway server for sending Apple iPhone
   * notifications.
   *
   * Most clients should use {@link #withSandboxDestination()}
   * or {@link #withProductionDestination()}.  Clients may use
   * this method to connect to mocking tests and such.
   *
   * @param host  hostname the notification gateway of Apple
   * @param port  port of the notification gateway of Apple
   * @return  this
   */
  def withGatewayDestination(host: String, port: Int) = {
    this.gatewayHost = host
    this.gatewayPort = port
    this
  }

  /**
   * Specify the Feedback for getting failed devices from
   * Apple iPhone Push servers.
   *
   * Most clients should use {@link #withSandboxDestination()}
   * or {@link #withProductionDestination()}.  Clients may use
   * this method to connect to mocking tests and such.
   *
   * @param host  hostname of the feedback server of Apple
   * @param port  port of the feedback server of Apple
   * @return this
   */
  def withFeedbackDestination(host: String, port: Int) = {
    this.feedbackHost = host
    this.feedbackPort = port
    this
  }

  /**
   * Specify to use Apple servers as iPhone gateway and feedback servers.
   *
   * If the passed {@code isProduction} is true, then it connects to the
   * production servers, otherwise, it connects to the sandbox servers
   *
   * @param isProduction  determines which Apple servers should be used:
   *                      production or sandbox
   * @return this
   */
  def withAppleDestination(isProduction: Boolean) = {
    if (isProduction) {
      withProductionDestination
    } else {
      withSandboxDestination
    }
  }

  /**
   * Specify to use the Apple sandbox servers as iPhone gateway
   * and feedback servers.
   *
   * This is desired when in testing and pushing notifications
   * with a development provision.
   *
   * @return  this
   */
  def withSandboxDestination = {
    withGatewayDestination(SANDBOX_GATEWAY_HOST, SANDBOX_GATEWAY_PORT)
      .withFeedbackDestination(SANDBOX_FEEDBACK_HOST, SANDBOX_FEEDBACK_PORT)
  }

  /**
   * Specify to use the Apple Production servers as iPhone gateway
   * and feedback servers.
   *
   * This is desired when sending notifications to devices with
   * a production provision (whether through App Store or Ad hoc
   * distribution).
   *
   * @return  this
   */
  def withProductionDestination = {
    withGatewayDestination(PRODUCTION_GATEWAY_HOST, PRODUCTION_GATEWAY_PORT)
      .withFeedbackDestination(PRODUCTION_FEEDBACK_HOST, PRODUCTION_FEEDBACK_PORT)
  }

  /**
   * Specify the reconnection policy for the socket connection.
   *
   * Note: This option has no effect when using non-blocking
   * connections.
   */
  def withReconnectPolicy(rp: ReconnectPolicy) = {
    this.reconnectPolicy = rp
    this
  }

  /**
   * Specify if the notification cache should auto adjust.
   * Default is true
   *
   * @param autoAdjustCacheLength the notification cache should auto adjust.
   * @return this
   */
  def withAutoAdjustCacheLength(autoAdjustCacheLength: Boolean) = {
    this.autoAdjustCacheLength = autoAdjustCacheLength
    this
  }

  /**
   * Specify the reconnection policy for the socket connection.
   *
   * Note: This option has no effect when using non-blocking
   * connections.
   */
  def withReconnectPolicy(rp: ReconnectPolicy.Provided) = {
    this.reconnectPolicy = rp.newObject
    this
  }

  /**
   * Specify the address of the SOCKS proxy the connection should
   * use.
   *
   * <p>Read the <a href="http://java.sun.com/javase/6/docs/technotes/guides/net/proxies.html">
   * Java Networking and Proxies</a> guide to understand the
   * proxies complexity.
   *
   * <p>Be aware that this method only handles SOCKS proxies, not
   * HTTPS proxies.  Use {@link #withProxy(Proxy)} instead.
   *
   * @param host  the hostname of the SOCKS proxy
   * @param port  the port of the SOCKS proxy server
   * @return  this
   */
  def withSocksProxy(host: String, port: Int) {
    val proxy = new Proxy(Proxy.Type.SOCKS,
      new InetSocketAddress(host, port))
    withProxy(proxy)
  }

  /**
   * Specify the proxy and the authentication parameters to be used
   * to establish the connections to Apple Servers.
   *
   * <p>Read the <a href="http://java.sun.com/javase/6/docs/technotes/guides/net/proxies.html">
   * Java Networking and Proxies</a> guide to understand the
   * proxies complexity.
   *
   * @param proxy the proxy object to be used to create connections
   * @param proxyUsername a String object representing the username of the proxy server
   * @param proxyPassword a String object representing the password of the proxy server
   * @return  this
   */
  def withAuthProxy(proxy: Proxy, proxyUsername: String, proxyPassword: String) = {
    this.proxy = proxy
    this.proxyUsername = proxyUsername
    this.proxyPassword = proxyPassword
    this
  }

  /**
   * Specify the proxy to be used to establish the connections
   * to Apple Servers
   *
   * <p>Read the <a href="http://java.sun.com/javase/6/docs/technotes/guides/net/proxies.html">
   * Java Networking and Proxies</a> guide to understand the
   * proxies complexity.
   *
   * @param proxy the proxy object to be used to create connections
   * @return  this
   */
  def withProxy(proxy: Proxy) = {
    this.proxy = proxy
    this
  }

  /**
   * Specify the number of notifications to cache for error purposes.
   * Default is 100
   *
   * @param cacheLength  Number of notifications to cache for error purposes
   * @return  this
   */
  def withCacheLength(cacheLength: Int) = {
    this.cacheLength = cacheLength
    this
  }

  /**
   * Specify the socket to be used as underlying socket to connect
   * to the APN service.
   *
   * This assumes that the socket connects to a SOCKS proxy.
   *
   * @deprecated use { @link ApnsServiceBuilder#withProxy(Proxy)} instead
   * @param proxySocket   the underlying socket for connections
   * @return  this
   */
  @Deprecated
  def withProxySocket(proxySocket: Socket) = {
    this.withProxy(new Proxy(Proxy.Type.SOCKS,
      proxySocket.getRemoteSocketAddress))
  }

  /**
   * Constructs a pool of connections to the notification servers.
   *
   * Apple servers recommend using a pooled connection up to
   * 15 concurrent persistent connections to the gateways.
   *
   * Note: This option has no effect when using non-blocking
   * connections.
   */
  def asPool(maxConnections: Int): ApnsServiceBuilder = {
    asPool(Executors.newFixedThreadPool(maxConnections), maxConnections)
  }

  /**
   * Constructs a pool of connections to the notification servers.
   *
   * Apple servers recommend using a pooled connection up to
   * 15 concurrent persistent connections to the gateways.
   *
   * Note: This option has no effect when using non-blocking
   * connections.
   *
   * Note: The maxConnections here is used as a hint to how many connections
   * get created.
   */
  def asPool(executor: ExecutorService, maxConnections: Int) = {
    this.pooledMax = maxConnections
    this.executor = executor
    this
  }

  /**
   * Constructs a new thread with a processing queue to process
   * notification requests.
   *
   * @return  this
   */
  def asQueued(): ApnsServiceBuilder = {
    asQueued(Executors.defaultThreadFactory())
  }

  /**
   * Constructs a new thread with a processing queue to process
   * notification requests.
   *
   * @param threadFactory
     * thread factory to use for queue processing
   * @return  this
   */
  def asQueued(threadFactory: ThreadFactory) = {
    this.isQueued = true
    this.queueThreadFactory = threadFactory
    this
  }

  /**
   * Construct service which will process notification requests in batch.
   * After each request batch will wait <code>waitTimeInSec (set as 5sec)</code> for more request to come
   * before executing but not more than <code>maxWaitTimeInSec (set as 10sec)</code>
   *
   * Note: It is not recommended to use pooled connection
   */
  def asBatched(): ApnsServiceBuilder = {
    asBatched(5, 10)
  }

  /**
   * Construct service which will process notification requests in batch.
   * After each request batch will wait <code>waitTimeInSec</code> for more request to come
   * before executing but not more than <code>maxWaitTimeInSec</code>
   *
   * Note: It is not recommended to use pooled connection
   *
   * @param waitTimeInSec
     * time to wait for more notification request before executing
   *   batch
   * @param maxWaitTimeInSec
     * maximum wait time for batch before executing
   */
  def asBatched(waitTimeInSec: Int, maxWaitTimeInSec: Int): ApnsServiceBuilder = {
    asBatched(waitTimeInSec, maxWaitTimeInSec, null)
  }

  /**
   * Construct service which will process notification requests in batch.
   * After each request batch will wait <code>waitTimeInSec</code> for more request to come
   * before executing but not more than <code>maxWaitTimeInSec</code>
   *
   * Each batch creates new connection and close it after finished.
   * In case reconnect policy is specified it will be applied by batch processing.
   * E.g.: {@link ReconnectPolicy.Provided#EVERY_HALF_HOUR} will reconnect the connection in case batch is running for more than half an hour
   *
   * Note: It is not recommended to use pooled connection
   *
   * @param waitTimeInSec
     * time to wait for more notification request before executing
   *   batch
   * @param maxWaitTimeInSec
     * maximum wait time for batch before executing
   * @param threadFactory
     * thread factory to use for batch processing
   */
  def asBatched(waitTimeInSec: Int, maxWaitTimeInSec: Int, threadFactory: ThreadFactory) = {
    this.isBatched = true
    this.batchWaitTimeInSec = waitTimeInSec
    this.batchMaxWaitTimeInSec = maxWaitTimeInSec
    this.batchThreadFactory = threadFactory
    this
  }

  /**
   * Sets the delegate of the service, that gets notified of the
   * status of message delivery.
   *
   * Note: This option has no effect when using non-blocking
   * connections.
   */
  def withDelegate(delegate: ApnsDelegate) = {
    this.delegate = if (delegate == null) ApnsDelegate.EMPTY else delegate
    this
  }

  /**
   * Disables the enhanced error detection, enabled by the
   * enhanced push notification interface.  Error detection is
   * enabled by default.
   *
   * This setting is desired when the application shouldn't spawn
   * new threads.
   *
   * @return  this
   */
  def withNoErrorDetection() = {
    this.errorDetection = false
    this
  }

  /**
   * Provide a custom source for threads used for monitoring connections.
   *
   * This setting is desired when the application must obtain threads from a
   * controlled environment Google App Engine.
   * @param threadFactory
     * thread factory to use for error detection
   * @return  this
   */
  def withErrorDetectionThreadFactory(threadFactory: ThreadFactory) = {
    this.errorDetectionThreadFactory = threadFactory
    this
  }

  /**
   * Returns a fully initialized instance of {@link ApnsService},
   * according to the requested settings.
   *
   * @return  a new instance of ApnsService
   */
  def build(): ApnsService = {

    logger.info("build starting")

    checkInitialization()
    var service: ApnsService = null

    val sslFactory: SSLSocketFactory = sslContext.getSocketFactory
    val feedback: ApnsFeedbackConnection = new ApnsFeedbackConnection(sslFactory, feedbackHost, feedbackPort, proxy, readTimeout, connectTimeout, proxyUsername, proxyPassword)

    var conn: ApnsConnection = new ApnsConnectionImpl(sslFactory, gatewayHost,
      gatewayPort, proxy, proxyUsername, proxyPassword, reconnectPolicy,
      delegate, errorDetection, errorDetectionThreadFactory, cacheLength,
      autoAdjustCacheLength, readTimeout, connectTimeout)
    if (pooledMax != 1) {
      conn = new ApnsPooledConnection(conn, pooledMax, executor)
    }

    service = new ApnsServiceImpl(conn, feedback)

    if (isQueued) {
      service = new QueuedApnsService(service, queueThreadFactory)
    }

    if (isBatched) {
      service = new BatchApnsService(conn, feedback, batchWaitTimeInSec, batchMaxWaitTimeInSec, batchThreadFactory)
    }
    service.start()
    service
  }

  def checkInitialization() = {
    if (sslContext == null)
      throw new IllegalStateException(
        "SSL Certificates and attribute are not initialized\n"
          + "Use .withCert() methods.")
    if (gatewayHost == null || gatewayPort == -1)
      throw new IllegalStateException(
        "The Destination APNS server is not stated\n"
          + "Use .withDestination(), withSandboxDestination(), "
          + "or withProductionDestination().")
  }


}
