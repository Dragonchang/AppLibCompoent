package com.deepblue.logd

import android.text.TextUtils

class DeepBlueLogConfig {

    internal lateinit var mCachePath: String //mmap缓存路径
    internal lateinit var mPathPath: String //file文件路径

    internal var mMaxFile = DEFAULT_FILE_SIZE //删除文件最大值
    internal var mDay = DEFAULT_DAY //删除天数
    internal var mMaxQueue = DEFAULT_QUEUE.toLong()
    internal var mMinSDCard = DEFAULT_MIN_SDCARD_SIZE //最小sdk卡大小

    fun isValid(): Boolean {
        var valid = false
        if (!TextUtils.isEmpty(mCachePath) && !TextUtils.isEmpty(mPathPath)) {
            valid = true
        }
        return valid
    }

    private fun setCachePath(cachePath: String) {
        mCachePath = cachePath
    }

    private fun setPathPath(pathPath: String) {
        mPathPath = pathPath
    }

    private fun setMaxFile(maxFile: Long) {
        mMaxFile = maxFile
    }

    private fun setDay(day: Long) {
        mDay = day
    }

    private fun setMinSDCard(minSDCard: Long) {
        mMinSDCard = minSDCard
    }

    class Builder {
        private lateinit var mCachePath: String //mmap缓存路径
        private lateinit var mPath: String //file文件路径
        private var mMaxFile = DEFAULT_FILE_SIZE //删除文件最大值
        private var mDay = DEFAULT_DAY //删除天数
        private var mMinSDCard = DEFAULT_MIN_SDCARD_SIZE

        fun setCachePath(cachePath: String): Builder {
            mCachePath = cachePath
            return this
        }

        fun setPath(path: String): Builder {
            mPath = path
            return this
        }

        fun setMaxFile(maxFile: Long): Builder {
            mMaxFile = maxFile * M
            return this
        }

        fun setDay(day: Long): Builder {
            mDay = day * DAYS
            return this
        }

        fun setMinSDCard(minSDCard: Long): Builder {
            this.mMinSDCard = minSDCard
            return this
        }

        fun build(): DeepBlueLogConfig {
            val config = DeepBlueLogConfig()
            config.setCachePath(mCachePath)
            config.setPathPath(mPath)
            config.setMaxFile(mMaxFile)
            config.setMinSDCard(mMinSDCard)
            config.setDay(mDay)
            return config
        }
    }

    companion object {
        private const val DAYS = (24 * 60 * 60 * 1000).toLong() //天
        private const val M = (1024 * 1024).toLong() //M
        private const val DEFAULT_DAY = 5 * DAYS //默认删除天数
        private const val DEFAULT_FILE_SIZE = 10 * M
        private const val DEFAULT_MIN_SDCARD_SIZE = 50 * M //最小的SD卡小于这个大小不写入
        private const val DEFAULT_QUEUE = 500
    }
}

