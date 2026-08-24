package com.example.vivo200mpprobe

import android.content.Context
import android.os.Process
import androidx.annotation.Keep

class ShellUserService : ICommandService.Stub() {

    constructor()

    @Keep
    constructor(context: Context)

    override fun getUid(): Int {
        return Process.myUid()
    }

    override fun getPid(): Int {
        return Process.myPid()
    }
}
