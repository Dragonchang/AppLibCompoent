package com.deepblue.librariesx

import android.app.Application
import android.os.Environment

import com.deepblue.logd.DeepBlueLog
import com.deepblue.logd.DeepBlueLogConfig

import java.io.File

class FileManagerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        //如果log的path是sdcard，app需要WRITE_EXTERNAL_STORAGE/READ_EXTERNAL_STORAGE权限
        val storage = Environment.getExternalStorageDirectory().absolutePath
        val logPath = storage + File.separator + FILE_NAME
        val config = DeepBlueLogConfig.Builder()
            .setCachePath(applicationContext.filesDir.absolutePath)
            .setPath(logPath).build()
        DeepBlueLog.init(config)
    }

    companion object {
        private val FILE_NAME = "TestLog"
    }
}
