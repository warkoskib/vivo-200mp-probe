package com.example.vivo200mpprobe

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
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

    private val targetPackage = "com.android.camera"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildUi()

        log("VIVO CAMERA INTENT PROBE")
        log("==============================")
        log("")
        log("Target package:")
        log(targetPackage)
        log("")
        log("Press SCAN CAMERA INTENTS.")
    }

    private fun buildUi() {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setPadding(20, 30, 20, 30)

        val scanButton = Button(this)

        scanButton.text = "SCAN CAMERA INTENTS"

        scanButton.setOnClickListener {

            output.text = ""

            Thread {
                runProbe()
            }.start()
        }

        root.addView(scanButton)

        val copyButton = Button(this)

        copyButton.text = "COPY OUTPUT"

        copyButton.setOnClickListener {

            val text = output.text.toString()

            if (text.isBlank()) {

                Toast.makeText(
                    this,
                    "No output yet.",
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
                    "Vivo Camera Intent Probe",
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

        scroll = ScrollView(this)

        output = TextView(this)

        output.textSize = 13f
        output.setTextIsSelectable(true)
        output.setPadding(0, 20, 0, 120)

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

    private fun runProbe() {

        log("VIVO CAMERA INTENT PROBE")
        log("==============================")
        log("")

        try {

            val flags =
                PackageManager.GET_ACTIVITIES or
                PackageManager.GET_RECEIVERS or
                PackageManager.GET_SERVICES or
                PackageManager.GET_PROVIDERS or
                PackageManager.GET_META_DATA or
                PackageManager.GET_PERMISSIONS

            val packageInfo =
                if (Build.VERSION.SDK_INT >= 33) {

                    packageManager.getPackageInfo(
                        targetPackage,
                        PackageManager.PackageInfoFlags.of(
                            flags.toLong()
                        )
                    )

                } else {

                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(
                        targetPackage,
                        flags
                    )
                }

            log("Package found.")
            log("Version: ${packageInfo.versionName}")
            log("")

            log("==============================")
            log("EXPORTED ACTIVITIES")
            log("==============================")

            packageInfo.activities
                ?.filter { it.exported }
                ?.forEach { activity ->

                    dumpActivity(activity)

                    probeCommonIntents(
                        activity.name
                    )
                }

            log("")
            log("==============================")
            log("EXPORTED RECEIVERS")
            log("==============================")

            packageInfo.receivers
                ?.filter { it.exported }
                ?.forEach { receiver ->

                    log("")
                    log("--------------------------------")
                    log("RECEIVER")
                    log("--------------------------------")

                    log("Name: ${receiver.name}")
                    log("Enabled: ${receiver.enabled}")
                    log("Permission: ${receiver.permission ?: "NONE"}")

                    dumpMetadata(
                        receiver.metaData
                    )
                }

            log("")
            log("==============================")
            log("EXPORTED SERVICES")
            log("==============================")

            packageInfo.services
                ?.filter { it.exported }
                ?.forEach { service ->

                    log("")
                    log("--------------------------------")
                    log("SERVICE")
                    log("--------------------------------")

                    log("Name: ${service.name}")
                    log("Enabled: ${service.enabled}")
                    log("Permission: ${service.permission ?: "NONE"}")

                    dumpMetadata(
                        service.metaData
                    )
                }

            log("")
            log("==============================")
            log("EXPORTED PROVIDERS")
            log("==============================")

            packageInfo.providers
                ?.filter { it.exported }
                ?.forEach { provider ->

                    log("")
                    log("--------------------------------")
                    log("PROVIDER")
                    log("--------------------------------")

                    log("Name: ${provider.name}")
                    log("Authority: ${provider.authority}")
                    log("Read permission: ${provider.readPermission ?: "NONE"}")
                    log("Write permission: ${provider.writePermission ?: "NONE"}")

                    dumpMetadata(
                        provider.metaData
                    )
                }

            log("")
            log("==============================")
            log("KNOWN CAMERA INTENT RESOLUTION")
            log("==============================")

            val testActions = listOf(
                "android.media.action.IMAGE_CAPTURE",
                "android.media.action.IMAGE_CAPTURE_SECURE",
                "android.media.action.VIDEO_CAPTURE",
                "android.intent.action.MAIN",
                "android.media.action.STILL_IMAGE_CAMERA",
                "android.media.action.STILL_IMAGE_CAMERA_SECURE"
            )

            for (action in testActions) {

                log("")
                log("ACTION:")
                log(action)

                try {

                    val intent =
                        android.content.Intent(action)

                    intent.setPackage(
                        targetPackage
                    )

                    val results =
                        packageManager.queryIntentActivities(
                            intent,
                            PackageManager.MATCH_DEFAULT_ONLY
                        )

                    if (results.isEmpty()) {

                        log("No matching exported activity.")

                    } else {

                        for (result in results) {

                            log(
                                "MATCH: " +
                                    result.activityInfo.name
                            )
                        }
                    }

                } catch (e: Throwable) {

                    log(
                        "Query error: " +
                            e.javaClass.simpleName
                    )
                }
            }

            log("")
            log("==============================")
            log("PROBE COMPLETE")
            log("==============================")
            log("")
            log("Press COPY OUTPUT.")

        } catch (e: Throwable) {

            log("")
            log("PROBE ERROR")
            log(e.javaClass.name)
            log(e.message ?: "")
        }
    }

    private fun dumpActivity(
        activity: ActivityInfo
    ) {

        log("")
        log("--------------------------------")
        log("ACTIVITY")
        log("--------------------------------")

        log("Name: ${activity.name}")
        log("Exported: ${activity.exported}")
        log("Enabled: ${activity.enabled}")
        log("Permission: ${activity.permission ?: "NONE"}")
        log("Launch mode: ${activity.launchMode}")
        log("Task affinity: ${activity.taskAffinity ?: "NONE"}")

        dumpMetadata(
            activity.metaData
        )
    }

    private fun dumpMetadata(
        bundle: Bundle?
    ) {

        if (bundle == null) {
            return
        }

        if (bundle.keySet().isEmpty()) {
            return
        }

        log("Metadata:")

        for (key in bundle.keySet()) {

            try {

                log(
                    "  $key = ${bundle.get(key)}"
                )

            } catch (_: Throwable) {

                log(
                    "  $key = <ERROR>"
                )
            }
        }
    }

    private fun probeCommonIntents(
        activityName: String
    ) {

        val actions = listOf(
            "android.media.action.IMAGE_CAPTURE",
            "android.media.action.IMAGE_CAPTURE_SECURE",
            "android.media.action.VIDEO_CAPTURE",
            "android.media.action.STILL_IMAGE_CAMERA",
            "android.media.action.STILL_IMAGE_CAMERA_SECURE"
        )

        log("Intent action probes:")

        for (action in actions) {

            try {

                val intent =
                    android.content.Intent(action)

                intent.setClassName(
                    targetPackage,
                    activityName
                )

                val resolved =
                    packageManager.resolveActivity(
                        intent,
                        PackageManager.MATCH_DEFAULT_ONLY
                    )

                if (resolved != null) {

                    log(
                        "  ACCEPTS/RESOLVES: $action"
                    )

                } else {

                    log(
                        "  no match: $action"
                    )
                }

            } catch (e: Throwable) {

                log(
                    "  error: $action"
                )
            }
        }
    }

    private fun log(message: String) {

        runOnUiThread {

            output.append(message)
            output.append("\n")
        }
    }
}
