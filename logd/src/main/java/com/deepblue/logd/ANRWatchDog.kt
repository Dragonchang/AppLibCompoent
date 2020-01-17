package com.deepblue.logd

import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * A watchdog timer thread that detects when the UI thread has frozen.
 */
class ANRWatchDog
/**
 * Constructs a watchdog that checks the ui thread every given interval
 *
 * @param timeoutInterval The interval, in milliseconds, between to checks of the UI thread.
 * It is therefore the maximum time the UI may freeze before being reported as ANR.
 */
    (
    /**
     * @return The interval the WatchDog
     */
    val timeoutInterval: Int
) : Thread() {

    private var _anrListener: ANRListener? = null
    private var _anrInterceptor = DEFAULT_ANR_INTERCEPTOR
    private var _interruptionListener = DEFAULT_INTERRUPTION_LISTENER

    private val _uiHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var _tick: Long = 0
    @Volatile
    private var _reported = false

    private val _ticker = Runnable {
        _tick = 0
        _reported = false
    }

    interface ANRListener {
        /**
         * Called when an ANR is detected.
         *
         * @param error The error describing the ANR.
         */
        fun onAppNotResponding(error: ANRError)
    }

    interface ANRInterceptor {
        /**
         * Called when main thread has froze more time than defined by the timeout.
         *
         * @param duration The minimum time (in ms) the main thread has been frozen (may be more).
         * @return 0 or negative if the ANR should be reported immediately. A positive number of ms to postpone the reporting.
         */
        fun intercept(duration: Long): Long
    }

    interface InterruptionListener {
        fun onInterrupted(exception: InterruptedException)
    }

    /**
     * Sets an interface for when an ANR is detected.
     * If not set, the default behavior is to throw an error and crash the application.
     *
     * @param listener The new listener or null
     * @return itself for chaining.
     */
    fun setANRListener(listener: ANRListener?): ANRWatchDog {
        if (listener == null) {
            _anrListener = null
        } else {
            _anrListener = listener
        }
        return this
    }

    /**
     * Sets an interface to intercept ANRs before they are reported.
     * If set, you can define if, given the current duration of the detected ANR and external context, it is necessary to report the ANR.
     *
     * @param interceptor The new interceptor or null
     * @return itself for chaining.
     */
    fun setANRInterceptor(interceptor: ANRInterceptor?): ANRWatchDog {
        if (interceptor == null) {
            _anrInterceptor = DEFAULT_ANR_INTERCEPTOR
        } else {
            _anrInterceptor = interceptor
        }
        return this
    }

    /**
     * Sets an interface for when the watchdog thread is interrupted.
     * If not set, the default behavior is to just log the interruption message.
     *
     * @param listener The new listener or null.
     * @return itself for chaining.
     */
    fun setInterruptionListener(listener: InterruptionListener?): ANRWatchDog {
        if (listener == null) {
            _interruptionListener = DEFAULT_INTERRUPTION_LISTENER
        } else {
            _interruptionListener = listener
        }
        return this
    }

    override fun run() {
        name = "WatchDog-Anr"

        var interval = timeoutInterval.toLong()
        while (!isInterrupted) {
            val needPost = _tick == 0L
            _tick += interval
            if (needPost) {
                _uiHandler.post(_ticker)
            }

            try {
                Thread.sleep(interval)
            } catch (e: InterruptedException) {
                _interruptionListener.onInterrupted(e)
                return
            }

            // If the main thread has not handled _ticker, it is blocked. ANR.
            if (_tick != 0L && !_reported) {
                interval = _anrInterceptor.intercept(_tick)
                if (interval > 0) {
                    continue
                }

                val error = ANRError(_tick)
                _anrListener!!.onAppNotResponding(error)
                interval = timeoutInterval.toLong()
                _reported = true
            }
        }
    }

    companion object {

        private val DEFAULT_ANR_TIMEOUT = 5000

        private val DEFAULT_ANR_INTERCEPTOR: ANRInterceptor = object : ANRInterceptor {
            override fun intercept(duration: Long): Long {
                return 0
            }
        }

        private val DEFAULT_INTERRUPTION_LISTENER: InterruptionListener =
            object : InterruptionListener {
                override fun onInterrupted(exception: InterruptedException) {
                    Log.w("ANRWatchdog", "Interrupted: " + exception.message)
                }
            }
    }

}
