package com.example.vivo200mpprobe

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var outputText: TextView

    private var commandService: ICommandService? = null

    private var serviceBound = false

    // ------------------------------------------------------------
    // SHIZUKU USER SERVICE
    // ------------------------------------------------------------

    private val userServiceArgs by lazy {

        Shizuku.UserServiceArgs(
            ComponentName(
                packageName,
                ShellUserService::class.java.name
            )
        )
            .daemon(false)
            .processNameSuffix("vivo_shell")
            .debuggable(BuildConfig.DEBUG)
            .version(1)
    }

    // ------------------------------------------------------------
    // SERVICE CONNECTION
    // ------------------------------------------------------------

    private val serviceConnection =
        object : ServiceConnection {

            override fun onServiceConnected(
                name: ComponentName?,
                binder: IBinder?
            ) {

                commandService =
                    ICommandService.Stub.asInterface(binder)

                serviceBound = true

                log("")
                log("==============================")
                log("SHIZUKU SHELL SERVICE CONNECTED")
                log("==============================")

                try {

                    val service = commandService

                    if (service != null) {

                        val uid = service.uid()
                        val pid = service.pid()

                        log("Service UID = $uid")
                        log("Service PID = $pid")

                        if (uid == 2000) {

                            log("")
                            log("*** SHELL ACCESS CONFIRMED ***")

                        } else {

                            log("")
                            log(
                                "WARNING: expected shell UID 2000, got $uid"
                            )
                        }
                    }

                } catch (e: Throwable) {

                    log(
                        "Service test failed: " +
                            "${e.javaClass.simpleName}: ${e.message}"
                    )
                }
            }

            override fun onServiceDisconnected(
                name: ComponentName?
            ) {

                commandService = null
                serviceBound = false

                log("")
                log("SHIZUKU SHELL SERVICE DISCONNECTED")
            }
        }

    // ------------------------------------------------------------
    // SHIZUKU BINDER LISTENER
    // ------------------------------------------------------------

    private val binderReceivedListener =
        Shizuku.OnBinderReceivedListener {

            runOnUiThread {

                log("")
                log("Shizuku binder received.")

                checkShizuku()
            }
        }

    private val binderDeadListener =
        Shizuku.OnBinderDeadListener {

            runOnUiThread {

                commandService = null
                serviceBound = false

                log("")
                log("Shizuku binder died.")
            }
        }

    // ------------------------------------------------------------
    // PERMISSION RESULT
    // ------------------------------------------------------------

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
                        android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {

                        log("Permission GRANTED.")

                    } else {

                        log("Permission DENIED.")
                    }
                }
            }
        }

    // ------------------------------------------------------------
    // CREATE
    // ------------------------------------------------------------

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        Shizuku.addBinderReceivedListenerSticky(
            binderReceivedListener
        )

        Shizuku.addBinderDeadListener(
            binderDeadListener
        )

        Shizuku.addRequestPermissionResultListener(
            permissionResultListener
        )

        buildInterface()

        log("==============================")
        log("VIVO CAMERA SHIZUKU PROBE")
        log("==============================")
        log("")
        log(
            "This app runs shell-level Vivo camera diagnostics through Shizuku."
        )
        log("")
        log("Start with:")
        log("1 - CHECK SHIZUKU")
    }

    // ------------------------------------------------------------
    // USER INTERFACE
    // ------------------------------------------------------------

    private fun buildInterface() {

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    16,
                    16,
                    16,
                    16
                )
            }

        fun addButton(
            title: String,
            action: () -> Unit
        ) {

            val button =
                Button(this).apply {

                    text = title

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
            "3 - CONNECT SHELL SERVICE"
        ) {
            connectShellService()
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
            cameraProperties()
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
            "OPEN VIVO CAMERA"
        ) {
            openVivoCamera()
        }

        addButton(
            "COPY OUTPUT"
        ) {

            val clipboard =
                getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as android.content.ClipboardManager

            clipboard.setPrimaryClip(
                android.content.ClipData.newPlainText(
                    "Vivo Camera Probe",
                    outputText.text
                )
            )

            log("")
            log("[OUTPUT COPIED]")
        }

        addButton(
            "CLEAR OUTPUT"
        ) {

            outputText.text = ""
        }

        outputText =
            TextView(this).apply {

                textSize = 14f

                setPadding(
                    4,
                    24,
                    4,
                    40
                )

                setTextIsSelectable(true)

                movementMethod =
                    ScrollingMovementMethod()
            }

        root.addView(
            outputText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val scrollView =
            ScrollView(this).apply {

                addView(root)
            }

        setContentView(scrollView)
    }

    // ------------------------------------------------------------
    // CHECK SHIZUKU
    // ------------------------------------------------------------

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

                log("Shizuku is not running.")
                return
            }

            log("Shizuku is RUNNING.")

            val version =
                Shizuku.getVersion()

            log("Shizuku version = $version")

            val uid =
                Shizuku.getUid()

            log("Shizuku UID = $uid")

            val permission =
                Shizuku.checkSelfPermission()

            log(
                "Permission = " +
                    if (
                        permission ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        "GRANTED"
                    } else {
                        "NOT GRANTED"
                    }
            )

        } catch (e: Throwable) {

            log(
                "SHIZUKU ERROR: " +
                    "${e.javaClass.name}: ${e.message}"
            )
        }
    }

    // ------------------------------------------------------------
    // REQUEST SHIZUKU PERMISSION
    // ------------------------------------------------------------

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
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {

                log("Permission already granted.")
                return
            }

            if (
                Shizuku.shouldShowRequestPermissionRationale()
            ) {

                log(
                    "Permission was previously denied."
                )
            }

            log("Requesting permission...")

            Shizuku.requestPermission(
                SHIZUKU_PERMISSION_CODE
            )

        } catch (e: Throwable) {

            log(
                "PERMISSION ERROR: " +
                    "${e.javaClass.name}: ${e.message}"
            )
        }
    }

    // ------------------------------------------------------------
    // CONNECT USER SERVICE
    // ------------------------------------------------------------

    private fun connectShellService() {

        log("")
        log("==============================")
        log("CONNECT SHELL SERVICE")
        log("==============================")

        try {

            if (!Shizuku.pingBinder()) {

                log("ERROR: Shizuku is not running.")
                return
            }

            if (
                Shizuku.checkSelfPermission() !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {

                log(
                    "ERROR: Shizuku permission not granted."
                )

                return
            }

            if (serviceBound) {

                log(
                    "Shell service is already connected."
                )

                return
            }

            log(
                "Binding ShellUserService..."
            )

            Shizuku.bindUserService(
                userServiceArgs,
                serviceConnection
            )

        } catch (e: Throwable) {

            log(
                "SERVICE ERROR: " +
                    "${e.javaClass.name}: ${e.message}"
            )
        }
    }

    // ------------------------------------------------------------
    // EXECUTE SHELL COMMAND
    // ------------------------------------------------------------

    private fun runShellCommand(
        command: String,
        title: String
    ) {

        log("")
        log("==============================")
        log(title)
        log("==============================")
        log("")
        log("$ $command")
        log("")

        val service =
            commandService

        if (service == null) {

            log(
                "ERROR: Shell service is not connected."
            )

            log(
                "Press 3 - CONNECT SHELL SERVICE first."
            )

            return
        }

        Thread {

            try {

                /*
                 * ICommandService now uses byte[] instead
                 * of String.
                 */

                val commandBytes =
                    command.toByteArray(
                        Charsets.UTF_8
                    )

                val resultBytes =
                    service.runCommand(
                        commandBytes
                    )

                val result =
                    resultBytes?.toString(
                        Charsets.UTF_8
                    ) ?: "[NULL RESULT]"

                runOnUiThread {

                    log(result)
                }

            } catch (e: Throwable) {

                runOnUiThread {

                    log("")
                    log("COMMAND ERROR:")

                    log(
                        "${e.javaClass.name}: ${e.message}"
                    )
                }
            }

        }.start()
    }

    // ------------------------------------------------------------
    // CAMERA DIAGNOSTICS
    // ------------------------------------------------------------

    private fun runCameraDiagnostics() {

        val command = """

            echo "=============================="
            echo "IDENTITY"
            echo "=============================="
            id
            echo

            echo "=============================="
            echo "CAMERA SERVICE"
            echo "=============================="
            dumpsys media.camera 2>&1
            echo

            echo "=============================="
            echo "VIVO CAMERA PROCESS"
            echo "=============================="
            ps -A | grep -i camera
            echo

            echo "=============================="
            echo "CAMERA PROVIDER"
            echo "=============================="
            ps -A | grep -i provider
            echo

        """.trimIndent()

        runShellCommand(
            command,
            "CAMERA DIAGNOSTICS"
        )
    }

    // ------------------------------------------------------------
    // DUMP CAMERA PACKAGE
    // ------------------------------------------------------------

    private fun dumpVivoCameraPackage() {

        val command = """

            echo "=============================="
            echo "VIVO CAMERA PACKAGE"
            echo "=============================="

            dumpsys package com.android.camera

            echo
            echo "=============================="
            echo "APK PATH"
            echo "=============================="

            pm path com.android.camera

        """.trimIndent()

        runShellCommand(
            command,
            "VIVO CAMERA PACKAGE DUMP"
        )
    }

    // ------------------------------------------------------------
    // FILESYSTEM SCAN
    // ------------------------------------------------------------

    private fun scanCameraFilesystem() {

        val command = """

            echo "=============================="
            echo "CAMERA DIRECTORY SCAN"
            echo "=============================="

            echo
            echo "--- DCIM CAMERA ---"
            ls -lah /sdcard/DCIM/Camera 2>&1

            echo
            echo "--- RECENT CAMERA FILES ---"

            find /sdcard/DCIM/Camera \
            -type f \
            -mmin -10 \
            -exec ls -lah {} \; \
            2>&1

            echo
            echo "--- RAW / DNG / BIN / YUV SEARCH ---"

            find /sdcard \
            -type f \
            \( \
            -iname "*.dng" \
            -o -iname "*.raw" \
            -o -iname "*.bin" \
            -o -iname "*.yuv" \
            -o -iname "*.raw10" \
            -o -iname "*.raw16" \
            \) \
            2>/dev/null | head -200

        """.trimIndent()

        runShellCommand(
            command,
            "CAMERA FILESYSTEM SCAN"
        )
    }

    // ------------------------------------------------------------
    // CAMERA PROPERTIES
    // ------------------------------------------------------------

    private fun cameraProperties() {

        val command = """

            echo "=============================="
            echo "CAMERA PROPERTIES"
            echo "=============================="

            getprop | grep -i camera

            echo
            echo "=============================="
            echo "VIVO PROPERTIES"
            echo "=============================="

            getprop | grep -i vivo

            echo
            echo "=============================="
            echo "MEDIATEK CAMERA PROPERTIES"
            echo "=============================="

            getprop | grep -Ei "mtk|mediatek" | grep -i camera

        """.trimIndent()

        runShellCommand(
            command,
            "CAMERA PROPERTIES"
        )
    }

    // ------------------------------------------------------------
    // CAMERA LOGCAT
    // ------------------------------------------------------------

    private fun recentCameraLogcat() {

        val command = """

            echo "=============================="
            echo "RECENT CAMERA LOGCAT"
            echo "=============================="

            logcat -d -v threadtime \
            | grep -Ei \
            "camera|vivo|vcf|raw|dng|remosaic|200mp|sensor|jpeg|heic|imagewriter|imagereader" \
            | tail -1500

        """.trimIndent()

        runShellCommand(
            command,
            "RECENT CAMERA LOGCAT"
        )
    }

    // ------------------------------------------------------------
    // CLEAR LOGCAT
    // ------------------------------------------------------------

    private fun clearLogcat() {

        runShellCommand(
            "logcat -c && echo LOGCAT CLEARED",
            "CLEAR LOGCAT"
        )
    }

    // ------------------------------------------------------------
    // OPEN OEM CAMERA
    // ------------------------------------------------------------

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

            if (intent == null) {

                log(
                    "Could not obtain launch intent for com.android.camera"
                )

                return
            }

            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )

            startActivity(intent)

            log("Vivo Camera launched.")

            log("")
            log("For the next RAW test:")
            log("1. Select 200 MP")
            log("2. Take ONE photograph")
            log("3. Wait for processing")
            log("4. Return here")
            log("5. Press RECENT CAMERA LOGCAT")

        } catch (e: Throwable) {

            log(
                "CAMERA LAUNCH ERROR: " +
                    "${e.javaClass.name}: ${e.message}"
            )
        }
    }

    // ------------------------------------------------------------
    // LOG
    // ------------------------------------------------------------

    private fun log(
        message: String
    ) {

        outputText.append(
            message
        )

        outputText.append(
            "\n"
        )
    }

    // ------------------------------------------------------------
    // DESTROY
    // ------------------------------------------------------------

    override fun onDestroy() {

        try {

            if (serviceBound) {

                Shizuku.unbindUserService(
                    userServiceArgs,
                    serviceConnection,
                    true
                )

                serviceBound = false
            }

        } catch (_: Throwable) {
        }

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

    companion object {

        private const val SHIZUKU_PERMISSION_CODE =
            1001
    }
}
