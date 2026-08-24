package com.example.vivo200mpprobe

import android.content.Context
import android.os.RemoteException
import java.io.BufferedReader
import java.io.InputStreamReader

class ShellUserService() : IShellService.Stub() {

    constructor(context: Context) : this()

    @Throws(RemoteException::class)
    override fun exec(command: String): String {

        val result = StringBuilder()

        try {

            val process = Runtime.getRuntime().exec(
                arrayOf(
                    "sh",
                    "-c",
                    command
                )
            )

            val stdout = BufferedReader(
                InputStreamReader(
                    process.inputStream
                )
            )

            val stderr = BufferedReader(
                InputStreamReader(
                    process.errorStream
                )
            )

            var line: String?

            while (stdout.readLine().also { line = it } != null) {
                result.append(line)
                result.append('\n')
            }

            val errorText = StringBuilder()

            while (stderr.readLine().also { line = it } != null) {
                errorText.append(line)
                errorText.append('\n')
            }

            val exitCode = process.waitFor()

            if (errorText.isNotEmpty()) {
                result.append('\n')
                result.append("===== STDERR =====\n")
                result.append(errorText)
            }

            result.append('\n')
            result.append("Exit code = ")
            result.append(exitCode)
            result.append('\n')

        } catch (e: Throwable) {

            result.append("SHELL EXECUTION ERROR\n")
            result.append(e.javaClass.name)
            result.append(": ")
            result.append(e.message ?: "null")
            result.append('\n')

            result.append(
                e.stackTraceToString()
            )
        }

        return result.toString()
    }

    override fun destroy() {
        /*
         * Called by Shizuku when the UserService is destroyed.
         * Nothing special is required here.
         */
    }
}
