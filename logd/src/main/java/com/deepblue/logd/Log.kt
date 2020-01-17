package com.deepblue.logd

object Log {
    fun v(tag: String, log: String) {
        DebugLog.v(tag, log)
        DeepBlueLog.w("$tag： $log", DeepBlueLog.V)
    }

    fun d(tag: String, log: String) {
        DebugLog.d(tag, log)
        DeepBlueLog.w("$tag： $log", DeepBlueLog.D)
    }

    fun i(tag: String, log: String) {
        DebugLog.i(tag, log)
        DeepBlueLog.w("$tag： $log", DeepBlueLog.I)
    }

    fun w(tag: String, log: String) {
        DebugLog.w(tag, log)
        DeepBlueLog.w("$tag： $log", DeepBlueLog.W)
    }

    fun e(tag: String, log: String) {
        DebugLog.e(tag, log)
        DeepBlueLog.w("$tag： $log", DeepBlueLog.E)
    }

    fun t(e: Throwable) {
        DebugLog.t(e)
        DeepBlueLog.printTrace(e, DeepBlueLog.T)
    }

    fun c(e: Throwable) {
        DebugLog.t(e)
        DeepBlueLog.printTrace(e, DeepBlueLog.C)
    }
}
