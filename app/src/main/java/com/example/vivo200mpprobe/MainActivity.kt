package com.example.vivo200mpprobe

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.ComponentInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var output: TextView
    private lateinit var scroll: ScrollView

    private val targetPackages = listOf(
        "com.android.camera",
        "com.vivo.camera2",
        "com.vivo.camera2pd",
        "com.vivo.engineercamera",
        "com.vivo.alphacamera",
        "test.com.vivo.yzz.cameratestforapi"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildUi()

        log("VIVO CAMERA PACKAGE PROBE")
        log("==============================")
        log("")
        log("Ready.")
        log("")
        log("Press SCAN VIVO CAMERA PACKAGES.")
    }

    private fun buildUi() {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL

        root.setPadding(
            20,
            30,
            20,
            30
        )

        // ================================================
        // SCAN BUTTON
        // ================================================

        val scanButton = Button(this)

        scanButton.text = "SCAN VIVO CAMERA PACKAGES"

        scanButton.setOnClickListener {

            output.text = ""

            scanButton.isEnabled = false

            Thread {

                try {

                    runProbe()

                } catch (e: Throwable) {

                    log("")
                    log("FATAL ERROR")
                    log("==============================")
                    log(e.javaClass.name)
                    log(e.message ?: "No error message")

                } finally {

                    runOnUiThread {
                        scanButton.isEnabled = true
                    }
                }

            }.start()
        }

        root.addView(scanButton)

        // ================================================
        // COPY BUTTON
        // ================================================

        val copyButton = Button(this)

        copyButton.text = "COPY OUTPUT"

        copyButton.setOnClickListener {

            val text = output.text.toString()

            if (text.isBlank()) {

                Toast.makeText(
                    this,
                    "No output to copy.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val clipboard =
                getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as ClipboardManager

            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    "Vivo Camera Probe",
                    text
                )
            )

            Toast.makeText(
                this,
                "Copied",
                Toast.LENGTH_SHORT
            ).show()
        }

        root.addView(copyButton)

        // ================================================
        // CLEAR BUTTON
        // ================================================

        val clearButton = Button(this)

        clearButton.text = "CLEAR"

        clearButton.setOnClickListener {

            output.text = ""
        }

        root.addView(clearButton)

        // ================================================
        // OUTPUT
        // ================================================

        scroll = ScrollView(this)

        output = TextView(this)

        output.textSize = 13f

        output.setTextIsSelectable(true)

        output.setPadding(
            0,
            20,
            0,
            100
        )

        scroll.addView(output)

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

    // ====================================================
    // MAIN SCAN
    // ====================================================

    @Suppress("DEPRECATION")
    private fun runProbe() {

        log("VIVO CAMERA PACKAGE PROBE")
        log("==============================")

        log("")
        log("Android version:")
        log(android.os.Build.VERSION.RELEASE)

        log("")
        log("SDK:")
        log(android.os.Build.VERSION.SDK_INT.toString())

        log("")
        log("Device:")
        log(android.os.Build.DEVICE)

        log("")
        log("Model:")
        log(android.os.Build.MODEL)

        val pm = packageManager

        var installedCount = 0
        var exportedCount = 0

        for (packageName in targetPackages) {

            log("")
            log("")
            log("################################")
            log(packageName)
            log("################################")

            try {

                val flags =
                    PackageManager.GET_ACTIVITIES or
                    PackageManager.GET_SERVICES or
                    PackageManager.GET_RECEIVERS or
                    PackageManager.GET_PROVIDERS or
                    PackageManager.GET_PERMISSIONS or
                    PackageManager.GET_META_DATA

                val info =
                    pm.getPackageInfo(
                        packageName,
                        flags
                    )

                installedCount++

                log("")
                log("STATUS: INSTALLED / VISIBLE")

                dumpPackageInfo(info)

                exportedCount +=
                    countExported(info)

            } catch (
                e: PackageManager.NameNotFoundException
            ) {

                log("")
                log("STATUS: NOT FOUND / NOT VISIBLE")

            } catch (e: Throwable) {

                log("")
                log("ERROR")

                log(
                    e.javaClass.name
                )

                log(
                    e.message ?: ""
                )
            }
        }

        log("")
        log("")
        log("==============================")
        log("FINAL SUMMARY")
        log("==============================")

        log(
            "Target packages: ${targetPackages.size}"
        )

        log(
            "Installed / visible: $installedCount"
        )

        log(
            "Exported components: $exportedCount"
        )

        log("")
        log("==============================")
        log("SCAN COMPLETE")
        log("==============================")

        log("")
        log("Press COPY OUTPUT.")
    }

    // ====================================================
    // PACKAGE INFORMATION
    // ====================================================

    @Suppress("DEPRECATION")
    private fun dumpPackageInfo(
        info: PackageInfo
    ) {

        val app = info.applicationInfo

        log("")
        log("------------------------------")
        log("PACKAGE INFORMATION")
        log("------------------------------")

        log(
            "Package: ${info.packageName}"
        )

        log(
            "Version name: ${info.versionName}"
        )

        log(
            "Version code: ${info.longVersionCode}"
        )

        if (app != null) {

            log(
                "UID: ${app.uid}"
            )

            log("")
            log("Source APK:")

            log(
                app.sourceDir ?: "null"
            )

            log("")
            log("Native library directory:")

            log(
                app.nativeLibraryDir ?: "null"
            )

            val systemApp =
                app.flags and
                    ApplicationInfo.FLAG_SYSTEM != 0

            val updatedSystemApp =
                app.flags and
                    ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0

            val debuggable =
                app.flags and
                    ApplicationInfo.FLAG_DEBUGGABLE != 0

            log("")
            log(
                "System app: $systemApp"
            )

            log(
                "Updated system app: $updatedSystemApp"
            )

            log(
                "Debuggable: $debuggable"
            )

            dumpApplicationMetadata(app)
        }

        dumpRequestedPermissions(info)

        dumpActivities(info)

        dumpServices(info)

        dumpProviders(info)

        dumpReceivers(info)
    }

    // ====================================================
    // APPLICATION METADATA
    // ====================================================

    private fun dumpApplicationMetadata(
        app: ApplicationInfo
    ) {

        log("")
        log("------------------------------")
        log("APPLICATION METADATA")
        log("------------------------------")

        val meta = app.metaData

        if (
            meta == null ||
            meta.isEmpty
        ) {

            log("None.")

            return
        }

        for (key in meta.keySet()) {

            val value =
                try {

                    meta.get(key)

                } catch (_: Throwable) {

                    "<ERROR>"
                }

            log(
                "$key = $value"
            )
        }
    }

    // ====================================================
    // REQUESTED PERMISSIONS
    // ====================================================

    private fun dumpRequestedPermissions(
        info: PackageInfo
    ) {

        log("")
        log("------------------------------")
        log("REQUESTED PERMISSIONS")
        log("------------------------------")

        val permissions =
            info.requestedPermissions

        if (
            permissions == null ||
            permissions.isEmpty()
        ) {

            log("None.")

            return
        }

        for (permission in permissions) {

            log(permission)
        }
    }

    // ====================================================
    // ACTIVITIES
    // ====================================================

    private fun dumpActivities(
        info: PackageInfo
    ) {

        log("")
        log("==============================")
        log("ACTIVITIES")
        log("==============================")

        val items =
            info.activities

        if (
            items == null ||
            items.isEmpty()
        ) {

            log("None visible.")

            return
        }

        for (item in items) {

            dumpComponent(
                "ACTIVITY",
                item
            )
        }
    }

    // ====================================================
    // SERVICES
    // ====================================================

    private fun dumpServices(
        info: PackageInfo
    ) {

        log("")
        log("==============================")
        log("SERVICES")
        log("==============================")

        val items =
            info.services

        if (
            items == null ||
            items.isEmpty()
        ) {

            log("None visible.")

            return
        }

        for (item in items) {

            dumpComponent(
                "SERVICE",
                item
            )
        }
    }

    // ====================================================
    // PROVIDERS
    // ====================================================

    private fun dumpProviders(
        info: PackageInfo
    ) {

        log("")
        log("==============================")
        log("PROVIDERS")
        log("==============================")

        val items =
            info.providers

        if (
            items == null ||
            items.isEmpty()
        ) {

            log("None visible.")

            return
        }

        for (item in items) {

            log("")
            log("--------------------------------")
            log("PROVIDER")
            log("--------------------------------")

            log(
                "Name: ${item.name}"
            )

            log(
                "Exported: ${item.exported}"
            )

            log(
                "Enabled: ${item.enabled}"
            )

            log(
                "Authority: ${item.authority ?: "NONE"}"
            )

            log(
                "Read permission: ${
                    item.readPermission ?: "NONE"
                }"
            )

            log(
                "Write permission: ${
                    item.writePermission ?: "NONE"
                }"
            )

            dumpMetadata(
                item.metaData
            )

            if (item.exported) {

                log("")
                log("*** EXPORTED PROVIDER ***")
            }
        }
    }

    // ====================================================
    // RECEIVERS
    // ====================================================

    private fun dumpReceivers(
        info: PackageInfo
    ) {

        log("")
        log("==============================")
        log("RECEIVERS")
        log("==============================")

        val items =
            info.receivers

        if (
            items == null ||
            items.isEmpty()
        ) {

            log("None visible.")

            return
        }

        for (item in items) {

            dumpComponent(
                "RECEIVER",
                item
            )
        }
    }

    // ====================================================
    // GENERIC COMPONENT
    // ====================================================

    private fun dumpComponent(
        type: String,
        item: ComponentInfo
    ) {

        log("")
        log("--------------------------------")
        log(type)
        log("--------------------------------")

        log(
            "Name: ${item.name}"
        )

        log(
            "Exported: ${item.exported}"
        )

        log(
            "Enabled: ${item.enabled}"
        )

        log(
            "Permission: ${
                item.permission ?: "NONE"
            }"
        )

        dumpMetadata(
            item.metaData
        )

        if (
            item.exported &&
            item.permission == null
        ) {

            log("")
            log(
                "*** EXPORTED + NO PERMISSION ***"
            )
        }
    }

    // ====================================================
    // COMPONENT METADATA
    // ====================================================

    private fun dumpMetadata(
        meta: Bundle?
    ) {

        if (
            meta == null ||
            meta.isEmpty
        ) {

            return
        }

        log("Metadata:")

        for (key in meta.keySet()) {

            val value =
                try {

                    meta.get(key)

                } catch (_: Throwable) {

                    "<ERROR>"
                }

            log(
                "  $key = $value"
            )
        }
    }

    // ====================================================
    // COUNT EXPORTED COMPONENTS
    // ====================================================

    private fun countExported(
        info: PackageInfo
    ): Int {

        var count = 0

        info.activities?.forEach {

            if (it.exported) {
                count++
            }
        }

        info.services?.forEach {

            if (it.exported) {
                count++
            }
        }

        info.receivers?.forEach {

            if (it.exported) {
                count++
            }
        }

        info.providers?.forEach {

            if (it.exported) {
                count++
            }
        }

        return count
    }

    // ====================================================
    // OUTPUT
    // ====================================================

    private fun log(
        text: String
    ) {

        runOnUiThread {

            output.append(text)

            output.append("\n")
        }
    }
}
