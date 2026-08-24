package com.example.vivo200mpprobe

import android.content.Context
import android.os.Process
import androidx.annotation.Keep
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

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

    override fun exec(command: String): String {

        return try {

            val shellProcess =
                ProcessBuilder(
                    "/system/bin/sh",
                    "-c",
                    command
                )
                    .redirectErrorStream(true)
                    .start()

            val reader =
                BufferedReader(
                    InputStreamReader(
                        shellProcess.inputStream
                    )
                )

            val result =
                StringBuilder()

            var line: String?

            while (
                reader.readLine()
                    .also { line = it } != null
            ) {

                result.append(line)
                result.append('\n')
            }

            val finished =
                shellProcess.waitFor(
                    25,
                    TimeUnit.SECONDS
                )

            if (!finished) {

                shellProcess.destroyForcibly()

                result.append(
                    "\n[TIMEOUT - PROCESS KILLED]\n"
                )
            }

            result.toString()

        } catch (e: Throwable) {

            "ERROR: ${e.javaClass.name}: ${e.message}"
        }
    }
}
