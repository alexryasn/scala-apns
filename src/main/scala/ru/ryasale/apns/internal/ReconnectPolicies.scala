package ru.ryasale.apns.internal

import ru.ryasale.apns.ReconnectPolicy

/**
 * !Ready!
 * Created by ryasale on 22.09.15.
 */
object ReconnectPolicies {

  class Never extends ReconnectPolicy {

    def shouldReconnect: Boolean = {
      false
    }

    def reconnected {}

    def copy: ReconnectPolicies.Never = {
      this
    }

  }

  class Always extends ReconnectPolicy {

    def shouldReconnect: Boolean = {
      true
    }

    def reconnected {}

    def copy: ReconnectPolicies.Always = {
      this
    }

  }

  object EveryHalfHour {
    private val PERIOD: Long = 30 * 60 * 1000
  }

  class EveryHalfHour extends ReconnectPolicy {
    private var lastRunning: Long = System.currentTimeMillis

    def shouldReconnect: Boolean = {
      return System.currentTimeMillis - lastRunning > EveryHalfHour.PERIOD
    }

    def reconnected {
      lastRunning = System.currentTimeMillis
    }

    def copy: ReconnectPolicies.EveryHalfHour = {
      this
    }

  }

}
