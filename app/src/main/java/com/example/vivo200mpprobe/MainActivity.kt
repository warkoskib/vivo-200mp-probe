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

    private val targetFile =
        "/system/etc/public.libraries-mtk.txt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildUi()
        readFile()
    }

    private fun buildUi() {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setPadding(20, 30, 20, 30)

        val button = Button(this)

        button.text = "READ MEDIATEK LIBRARIES"

        button.setOnClickListener {
            output.text = ""
            readFile()
        }

        root.addView(button)

        scroll = ScrollView(this)

        output = TextView(this)

        output.textSize = 14f
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

    private fun readFile() {

        log("MEDIATEK PUBLIC LIBRARIES")
        log("==============================")
        log("")
        log(targetFile)
        log("")

        val file = File(targetFile)

        log("Exists: ${file.exists()}")
        log("Readable: ${file.canRead()}")
        log("Size: ${if (file.exists()) file.length() else -1} bytes")

        if (!file.exists()) {
            log("")
            log("FILE DOES NOT EXIST")
            return
        }

        if (!file.canRead()) {
            log("")
            log("ACCESS DENIED")
            return
        }

        try {

            val lines = file.readLines()

            log("")
            log("==============================")
            log("ALL MEDIATEK LIBRARIES")
            log("==============================")

            for ((index, line) in lines.withIndex()) {

                log("${index + 1}: $line")
            }

            log("")
            log("==============================")
            log("CAMERA / SENSOR / ISP MATCHES")
            log("==============================")

            val terms = listOf(
                "camera",
                "cam",
                "sensor",
                "isp",
                "jpeg",
                "image",
                "raw",
                "remosaic",
                "mtk",
                "mediatek",
                "3a",
                "aaa",
                "p1",
                "p2"
            )

            var matches = 0

            for ((index, line) in lines.withIndex()) {

                val lower = line.lowercase()

                if (
                    terms.any {
                        lower.contains(it)
                    }
                ) {

                    matches++

                    log(
                        "*** LINE ${index + 1}: $line"
                    )
                }
            }

            log("")
            log("==============================")
            log("RESULT")
            log("==============================")

            log(
                "Total libraries: ${lines.size}"
            )

            log(
                "Interesting matches: $matches"
            )

        } catch (e: Throwable) {

            log("")
            log("==============================")
            log("READ ERROR")
            log("==============================")

            log(e.javaClass.name)
            log(e.message ?: "")
        }
    }

    private fun log(message: String) {

        runOnUiThread {

            output.append(message)
            output.append("\n")

            scroll.post {

                scroll.fullScroll(
                    ScrollView.FOCUS_DOWN
                )
            }
        }
    }
}
