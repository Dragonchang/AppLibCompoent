package com.deepblue.logd

import android.os.Looper

import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Comparator
import java.util.Date
import java.util.TreeMap

class ANRError(
    /**
     * The minimum duration, in ms, for which the main thread has been blocked. May be more.
     */
    val duration: Long
) {

    private val mMainThread = Looper.getMainLooper().thread
    private val mStackTraces: MutableMap<Thread, Array<StackTraceElement>> =
        TreeMap(Comparator { lhs, rhs ->
            if (lhs === rhs)
                return@Comparator 0
            if (lhs === mMainThread)
                return@Comparator -1
            if (rhs === mMainThread) -1 else rhs.name.compareTo(lhs.name)
        })

    init {

        for ((key, value) in Thread.getAllStackTraces()) {
            if (key === mMainThread || value.size > 0) {
                mStackTraces[key] = value
            }
        }

        // Sometimes main is not returned in getAllStackTraces() - ensure that we list it
        if (!mStackTraces.containsKey(mMainThread)) {
            mStackTraces[mMainThread] = mMainThread.stackTrace
        }
    }

    @Throws(IOException::class)
    fun writeStackToFile(mFileOutput: FileOutputStream) {
        try {
            var writeStr =
                "----- pid " + android.os.Process.myPid() + " at " + SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(
                    Date(System.currentTimeMillis())
                ) + " -----\n"
            mFileOutput.write(writeStr.toByteArray())
            writeStr = "main thread block cost :$duration\n"
            mFileOutput.write(writeStr.toByteArray())
            for ((thread, value) in mStackTraces) {
                writeStr = "   \n"
                mFileOutput.write(writeStr.toByteArray())
                if (thread != null) {
                    writeStr = ("\"" + thread.name + "\" "
                            + " prio=" + thread.priority
                            + " tid=" + thread.id
                            + " status=" + thread.state + "\n")
                    mFileOutput.write(writeStr.toByteArray())
                    for (traceElement in value) {
                        writeStr = "  at $traceElement\n"
                        mFileOutput.write(writeStr.toByteArray())
                    }
                    writeStr = "   \n"
                    mFileOutput.write(writeStr.toByteArray())
                }
            }
        } catch (e: IOException) {
            throw e
        }

    }
}