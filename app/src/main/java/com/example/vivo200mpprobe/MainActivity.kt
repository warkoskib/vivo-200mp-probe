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
        "/system/etc/vivo_camera_third_compat.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildUi()

        readFile()
    }

    private fun buildUi() {

        val root = LinearLayout(this)

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
            "READ VIVO CAMERA CONFIG"

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

        log("VIVO THIRD-PARTY CAMERA CONFIG")
        log("==============================")
        log("")
        log("FILE:")
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

            val text =
                file.readText()

            log("")
            log("==============================")
            log("FULL FILE CONTENT")
            log("==============================")
            log("")

            log(text)

            log("")
            log("==============================")
            log("INTERESTING LINES")
            log("==============================")

            val terms =
                listOf(
                    "camera",
                    "package",
                    "third",
                    "allow",
                    "deny",
                    "white",
                    "black",
                    "resolution",
                    "high",
                    "200",
                    "200mp",
                    "16320",
                    "12288",
                    "pixel",
                    "sensor",
                    "jpeg",
                    "raw",
                    "remosaic",
                    "full"
                )

            var matches = 0

            for (
                (index, line)
                in text.lines()
                    .withIndex()
            ) {

                val lower =
                    line.lowercase()

                if (
                    terms.any {
                        lower.contains(it)
                    }
                ) {

                    matches++

                    log(
                        "LINE ${index + 1}:"
                    )

                    log(line)

                    log("")
                }
            }

            log(
                "Interesting lines found: $matches"
            )

        } catch (e: Throwable) {

            log("")
            log("READ FAILED")

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
                message
            )

            output.append(
                "\n"
            )

            scroll.post {

                scroll.fullScroll(
                    ScrollView.FOCUS_DOWN
                )
            }
        }
    }
}
