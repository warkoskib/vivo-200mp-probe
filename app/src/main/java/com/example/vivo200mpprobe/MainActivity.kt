package com.example.vivo200mpprobe

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
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    // ============================================================
    // UI
    // ============================================================

    private lateinit var outputText: TextView

    private val output = StringBuilder()

    // ============================================================
    // SHIZUKU / USER SERVICE
    // ============================================================

    private var shellService: IShellService? = null
    private var serviceBound = false

    private val permissionRequestCode = 1001

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(
                packageName,
                ShellUserService::class.java.name
            )
        )
            .daemon(false)
            .processNameSuffix("shell")
            .debuggable(BuildConfig.DEBUG)
            .version(1)
    }

    // ============================================================
    // SHIZUKU BINDER LISTENER
    // ============================================================

    private val binderReceivedListener =
        Shizuku.OnBinderReceivedListener {

            append(
                """
                
                SHIZUKU BINDER RECEIVED
                """.trimIndent()
            )

            showShizukuStatus()
        }

    private val binderDeadListener =
        Shizuku.OnBinderDeadListener {

            append(
                """
                
                !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                SHIZUKU BINDER DIED
                !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                """.trimIndent()
            )

            shellService = null
            serviceBound = false
        }

    // ============================================================
    // SHIZUKU PERMISSION
    // ============================================================

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->

            if (requestCode != permissionRequestCode) {
                return@OnRequestPermissionResultListener
            }

            append("")
            append("==============================")
            append("SHIZUKU PERMISSION RESULT")
            append("==============================")

            if (grantResult == PackageManager.PERMISSION_GRANTED) {

                append("GRANTED")

                showShizukuStatus()

            } else {

                append("DENIED")
            }
        }

    // ============================================================
    // USER SERVICE CONNECTION
    // ============================================================

    private val userServiceConnection =
        object : ServiceConnection {

            override fun onServiceConnected(
                name: ComponentName?,
                binder: IBinder?
            ) {

                append("")
                append("==============================")
                append("SHELL USER SERVICE CONNECTED")
                append("==============================")

                shellService =
                    IShellService.Stub.asInterface(binder)

                serviceBound = shellService != null

                append("Service bound = $serviceBound")

                if (serviceBound) {

                    try {

                        val result =
                            shellService?.exec(
                                """
                                echo "===== USER SERVICE TEST ====="
                                whoami
                                id
                                echo "PID:"
                                echo ${'$'}${'$'}
                                """.trimIndent()
                            )

                        append("")
                        append(result ?: "(no output)")

                    } catch (e: Exception) {

                        append("")
                        append("SERVICE TEST ERROR:")
                        append(e.stackTraceToString())
                    }
                }
            }

            override fun onServiceDisconnected(
                name: ComponentName?
            ) {

                append("")
                append("SHELL USER SERVICE DISCONNECTED")

                shellService = null
                serviceBound = false
            }
        }

    // ============================================================
    // ACTIVITY
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildInterface()

        Shizuku.addBinderReceivedListenerSticky(
            binderReceivedListener
        )

        Shizuku.addBinderDeadListener(
            binderDeadListener
        )

        Shizuku.addRequestPermissionResultListener(
            permissionResultListener
        )

        append(
            """
            VIVO CAMERA SHIZUKU / 200 MP PROBE
            =================================
            
            Purpose:
            
            • Run shell diagnostics through Shizuku
            • Inspect Vivo camera services
            • Inspect camera processes
            • inspect vendor camera properties
            • capture targeted logcat
            • watch Vivo 200 MP capture activity
            • search for RAW / remosaic / VIF activity
            
            This build uses Shizuku UserService.
            Shizuku.newProcess() is NOT used.
            """.trimIndent()
        )

        showShizukuStatus()
    }

    // ============================================================
    // BUILD UI
    // ============================================================

    private fun buildInterface() {

        val root =
            LinearLayout(this).apply {

                orientation = LinearLayout.VERTICAL

                setPadding(
                    20,
                    20,
                    20,
                    20
                )
            }

        fun addButton(
            text: String,
            action: () -> Unit
        ) {

            val button =
                Button(this).apply {

                    this.text = text

                    setOnClickListener {
                        action()
                    }
                }

            root.addView(button)
        }

        addButton(
            "1 - CHECK SHIZUKU"
        ) {
            showShizukuStatus()
        }

        addButton(
            "2 - REQUEST SHIZUKU ACCESS"
        ) {
            requestShizukuPermission()
        }

        addButton(
            "3 - CONNECT SHELL SERVICE"
        ) {
            connectShellService()
        }

        addButton(
            "4 - TEST SHELL ACCESS"
        ) {
            testShell()
        }

        addButton(
            "5 - CAMERA SERVICES"
        ) {
            cameraServices()
        }

        addButton(
            "6 - VIVO CAMERA PACKAGE"
        ) {
            vivoCameraPackage()
        }

        addButton(
            "7 - CAMERA PROPERTIES"
        ) {
            cameraProperties()
        }

        addButton(
            "8 - CAMERA PROCESSES"
        ) {
            cameraProcesses()
        }

        addButton(
            "9 - CAMERA FILESYSTEM SCAN"
        ) {
            cameraFilesystemScan()
        }

        addButton(
            "10 - PREP 200MP LOG WATCH"
        ) {
            prepare200MpLog()
        }

        addButton(
            "11 - RECENT 200MP LOGCAT"
        ) {
            recent200MpLog()
        }

        addButton(
            "12 - DEEP CAMERA LOGCAT"
        ) {
            deepCameraLog()
        }

        addButton(
            "13 - SEARCH RAW / VIF / REMOSAIC"
        ) {
            rawVifSearch()
        }

        addButton(
            "14 - CAMERA SERVICE DUMP"
        ) {
            cameraServiceDump()
        }

        addButton(
            "15 - MEDIASTORE RECENT CAMERA FILES"
        ) {
            recentCameraMedia()
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

            output.clear()
            outputText.text = ""
        }

        val scroll =
            ScrollView(this)

        outputText =
            TextView(this).apply {

                textSize = 13f

                setTextIsSelectable(true)

                setPadding(
                    0,
                    20,
                    0,
                    100
                )
            }

        scroll.addView(outputText)

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

    // ============================================================
    // OUTPUT
    // ============================================================

    private fun append(text: String) {

        runOnUiThread {

            output.append(text)
            output.append("\n")

            outputText.text =
                output.toString()
        }
    }

    // ============================================================
    // SHIZUKU STATUS
    // ============================================================

    private fun showShizukuStatus() {

        append("")
        append("==============================")
        append("SHIZUKU STATUS")
        append("==============================")

        try {

            val alive =
                Shizuku.pingBinder()

            append("Binder alive = $alive")

            if (!alive) {

                append("Shizuku is NOT running.")
                return
            }

            append("Shizuku is RUNNING.")

            append(
                "Version = ${Shizuku.getVersion()}"
            )

            append(
                "Server UID = ${Shizuku.getUid()}"
            )

            val permission =
                Shizuku.checkSelfPermission()

            append(
                "App permission = ${
                    if (
                        permission ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        "GRANTED"
                    } else {
                        "NOT GRANTED"
                    }
                }"
            )

            append(
                "UserService bound = $serviceBound"
            )

        } catch (e: Exception) {

            append("STATUS ERROR:")
            append(e.stackTraceToString())
        }
    }

    // ============================================================
    // REQUEST SHIZUKU PERMISSION
    // ============================================================

    private fun requestShizukuPermission() {

        try {

            if (!Shizuku.pingBinder()) {

                append("")
                append("ERROR:")
                append("Shizuku is not running.")
                return
            }

            if (
                Shizuku.checkSelfPermission() ==
                PackageManager.PERMISSION_GRANTED
            ) {

                append("")
                append("Shizuku permission already GRANTED.")
                return
            }

            append("")
            append("Requesting Shizuku permission...")

            Shizuku.requestPermission(
                permissionRequestCode
            )

        } catch (e: Exception) {

            append("")
            append("PERMISSION ERROR:")
            append(e.stackTraceToString())
        }
    }

    // ============================================================
    // CONNECT USER SERVICE
    // ============================================================

    private fun connectShellService() {

        append("")
        append("==============================")
        append("CONNECT SHELL USER SERVICE")
        append("==============================")

        try {

            if (!Shizuku.pingBinder()) {

                append("ERROR: Shizuku is not running.")
                return
            }

            if (
                Shizuku.checkSelfPermission() !=
                PackageManager.PERMISSION_GRANTED
            ) {

                append("ERROR: Shizuku permission not granted.")
                append("Press 2 first.")
                return
            }

            if (serviceBound) {

                append("Service is already connected.")
                return
            }

            append("Binding UserService...")

            Shizuku.bindUserService(
                userServiceArgs,
                userServiceConnection
            )

        } catch (e: Exception) {

            append("BIND ERROR:")
            append(e.stackTraceToString())
        }
    }

    // ============================================================
    // SHELL EXECUTION
    // ============================================================

    private fun runShell(
        title: String,
        command: String
    ) {

        append("")
        append("==============================")
        append(title)
        append("==============================")
        append("")
        append("$ $command")
        append("")

        val service = shellService

        if (service == null) {

            append("ERROR:")
            append("Shell UserService is not connected.")
            append("")
            append("Press:")
            append("3 - CONNECT SHELL SERVICE")

            return
        }

        Thread {

            try {

                val result =
                    service.exec(command)

                append(
                    result ?: "(no output)"
                )

            } catch (e: Exception) {

                append("")
                append("COMMAND ERROR:")
                append(e.stackTraceToString())

                shellService = null
                serviceBound = false
            }

        }.start()
    }

    // ============================================================
    // TEST SHELL
    // ============================================================

    private fun testShell() {

        runShell(
            "TEST SHELL ACCESS",
            """
            echo "===== WHOAMI ====="
            whoami
            
            echo ""
            echo "===== ID ====="
            id
            
            echo ""
            echo "===== SELINUX ====="
            id -Z 2>/dev/null
            
            echo ""
            echo "===== PROCESS ====="
            echo ${'$'}${'$'}
            """.trimIndent()
        )
    }

    // ============================================================
    // CAMERA SERVICES
    // ============================================================

    private fun cameraServices() {

        runShell(
            "CAMERA SERVICES",
            """
            service list 2>/dev/null \
            | grep -Ei \
            'camera|vivo.*camera|media'
            """.trimIndent()
        )
    }

    // ============================================================
    // CAMERA PACKAGE
    // ============================================================

    private fun vivoCameraPackage() {

        runShell(
            "VIVO CAMERA PACKAGE",
            """
            echo "===== PACKAGE PATH ====="
            pm path com.android.camera
            
            echo ""
            echo "===== PACKAGE INFO ====="
            dumpsys package com.android.camera \
            | grep -Ei \
            'versionName|versionCode|codePath|dataDir|nativeLibraryDir|userId|sharedUser'
            
            echo ""
            echo "===== APK PATHS ====="
            pm path com.android.camera
            """.trimIndent()
        )
    }

    // ============================================================
    // CAMERA PROPERTIES
    // ============================================================

    private fun cameraProperties() {

        runShell(
            "CAMERA PROPERTIES",
            """
            getprop \
            | grep -Ei \
            'camera|sensor|vivo|mtk|mediatek|imx|hp9|200mp|remosaic|raw'
            """.trimIndent()
        )
    }

    // ============================================================
    // CAMERA PROCESSES
    // ============================================================

    private fun cameraProcesses() {

        runShell(
            "CAMERA PROCESSES",
            """
            ps -A \
            | grep -Ei \
            'camera|cameraserver|vivo|mtkcam|provider'
            """.trimIndent()
        )
    }

    // ============================================================
    // FILESYSTEM SCAN
    // ============================================================

    private fun cameraFilesystemScan() {

        runShell(
            "CAMERA FILESYSTEM SCAN",
            """
            echo "===== CAMERA-RELATED DIRECTORIES ====="
            
            find /data/vendor \
            /data/misc \
            /sdcard/DCIM \
            /sdcard/Pictures \
            -maxdepth 4 \
            -type f \
            2>/dev/null \
            | grep -Ei \
            '\.(dng|raw|bin|yuv|raw10|raw12|raw16)$|camera|remosaic|vif|vcf' \
            | tail -n 1000
            """.trimIndent()
        )
    }

    // ============================================================
    // PREPARE 200 MP LOG
    // ============================================================

    private fun prepare200MpLog() {

        runShell(
            "200 MP LOG WATCH PREP",
            """
            logcat -c
            
            echo "LOGCAT CLEARED"
            echo ""
            echo "NOW:"
            echo "1. Open Vivo Camera"
            echo "2. Select 200 MP"
            echo "3. Take ONE photo"
            echo "4. Wait until processing finishes"
            echo "5. Return to this app"
            echo "6. Press 11 - RECENT 200MP LOGCAT"
            """.trimIndent()
        )
    }

    // ============================================================
    // RECENT 200MP LOG
    // ============================================================

    private fun recent200MpLog() {

        runShell(
            "RECENT 200 MP LOGCAT",
            """
            logcat -d -v threadtime \
            | grep -Ei \
            '200mp|200 mp|16320|12288|remosaic|rawvif|raw vif|vif|vcf|raw10|raw12|raw16|raw_sensor|proraw|SaveRaw|SaveProRaw|sensorMode|sensorScenario|fullsize|full.size|highresolution|high_resolution|niceCaptureSensorMode|real200mp|camera3|mtkcam|com.android.camera' \
            | tail -n 8000
            """.trimIndent()
        )
    }

    // ============================================================
    // DEEP CAMERA LOG
    // ============================================================

    private fun deepCameraLog() {

        runShell(
            "DEEP CAMERA LOGCAT",
            """
            logcat -d -v threadtime \
            | grep -Ei \
            'camera|capture|sensor|raw|isp|p1node|p2node|mtkcam|hal3|hal|jpeg|heic|remosaic|fullsize|16320|12288' \
            | tail -n 12000
            """.trimIndent()
        )
    }

    // ============================================================
    // RAW / VIF SEARCH
    // ============================================================

    private fun rawVifSearch() {

        runShell(
            "RAW / VIF / REMOSAIC SEARCH",
            """
            echo "===== LOGCAT ====="
            
            logcat -d \
            | grep -Ei \
            'rawvif|raw vif|vif|vcf|raw10|raw12|raw16|raw_sensor|remosaic|proraw|fullsize|16320|12288|200mp' \
            | tail -n 5000
            
            echo ""
            echo "===== PROPERTIES ====="
            
            getprop \
            | grep -Ei \
            'raw|vif|vcf|remosaic|200mp|sensor'
            
            echo ""
            echo "===== SERVICES ====="
            
            service list \
            | grep -Ei \
            'camera|vivo|media'
            """.trimIndent()
        )
    }

    // ============================================================
    // CAMERA SERVICE DUMP
    // ============================================================

    private fun cameraServiceDump() {

        runShell(
            "CAMERA SERVICE DUMP",
            """
            dumpsys media.camera \
            | grep -Ei \
            'camera id|device|client|package|sensor|stream|raw|jpeg|16320|12288|4080|3072|physical|logical' \
            | tail -n 8000
            """.trimIndent()
        )
    }

    // ============================================================
    // RECENT CAMERA MEDIA
    // ============================================================

    private fun recentCameraMedia() {

        runShell(
            "RECENT CAMERA MEDIA",
            """
            echo "===== DCIM CAMERA ====="
            
            ls -lahtr /sdcard/DCIM/Camera 2>/dev/null \
            | tail -n 50
            
            echo ""
            echo "===== RECENT LARGE FILES ====="
            
            find /sdcard/DCIM/Camera \
            -type f \
            -size +10M \
            -exec ls -lah {} \; \
            2>/dev/null \
            | tail -n 100
            """.trimIndent()
        )
    }

    // ============================================================
    // OPEN VIVO CAMERA
    // ============================================================

    private fun openVivoCamera() {

        append("")
        append("==============================")
        append("OPENING VIVO CAMERA")
        append("==============================")

        try {

            val intent =
                packageManager.getLaunchIntentForPackage(
                    "com.android.camera"
                )

            if (intent == null) {

                append(
                    "Could not obtain Vivo Camera launch intent."
                )

                return
            }

            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )

            startActivity(intent)

            append("Vivo Camera launched.")

        } catch (e: Exception) {

            append("CAMERA LAUNCH ERROR:")
            append(e.stackTraceToString())
        }
    }

    // ============================================================
    // COPY OUTPUT
    // ============================================================

    private fun copyOutput() {

        val clipboard =
            getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "Vivo Camera Probe Output",
                output.toString()
            )
        )

        append("")
        append("OUTPUT COPIED TO CLIPBOARD")
    }

    // ============================================================
    // DESTROY
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

        } catch (_: Exception) {
        }

        super.onDestroy()
    }
}
