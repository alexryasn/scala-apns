package ru.ryasale.apns.internal

import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{BlockingQueue, Executors, LinkedBlockingQueue, ThreadFactory}
import org.slf4j.{Logger, LoggerFactory}
import ru.ryasale.apns.{ApnsNotification, ApnsService}
import ru.ryasale.apns.exceptions.NetworkIOException

/** !Ready!
 * Created by ryasale on 23.09.15.
 */
class QueuedApnsService(service: ApnsService, tf: ThreadFactory) extends AbstractApnsService(null) {

  private val logger = LoggerFactory.getLogger(classOf[QueuedApnsService])

  private var queue: BlockingQueue[ApnsNotification] = new LinkedBlockingQueue[ApnsNotification]
  private var thread: Thread = _
  private val threadFactory = if (tf == null) Executors.defaultThreadFactory() else tf

  private val started: AtomicBoolean = new AtomicBoolean(false)
  @volatile private var shouldContinue: Boolean = _

  def this(service: ApnsService) = {
    this(service, null)
    queue = null
  }

  @throws(classOf[IllegalStateException])
  override def push(msg: ApnsNotification) {
    if (!started.get()) throw new IllegalStateException("service hasn't be started or was closed")
    queue.add(msg)
  }

  override def start(): Unit = {
    if (started.getAndSet(true)) {
      // I prefer if we throw a runtime IllegalStateException here,
      // but I want to maintain semantic backward compatibility.
      // So it is returning immediately here
      return
    }

    service.start()
    shouldContinue = true
    thread = threadFactory.newThread(new Runnable() {
      override def run() {
        while (shouldContinue) {
          try {
            val msg = queue.take()
            service.push(msg)
          } catch {
            case ie: InterruptedException => // ignore
            case ne: NetworkIOException => // ignore: failed connect...
            case ee: Exception => {
              // weird if we reached here - something wrong is happening, but we shouldn't stop the service anyway!
              logger.warn("Unexpected message caught... Shouldn't be here", ee)
            }
          }
        }
      }
    })
    thread.start()
  }

  override def stop() = {
    started.set(false)
    shouldContinue = false
    thread.interrupt()
    service.stop()
  }

  @throws(classOf[NetworkIOException])
  override def getInactiveDevices: Map[String, Date] = {
    service.getInactiveDevices
  }

  @throws(classOf[NetworkIOException])
  override def testConnection() = {
    service.testConnection()
  }

}

object QueuedApnsService {
  private val logger: Logger = LoggerFactory.getLogger(classOf[QueuedApnsService])
}