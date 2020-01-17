package com.deepblue.logd

import android.os.Debug
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.system.Os
import android.text.TextUtils

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.regex.Pattern

class DeepBlueMemoryMonitor internal constructor(path: String) {
    private val mPath: String //hprof文件保存路径
    private var mCurrentDay: Long = 0
    private val mSaveTime: Long //存储时间
    private var mMemoryMonitorThread: HandlerThread
    private var mMemoryMonitorHandler: MemoryMonitorHandler
    private val mDataFormat = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")
    private val mFdPath: String
    private val mMaxCount: Int

    private val fdCount: Int
        get() {
            var ret = 0
            val file = File(mFdPath)
            if (file.isDirectory) {
                val subFiles = file.listFiles()
                if (subFiles != null) {
                    ret = subFiles.size
                }
            }
            Log.i(TAG, "getFDCount ret: $ret")
            return ret
        }

    private val maxOpenFile: Int
        get() {
            val maxCount = 1024
            val maxFile = "/proc/" + android.os.Process.myPid() + "/limits"
            val file = File(maxFile)
            var read: InputStreamReader? = null
            try {
                if (file.exists() && file.isFile) {
                    read = InputStreamReader(
                        FileInputStream(file), "GBK"
                    )
                    val bufferedReader = BufferedReader(read)
                    var lineTxt: String? = null
                    var strTemp = ""
                    bufferedReader.lineSequence().forEach {
                        lineTxt = it
                        if (lineTxt!!.contains("Max open files")) {
                            val pattern = Pattern.compile("[0-9]")
                            val matcher = pattern.matcher(lineTxt)
                            var lastEnd = 0
                            var first = true
                            while (matcher.find()) {
                                if (!first && matcher.end() - lastEnd > 1) {
                                    break
                                }
                                first = false
                                lastEnd = matcher.end()
                                strTemp += matcher.group()
                            }
                            Log.i(TAG, " strTemp: $strTemp")
                            if (!TextUtils.isEmpty(strTemp)) {
                                try {
                                    return Integer.parseInt(strTemp)
                                } catch (e: NumberFormatException) {
                                    Log.t(e)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.t(e)
            } finally {
                Util.closeQuietly(read)
            }
            return maxCount
        }

    init {
        mPath = path + MEMORY_PATH
        val file = File(mPath)
        if (!file.exists()) {
            file.mkdir()
        }
        mMemoryMonitorThread = HandlerThread("MemoryMonitor")
        mMemoryMonitorThread.start()
        mMemoryMonitorHandler = MemoryMonitorHandler(mMemoryMonitorThread.looper)
        mCurrentDay = Util.currentTime
        mSaveTime = 2 * DAYS
        mFdPath = "/proc/" + android.os.Process.myPid() + "/fd"
        mMaxCount = maxOpenFile
        mMemoryMonitorHandler.sendEmptyMessageDelayed(MSG_CHECK_MEMORY, 2000)
    }

    fun asyncDumpHprof() {
        mMemoryMonitorHandler.sendEmptyMessage(MSG_DUMP_HPROF)
    }

    fun syncDumpHprof() {
        val lock = java.lang.Object()
        synchronized(lock) {
            val message = mMemoryMonitorHandler.obtainMessage(MSG_DUMP_HPROF)
            message.obj = lock
            mMemoryMonitorHandler.sendMessage(message)
            try {
                lock.wait()
            } catch (e: InterruptedException) {
                Log.t(e)
            }

        }
    }

    fun asyncDumpFD() {
        mMemoryMonitorHandler.sendEmptyMessage(MSG_DUMP_FD)
    }

    fun syncDumpFD() {
        val lock = java.lang.Object()
        synchronized(lock) {
            val message = mMemoryMonitorHandler.obtainMessage(MSG_DUMP_FD)
            message.obj = lock
            mMemoryMonitorHandler.sendMessage(message)
            try {
                lock.wait()
            } catch (e: InterruptedException) {
                Log.t(e)
            }

        }
    }


    private fun ensureRootPath() {
        val file = File(mPath)
        if (!file.exists()) {
            file.mkdir()
        }
    }

    private inner class MemoryMonitorHandler internal constructor(looper: Looper) :
        Handler(looper) {

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_CHECK_MEMORY -> {
                    if (outOfJavaHeap() || outOfOpenFile()) {
                        dumpFD()
                        dumpHprof()
                    } else {
                        mMemoryMonitorHandler.sendEmptyMessageDelayed(
                            MSG_CHECK_MEMORY,
                            2000
                        )
                    }
                }
                MSG_DUMP_HPROF -> {
                    dumpHprof()
                    val lock = msg.obj as java.lang.Object
                    if (lock != null) {
                        synchronized(lock) {
                            lock.notify()
                        }
                    }
                }
                MSG_DUMP_FD -> {
                    dumpFD()
                    val lock = msg.obj as java.lang.Object
                    if (lock != null) {
                        synchronized(lock) {
                            lock.notify()
                        }
                    }
                }
            }
        }


    }

    private fun outOfJavaHeap(): Boolean {
        val rt = Runtime.getRuntime()
        val maxMemory = rt.maxMemory()
        val totalMemory = rt.totalMemory()
        val freeMemory = rt.freeMemory()
        val availableMemory = maxMemory - totalMemory + freeMemory
        Log.i(
            TAG,
            "outOfJavaHeap maxMemory:$maxMemory freeMemory:$freeMemory totalMemory:$totalMemory availableMemory: $availableMemory"
        )
        return (availableMemory < 10 * M)
    }

    private fun outOfOpenFile(): Boolean {
        return (fdCount > mMaxCount - mMaxCount / 10)
    }

    private fun dumpHprof() {
        ensureRootPath()
        mCurrentDay = Util.currentTime
        deleteExpiredHprofFile(mCurrentDay - mSaveTime)
        val fileName =
            HPROF_HEADER + mDataFormat.format(Date(System.currentTimeMillis())) + "-" + mCurrentDay + HPROF
        val hprofFile = mPath + File.separator + fileName
        Log.e(TAG, "dumpHprof********************begin hprofFile: $hprofFile")
        try {
            Debug.dumpHprofData(hprofFile)
        } catch (e: Exception) {
            Log.t(e)
        }

        Log.e(TAG, "dumpHprof*******************end hprofFile: $hprofFile")
    }

    private fun dumpFD() {
        ensureRootPath()
        mCurrentDay = Util.currentTime
        deleteExpiredFDFile(mCurrentDay - mSaveTime)
        val fileName =
            FD_HEADER + mDataFormat.format(Date(System.currentTimeMillis())) + "-" + mCurrentDay + FD
        val fdFile = mPath + File.separator + fileName
        Log.e(TAG, "dumpFD********************begin fdFile: $fdFile")
        val filepath = File(fdFile)
        var fos: FileOutputStream? = null
        try {
            if (!filepath.exists()) {
                filepath.createNewFile()
            }
            fos = FileOutputStream(filepath)
            val file = File(mFdPath)
            var writeStr: String? = null
            if (file.isDirectory) {
                val subfiles = file.listFiles()
                if (subfiles != null) {
                    for (f in subfiles) {
                        if (f.exists()) {
                            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                .format(Date(f.lastModified()))
                            writeStr =
                                time + "    " + f.name + " -> " + Os.readlink(f.absolutePath) + "\n"
                        }
                        if (writeStr != null) {
                            fos.write(writeStr.toByteArray())
                        }

                    }
                }
            }
        } catch (e: Exception) {
            Log.t(e)
            if (filepath.exists()) {
                filepath.delete()
            }
        } finally {
            Util.closeQuietly(fos)
        }
        Log.e(TAG, "dumpFD*******************end fdFile: $fdFile")
    }


    private fun deleteExpiredHprofFile(deleteTime: Long) {
        val dir = File(mPath)
        if (dir.isDirectory) {
            val files = dir.list()
            if (files != null) {
                for (item in files) {
                    if (!TextUtils.isEmpty(item) && item.contains(HPROF_HEADER)) {
                        val longStrArray =
                            item.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                        if (longStrArray!= null && longStrArray.isNotEmpty()) {  //小于时间就删除
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

    private fun deleteExpiredFDFile(deleteTime: Long) {
        val dir = File(mPath)
        if (dir.isDirectory) {
            val files = dir.list()
            if (files != null) {
                for (item in files) {
                    if (!TextUtils.isEmpty(item) && item.contains(FD_HEADER)) {
                        val longStrArray =
                            item.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                        if (longStrArray != null && longStrArray.isNotEmpty()) {  //小于时间就删除
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
        private const val TAG = "DeepBlueMemoryMonitor"
        private const val HPROF = ".hprof"
        private const val FD = ".log"
        private const val HPROF_HEADER = "HPROF-"
        private const val FD_HEADER = "FD-"
        private const val MEMORY_PATH = "/memory_file"
        private const val DAYS = (24 * 60 * 60 * 1000).toLong() //天
        private const val M = (1024 * 1024).toLong() //M

        private const val MSG_CHECK_MEMORY = 1
        private const val MSG_DUMP_HPROF = 2
        private const val MSG_DUMP_FD = 3

    }
}
