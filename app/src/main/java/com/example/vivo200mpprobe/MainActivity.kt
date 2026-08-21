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
        "/system/etc/public.libraries-vivo.txt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildUi()
        readFile()
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

        val button =
            Button(this)

        button.text =
            "READ VIVO LIBRARIES"

        button.setOnClickListener {
            output.text = ""
            readFile()
        }

        root.addView(button)

        scroll =
            ScrollView(this)

        output =
            TextView(this)

        output.textSize =
            14f

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

    private fun readFile() {

        log("VIVO PUBLIC LIBRARIES")
        log("==============================")
        log("")
        log(targetFile)
        log("")

        val file =
            File(targetFile)

        log(
            "Exists: ${file.exists()}"
        )

        log(
            "Readable: ${file.canRead()}"
        )

        if (
            !file.exists() ||
            !file.canRead()
        ) {

            log("")
            log("FILE UNAVAILABLE")

            return
        }

        try {

            val lines =
                file.readLines()

            log("")
            log("==============================")
            log("ALL LIBRARIES")
            log("==============================")

            for (line in lines) {
                log(line)
            }

            log("")
            log("==============================")
            log("CAMERA / SENSOR / ISP MATCHES")
            log("==============================")

            var matches = 0

            for (line in lines) {

                val lower =
                    line.lowercase()

                if (
                    lower.contains("camera") ||
                    lower.contains("cam") ||
                    lower.contains("sensor") ||
                    lower.contains("isp") ||
                    lower.contains("jpeg") ||
                    lower.contains("image") ||
                    lower.contains("vivo") ||
                    lower.contains("hal") ||
                    lower.contains("raw")
                ) {

                    matches++

                    log(
                        "*** $line"
                    )
                }
            }

            log("")
            log(
                "Interesting libraries found: $matches"
            )

        } catch (e: Throwable) {

            log("")
            log("READ ERROR")
            log(
                e.javaClass.name
            )
            log(
                e.message ?: ""
            )
        }
    }

    private fun log(
        message: String
    ) {

        runOnUiThread {

            output.append(
                "$message\n"
            )

            scroll.post {
                scroll.fullScroll(
                    ScrollView.FOCUS_DOWN
                )
            }
        }
    }
}
