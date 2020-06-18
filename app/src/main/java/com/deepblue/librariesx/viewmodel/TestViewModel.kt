package com.deepblue.librariesx.viewmodel


import androidx.databinding.Bindable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TestViewModel : ViewModel(){
    val _testParam = MutableLiveData<String>("init value")
    val parm:String = "second"

    fun setParmByClick() {
        _testParam.postValue("test")
    }

}