package com.example.vivo200mpprobe

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    companion object {
        private const val SHIZUKU_PERMISSION_CODE = 1001
    }

    private lateinit var outputText: TextView

    // ============================================================
    // SHIZUKU LISTENERS
    // ============================================================

    private val binderReceivedListener =
        Shizuku.OnBinderReceivedListener {

            runOnUiThread {
                log("")
                log("SHIZUKU BINDER RECEIVED")
                checkShizuku()
            }
        }

    private val binderDeadListener =
        Shizuku.OnBinderDeadListener {

            runOnUiThread {
                log("")
                log("SHIZUKU BINDER DISCONNECTED")
            }
        }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener {
                requestCode,
                grantResult ->

            if (requestCode == SHIZUKU_PERMISSION_CODE) {

                runOnUiThread {

                    log("")
                    log("==============================")
                    log("SHIZUKU PERMISSION RESULT")
                    log("==============================")

                    if (
                        grantResult ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        log("Permission = GRANTED")
                    } else {
                        log("Permission = DENIED")
                    }

                    checkShizuku()
                }
            }
        }

    // ============================================================
    // ACTIVITY
    // ============================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        buildUi()

        try {

            Shizuku.addBinderReceivedListenerSticky(
                binderReceivedListener
            )

            Shizuku.addBinderDeadListener(
                binderDeadListener
            )

            Shizuku.addRequestPermissionResultListener(
                permissionResultListener
            )

        } catch (e: Throwable) {

            log("Shizuku listener setup error:")
            log("${e.javaClass.name}: ${e.message}")
        }

        log("VIVO CAMERA SHIZUKU PROBE")
        log("==========================")
        log("")
        log("No AIDL/UserService is used.")
        log("Commands execute through the")
        log("Shizuku remote-process interface.")
        log("")
        log("Start with:")
        log("1 - CHECK SHIZUKU")
    }

    // ============================================================
    // UI
    // ============================================================

    private fun buildUi() {

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    18,
                    18,
                    18,
                    18
                )
            }

        fun addButton(
            textValue: String,
            action: () -> Unit
        ) {

            val button =
                Button(this).apply {

                    text = textValue

                    setOnClickListener {
                        action()
                    }
                }

            root.addView(
                button,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        addButton(
            "1 - CHECK SHIZUKU"
        ) {
            checkShizuku()
        }

        addButton(
            "2 - REQUEST SHIZUKU ACCESS"
        ) {
            requestShizukuPermission()
        }

        addButton(
            "3 - TEST SHELL"
        ) {
            testShell()
        }

        addButton(
            "4 - RUN CAMERA DIAGNOSTICS"
        ) {
            runCameraDiagnostics()
        }

        addButton(
            "5 - DUMP VIVO CAMERA PACKAGE"
        ) {
            dumpVivoCameraPackage()
        }

        addButton(
            "6 - SCAN CAMERA FILESYSTEM"
        ) {
            scanCameraFilesystem()
        }

        addButton(
            "7 - CAMERA PROPERTIES"
        ) {
            dumpCameraProperties()
        }

        addButton(
            "8 - RECENT CAMERA LOGCAT"
        ) {
            recentCameraLogcat()
        }

        addButton(
            "9 - CLEAR LOGCAT"
        ) {
            clearLogcat()
        }

        addButton(
            "10 - START 200MP LOG WATCH"
        ) {
            start200MpWatch()
        }

        addButton(
            "OPEN VIVO CAMERA"
        ) {
            openVivoCamera()
        }

        addButton(
            "COPY OUTPUT"
        ) {
            copyOutput()
        }

        addButton(
            "CLEAR OUTPUT"
        ) {
            outputText.text = ""
        }

        outputText =
            TextView(this).apply {

                textSize = 13f

                setTextIsSelectable(true)

                setPadding(
                    4,
                    20,
                    4,
                    120
                )
            }

        root.addView(
            outputText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val scroll =
            ScrollView(this).apply {
                addView(root)
            }

        setContentView(scroll)
    }

    // ============================================================
    // SHIZUKU STATUS
    // ============================================================

    private fun checkShizuku() {

        log("")
        log("==============================")
        log("SHIZUKU STATUS")
        log("==============================")

        try {

            val alive =
                Shizuku.pingBinder()

            log("Binder alive = $alive")

            if (!alive) {

                log("Shizuku is NOT connected.")
                return
            }

            log("Shizuku is RUNNING.")
            log("Version = ${Shizuku.getVersion()}")
            log("Server UID = ${Shizuku.getUid()}")

            val permission =
                Shizuku.checkSelfPermission()

            if (
                permission ==
                PackageManager.PERMISSION_GRANTED
            ) {

                log("App permission = GRANTED")

            } else {

                log("App permission = NOT GRANTED")
            }

            when (Shizuku.getUid()) {

                2000 ->
                    log("Backend = ADB / SHELL")

                0 ->
                    log("Backend = ROOT")

                else ->
                    log(
                        "Backend UID = ${Shizuku.getUid()}"
                    )
            }

        } catch (e: Throwable) {

            log("SHIZUKU ERROR:")
            log("${e.javaClass.name}: ${e.message}")
        }
    }

    // ============================================================
    // PERMISSION
    // ============================================================

    private fun requestShizukuPermission() {

        log("")
        log("==============================")
        log("REQUEST SHIZUKU ACCESS")
        log("==============================")

        try {

            if (!Shizuku.pingBinder()) {

                log("Shizuku is not running.")
                return
            }

            if (
                Shizuku.checkSelfPermission() ==
                PackageManager.PERMISSION_GRANTED
            ) {

                log("Permission already granted.")
                return
            }

            Shizuku.requestPermission(
                SHIZUKU_PERMISSION_CODE
            )

            log("Permission request sent.")

        } catch (e: Throwable) {

            log("Permission request error:")
            log("${e.javaClass.name}: ${e.message}")
        }
    }

    // ============================================================
    // SHIZUKU REMOTE PROCESS
    // ============================================================

    private fun createShizukuProcess(
        command: String
    ): ShizukuRemoteProcess {

        /*
         * Shizuku.newProcess(...) exists in API 13,
         * but is not publicly exposed in the current source.
         *
         * We call it reflectively.
         */

        val method =
            Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )

        method.isAccessible = true

        val commandArray =
            arrayOf(
                "/system/bin/sh",
                "-c",
                command
            )

        @Suppress("UNCHECKED_CAST")
        return method.invoke(
            null,
            commandArray,
            null,
            null
        ) as ShizukuRemoteProcess
    }

    // ============================================================
    // RUN COMMAND
    // ============================================================

    private fun runShellCommand(
        title: String,
        command: String
    ) {

        log("")
        log("==============================")
        log(title)
        log("==============================")

        if (!ensureShizukuReady()) {
            return
        }

        log("")
        log("$ $command")
        log("")

        Thread {

            var process:
                ShizukuRemoteProcess? = null

            try {

                process =
                    createShizukuProcess(
                        command
                    )

                val stdoutReader =
                    BufferedReader(
                        InputStreamReader(
                            process.inputStream
                        )
                    )

                val stderrReader =
                    BufferedReader(
                        InputStreamReader(
                            process.errorStream
                        )
                    )

                val stdout =
                    stdoutReader.readText()

                val stderr =
                    stderrReader.readText()

                val exitCode =
                    process.waitFor()

                runOnUiThread {

                    if (stdout.isNotBlank()) {
                        log(stdout.trimEnd())
                    }

                    if (stderr.isNotBlank()) {

                        log("")
                        log("--- STDERR ---")
                        log(stderr.trimEnd())
                    }

                    log("")
                    log("Exit code = $exitCode")
                }

            } catch (e: Throwable) {

                runOnUiThread {

                    log("")
                    log("COMMAND FAILED")
                    log("${e.javaClass.name}: ${e.message}")

                    e.cause?.let {

                        log(
                            "Cause: ${it.javaClass.name}: ${it.message}"
                        )
                    }
                }

            } finally {

                try {
                    process?.destroy()
                } catch (_: Throwable) {
                }
            }

        }.start()
    }

    private fun ensureShizukuReady(): Boolean {

        try {

            if (!Shizuku.pingBinder()) {

                log("ERROR: Shizuku binder is not alive.")
                return false
            }

            if (
                Shizuku.checkSelfPermission() !=
                PackageManager.PERMISSION_GRANTED
            ) {

                log(
                    "ERROR: Shizuku permission has not been granted."
                )

                log(
                    "Press 2 - REQUEST SHIZUKU ACCESS."
                )

                return false
            }

            return true

        } catch (e: Throwable) {

            log(
                "Shizuku readiness check failed:"
            )

            log(
                "${e.javaClass.name}: ${e.message}"
            )

            return false
        }
    }

    // ============================================================
    // TEST SHELL
    // ============================================================

    private fun testShell() {

        runShellCommand(
            "TEST SHELL ACCESS",
            """
            echo "===== WHOAMI ====="
            whoami

            echo ""
            echo "===== ID ====="
            id

            echo ""
            echo "===== PROCESS ====="
            echo $$
            """.trimIndent()
        )
    }

    // ============================================================
    // CAMERA DIAGNOSTICS
    // ============================================================

    private fun runCameraDiagnostics() {

        runShellCommand(
            "CAMERA DIAGNOSTICS",
            """
            echo "===== IDENTITY ====="
            id

            echo ""
            echo "===== CAMERA SERVICE ====="
            dumpsys media.camera 2>&1

            echo ""
            echo "===== CAMERA PROCESSES ====="
            ps -A | grep -Ei 'camera|vcf|vif|vivo|mtk|mediatek'

            echo ""
            echo "===== CAMERA SERVICES ====="
            service list | grep -Ei 'camera|media'
            """.trimIndent()
        )
    }

    // ============================================================
    // PACKAGE DUMP
    // ============================================================

    private fun dumpVivoCameraPackage() {

        runShellCommand(
            "VIVO CAMERA PACKAGE",
            """
            echo "===== PACKAGE ====="
            dumpsys package com.android.camera

            echo ""
            echo "===== APK PATH ====="
            pm path com.android.camera

            echo ""
            echo "===== UID / PERMISSIONS ====="
            dumpsys package com.android.camera \
            | grep -Ei \
            'userId|uid=|sharedUser|permission|SYSTEM_CAMERA|WRITE_SECURE|signature'
            """.trimIndent()
        )
    }

    // ============================================================
    // CAMERA FILESYSTEM
    // ============================================================

    private fun scanCameraFilesystem() {

        runShellCommand(
            "CAMERA FILESYSTEM SCAN",
            """
            echo "===== DATA VENDOR CAMERA ====="

            ls -la /data/vendor/camera 2>&1

            echo ""
            echo "===== RECENT DATA/VENDOR CAMERA FILES ====="

            find /data/vendor/camera \
            -type f \
            -mmin -30 \
            -exec ls -lah {} \; \
            2>/dev/null \
            | head -n 500

            echo ""
            echo "===== VENDOR CAMERA CONFIG ====="

            find /vendor/etc \
            -maxdepth 5 \
            \( \
            -iname '*camera*' \
            -o -iname '*sensor*' \
            -o -iname '*raw*' \
            -o -iname '*remosaic*' \
            -o -iname '*vcf*' \
            -o -iname '*vif*' \
            \) \
            2>/dev/null \
            | head -n 1000

            echo ""
            echo "===== ODM CAMERA CONFIG ====="

            find /odm/etc \
            -maxdepth 5 \
            \( \
            -iname '*camera*' \
            -o -iname '*sensor*' \
            -o -iname '*raw*' \
            -o -iname '*remosaic*' \
            -o -iname '*vcf*' \
            -o -iname '*vif*' \
            \) \
            2>/dev/null \
            | head -n 1000

            echo ""
            echo "===== SHARED RAW-LIKE FILES ====="

            find /sdcard \
            -type f \
            \( \
            -iname '*.raw' \
            -o -iname '*.dng' \
            -o -iname '*.yuv' \
            -o -iname '*.bin' \
            -o -iname '*.raw10' \
            -o -iname '*.raw16' \
            \) \
            2>/dev/null \
            | head -n 500
            """.trimIndent()
        )
    }

    // ============================================================
    // CAMERA PROPERTIES
    // ============================================================

    private fun dumpCameraProperties() {

        runShellCommand(
            "CAMERA PROPERTIES",
            """
            getprop \
            | grep -Ei \
            'camera|vivo|sensor|raw|remosaic|vcf|vif|mtk|mediatek'
            """.trimIndent()
        )
    }

    // ============================================================
    // LOGCAT
    // ============================================================

    private fun clearLogcat() {

        runShellCommand(
            "CLEAR LOGCAT",
            """
            logcat -c
            echo "LOGCAT CLEARED"
            """.trimIndent()
        )
    }

    private fun recentCameraLogcat() {

        runShellCommand(
            "RECENT CAMERA LOGCAT",
            """
            logcat -d -v threadtime \
            | grep -Ei \
            '200mp|200 mp|remosaic|rawvif|raw vif|vif|vcf|raw10|raw16|proraw|SaveRaw|SaveProRaw|sensorMode|sensorScenario|fullsize|highresolution|high_resolution|niceCaptureSensorMode|real200mp|camera3|com.android.camera' \
            | tail -n 5000
            """.trimIndent()
        )
    }

    // ============================================================
    // 200 MP LIVE WATCH
    // ============================================================

    private fun start200MpWatch() {

        runShellCommand(
            "200 MP LOG WATCH PREP",
            """
            logcat -c

            echo "LOGCAT CLEARED"
            echo ""
            echo "NOW:"
            echo "1. Open Vivo Camera"
            echo "2. Select 200 MP"
            echo "3. Take ONE photo"
            echo "4. Wait for processing"
            echo "5. Return to probe"
            echo "6. Press RECENT CAMERA LOGCAT"
            """.trimIndent()
        )
    }

    // ============================================================
    // OPEN OEM CAMERA
    // ============================================================

    private fun openVivoCamera() {

        log("")
        log("==============================")
        log("OPEN VIVO CAMERA")
        log("==============================")

        try {

            val intent =
                packageManager
                    .getLaunchIntentForPackage(
                        "com.android.camera"
                    )

            if (intent != null) {

                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )

                startActivity(intent)

                log("Vivo Camera launched.")

                return
            }

            val explicit =
                Intent().apply {

                    setClassName(
                        "com.android.camera",
                        "com.android.camera.CameraActivity"
                    )

                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }

            startActivity(explicit)

            log(
                "Vivo CameraActivity launched."
            )

        } catch (e: Throwable) {

            log("CAMERA LAUNCH ERROR:")
            log("${e.javaClass.name}: ${e.message}")
        }
    }

    // ============================================================
    // COPY
    // ============================================================

    private fun copyOutput() {

        val clipboard =
            getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "Vivo Camera Shizuku Probe",
                outputText.text.toString()
            )
        )

        Toast.makeText(
            this,
            "Output copied",
            Toast.LENGTH_SHORT
        ).show()
    }

    // ============================================================
    // LOG
    // ============================================================

    private fun log(
        message: String
    ) {

        runOnUiThread {

            outputText.append(message)

            if (!message.endsWith("\n")) {
                outputText.append("\n")
            }
        }
    }

    // ============================================================
    // CLEANUP
    // ============================================================

    override fun onDestroy() {

        try {

            Shizuku.removeBinderReceivedListener(
                binderReceivedListener
            )

            Shizuku.removeBinderDeadListener(
                binderDeadListener
            )

            Shizuku.removeRequestPermissionResultListener(
                permissionResultListener
            )

        } catch (_: Throwable) {
        }

        super.onDestroy()
    }
}
