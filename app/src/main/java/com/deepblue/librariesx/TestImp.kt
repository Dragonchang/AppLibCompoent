package com.deepblue.librariesx

import android.util.Log
import com.deepblue.librariesx.bean.TestParm
import com.google.gson.Gson

class TestImp :ITest{

    override fun test() {
        Log.d("TestImp", "this is TestImp test function")
        val parm = TestParm()
        parm.parm1 = 2
        parm.parm2 = "this test param"

        val gson = Gson()
        val param = gson.toJson(parm)
        Log.d("TestImp", "param = "+param)
    }
    override fun test1() {
        Log.d("TestImp","this is TestImp test1 function")
    }
}