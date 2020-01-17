package com.deepblue.logd

import android.util.Log

object DebugLog {
    private const val isDebug = true
    fun v(tag: String, log: String?) {
        if (isDebug && log != null) Log.v(tag, log)
    }

    fun d(tag: String, log: String?) {
        if (isDebug && log != null) Log.d(tag, log)
    }

    fun i(tag: String, log: String?) {
        if (isDebug && log != null) Log.i(tag, log)
    }

    fun w(tag: String, log: String?) {
        if (isDebug && log != null) Log.w(tag, log)
    }

    fun e(tag: String, log: String?) {
        if (isDebug && log != null) Log.e(tag, log)
    }

    fun t(e: Throwable?) {
        if (isDebug && e != null) {
            e.printStackTrace()
        }
    }
}
