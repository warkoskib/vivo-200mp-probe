package com.example.vivo200mpprobe

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku

class MainActivity : Activity() {

    companion object {
        private const val SHIZUKU_PERMISSION_CODE = 1001
    }

    private lateinit var output: TextView

    private var commandService: ICommandService? = null

    private val userServiceArgs by lazy {

        Shizuku.UserServiceArgs(
            ComponentName(
                packageName,
                ShellUserService::class.java.name
            )
        )
            .daemon(false)
            .processNameSuffix("camera_shell")
            .debuggable(BuildConfig.DEBUG)
            .version(1)
            .tag("vivo_camera_probe_shell")
    }

    private val binderReceivedListener =
        Shizuku.OnBinderReceivedListener {

            runOnUiThread {

                log("")
                log("SHIZUKU BINDER RECEIVED")

                showShizukuStatus()
            }
        }

    private val binderDeadListener =
        Shizuku.OnBinderDeadListener {

            commandService = null

            runOnUiThread {

                log("")
                log("SHIZUKU BINDER DIED")
            }
        }

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener {
                requestCode,
                grantResult ->

            if (
                requestCode ==
                SHIZUKU_PERMISSION_CODE
            ) {

                runOnUiThread {

                    log("")
                    log("SHIZUKU PERMISSION RESULT")

                    log(
                        "Granted = ${
                            grantResult ==
                                PackageManager.PERMISSION_GRANTED
                        }"
                    )

                    showShizukuStatus()
                }
            }
        }

    private val serviceConnection =
        object :
            ServiceConnection {

            override fun onServiceConnected(
                name: ComponentName?,
                binder: IBinder?
            ) {

                commandService =
                    ICommandService.Stub.asInterface(
                        binder
                    )

                log("")
                log("==============================")
                log("SHIZUKU USER SERVICE CONNECTED")
                log("==============================")

                try {

                    log(
                        "Service UID = " +
                            commandService!!.uid
                    )

                    log(
                        "Service PID = " +
                            commandService!!.pid
                    )

                    if (
                        commandService!!.uid == 2000
                    ) {

                        log(
                            "SUCCESS: RUNNING AS SHELL UID 2000"
                        )

                    } else {

                        log(
                            "NOTE: SERVICE UID IS NOT 2000"
                        )
                    }

                } catch (e: Throwable) {

                    log(
                        "Service identity read error:"
                    )

                    log(
                        "${e.javaClass.simpleName}: ${e.message}"
                    )
                }
            }

            override fun onServiceDisconnected(
                name: ComponentName?
            ) {

                commandService = null

                log("")
                log(
                    "Shizuku UserService disconnected."
                )
            }
        }

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
                permissionListener
            )

        } catch (e: Throwable) {

            log("")
            log("SHIZUKU LISTENER SETUP ERROR")
            log(
                "${e.javaClass.simpleName}: ${e.message}"
            )
        }

        log(
            "VIVO SHIZUKU CAMERA DIAGNOSTICS"
        )

        log(
            "================================"
        )

        log("")
        log(
            "This app runs shell-level diagnostics"
        )

        log(
            "through Shizuku."
        )

        showShizukuStatus()
    }

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

        fun makeButton(
            label: String,
            action: () -> Unit
        ): Button {

            return Button(this).apply {

                text = label

                setOnClickListener {
                    action()
                }
            }
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
                "3 - CONNECT SHELL SERVICE"
            ) {
                bindShellService()
            }
        )

        root.addView(
            makeButton(
                "4 - RUN CAMERA DIAGNOSTICS"
            ) {
                runCameraDiagnostics()
            }
        )

        root.addView(
            makeButton(
                "5 - DUMP VIVO CAMERA PACKAGE"
            ) {
                dumpVivoCameraPackage()
            }
        )

        root.addView(
            makeButton(
                "6 - SCAN CAMERA FILESYSTEM"
            ) {
                scanCameraFilesystem()
            }
        )

        root.addView(
            makeButton(
                "7 - CAMERA PROPERTIES"
            ) {
                dumpCameraProperties()
            }
        )

        root.addView(
            makeButton(
                "8 - RECENT CAMERA LOGCAT"
            ) {
                captureCameraLogcat()
            }
        )

        root.addView(
            makeButton(
                "9 - CLEAR LOGCAT"
            ) {
                clearLogcat()
            }
        )

        root.addView(
            makeButton(
                "OPEN VIVO CAMERA"
            ) {
                launchVivoCamera()
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

                textSize = 12f

                setTextIsSelectable(
                    true
                )

                setPadding(
                    0,
                    15,
                    0,
                    150
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

    private fun showShizukuStatus() {

        log("")
        log("==============================")
        log("SHIZUKU STATUS")
        log("==============================")

        try {

            val alive =
                Shizuku.pingBinder()

            log(
                "Binder alive = $alive"
            )

            if (!alive) {

                log(
                    "Shizuku is not running."
                )

                return
            }

            log(
                "Shizuku version = " +
                    Shizuku.getVersion()
            )

            log(
                "Shizuku UID = " +
                    Shizuku.getUid()
            )

            val permission =
                Shizuku.checkSelfPermission()

            log(
                "App permission = ${
                    if (
                        permission ==
                        PackageManager.PERMISSION_GRANTED
                    )
                        "GRANTED"
                    else
                        "NOT GRANTED"
                }"
            )

            if (
                Shizuku.getUid() == 2000
            ) {

                log(
                    "Backend = ADB / SHELL"
                )
            }

        } catch (e: Throwable) {

            log(
                "Shizuku error:"
            )

            log(
                "${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }

    private fun requestShizukuPermission() {

        try {

            if (!Shizuku.pingBinder()) {

                log(
                    "Shizuku isn't running."
                )

                return
            }

            if (
                Shizuku.checkSelfPermission() ==
                PackageManager.PERMISSION_GRANTED
            ) {

                log(
                    "Shizuku permission already granted."
                )

                return
            }

            Shizuku.requestPermission(
                SHIZUKU_PERMISSION_CODE
            )

            log(
                "Permission request sent."
            )

        } catch (e: Throwable) {

            log(
                "Permission request failed:"
            )

            log(
                "${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }

    private fun bindShellService() {

        try {

            if (!Shizuku.pingBinder()) {

                log(
                    "Shizuku is not running."
                )

                return
            }

            if (
                Shizuku.checkSelfPermission() !=
                PackageManager.PERMISSION_GRANTED
            ) {

                log(
                    "Grant Shizuku permission first."
                )

                return
            }

            log("")
            log(
                "Starting Shizuku UserService..."
            )

            Shizuku.bindUserService(
                userServiceArgs,
                serviceConnection
            )

        } catch (e: Throwable) {

            log(
                "UserService bind failed:"
            )

            log(
                "${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }

    private fun shell(
        command: String
    ) {

        val service =
            commandService

        if (service == null) {

            log("")
            log(
                "Shell service is not connected."
            )

            log(
                "Press 3 - CONNECT SHELL SERVICE first."
            )

            return
        }

        log("")
        log(
            "$ $command"
        )

        Thread {

            try {

                val result =
                    service.exec(
                        command
                    )

                runOnUiThread {

                    log(result)
                }

            } catch (e: Throwable) {

                runOnUiThread {

                    log(
                        "COMMAND ERROR:"
                    )

                    log(
                        "${e.javaClass.simpleName}: ${e.message}"
                    )
                }
            }

        }.start()
    }

    private fun runCameraDiagnostics() {

        shell(
            """
            echo "===== IDENTITY ====="
            id

            echo ""
            echo "===== CAMERA SERVICE ====="
            dumpsys media.camera 2>&1

            echo ""
            echo "===== CAMERA PROCESSES ====="
            ps -A | grep -Ei 'camera|vivo|vcf|mtk'

            echo ""
            echo "===== CAMERA / MEDIA SERVICES ====="
            service list | grep -Ei 'camera|media'
            """.trimIndent()
        )
    }

    private fun dumpVivoCameraPackage() {

        shell(
            """
            echo "===== VIVO CAMERA PACKAGE ====="
            dumpsys package com.android.camera

            echo ""
            echo "===== APK PATH ====="
            pm path com.android.camera

            echo ""
            echo "===== CAMERA APP UID / PERMISSIONS ====="
            dumpsys package com.android.camera \
            | grep -Ei 'userId|sharedUser|uid|SYSTEM_CAMERA|WRITE_SECURE|signature|permission'
            """.trimIndent()
        )
    }

    private fun scanCameraFilesystem() {

        shell(
            """
            echo "===== VENDOR CAMERA FILES ====="

            find /vendor/etc -maxdepth 4 \
            \( -iname '*camera*' \
            -o -iname '*sensor*' \
            -o -iname '*raw*' \
            -o -iname '*remosaic*' \
            -o -iname '*vcf*' \
            -o -iname '*vif*' \) \
            2>/dev/null

            echo ""
            echo "===== ODM CAMERA FILES ====="

            find /odm/etc -maxdepth 5 \
            \( -iname '*camera*' \
            -o -iname '*sensor*' \
            -o -iname '*raw*' \
            -o -iname '*remosaic*' \
            -o -iname '*vcf*' \
            -o -iname '*vif*' \) \
            2>/dev/null

            echo ""
            echo "===== DATA VENDOR CAMERA ====="

            find /data/vendor/camera \
            -maxdepth 4 \
            -type f \
            2>/dev/null \
            | head -n 2000
            """.trimIndent()
        )
    }

    private fun dumpCameraProperties() {

        shell(
            """
            echo "===== CAMERA / VIVO PROPERTIES ====="

            getprop \
            | grep -Ei \
            'camera|vivo|sensor|remosaic|raw|vcf|vif|mtk|mediatek'
            """.trimIndent()
        )
    }

    private fun clearLogcat() {

        shell(
            """
            logcat -c
            echo "LOGCAT CLEARED"
            """.trimIndent()
        )
    }

    private fun captureCameraLogcat() {

        shell(
            """
            echo "===== RECENT CAMERA LOGCAT ====="

            logcat -d -v threadtime \
            | grep -Ei \
            'camera|vivo|vcf|vif|remosaic|raw16|raw10|200mp|fullsize|SaveRaw|ProRaw|sensorMode|highresolution|high_resolution|jpeg|dng' \
            | tail -n 3500
            """.trimIndent()
        )
    }

    private fun launchVivoCamera() {

        try {

            val intent =
                packageManager
                    .getLaunchIntentForPackage(
                        "com.android.camera"
                    )

            if (intent != null) {

                startActivity(
                    intent
                )

                log(
                    "Vivo Camera launched."
                )

            } else {

                val fallback =
                    Intent().apply {

                        setClassName(
                            "com.android.camera",
                            "com.android.camera.CameraActivity"
                        )
                    }

                startActivity(
                    fallback
                )

                log(
                    "Vivo CameraActivity launched."
                )
            }

        } catch (e: Throwable) {

            log(
                "Camera launch failed:"
            )

            log(
                "${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }

    private fun copyOutput() {

        val clipboard =
            getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "Vivo Shizuku Camera Diagnostics",
                output.text.toString()
            )
        )

        Toast.makeText(
            this,
            "Copied",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun log(
        message: String
    ) {

        runOnUiThread {

            output.append(
                message
            )

            if (
                !message.endsWith(
                    "\n"
                )
            ) {
                output.append(
                    "\n"
                )
            }
        }
    }

    override fun onDestroy() {

        try {

            Shizuku.removeBinderReceivedListener(
                binderReceivedListener
            )

        } catch (_: Throwable) {
        }

        try {

            Shizuku.removeBinderDeadListener(
                binderDeadListener
            )

        } catch (_: Throwable) {
        }

        try {

            Shizuku.removeRequestPermissionResultListener(
                permissionListener
            )

        } catch (_: Throwable) {
        }

        super.onDestroy()
    }
}
