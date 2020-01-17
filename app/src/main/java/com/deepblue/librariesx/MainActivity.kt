package com.deepblue.librariesx

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Button
import com.deepblue.logd.Log


class MainActivity : AppCompatActivity() {
    private var appCopy: Button? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val thread = object : Thread() {
            override fun run() {
                var i = 1
                while (true) {
                    SystemClock.sleep(1000)
                    Log.i("test", "loglgogogogogogogogogoggo"+i)
                    i++
                }
            }
        }
        thread.start()


        appCopy = findViewById(R.id.button)
        appCopy?.setOnClickListener(View.OnClickListener {
            SystemClock.sleep(10000000)
        })
    }
}
