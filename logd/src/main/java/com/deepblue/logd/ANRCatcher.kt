package com.deepblue.logd

import android.os.FileObserver
import android.text.TextUtils

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class ANRCatcher(path: String) {
    private var mTraceFileObserver: TraceFileObserver? = null
    private var mAnrWatchDog: ANRWatchDog
    internal var duration = 4
    private val mPath: String
    internal var mFileOutput: FileOutputStream? = null
    private val mDataFormat = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")
    private var mCurrentDay: Long = 0
    private val mSaveTime: Long //存储时间

    private inner class TraceFileObserver : FileObserver(mSystemAnrFile, FileObserver.CLOSE_WRITE) {

        override fun onEvent(event: Int, path: String?) {
            if (path == null) {
                return
            }
            Log.d(TAG, "TraceFileObserver : $event path: $path")
        }
    }


    init {
        mPath = path + ANR_PATH
        val file = File(mPath)
        if (!file.exists()) {
            file.mkdir()
        }
        mSaveTime = 5 * DAYS
        //在6.0以上使用anr watchdog
        mAnrWatchDog = ANRWatchDog(2000)
        mAnrWatchDog.setANRListener(object : ANRWatchDog.ANRListener {
            override fun onAppNotResponding(error: ANRError) {
                Log.e(TAG, "Detected Application Not Responding!")
                ensureRootPath()
                mCurrentDay = Util.currentTime
                deleteExpiredANRFile(mCurrentDay - mSaveTime)
                val fileName =
                    ANR_HEADER + mDataFormat.format(Date(System.currentTimeMillis())) + "-" + mCurrentDay + PREFIX
                val anrFile = mPath + File.separator + fileName
                try {
                    val file = File(anrFile)
                    if (!file.exists()) {
                        file.createNewFile()
                    }
                    mFileOutput = FileOutputStream(file)
                    error.writeStackToFile(mFileOutput!!)
                    Log.i(TAG, "Error was successfully write to: $anrFile")
                } catch (e: IOException) {
                    Log.t(e)
                } finally {
                    Util.closeQuietly(mFileOutput)
                }
            }
        })
            .setANRInterceptor(object : ANRWatchDog.ANRInterceptor {
                override fun intercept(dur: Long): Long {
                    val ret = duration * 1000 - dur
                    if (ret > 0)
                        Log.w(
                            TAG,
                            "Intercepted ANR that is too short ($dur ms), postponing for $ret ms."
                        )
                    return ret
                }
            })
        mAnrWatchDog.start()
        //在6.0以下使用file observer来监听anr
        if (mTraceFileObserver == null) {
            mTraceFileObserver = TraceFileObserver()
            mTraceFileObserver!!.startWatching()
        }
    }


    private fun ensureRootPath() {
        val file = File(mPath)
        if (!file.exists()) {
            file.mkdir()
        }
    }

    private fun deleteExpiredANRFile(deleteTime: Long) {
        val dir = File(mPath)
        if (dir.isDirectory) {
            val files = dir.list()
            if (files != null) {
                for (item in files) {
                    if (!TextUtils.isEmpty(item) && item.contains(ANR_HEADER)) {
                        val longStrArray =
                            item.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                        if (longStrArray != null && longStrArray.size > 0) {  //小于时间就删除
                            val fileMillsName =
                                longStrArray[0].split("-".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray()
                            if (fileMillsName != null && fileMillsName.size == 3) {
                                val longItem = java.lang.Long.valueOf(fileMillsName[2])
                                if (longItem <= deleteTime) {
                                    File(mPath, item).delete() //删除文件
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "ANRCatcher"
        private const val mSystemAnrFile = "/data/anr"
        private const val ANR_PATH = "/anr"
        private const val ANR_HEADER = "ANR-"
        private const val PREFIX = ".log"
        private const val DAYS = (24 * 60 * 60 * 1000).toLong() //天
    }
}
