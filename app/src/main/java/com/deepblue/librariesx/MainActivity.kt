package com.deepblue.librariesx

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Button
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.alibaba.android.arouter.launcher.ARouter
import com.deepblue.librariesx.bean.TestParm
import com.deepblue.librariesx.databinding.ActivityMainBinding
import com.deepblue.librariesx.viewmodel.TestViewModel
import com.deepblue.logd.Log
import android.app.AppOpsManager
import android.content.Intent
import android.os.Build
import android.provider.Settings


class MainActivity : AppCompatActivity() {
    private var appCopy: Button? = null

    private var activityGirlsBinding: ActivityMainBinding? = null
    private var testViewModel:TestViewModel? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        activityGirlsBinding =
            DataBindingUtil.setContentView(this@MainActivity, R.layout.activity_main)
        activityGirlsBinding?.lifecycleOwner = this
        testViewModel =
            ViewModelProviders.of(this).get(TestViewModel::class.java)

        activityGirlsBinding?.model = testViewModel
        testViewModel?._testParam?.observe(this, Observer {
            Log.i("test", "testViewModel change："+it)
        })


        val parm = TestParm()
        parm.parm2 = "this test"
        activityGirlsBinding?.parm = parm
        val thread = object : Thread() {
            override fun run() {
//                var i = 1
//                while (true) {
//                    SystemClock.sleep(1000)
//                    Log.i("test", "loglgogogogogogogogogoggo"+i)
//                    i++
//                }
                //SystemClock.sleep(10000)
                getTopApp()
            }
        }
        //thread.start()

        getTopApp()
        appCopy = findViewById(R.id.button)
        appCopy?.setOnClickListener(View.OnClickListener {
            //SystemClock.sleep(10000000)
//            val test :TestWrapper = TestWrapper()
//            test.test()
//            test.test1()
            //ARouter.getInstance().build("/test/login").navigation()

            testViewModel?.setParmByClick()

//            val thread = object : Thread() {
//                override fun run() {
//                    SystemClock.sleep(10000)
//                    getTopApp()
//                }
//            }
//            thread.start()

            test(null)
        })

    }

    fun test(str:String?) {
        str.let {
            Log.d("test", "test ——————str: $it")
        }
        str?.let {
            Log.d("test", "test ——————str: $it")
        }
//        val length = str!!.length;
        val length = str?.length;
        Log.d("test", "test ——————length: $length")
    }
    fun getTopApp() {
        Log.d("test", "getTopApp ——————begin******hasPermission： "+ hasPermission())
        if(!hasPermission()) {
            startActivityForResult(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
                MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS
            )
        }
        val usage = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usage.queryUsageStats(UsageStatsManager.INTERVAL_BEST, time - 60 * 1000, time)
        var topActivity = ""
        //取得最近运行的一个app，即当前运行的app
        Log.i("test", "getTopApp Running app number in last 60 seconds : ${stats.size}")
        if (stats != null && stats.isNotEmpty()) {
            var j = 0
            for (i in stats.indices) {
                if (stats[i].lastTimeUsed > stats[j].lastTimeUsed) {
                    j = i
                }
            }
            topActivity = stats[j].packageName
        }
        Log.i("test", "getTopApp ——————end top running app is : $topActivity")

    }


    private fun hasPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        var mode = 0
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private val MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS = 1101
}
