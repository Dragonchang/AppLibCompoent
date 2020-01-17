# DeepBlue Log to file
将app中的log打印到文件和logcat

## 介绍：  
使用mmap机制将log信息缓存在内存中，每隔两分钟进行一次flush，在进程退出的时候不会有log丢失的问题。   
可以将log打印到logcat。   
可以抓取app的java crash到文件和logcat中。  
可以删除超过固定天数的log文件。  
每个log文件有固定大小（默认10M），超过固定大小重新创建文件。  

## 使用：
1.init   
在自定义的Application类中初始化log 模块代码如下所示：  
注： 如果log是要保存到sdcard中app需要WRITE_EXTERNAL_STORAGE/READ_EXTERNAL_STORAGE权限   
~~~ shell
        val storage = Environment.getExternalStorageDirectory().absolutePath
        val logPath = storage + File.separator + FILE_NAME
        val config = DeepBlueLogConfig.Builder()
            .setCachePath(applicationContext.filesDir.absolutePath)
            .setPath(logPath).build()
        DeepBlueLog.init(config)
~~~~

2.打印log   
~~~ shell
import com.deepblue.logd.Log
~~~~
替代   
~~~ shell
import android.util.Log
~~~~

3.使用详解   
Log.t 代替 Throwable.printStackTrace   

version: 0.0.1  
updateTime: 2019-12-26   
owner： zhangfl@deepblueai.com  

改进计划：  
实现抓取native crash log。  
添加更好的flush机制。  
添加固定格式输出log方式，开发对应的工具去分析log。

updateTime: 2019-12-30
添加fd leak 和java heap大小监控的功能。