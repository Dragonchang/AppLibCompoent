package com.deepblue.logd

class DeepBlueNativeLog {
    private var mIsLoganInit: Boolean = false
    private var mIsLoganOpen: Boolean = false

    external fun native_init(cache_path: String, dir_path: String, max_file: Int): Int
    external fun native_open(file_name: String): Int
    external fun native_write(
        flag: Int, log: String?, local_time: Long, thread_name: String,
        thread_id: Long, is_main: Int
    ): Int

    external fun native_flush()


    fun logan_init(cache_path: String, dir_path: String, max_file: Int): Int {
        if (mIsLoganInit) {
            return ConstantCode.CLOGAN_INIT_SUCESS_ALREADY
        }
        if (!sIsCloganOk) {
            return ConstantCode.CLOGAN_LOAD_SO_FAIL
        }
        var code = ConstantCode.CLOGAN_LOAD_SO_FAIL
        try {
            code = native_init(cache_path, dir_path, max_file)
            mIsLoganInit = true
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }

        return code
    }

    fun logan_open(file_name: String): Int {
        var code = ConstantCode.CLOGAN_LOAD_SO_FAIL
        if (!mIsLoganInit || !sIsCloganOk) {
            return code
        }
        try {
            code = native_open(file_name)
            mIsLoganOpen = true
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }

        return code
    }

    fun logan_write(
        flag: Int, log: String?, local_time: Long, thread_name: String,
        thread_id: Long, is_main: Boolean
    ): Int {
        var code = ConstantCode.CLOGAN_LOAD_SO_FAIL
        if (!mIsLoganOpen || !sIsCloganOk) {
            return code
        }
        try {
            val isMain = if (is_main) 1 else 0
            code = native_write(
                flag, log, local_time, thread_name, thread_id,
                isMain
            )
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }

        return code
    }

    fun logan_flush() {
        if (!mIsLoganOpen || !sIsCloganOk) {
            return
        }
        try {
            native_flush()
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }

    }

    companion object {
        @Volatile
        private var sDeepBlueCLog: DeepBlueNativeLog? = null

        private val LIBRARY_NAME = "clogan"

        private var sIsCloganOk: Boolean = false


        init {
            try {
                if (!Util.loadLibrary(LIBRARY_NAME, DeepBlueNativeLog::class.java)) {
                    System.loadLibrary(LIBRARY_NAME)
                }
                sIsCloganOk = true
            } catch (e: Throwable) {
                e.printStackTrace()
                sIsCloganOk = false
            }

        }

        internal fun newInstance(): DeepBlueNativeLog? {
            if (sDeepBlueCLog == null) {
                synchronized(DeepBlueNativeLog::class.java) {
                    if (sDeepBlueCLog == null) {
                        sDeepBlueCLog = DeepBlueNativeLog()
                    }
                }
            }
            return sDeepBlueCLog
        }
    }
}

