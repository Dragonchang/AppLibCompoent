package com.deepblue.logd

import android.os.Looper
import android.text.TextUtils
import java.util.concurrent.ConcurrentLinkedQueue

class DeepBlueLogControl private constructor(config: DeepBlueLogConfig) {

    private val mCachePath: String // 缓存文件路径
    private val mPath: String //文件路径
    private val mSaveTime: Long //存储时间
    private val mMaxLogFile: Long//最大文件大小
    private val mMinSDCard: Long
    private val mMaxQueue: Long //最大队列数
    private val mPid: Long

    private var mLogThread: DeepBlueLogThread? = null
    private var mDeepBlueMemoryMonitor: DeepBlueMemoryMonitor? = null
    private var mANRCatcher: ANRCatcher? = null
    private val mCacheLogQueue = ConcurrentLinkedQueue<Action>()

    init {
        if (!config.isValid()) {
            throw NullPointerException("config's param is invalid")
        }

        mPath = config.mPathPath
        mCachePath = config.mCachePath
        mSaveTime = config.mDay
        mMinSDCard = config.mMinSDCard
        mMaxLogFile = config.mMaxFile
        mMaxQueue = config.mMaxQueue
        mPid = android.os.Process.myPid().toLong()
        init()
    }

    private fun init() {
        if (mLogThread == null) {
            mLogThread = DeepBlueLogThread(
                mCacheLogQueue, mCachePath, mPath, mSaveTime,
                mMaxLogFile, mMinSDCard
            )
            mLogThread!!.name = "log-thread"
            mLogThread!!.start()
        }
        if (mDeepBlueMemoryMonitor == null) {
            mDeepBlueMemoryMonitor = DeepBlueMemoryMonitor(mPath)
        }
        mANRCatcher = ANRCatcher(mPath)
    }
    internal fun dumpHprof(isSync: Boolean) {
        if (isSync) {
            mDeepBlueMemoryMonitor!!.syncDumpHprof()
        } else {
            mDeepBlueMemoryMonitor!!.asyncDumpHprof()
        }
    }

    internal fun dumpFD(isSync: Boolean) {
        if (isSync) {
            mDeepBlueMemoryMonitor!!.syncDumpFD()
        } else {
            mDeepBlueMemoryMonitor!!.asyncDumpFD()
        }
    }

    internal fun write(log: String, flag: Int) {
        if (TextUtils.isEmpty(log)) {
            return
        }
        val action = WriteAction()
        val threadLog = Thread.currentThread().id
        var isMain = false
        if (Looper.getMainLooper() == Looper.myLooper()) {
            isMain = true
        }
        action.log = log
        action.localTime = System.currentTimeMillis()
        action.flag = flag
        action.isMainThread = isMain
        action.threadId = threadLog
        action.pid = mPid
        action.threadName = getThreadName()
        mCacheLogQueue.add(action)
        if (mLogThread != null) {
            mLogThread!!.notifyRun()
        }
    }

    internal fun writeTrace(e: Throwable?, flag: Int) {
        if (e == null) {
            return
        }
        val action = WriteAction()
        val threadLog = Thread.currentThread().id
        var isMain = false
        if (Looper.getMainLooper() == Looper.myLooper()) {
            isMain = true
        }
        action.exception = e
        action.localTime = System.currentTimeMillis()
        action.flag = flag
        action.isMainThread = isMain
        action.threadId = threadLog
        action.pid = mPid
        action.threadName = getThreadName()
        mCacheLogQueue.add(action)
        if (mLogThread != null) {
            mLogThread!!.notifyRun()
        }
    }

    internal fun flush() {
        if (TextUtils.isEmpty(mPath)) {
            return
        }
        val model = FlushAction()
        mCacheLogQueue.add(model)
        if (mLogThread != null) {
            mLogThread!!.notifyRun()
        }
    }

    private fun getThreadName(): String {
        var threadName = Thread.currentThread().name
        if (!TextUtils.isEmpty(threadName)) {
            threadName = if (threadName.length < THREAD_NAME_LENGTH) {
                var left = THREAD_NAME_LENGTH - threadName.length
                val str = StringBuilder(threadName)
                do {
                    str.append(" ")
                    --left
                } while (left > 0)
                str.toString()
            } else {
                threadName.substring(0, THREAD_NAME_LENGTH)
            }
        }
        return threadName
    }

    companion object {

        private const val THREAD_NAME_LENGTH = 13
        @Volatile
        private var sLoganControlCenter: DeepBlueLogControl? = null

        internal fun instance(config: DeepBlueLogConfig): DeepBlueLogControl? {
            if (sLoganControlCenter == null) {
                synchronized(DeepBlueLogControl::class.java) {
                    if (sLoganControlCenter == null) {
                        sLoganControlCenter = DeepBlueLogControl(config)
                    }
                }
            }
            return sLoganControlCenter
        }
    }

}


