package com.deepblue.logd

import android.os.SystemClock

import java.lang.Thread.UncaughtExceptionHandler

class CrashHandler : UncaughtExceptionHandler {
    // 系统默认的 UncaughtException 处理类
    private var mDefaultHandler: UncaughtExceptionHandler? = null

    fun init() {
        // 获取系统默认的 UncaughtException 处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        // 设置该 CrashHandler 为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    /**
     * 当 UncaughtException 发生时会转入该函数来处理
     */
    override fun uncaughtException(thread: Thread, ex: Throwable) {
        if (!handleException(ex) && mDefaultHandler != null) {
            mDefaultHandler!!.uncaughtException(thread, ex)
        } else {
            //杀死或重启进程
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(0)
        }
    }

    /**
     * 自定义错误处理，收集错误信息，发送错误报告等操作均在此完成
     *
     * @param ex Throwable
     * @return true：如果处理了该异常信息；否则返回 false
     */
    private fun handleException(ex: Throwable?): Boolean {
        if (ex == null) {
            return false
        }
        Log.i("CrashHandler", "app happen crash!!!!!!!!!!!!!!")
        Log.c(ex)
        DeepBlueLog.f()
        //if(ex instanceof OutOfMemoryError) {
        DeepBlueLog.dumpHprof(true)
        DeepBlueLog.dumpFD(true)
        //}
        SystemClock.sleep(500)
        return true
    }

    companion object {
        private var instance: CrashHandler? = null

        fun getInstance(): CrashHandler? {
            if (null == instance) {
                synchronized(CrashHandler::class.java) {
                    instance = CrashHandler()
                }
            }
            return instance
        }
    }
}
