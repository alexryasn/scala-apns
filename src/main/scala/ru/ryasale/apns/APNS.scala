package ru.ryasale.apns

/**
 * Created by ryasale on 16.09.15.
 *
 * The main class to interact with the APNS Service.
 *
 * Provides an interface to create the {@link ApnsServiceBuilder} and
 * {@code ApnsNotification} payload.
 *
 */
object APNS {

  /**
   * Returns a new Payload builder
   */
  def newPayload() = {
    new PayloadBuilder()
  }

  /**
   * Returns a new APNS Service for sending iPhone notifications
   */
  def newService() = {
    new ApnsServiceBuilder()
  }
}