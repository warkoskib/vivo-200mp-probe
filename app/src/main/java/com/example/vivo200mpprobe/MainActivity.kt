package com.example.vivo200mpprobe

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
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
    }

    private fun buildUi() {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setPadding(20, 30, 20, 30)

        val button = Button(this)

        button.text = "SCAN CAMERA FILES"

        button.setOnClickListener {

            output.text = ""

            Thread {
                runScan()
            }.start()
        }

        root.addView(button)

        scroll = ScrollView(this)

        output = TextView(this)

        output.textSize = 13f
        output.setPadding(0, 20, 0, 100)

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

        var totalMatches = 0

        for (rootPath in roots) {

            log("")
            log("==============================")
            log("SCANNING")
            log(rootPath)
            log("==============================")

            val root = File(rootPath)

            log("Exists: ${root.exists()}")
            log("Readable: ${root.canRead()}")

            if (!root.exists() || !root.canRead()) {
                log("SKIPPED")
                continue
            }

            try {

                val children = root.listFiles()

                if (children == null) {

                    log("Cannot list directory.")
                    continue
                }

                log("Entries visible: ${children.size}")
                log("")

                for (file in children) {

                    val name = file.name.lowercase()

                    if (
                        terms.any {
                            name.contains(it)
                        }
                    ) {

                        totalMatches++

                        log("--------------------------------")
                        log("MATCH #$totalMatches")
                        log("Name: ${file.name}")
                        log("Path: ${file.absolutePath}")

                        log(
                            "Type: ${
                                if (file.isDirectory)
                                    "DIRECTORY"
                                else
                                    "FILE"
                            }"
                        )

                        log("Readable: ${file.canRead()}")

                        if (file.isFile) {

                            log(
                                "Size: ${file.length()} bytes"
                            )
                        }
                    }
                }

            } catch (e: Throwable) {

                log("ERROR:")
                log(e.javaClass.name)
                log(e.message ?: "")
            }
        }

        log("")
        log("")
        log("==============================")
        log("SCAN COMPLETE")
        log("==============================")
        log("Total matches: $totalMatches")
    }

    private fun log(message: String) {

        runOnUiThread {

            output.append(message)
            output.append("\n")
        }
    }
}
