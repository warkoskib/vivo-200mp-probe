package com.example.vivo200mpprobe

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var output: TextView
    private lateinit var scroll: ScrollView

    private val roots = listOf(
        "/vendor/lib64",
        "/vendor/lib",
        "/vendor/etc",
        "/system/lib64",
        "/system/lib",
        "/system/etc",
        "/odm/lib64",
        "/odm/lib",
        "/odm/etc"
    )

    private val terms = listOf(
        "camera",
        "cam_",
        "cam.",
        "sensor",
        "remosaic",
        "raw",
        "jpeg",
        "isp",
        "imgsensor",
        "mtkcam",
        "vivo"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildUi()

        log("CAMERA FILE DISCOVERY")
        log("==============================")
        log("")
        log("Press SCAN CAMERA FILES.")
        log("")
        log("When finished, press COPY OUTPUT")
        log("and paste the results into ChatGPT.")
    }

    private fun buildUi() {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setPadding(20, 30, 20, 30)

        // -------------------------------------------------
        // SCAN BUTTON
        // -------------------------------------------------

        val scanButton = Button(this)

        scanButton.text = "SCAN CAMERA FILES"

        scanButton.setOnClickListener {

            output.text = ""

            scanButton.isEnabled = false

            Thread {

                try {
                    runScan()
                } finally {

                    runOnUiThread {
                        scanButton.isEnabled = true
                    }
                }

            }.start()
        }

        root.addView(scanButton)

        // -------------------------------------------------
        // COPY BUTTON
        // -------------------------------------------------

        val copyButton = Button(this)

        copyButton.text = "COPY OUTPUT"

        copyButton.setOnClickListener {

            val text =
                output.text.toString()

            if (text.isBlank()) {

                Toast.makeText(
                    this,
                    "There is no output to copy yet.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val clipboard =
                getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as ClipboardManager

            val clip =
                ClipData.newPlainText(
                    "Vivo Camera Scan Results",
                    text
                )

            clipboard.setPrimaryClip(clip)

            Toast.makeText(
                this,
                "Output copied to clipboard",
                Toast.LENGTH_SHORT
            ).show()
        }

        root.addView(copyButton)

        // -------------------------------------------------
        // CLEAR BUTTON
        // -------------------------------------------------

        val clearButton = Button(this)

        clearButton.text = "CLEAR OUTPUT"

        clearButton.setOnClickListener {
            output.text = ""
        }

        root.addView(clearButton)

        // -------------------------------------------------
        // OUTPUT AREA
        // -------------------------------------------------

        scroll = ScrollView(this)

        output = TextView(this)

        output.textSize = 13f

        output.setPadding(
            0,
            20,
            0,
            100
        )

        output.setTextIsSelectable(true)

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

    private fun runScan() {

        log("CAMERA FILE DISCOVERY")
        log("==============================")
        log("")

        log("Searching:")
        log("/vendor")
        log("/system")
        log("/odm")

        log("")
        log("Search terms:")
        log(
            terms.joinToString(
                separator = ", "
            )
        )

        var totalMatches = 0

        for (rootPath in roots) {

            log("")
            log("")
            log("==============================")
            log("SCANNING")
            log(rootPath)
            log("==============================")

            val root = File(rootPath)

            log(
                "Exists: ${root.exists()}"
            )

            log(
                "Readable: ${root.canRead()}"
            )

            log(
                "Directory: ${root.isDirectory}"
            )

            if (!root.exists()) {

                log("RESULT: DOES NOT EXIST")
                continue
            }

            if (!root.canRead()) {

                log("RESULT: ACCESS DENIED")
                continue
            }

            if (!root.isDirectory) {

                log("RESULT: NOT A DIRECTORY")
                continue
            }

            try {

                val children =
                    root.listFiles()

                if (children == null) {

                    log(
                        "RESULT: Directory exists but cannot be listed."
                    )

                    continue
                }

                log(
                    "Entries visible: ${children.size}"
                )

                var rootMatches = 0

                for (file in children) {

                    val name =
                        file.name.lowercase()

                    if (
                        terms.any {
                            name.contains(it)
                        }
                    ) {

                        totalMatches++
                        rootMatches++

                        log("")
                        log("--------------------------------")
                        log("MATCH #$totalMatches")
                        log("--------------------------------")

                        log(
                            "Name: ${file.name}"
                        )

                        log(
                            "Path: ${file.absolutePath}"
                        )

                        log(
                            "Type: ${
                                if (file.isDirectory)
                                    "DIRECTORY"
                                else
                                    "FILE"
                            }"
                        )

                        log(
                            "Readable: ${file.canRead()}"
                        )

                        log(
                            "Writable: ${file.canWrite()}"
                        )

                        if (file.isFile) {

                            log(
                                "Size: ${file.length()} bytes"
                            )

                            log(
                                "Extension: ${file.extension}"
                            )
                        }

                        if (file.isDirectory) {

                            try {

                                val subCount =
                                    file.listFiles()?.size

                                log(
                                    "Visible child entries: " +
                                        (subCount ?: "UNKNOWN")
                                )

                            } catch (e: Throwable) {

                                log(
                                    "Cannot inspect child directory: " +
                                        (e.message ?: "")
                                )
                            }
                        }
                    }
                }

                log("")
                log(
                    "Matches in $rootPath: $rootMatches"
                )

            } catch (e: Throwable) {

                log("")
                log("SCAN ERROR")

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
        log("SCAN COMPLETE")
        log("==============================")

        log(
            "Total matches: $totalMatches"
        )

        log("")
        log(
            "Press COPY OUTPUT."
        )
    }

    private fun log(message: String) {

        runOnUiThread {

            output.append(message)
            output.append("\n")
        }
    }
}
