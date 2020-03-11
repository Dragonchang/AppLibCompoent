package com.deepblue.logd

import android.os.StatFs
import android.text.TextUtils
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class DeepBlueLogThread internal constructor(
    // 发送缓存队列
    private val mCacheLogQueue: ConcurrentLinkedQueue<Action>,
    private val mCachePath: String // 缓存文件路径
    ,
    private val mPath: String //文件路径
    ,
    private val mSaveTime: Long //存储时间
    ,
    private val mMaxLogFile: Long//最大文件大小
    ,
    private val mMinSDCard: Long
) : Thread() {
    private val sync =  java.lang.Object()
    private val mIsRun = true
    private var mIsWorking: Boolean = false
    private val mDataFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    private var mDeepBlueNativeLog: DeepBlueNativeLog? = null

    private var mInitCode: Int = 0
    private var mOpenCode: Int = 0

    private var mCurrentDay: Long = 0
    private var mIsSDCard: Boolean = false
    private var mLastTime: Long = 0
    private var mFileSection: Int = 0
    /*
     *是否在一天之内
     * return ture 是在一天之内/false 超过一天或者第一次打开
     */
    private val isDay: Boolean
        get() {
            val currentTime = System.currentTimeMillis()
            return mCurrentDay < currentTime && mCurrentDay + DAY > currentTime
        }

    private//判断SDK卡
    val isCanWriteSDCard: Boolean
        get() {
            var item = false
            try {
                val stat = StatFs(mPath)
                val blockSize = stat.blockSizeLong
                val availableBlocks = stat.availableBlocksLong
                val total = availableBlocks * blockSize
                if (total > mMinSDCard) {
                    item = true
                }
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }

            return item
        }

    init {
        mLastTime = System.currentTimeMillis()
        mIsSDCard = true
    }


    internal fun notifyRun() {
        if (!mIsWorking) {
            synchronized(sync) {
                sync.notify()
            }
        }
    }

    override fun run() {
        super.run()
        while (mIsRun) {
            synchronized(sync) {
                mIsWorking = true
                try {
                    val model = mCacheLogQueue.poll()
                    if (model == null) {
                        mIsWorking = false
                        sync.wait()
                        mIsWorking = true
                    } else {
                        action(model)
                    }
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    mIsWorking = false
                }

            }
        }
    }

    private fun action(action: Action?) {
        if (action == null || !action.isValid()) {
            return
        }
        if (mDeepBlueNativeLog == null || !(mInitCode == ConstantCode.CLOGAN_INIT_SUCCESS_MMAP
                    || mInitCode == ConstantCode.CLOGAN_INIT_SUCCESS_MEMORY
                    || mInitCode == ConstantCode.CLOGAN_INIT_SUCESS_ALREADY)) {
            mDeepBlueNativeLog = DeepBlueNativeLog.newInstance()
            mInitCode = mDeepBlueNativeLog!!.logan_init(mCachePath, mPath, mMaxLogFile.toInt())
        }
        if (mInitCode == ConstantCode.CLOGAN_INIT_SUCCESS_MMAP
            || mInitCode == ConstantCode.CLOGAN_INIT_SUCCESS_MEMORY
            || mInitCode == ConstantCode.CLOGAN_INIT_SUCESS_ALREADY
        ) {
            if (action.mAction == Action.WRITE) {
                doWriteLog2File(action as WriteAction)
            } else if (action.mAction == Action.FLUSH) {
                doFlushLog2File()
            }
        } else {
            Log.e("DeepBlueLogThread", "init failed with code: $mInitCode")
        }
    }


    private fun doWriteLog2File(action: WriteAction) {
        if (!isDay) {
            mFileSection = 0
            val tempCurrentDay = Util.currentTime
            //save时间
            val deleteTime = tempCurrentDay - mSaveTime
            deleteExpiredFile(deleteTime)//删除5天之前的log
            mCurrentDay = tempCurrentDay
            mOpenCode =
                mDeepBlueNativeLog!!.logan_open(Util.getDateStr(mCurrentDay) + "-" + mCurrentDay + "-" + mFileSection)
        }
        if (mOpenCode != ConstantCode.CLOGAN_OPEN_SUCCESS) {
            mOpenCode =
                mDeepBlueNativeLog!!.logan_open(Util.getDateStr(mCurrentDay) + "-" + mCurrentDay + "-" + mFileSection)
            if (mOpenCode != ConstantCode.CLOGAN_OPEN_SUCCESS) {
                Log.e("DeepBlueLogThread", "logan_open failed with code: $mOpenCode")
                return
            }
        }
        val currentTime = System.currentTimeMillis() //每隔2分钟判断一次
        if (currentTime - mLastTime > 2 * MINUTE) {
            doFlushLog2File()
            mIsSDCard = isCanWriteSDCard
            mLastTime = System.currentTimeMillis()
        }

        if (!mIsSDCard) { //如果大于50M 不让再次写入
            Log.e("DeepBlueLogThread", "sd card 小于50M！")
            return
        }
        val logPattern = (mDataFormat.format(Date(action.localTime))
                + " [" + action.pid + "/" + action.threadId + ": " + action.threadName
                + "] " + Util.getLogType(action.flag) + " ")
        if (!TextUtils.isEmpty(action.log)) {
            action.log = logPattern + action.log
            write(action)

        } else if (action.exception != null) {
            action.log = logPattern + action.exception.toString()
            write(action)
            for (traceElement in action.exception!!.stackTrace) {
                action.log = logPattern + traceElement.toString()
                write(action)
            }
        }
    }

    private fun write(action: WriteAction) {
        var code = mDeepBlueNativeLog!!.logan_write(
            action.flag, action.log, action.localTime, action.threadName,
            action.threadId, action.isMainThread
        )
        if (code == ConstantCode.CLOAGN_WRITE_FAIL_MAXFILE) {
            do {
                Log.e("DeepBlueLogThread", "write file more than 10M mFileSection:$mFileSection")
                mFileSection ++
                mOpenCode =
                    mDeepBlueNativeLog!!.logan_open(Util.getDateStr(mCurrentDay) + "-" + mCurrentDay + "-" + mFileSection)
                code = mDeepBlueNativeLog!!.logan_write(
                    action.flag, action.log, action.localTime, action.threadName,
                    action.threadId, action.isMainThread
                )
            } while (code == ConstantCode.CLOAGN_WRITE_FAIL_MAXFILE)
        }
    }

    private fun deleteExpiredFile(deleteTime: Long) {
        val dir = File(mPath)
        if (dir.isDirectory) {
            val files = dir.list()
            if (files != null) {
                for (item in files) {
                    try {
                        if (TextUtils.isEmpty(item)) {
                            continue
                        }
                        val longStrArray =
                            item.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                        if (longStrArray.isNotEmpty()) {  //小于时间就删除
                            val fileMillsName =
                                longStrArray[0].split("-".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray()
                            if (fileMillsName != null && fileMillsName.size > 1) {
                                val longItem = java.lang.Long.valueOf(fileMillsName[1])
                                if (longItem <= deleteTime) {
                                    File(mPath, item).delete() //删除文件
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
            }
        }
    }

    private fun doFlushLog2File() {
        if (mDeepBlueNativeLog != null) {
            mDeepBlueNativeLog!!.logan_flush()
        }
    }

    companion object {
        private const val MINUTE = 60 * 1000
        private const val DAY = (24 * 60 * 60 * 1000).toLong()
    }
}
