package com.example.vivo200mpprobe

class NativeProbe {

    companion object {

        init {
            System.loadLibrary("native-lib")
        }
    }

    external fun runNativeProbe(): String
}
