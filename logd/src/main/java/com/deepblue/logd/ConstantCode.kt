package com.deepblue.logd

object ConstantCode {
    const val CLOGAN_INIT_SUCCESS_MMAP = -1010 //初始化成功, mmap内存
    const val CLOGAN_INIT_SUCCESS_MEMORY = -1020 //初始化成功, 堆内存
    const val CLOGAN_INIT_FAIL_NOCACHE = -1030 //初始化失败 , 没有缓存
    const val CLOGAN_INIT_FAIL_NOMALLOC = -1040 //初始化失败 , malloc失败
    const val CLOGAN_INIT_FAIL_HEADER = -1050 //初始化头失败
    const val CLOGAN_INIT_FAIL_JNI = -1060 //jni找不到对应C函数
    const val CLOGAN_INIT_SUCESS_ALREADY = -1070 //已经初始化过

    const val CLOGAN_OPEN_SUCCESS = -2010 //打开文件成功
    const val CLOGAN_OPEN_FAIL_IO = -2020 //打开文件IO失败
    const val CLOGAN_OPEN_FAIL_ZLIB = -2030 //打开文件zlib失败
    const val CLOGAN_OPEN_FAIL_MALLOC = -2040 //打开文件malloc失败
    const val CLOGAN_OPEN_FAIL_NOINIT = -2050 //打开文件没有初始化失败
    const val CLOGAN_OPEN_FAIL_HEADER = -2060 //打开文件头失败
    const val CLOGAN_OPEN_FAIL_JNI = -2070 //jni找不到对应C函数

    const val CLOGAN_WRITE_SUCCESS = -4010 //写入日志成功
    const val CLOGAN_WRITE_FAIL_PARAM = -4020 //写入失败, 可变参数错误
    const val CLOAGN_WRITE_FAIL_MAXFILE = -4030 //写入失败,超过文件最大值
    const val CLOGAN_WRITE_FAIL_MALLOC = -4040 //写入失败,malloc失败
    const val CLOGAN_WRITE_FAIL_HEADER = -4050 //写入头失败
    const val CLOGAN_WRITE_FAIL_JNI = -4060 //jni找不到对应C函数

    const val CLOGAN_LOAD_SO_FAIL = -5020 //加载的SO失败
}
