package com.deepblue.logd

open class Action {
    var mAction: Int = 0
    open fun isValid(): Boolean {
       return true
    }

    companion object {
        const val WRITE = 1
        const val FLUSH = 2
    }
}
