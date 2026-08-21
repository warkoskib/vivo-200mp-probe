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
import java.io.FileInputStream
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var output: TextView
    private lateinit var scroll: ScrollView

    private val targetApks = listOf(
        "/system/app/VivoCamera/VivoCamera.apk",
        "/system/app/AlphaCamera/AlphaCamera.apk"
    )

    private val searchTerms = listOf(
        "200mp",
        "200MP",
        "real200mp",
        "ultra_highresolution",
        "portrait_high_resolution",
        "advance_fullsize",
        "remosaic",
        "Remosaic",
        "EngineerRemosaicMode",
        "sensorScenario",
        "forceSensorMode",
        "16320",
        "12288",
        "picturesize",
        "snapJpegSize",
        "streamsUsage",
        "ModeSelector",
        "VivoModeSelector",
        "highresolution",
        "fullsize"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildUi()

        log("VIVO STOCK CAMERA APK PROBE")
        log("==============================")
        log("")
        log("Targets:")
        log("/system/app/VivoCamera/VivoCamera.apk")
        log("/system/app/AlphaCamera/AlphaCamera.apk")
        log("")
        log("Press EXTRACT / SCAN APKS.")
    }

    private fun buildUi() {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setPadding(20, 30, 20, 30)

        val runButton = Button(this)

        runButton.text = "EXTRACT / SCAN APKS"

        runButton.setOnClickListener {

            output.text = ""
            runButton.isEnabled = false

            Thread {

                try {
                    runProbe()
                } finally {

                    runOnUiThread {
                        runButton.isEnabled = true
                    }
                }

            }.start()
        }

        root.addView(runButton)

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
                    "Vivo APK Probe",
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

        val clearButton = Button(this)

        clearButton.text = "CLEAR OUTPUT"

        clearButton.setOnClickListener {
            output.text = ""
        }

        root.addView(clearButton)

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

        log("VIVO STOCK CAMERA APK PROBE")
        log("==============================")

        for (apkPath in targetApks) {

            log("")
            log("")
            log("################################")
            log(apkPath)
            log("################################")

            val source = File(apkPath)

            log("Exists: ${source.exists()}")
            log("Readable: ${source.canRead()}")
            log("Size: ${if (source.exists()) source.length() else -1} bytes")

            if (!source.exists()) {

                log("RESULT: FILE DOES NOT EXIST")
                continue
            }

            if (!source.canRead()) {

                log("RESULT: ACCESS DENIED")
                continue
            }

            val destination =
                File(
                    getExternalFilesDir(null),
                    source.name
                )

            try {

                copyFile(
                    source,
                    destination
                )

                log("")
                log("COPY SUCCESS")

                log(
                    "Saved to:"
                )

                log(
                    destination.absolutePath
                )

                log(
                    "Copied size: ${destination.length()} bytes"
                )

            } catch (e: Throwable) {

                log("")
                log("COPY FAILED")
                log(e.javaClass.name)
                log(e.message ?: "")
            }

            log("")
            log("==============================")
            log("STRING SCAN")
            log("==============================")

            try {

                scanFileForStrings(
                    source
                )

            } catch (e: Throwable) {

                log("")
                log("STRING SCAN ERROR")
                log(e.javaClass.name)
                log(e.message ?: "")
            }
        }

        log("")
        log("")
        log("==============================")
        log("PROBE COMPLETE")
        log("==============================")
        log("")
        log("Press COPY OUTPUT.")
    }

    private fun copyFile(
        source: File,
        destination: File
    ) {

        FileInputStream(source).use { input ->

            FileOutputStream(destination).use { output ->

                val buffer =
                    ByteArray(
                        1024 * 1024
                    )

                while (true) {

                    val count =
                        input.read(buffer)

                    if (count <= 0) {
                        break
                    }

                    output.write(
                        buffer,
                        0,
                        count
                    )
                }
            }
        }
    }

    private fun scanFileForStrings(
        file: File
    ) {

        val maxBytes =
            minOf(
                file.length(),
                300L * 1024L * 1024L
            )

        val chunkSize =
            4 * 1024 * 1024

        val overlap =
            256

        val input =
            FileInputStream(file)

        val buffer =
            ByteArray(chunkSize)

        var totalRead =
            0L

        val foundTerms =
            mutableSetOf<String>()

        var previousTail =
            ByteArray(0)

        input.use {

            while (
                totalRead <
                maxBytes
            ) {

                val remaining =
                    maxBytes - totalRead

                val wanted =
                    minOf(
                        chunkSize.toLong(),
                        remaining
                    ).toInt()

                val count =
                    it.read(
                        buffer,
                        0,
                        wanted
                    )

                if (count <= 0) {
                    break
                }

                val combined =
                    ByteArray(
                        previousTail.size +
                            count
                    )

                System.arraycopy(
                    previousTail,
                    0,
                    combined,
                    0,
                    previousTail.size
                )

                System.arraycopy(
                    buffer,
                    0,
                    combined,
                    previousTail.size,
                    count
                )

                val text =
                    buildAsciiView(
                        combined
                    )

                for (term in searchTerms) {

                    if (
                        !foundTerms.contains(term) &&
                        text.contains(
                            term,
                            ignoreCase = true
                        )
                    ) {

                        foundTerms.add(term)

                        log(
                            "FOUND: $term"
                        )
                    }
                }

                val tailLength =
                    minOf(
                        overlap,
                        combined.size
                    )

                previousTail =
                    combined.copyOfRange(
                        combined.size -
                            tailLength,
                        combined.size
                    )

                totalRead +=
                    count.toLong()
            }
        }

        log("")
        log(
            "Bytes scanned: $totalRead"
        )

        if (foundTerms.isEmpty()) {

            log(
                "No target strings found."
            )

        } else {

            log("")
            log(
                "MATCH COUNT: ${foundTerms.size}"
            )

            log("")
            log("MATCHED TERMS:")

            for (term in foundTerms) {
                log("*** $term")
            }
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
                b.toInt() and
                    0xFF

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

            output.append(message)
            output.append("\n")
        }
    }
}
