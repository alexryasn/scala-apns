package ru.ryasale.apns.exceptions

import java.io.IOException

import org.slf4j.LoggerFactory

/**
 * !Ready!
 * Created by ryasale on 16.09.15.
 *
 * Signals that the the provided SSL context settings (e.g.
 * keystore path, password, encryption type, etc) are invalid
 *
 * This Exception can be caused by any of the following:
 *
 * <ol>
 * <li>{@link KeyStoreException}</li>
 * <li>{@link NoSuchAlgorithmException}</li>
 * <li>{@link CertificateException}</li>
 * <li>{@link IOException}</li>
 * <li>{@link UnrecoverableKeyException}</li>
 * <li>{@link KeyManagementException}</li>
 * </ol>
 *
 */
class InvalidSSLConfig(message: String, cause: Throwable) extends ApnsException(message, cause) {
  override val SerialVersionUID: Long = -7283168775864517167L

  val logger = LoggerFactory.getLogger(getClass)

  def this() = this(null, null)

  def this(message: String) = this(message, null)

  def this(cause: Throwable) = {
    this(null, cause)
    logger.error("{}", cause)
  }
}
