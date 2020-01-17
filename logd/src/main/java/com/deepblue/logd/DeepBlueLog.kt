package com.deepblue.logd

open class DeepBlueLog {
    companion object {
        private var sLoganControlCenter: DeepBlueLogControl? = null

        fun init(loganConfig: DeepBlueLogConfig) {
            sLoganControlCenter = DeepBlueLogControl.instance(loganConfig)
            CrashHandler.getInstance()!!.init()
        }

        /**
         * @param log  表示日志内容
         * @param type 表示日志类型
         * @brief DeepBlueLog写入日志
         */
        fun w(log: String, type: Int) {
            if (sLoganControlCenter == null) {
                return
            }
            sLoganControlCenter!!.write(log, type)
        }

        /**
         * @param Throwable  表示日志内容
         * @param type 表示日志类型
         * @brief DeepBlueLog写入日志
         */
        fun printTrace(e: Throwable, type: Int) {
            if (sLoganControlCenter == null) {
                return
            }
            sLoganControlCenter!!.writeTrace(e, type)
        }

        /**
         * @brief 立即写入日志文件
         */
        fun f() {
            if (sLoganControlCenter == null) {
                return
            }
            sLoganControlCenter!!.flush()
        }
        /**
         * @brief 立即写入日志文件
         */
        fun dumpHprof(isSync: Boolean) {
            if (sLoganControlCenter == null) {
                return
            }
            sLoganControlCenter!!.dumpHprof(isSync)
        }

        /**
         * @brief 立即写入日志文件
         */
        fun dumpFD(isSync: Boolean) {
            if (sLoganControlCenter == null) {
                return
            }
            sLoganControlCenter!!.dumpFD(isSync)
        }

        const val V = 0x1
        const val D = 0x2
        const val I = 0x3
        const val W = 0x4
        const val E = 0x5
        const val T = 0x6
        const val C = 0x7
    }
}
