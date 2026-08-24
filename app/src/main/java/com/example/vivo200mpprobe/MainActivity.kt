package com.example.vivo200mpprobe

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    companion object {
        private const val VIVO_CAMERA_PACKAGE = "com.android.camera"

        private const val REQUEST_CODE_SHIZUKU = 1001
    }

    private lateinit var output: TextView

    private val executor =
        Executors.newSingleThreadExecutor()

    private var traceStartMs: Long = 0L

    // ============================================================
    // SHIZUKU BINDER LISTENER
    // ============================================================

    private val binderReceivedListener =
        Shizuku.OnBinderReceivedListener {

            log("")
            log("SHIZUKU BINDER RECEIVED")

            showShizukuStatus()
        }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener {
                requestCode,
                grantResult ->

            if (requestCode != REQUEST_CODE_SHIZUKU) {
                return@OnRequestPermissionResultListener
            }

            log("")
            log("==============================")
            log("SHIZUKU PERMISSION RESULT")
            log("==============================")

            log(
                "Granted = ${
                    grantResult ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
                }"
            )

            showShizukuStatus()
        }

    // ============================================================
    // ACTIVITY
    // ============================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        buildUi()

        Shizuku.addBinderReceivedListenerSticky(
            binderReceivedListener
        )

        Shizuku.addRequestPermissionResultListener(
            permissionResultListener
        )

        log("VIVO 200 MP OEM TRACE")
        log("=====================")
        log("")
        log("Purpose:")
        log("Trace the stock Vivo 200 MP pipeline")
        log("using Shizuku shell access.")
        log("")
        log("Recommended workflow:")
        log("1. CHECK SHIZUKU")
        log("2. START CLEAN 200MP TRACE")
        log("3. OPEN VIVO CAMERA")
        log("4. Take ONE 200 MP photo")
        log("5. Return here")
        log("6. COLLECT TRACE")
        log("7. COPY OUTPUT")
        log("")

        showShizukuStatus()
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
                    20,
                    20,
                    20,
                    20
                )
            }

        root.addView(
            makeButton(
                "1 - CHECK SHIZUKU"
            ) {
                showShizukuStatus()
            }
        )

        root.addView(
            makeButton(
                "2 - REQUEST SHIZUKU ACCESS"
            ) {
                requestShizukuPermission()
            }
        )

        root.addView(
            makeButton(
                "3 - TEST SHELL ACCESS"
            ) {
                testShell()
            }
        )

        root.addView(
            makeButton(
                "4 - DUMP VIVO CAMERA SERVICES"
            ) {
                dumpCameraServices()
            }
        )

        root.addView(
            makeButton(
                "5 - DUMP CAMERA PROCESSES"
            ) {
                dumpCameraProcesses()
            }
        )

        root.addView(
            makeButton(
                "6 - DUMP CAMERA HAL / PROVIDERS"
            ) {
                dumpCameraHalProviders()
            }
        )

        root.addView(
            makeButton(
                "7 - DUMP MEDIA.CAMERA"
            ) {
                dumpMediaCamera()
            }
        )

        root.addView(
            makeButton(
                "8 - START CLEAN 200MP TRACE"
            ) {
                startCleanTrace()
            }
        )

        root.addView(
            makeButton(
                "9 - OPEN VIVO CAMERA"
            ) {
                openVivoCamera()
            }
        )

        root.addView(
            makeButton(
                "10 - COLLECT 200MP TRACE"
            ) {
                collect200MpTrace()
            }
        )

        root.addView(
            makeButton(
                "11 - RAW / VIF LOG ONLY"
            ) {
                dumpRawVifLogs()
            }
        )

        root.addView(
            makeButton(
                "12 - MEDIASTORE 200MP CHECK"
            ) {
                dumpRecentCameraMedia()
            }
        )

        root.addView(
            makeButton(
                "COPY OUTPUT"
            ) {
                copyOutput()
            }
        )

        root.addView(
            makeButton(
                "CLEAR OUTPUT"
            ) {
                output.text = ""
            }
        )

        output =
            TextView(this).apply {

                textSize = 13f

                setTextIsSelectable(true)

                setPadding(
                    0,
                    20,
                    0,
                    200
                )
            }

        val scroll =
            ScrollView(this).apply {

                addView(output)
            }

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
    }

    private fun makeButton(
        text: String,
        onClick: () -> Unit
    ): Button {

        return Button(this).apply {

            this.text = text

            setOnClickListener {
                onClick()
            }
        }
    }

    // ============================================================
    // SHIZUKU STATUS
    // ============================================================

    private fun showShizukuStatus() {

        log("")
        log("==============================")
        log("SHIZUKU STATUS")
        log("==============================")

        try {

            val alive =
                Shizuku.pingBinder()

            log("Binder alive = $alive")

            if (!alive) {

                log("Shizuku is NOT running.")
                return
            }

            log("Shizuku is RUNNING.")

            try {
                log(
                    "Version = ${
                        Shizuku.getVersion()
                    }"
                )
            } catch (_: Throwable) {
            }

            try {
                log(
                    "Server UID = ${
                        Shizuku.getUid()
                    }"
                )
            } catch (_: Throwable) {
            }

            val granted =
                Shizuku.checkSelfPermission() ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED

            log(
                "App permission = ${
                    if (granted)
                        "GRANTED"
                    else
                        "NOT GRANTED"
                }"
            )

            if (granted) {
                log("Backend = ADB / SHELL")
            }

        } catch (e: Throwable) {

            log(
                "Status error: " +
                    "${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }

    private fun requestShizukuPermission() {

        try {

            if (!Shizuku.pingBinder()) {

                log("")
                log("Shizuku binder is not alive.")
                return
            }

            if (
                Shizuku.checkSelfPermission() ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {

                log("")
                log("Shizuku permission already granted.")
                return
            }

            Shizuku.requestPermission(
                REQUEST_CODE_SHIZUKU
            )

        } catch (e: Throwable) {

            log("")
            log(
                "Permission request failed: " +
                    "${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }

    // ============================================================
    // COMMAND EXECUTION
    // ============================================================

    private fun runShellCommand(
        title: String,
        command: String
    ) {

        executor.execute {

            log("")
            log("")
            log("==============================")
            log(title)
            log("==============================")
            log("")
            log("$ $command")
            log("")

            if (!checkShellReady()) {
                return@execute
            }

            try {

                /*
                 * Shizuku.newProcess() executes using the
                 * Shizuku shell-side process.
                 */
                val process =
                    Shizuku.newProcess(
                        arrayOf(
                            "sh",
                            "-c",
                            command
                        ),
                        null,
                        null
                    )

                val stdout =
                    process.inputStream
                        .bufferedReader()
                        .readText()

                val stderr =
                    process.errorStream
                        .bufferedReader()
                        .readText()

                val exit =
                    process.waitFor()

                if (stdout.isNotBlank()) {
                    log(stdout.trimEnd())
                }

                if (stderr.isNotBlank()) {

                    log("")
                    log("----- STDERR -----")
                    log(stderr.trimEnd())
                }

                log("")
                log("Exit code = $exit")

            } catch (e: Throwable) {

                log("")
                log(
                    "COMMAND ERROR: " +
                        "${e.javaClass.simpleName}: ${e.message}"
                )
            }
        }
    }

    private fun checkShellReady(): Boolean {

        try {

            if (!Shizuku.pingBinder()) {

                log("ERROR: Shizuku not running.")
                return false
            }

            if (
                Shizuku.checkSelfPermission() !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {

                log(
                    "ERROR: Shizuku permission not granted."
                )

                return false
            }

            return true

        } catch (e: Throwable) {

            log(
                "Shizuku readiness error: " +
                    "${e.javaClass.simpleName}: ${e.message}"
            )

            return false
        }
    }

    // ============================================================
    // BUTTON 3
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
            echo "===== SELINUX ====="
            id -Z 2>/dev/null || true
            echo ""
            echo "===== PROCESS ====="
            echo $$
            """.trimIndent()
        )
    }

    // ============================================================
    // BUTTON 4
    // ============================================================

    private fun dumpCameraServices() {

        runShellCommand(
            "VIVO CAMERA SERVICES",
            """
            echo "===== CAMERA/VIVO SERVICES ====="
            service list 2>/dev/null \
            | grep -Ei 'camera|vivo|vcf|media'
            
            echo ""
            echo "===== DUMPSYS SERVICES ====="
            dumpsys -l 2>/dev/null \
            | grep -Ei 'camera|vivo|media'
            """.trimIndent()
        )
    }

    // ============================================================
    // BUTTON 5
    // ============================================================

    private fun dumpCameraProcesses() {

        runShellCommand(
            "CAMERA / VIVO PROCESSES",
            """
            echo "===== PS ====="
            ps -A -o USER,PID,PPID,NAME,ARGS 2>/dev/null \
            | grep -Ei 'camera|vivo|mediatek|mtk|isp|provider|vcf'
            
            echo ""
            echo "===== PIDS ====="
            pidof com.android.camera 2>/dev/null || true
            pidof cameraserver 2>/dev/null || true
            """.trimIndent()
        )
    }

    // ============================================================
    // BUTTON 6
    // ============================================================

    private fun dumpCameraHalProviders() {

        runShellCommand(
            "CAMERA HAL / PROVIDERS",
            """
            echo "===== SERVICE LIST ====="
            service list 2>/dev/null \
            | grep -Ei 'camera|vivo'
            
            echo ""
            echo "===== LSHAL ====="
            lshal 2>/dev/null \
            | grep -Ei 'camera|vivo|mediatek|mtk'
            
            echo ""
            echo "===== AIDL/HAL SERVICES ====="
            ps -A 2>/dev/null \
            | grep -Ei 'camera-provider|cameraprovider|camera.provider|vivo.*camera|camera.*vivo'
            
            echo ""
            echo "===== EXPECTED VIVO PROVIDERS ====="
            service list 2>/dev/null \
            | grep -Ei 'IVivoCameraProvider|ICameraLogSystem'
            """.trimIndent()
        )
    }

    // ============================================================
    // BUTTON 7
    // ============================================================

    private fun dumpMediaCamera() {

        runShellCommand(
            "DUMPSYS MEDIA.CAMERA",
            """
            echo "===== MEDIA.CAMERA ====="
            dumpsys media.camera 2>&1
            
            echo ""
            echo "===== MEDIA.CAMERA.PROXY ====="
            dumpsys media.camera.proxy 2>&1
            """.trimIndent()
        )
    }

    // ============================================================
    // BUTTON 8
    // ============================================================

    private fun startCleanTrace() {

        traceStartMs =
            System.currentTimeMillis()

        val timestamp =
            traceStartMs

        runShellCommand(
            "START CLEAN 200 MP TRACE",
            """
            logcat -b all -c
            
            echo "ALL LOGCAT BUFFERS CLEARED"
            echo ""
            echo "TRACE START MS:"
            echo "$timestamp"
            echo ""
            echo "NOW:"
            echo "1. Press OPEN VIVO CAMERA"
            echo "2. Select 200 MP"
            echo "3. Take ONE photo"
            echo "4. Wait 10-15 seconds"
            echo "5. Return to probe"
            echo "6. Press COLLECT 200MP TRACE"
            """.trimIndent()
        )
    }

    // ============================================================
    // BUTTON 9
    // ============================================================

    private fun openVivoCamera() {

        log("")
        log("==============================")
        log("OPENING VIVO CAMERA")
        log("==============================")

        try {

            val intent =
                packageManager.getLaunchIntentForPackage(
                    VIVO_CAMERA_PACKAGE
                )

            if (intent != null) {

                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )

                startActivity(intent)

                log("OEM camera launch intent sent.")
                return
            }

        } catch (e: Throwable) {

            log(
                "Package launch failed: " +
                    "${e.javaClass.simpleName}: ${e.message}"
            )
        }

        try {

            val explicit =
                Intent().apply {

                    setClassName(
                        VIVO_CAMERA_PACKAGE,
                        "com.android.camera.CameraActivity"
                    )

                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }

            startActivity(explicit)

            log("CameraActivity launched.")

        } catch (e: Throwable) {

            log(
                "FAILED TO LAUNCH CAMERA: " +
                    "${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }

    // ============================================================
    // BUTTON 10
    // ============================================================

    private fun collect200MpTrace() {

        runShellCommand(
            "200 MP OEM CAPTURE TRACE",
            """
            echo "================================"
            echo "CAMERA PROCESSES"
            echo "================================"
            
            ps -A -o USER,PID,PPID,NAME,ARGS 2>/dev/null \
            | grep -Ei 'camera|vivo|mediatek|mtk|isp|vcf|provider'
            
            echo ""
            echo ""
            echo "================================"
            echo "CAMERA/VIVO SERVICES"
            echo "================================"
            
            service list 2>/dev/null \
            | grep -Ei 'camera|vivo|vcf'
            
            echo ""
            echo ""
            echo "================================"
            echo "CAMERA HAL"
            echo "================================"
            
            lshal 2>/dev/null \
            | grep -Ei 'camera|vivo|mediatek|mtk'
            
            echo ""
            echo ""
            echo "================================"
            echo "MEDIA CAMERA STATE"
            echo "================================"
            
            dumpsys media.camera 2>&1
            
            echo ""
            echo ""
            echo "================================"
            echo "FULL RELEVANT LOGCAT"
            echo "================================"
            
            logcat -b all -d -v threadtime 2>/dev/null \
            | grep -Ei \
            'camera|vivo|mediatek|mtk|vcf|vif|raw|remosaic|jpeg|capture|sensor|isp|p1|p2|mfnr|mcnr|algo|hal|provider|200mp|200 mp|fullsize|highresolution|high_resolution|proraw|raw10|raw16|dng' \
            | tail -n 20000
            """.trimIndent()
        )
    }

    // ============================================================
    // BUTTON 11
    // ============================================================

    private fun dumpRawVifLogs() {

        runShellCommand(
            "RAW / VIF / REMOSAIC LOGS",
            """
            logcat -b all -d -v threadtime 2>/dev/null \
            | grep -Ei \
            'rawvif|raw vif|vif|vcf|remosaic|raw10|raw16|proraw|dng|SaveRaw|SaveProRaw|YuvVif|sensorMode|niceCaptureSensorMode|real200mp|advance_fullsize|ultra_highresolution|highResolutionDngType|raw_capture_type|sensorScenario' \
            | tail -n 15000
            """.trimIndent()
        )
    }

    // ============================================================
    // BUTTON 12
    // ============================================================

    private fun dumpRecentCameraMedia() {

        runShellCommand(
            "RECENT CAMERA MEDIASTORE",
            """
            echo "===== RECENT MEDIASTORE CAMERA FILES ====="
            
            content query \
            --uri content://media/external/images/media \
            --projection _id:_display_name:_size:width:height:mime_type:date_added:date_modified:owner_package_name \
            --sort "date_added DESC" \
            2>&1 \
            | head -n 40
            
            echo ""
            echo "===== DCIM CAMERA FILES ====="
            
            ls -lah /sdcard/DCIM/Camera 2>&1 \
            | tail -n 30
            """.trimIndent()
        )
    }

    // ============================================================
    // LOGGING
    // ============================================================

    private fun log(
        text: String
    ) {

        runOnUiThread {

            output.append(text)
            output.append("\n")
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
                "Vivo 200 MP OEM Trace",
                output.text.toString()
            )
        )

        Toast.makeText(
            this,
            "Output copied",
            Toast.LENGTH_SHORT
        ).show()
    }

    // ============================================================
    // CLEANUP
    // ============================================================

    override fun onDestroy() {

        try {

            Shizuku.removeBinderReceivedListener(
                binderReceivedListener
            )

            Shizuku.removeRequestPermissionResultListener(
                permissionResultListener
            )

        } catch (_: Throwable) {
        }

        executor.shutdownNow()

        super.onDestroy()
    }
}
