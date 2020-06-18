package com.deepblue.librariesx

class TestWrapper (val imp : TestImp = TestImp()):ITest by imp {

}