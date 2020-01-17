package com.deepblue.logd

import java.io.Closeable
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.text.SimpleDateFormat
import java.util.Date

object Util {

    private val sDateFormat = SimpleDateFormat("yyyy_MM_dd")

    val currentTime: Long
        get() {
            val currentTime = System.currentTimeMillis()
            var tempTime: Long = 0
            try {
                val dataStr = sDateFormat.format(Date(currentTime))
                tempTime = sDateFormat.parse(dataStr)!!.time
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return tempTime
        }

    fun loadLibrary(loadName: String, className: Class<*>): Boolean {
        var isLoad = false
        try {
            val classLoader = className.classLoader
            val runtime = Runtime.getRuntime().javaClass
            val args = arrayOfNulls<Class<*>>(2)
            val version = android.os.Build.VERSION.SDK_INT
            var functionName = "loadLibrary"
            if (version > 24) {
                args[0] = ClassLoader::class.java
                args[1] = String::class.java
                functionName = "loadLibrary0"
                val loadMethod = runtime.getDeclaredMethod(functionName, *args)
                loadMethod.isAccessible = true
                loadMethod.invoke(Runtime.getRuntime(), classLoader, loadName)
            } else {
                args[0] = String::class.java
                args[1] = ClassLoader::class.java
                val loadMethod = runtime.getDeclaredMethod(functionName, *args)
                loadMethod.isAccessible = true
                loadMethod.invoke(Runtime.getRuntime(), loadName, classLoader)
            }
            isLoad = true
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        }

        return isLoad
    }

    fun getDateStr(time: Long): String {
        return sDateFormat.format(Date(time))
    }

    fun getLogType(type: Int): String {
        when (type) {
            DeepBlueLog.V -> return "V"
            DeepBlueLog.D -> return "D"
            DeepBlueLog.I -> return "I"
            DeepBlueLog.W -> return "W"
            DeepBlueLog.E -> return "E"
            DeepBlueLog.T -> return "W/System.err:"
            DeepBlueLog.C -> return "E AndroidRuntime:"
        }
        return ""
    }

    fun closeQuietly(closeable: Closeable?) {
        try {
            closeable?.close()
        } catch (ioe: IOException) {
            Log.t(ioe)
        }
    }
}
