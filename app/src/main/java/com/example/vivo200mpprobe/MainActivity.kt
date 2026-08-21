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
import java.io.RandomAccessFile

class MainActivity : AppCompatActivity() {

    private lateinit var output: TextView
    private lateinit var scroll: ScrollView

    private val rootPath =
        "/vendor/lib64/camera"

    private val searchTerms = listOf(
        "200mp",
        "200m",
        "16320",
        "12288",
        "remosaic",
        "fullsize",
        "full_size",
        "sensor",
        "sensormode",
        "sensor_mode",
        "scenario",
        "capture",
        "raw",
        "jpeg",
        "isp",
        "quad",
        "pixel"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildUi()

        log("VENDOR CAMERA DIRECTORY PROBE")
        log("==============================")
        log("")
        log("Target:")
        log(rootPath)
        log("")
        log("Press SCAN VENDOR CAMERA.")
    }

    private fun buildUi() {

        val root =
            LinearLayout(this)

        root.orientation =
            LinearLayout.VERTICAL

        root.setPadding(
            20,
            30,
            20,
            30
        )

        val scanButton =
            Button(this)

        scanButton.text =
            "SCAN VENDOR CAMERA"

        scanButton.setOnClickListener {

            output.text = ""

            scanButton.isEnabled = false

            Thread {

                try {
                    runProbe()
                } finally {

                    runOnUiThread {
                        scanButton.isEnabled = true
                    }
                }

            }.start()
        }

        root.addView(scanButton)

        val copyButton =
            Button(this)

        copyButton.text =
            "COPY OUTPUT"

        copyButton.setOnClickListener {

            val text =
                output.text.toString()

            if (text.isBlank()) {

                Toast.makeText(
                    this,
                    "No output to copy yet.",
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
                    "Vendor Camera Probe",
                    text
                )
            )

            Toast.makeText(
                this,
                "Output copied",
                Toast.LENGTH_SHORT
            ).show()
        }

        root.addView(copyButton)

        val clearButton =
            Button(this)

        clearButton.text =
            "CLEAR"

        clearButton.setOnClickListener {
            output.text = ""
        }

        root.addView(clearButton)

        scroll =
            ScrollView(this)

        output =
            TextView(this)

        output.textSize =
            13f

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

    private fun runProbe() {

        log("VENDOR CAMERA DIRECTORY PROBE")
        log("==============================")
        log("")

        val root =
            File(rootPath)

        log(
            "Exists: ${root.exists()}"
        )

        log(
            "Readable: ${root.canRead()}"
        )

        log(
            "Directory: ${root.isDirectory}"
        )

        if (
            !root.exists() ||
            !root.isDirectory
        ) {

            log("")
            log(
                "Target directory unavailable."
            )

            return
        }

        log("")
        log("==============================")
        log("DIRECTORY TREE")
        log("==============================")

        var fileCount = 0
        var dirCount = 0

        fun walk(
            file: File,
            depth: Int
        ) {

            val indent =
                "  ".repeat(depth)

            if (file.isDirectory) {

                dirCount++

                log("")
                log(
                    "${indent}[DIR] ${file.absolutePath}"
                )

                log(
                    "${indent}Readable: ${file.canRead()}"
                )

                val children =
                    try {
                        file.listFiles()
                    } catch (_: Throwable) {
                        null
                    }

                if (children == null) {

                    log(
                        "${indent}Cannot list directory."
                    )

                    return
                }

                log(
                    "${indent}Children: ${children.size}"
                )

                for (child in children) {
                    walk(
                        child,
                        depth + 1
                    )
                }

            } else {

                fileCount++

                log("")
                log(
                    "${indent}[FILE] ${file.absolutePath}"
                )

                log(
                    "${indent}Readable: ${file.canRead()}"
                )

                log(
                    "${indent}Writable: ${file.canWrite()}"
                )

                log(
                    "${indent}Size: ${file.length()} bytes"
                )

                if (
                    file.canRead() &&
                    file.isFile
                ) {

                    searchFile(
                        file,
                        indent
                    )
                }
            }
        }

        walk(
            root,
            0
        )

        log("")
        log("")
        log("==============================")
        log("SUMMARY")
        log("==============================")

        log(
            "Directories found: $dirCount"
        )

        log(
            "Files found: $fileCount"
        )

        log("")
        log("Probe complete.")
        log("Press COPY OUTPUT.")
    }

    private fun searchFile(
        file: File,
        indent: String
    ) {

        try {

            val maxBytes =
                8L * 1024L * 1024L

            val bytesToRead =
                minOf(
                    file.length(),
                    maxBytes
                ).toInt()

            if (bytesToRead <= 0) {
                return
            }

            val data =
                ByteArray(
                    bytesToRead
                )

            RandomAccessFile(
                file,
                "r"
            ).use { raf ->

                raf.readFully(
                    data
                )
            }

            val text =
                buildAsciiView(
                    data
                )

            var foundAny =
                false

            for (term in searchTerms) {

                if (
                    text.contains(
                        term,
                        ignoreCase = true
                    )
                ) {

                    if (!foundAny) {

                        log(
                            "${indent}*** STRING MATCHES ***"
                        )

                        foundAny =
                            true
                    }

                    log(
                        "${indent}FOUND: $term"
                    )
                }
            }

            if (foundAny) {

                log(
                    "${indent}Readable content contains camera-related strings."
                )
            }

        } catch (e: Throwable) {

            log(
                "${indent}Read/search failed: " +
                    e.javaClass.simpleName +
                    ": " +
                    (e.message ?: "")
            )
        }
    }

    private fun buildAsciiView(
        data: ByteArray
    ): String {

        val builder =
            StringBuilder(
                data.size
            )

        for (b in data) {

            val value =
                b.toInt() and 0xFF

            if (
                value in 32..126
            ) {

                builder.append(
                    value.toChar()
                )

            } else {

                builder.append(' ')
            }
        }

        return builder.toString()
    }

    private fun log(
        message: String
    ) {

        runOnUiThread {

            output.append(
                message
            )

            output.append(
                "\n"
            )
        }
    }
}
