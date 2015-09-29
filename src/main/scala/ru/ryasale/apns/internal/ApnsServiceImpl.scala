package ru.ryasale.apns.internal

import ru.ryasale.apns.ApnsNotification
import ru.ryasale.apns.exceptions.NetworkIOException

/**
 * Created by ryasale on 22.09.15.
 *
 */
class ApnsServiceImpl(connection: ApnsConnection, feedback: ApnsFeedbackConnection) extends AbstractApnsService(feedback: ApnsFeedbackConnection) {

  @throws(classOf[NetworkIOException])
  override def push(msg: ApnsNotification) = {
    connection.sendMessage(msg)
  }

  override def start() {}

  override def stop() {
    Utilities.close(connection)
  }

  override def testConnection() {
    connection.testConnection()
  }
}
