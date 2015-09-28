package ru.ryasale.apns

import java.io.InputStream

import org.junit.Test
import org.slf4j.LoggerFactory

class ApnsImplTestScala {

  val logger = LoggerFactory.getLogger(getClass)

  @Test
  def tryAPNS() {
    logger.info("start")
    val stream = getClass.getResourceAsStream("clientStore.p12")
    logger.info("stream {}", stream)
    val service = APNS.newService().withSert(stream, "123456").withSandboxDestination.build()
    logger.info("service {}", service)

    val payload = APNS.newPayload().alertBody("Can't be simpler than this!").build()
    val token = "fedfbcfb...."
    service.push(token, payload)
  }
}