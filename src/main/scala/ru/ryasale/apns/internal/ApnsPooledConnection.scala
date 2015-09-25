package ru.ryasale.apns.internal

import java.util.concurrent._

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.slf4j.LoggerFactory
import ru.ryasale.apns.ApnsNotification
import ru.ryasale.apns.exceptions.NetworkIOException

import scala.concurrent.ExecutionException

/**
 * !Ready!
 * Created by ryasale on 22.09.15.
 */
class ApnsPooledConnection(prototype: ApnsConnection, max: Int, executors: ExecutorService) extends ApnsConnection {
  val logger = LoggerFactory.getLogger(getClass)

  private val prototypes = new ConcurrentLinkedQueue[ApnsConnection]()

  def this(prototype: ApnsConnection, max: Int) = this(prototype, max, Executors.newFixedThreadPool(max))

  private val uniquePrototype = new ThreadLocal[ApnsConnection]() {
    protected override def initialValue: ApnsConnection = {
      val newCopy = prototype.copy()
      prototypes.add(newCopy)
      newCopy
    }
  }

  @throws(classOf[NetworkIOException])
  override def sendMessage(m: ApnsNotification) = {
    val future: Future[Void] = executors.submit(new Callable[Void]() {
      @throws(classOf[Exception])
      def call(): Void = {
        uniquePrototype.get().sendMessage(m)
        null
      }
    })
    try {
      future.get()
    } catch {
      case ie: InterruptedException => Thread.currentThread().interrupt()
      case ee: ExecutionException =>
        ee.getCause match {
          case exception: NetworkIOException => throw exception
          case _ =>
        }
    }
  }

  override def copy(): ApnsConnection = {
    // TODO: Should copy executor properly.... What should copy do really?!
    new ApnsPooledConnection(prototype, max)
  }

  override def close() = {
    executors.shutdown()
    try {
      executors.awaitTermination(10, TimeUnit.SECONDS)
    } catch {
      case e: InterruptedException => logger.warn("pool termination interrupted", e)
    }
    while (!prototypes.isEmpty) {
      Utilities.close(prototypes.poll())
    }
    Utilities.close(prototype)
  }

  @throws(classOf[NetworkIOException])
  override def testConnection() = {
    prototype.testConnection()
  }

  override def setCacheLength(cacheLength: Integer) = {
    while (!prototypes.isEmpty) {
      prototypes.poll().setCacheLength(cacheLength)
    }
  }

  @SuppressFBWarnings(value = Array("UG_SYNC_SET_UNSYNC_GET"), justification = "prototypes is a MT-safe container")
  override def getCacheLength(): Int = {
    prototypes.peek().getCacheLength()
  }

}