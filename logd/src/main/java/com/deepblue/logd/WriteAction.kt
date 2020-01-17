package com.deepblue.logd

import android.text.TextUtils

class WriteAction internal constructor() : Action() {

    internal var log: String? = null //日志
    internal var localTime: Long = 0
    internal var isMainThread: Boolean = false
    internal var pid: Long = 0
    internal var threadId: Long = 0
    internal var threadName = ""
    internal var flag: Int = 0
    internal var exception: Throwable? = null

    init {
        mAction = WRITE
    }

    override fun isValid(): Boolean {
        var valid = false
        if (!TextUtils.isEmpty(log) || exception != null) {
            valid = true
        }
        return valid
    }
}
