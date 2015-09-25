package ru.ryasale.apns.internal

import java.util
import java.util.concurrent._
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.ryasale.apns.ApnsNotification
import ru.ryasale.apns.exceptions.NetworkIOException

/**
 * !Ready!
 * Created by ryasale on 24.09.15.
 * @param batchWaitTimeInSec How many seconds to wait for more messages before batch is send.
 *                           Each message reset the wait time. @see #maxBatchWaitTimeInSec
 * @param maxBachWaitTimeInSec How many seconds can be batch delayed before execution.
 *                             This time is not exact amount after which the batch will run its roughly the time
 */
class BatchApnsService(prototype: ApnsConnection, feedback: ApnsFeedbackConnection, batchWaitTimeInSec: Int = 5, maxBachWaitTimeInSec: Int, tf: ThreadFactory) extends AbstractApnsService(feedback) {

  /**
   * How many seconds can be batch delayed before execution.
   * This time is not exact amount after which the batch will run its roughly the time
   */
  private val maxBatchWaitTimeInSec = 10

  private var firstMessageArrivedTime: Long = _

  private val batch: util.Queue[ApnsNotification] = new ConcurrentLinkedQueue[ApnsNotification]()

  private val scheduleService: ScheduledExecutorService = new ScheduledThreadPoolExecutor(1, if (tf == null) Executors.defaultThreadFactory() else tf)
  private var taskFuture: ScheduledFuture[_] = _

  private val batchRunner: Runnable = new SendMessagesBatch()

  override def start() {
    // no code
  }

  override def stop(): Unit = {
    Utilities.close(prototype)
    if (taskFuture != null) {
      taskFuture.cancel(true)
    }
    scheduleService.shutdownNow()
  }

  @throws(classOf[NetworkIOException])
  override def testConnection() = {
    prototype.testConnection()
  }

  @throws(classOf[NetworkIOException])
  override def push(message: ApnsNotification) = {
    if (batch.isEmpty) {
      firstMessageArrivedTime = System.nanoTime()
    }

    val sinceFirstMessageSec: Long = (System.nanoTime() - firstMessageArrivedTime) / 1000 / 1000 / 1000

    if (taskFuture != null && sinceFirstMessageSec < maxBatchWaitTimeInSec) {
      taskFuture.cancel(false)
    }

    batch.add(message)

    if (taskFuture == null || taskFuture.isDone) {
      taskFuture = scheduleService.schedule(batchRunner, batchWaitTimeInSec, TimeUnit.SECONDS)
    }
  }

  class SendMessagesBatch extends Runnable {
    override def run() {
      val newConnection: ApnsConnection = prototype.copy()
      try {
        while (!(batch.poll() == null)) {
          try {
            newConnection.sendMessage(batch.poll())
          } catch {
            case e: NetworkIOException => BatchApnsService.logger.warn("Network exception sending message msg " + batch.poll().getIdentifier, e)
          }
        }
      } finally {
        Utilities.close(newConnection)
      }
    }
  }

}

object BatchApnsService {
  protected val logger: Logger = LoggerFactory.getLogger(classOf[BatchApnsService])
}
