package ru.ryasale.apns

import java.io.InputStream

import org.junit.Test
import org.slf4j.LoggerFactory
import ru.ryasale.apns.utils.FixedCertificates._

class ApnsImplTestScala {

  val logger = LoggerFactory.getLogger(getClass)

  @Test
  def pushSimpleAlert() {
    logger.info("start")
    val stream = getClass.getResourceAsStream(OWN_CERTIFICATE)
    logger.info("stream {}", stream)
    val service = APNS.newService().withSert(stream, OWN_PASSWORD).withSandboxDestination.build()
    logger.info("service {}", service)

    val payload = APNS.newPayload().alertBody("Can't be simpler than this!").build()
    val token = "fedfbcfb...."
    service.push(token, payload)
  }

  @Test
  def checkFeedbackService(): Unit = {
    val stream = getClass.getResourceAsStream(OWN_CERTIFICATE)
    val service = APNS.newService().withSert(stream, OWN_PASSWORD).withSandboxDestination.build()
    val inactiveDevices = service.getInactiveDevices
    for (device <- inactiveDevices) {
     logger.debug("inactive device {}", device)
    }
  }
}