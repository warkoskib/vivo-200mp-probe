package com.example.vivo200mpprobe

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var runButton: Button
    private lateinit var outputText: TextView
    private lateinit var scrollView: ScrollView

    private val nativeProbe = NativeProbe()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildUi()

        log("VIVO 200 MP NATIVE HAL PROBE")
        log("============================")
        log("")
        log("This test runs below the Java")
        log("Camera2 layer using C++/JNI.")
        log("")
        log("It will inspect:")
        log("/dev camera/video/media nodes")
        log("/sys camera-related paths")
        log("vendor camera libraries")
        log("camera-related configuration files")
        log("native access permissions")
        log("")
        log("Press RUN NATIVE HAL PROBE.")
    }

    private fun buildUi() {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL

        root.setPadding(
            24,
            40,
            24,
            40
        )

        runButton = Button(this)

        runButton.text =
            "RUN NATIVE HAL PROBE"

        runButton.setOnClickListener {
            runProbe()
        }

        root.addView(
            runButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        scrollView =
            ScrollView(this)

        outputText =
            TextView(this)

        outputText.textSize =
            13f

        outputText.setPadding(
            0,
            20,
            0,
            120
        )

        scrollView.addView(
            outputText
        )

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
    }

    private fun runProbe() {

        runButton.isEnabled = false

        runButton.text =
            "RUNNING..."

        log("")
        log("")
        log("============================")
        log("STARTING NATIVE PROBE")
        log("============================")
        log("")

        thread {

            try {

                val result =
                    nativeProbe.runNativeProbe()

                runOnUiThread {

                    outputText.append(
                        result
                    )

                    outputText.append(
                        "\n"
                    )

                    scrollView.post {

                        scrollView.fullScroll(
                            View.FOCUS_DOWN
                        )
                    }

                    runButton.text =
                        "RUN AGAIN"

                    runButton.isEnabled =
                        true
                }

            } catch (e: Throwable) {

                runOnUiThread {

                    log("")
                    log("NATIVE PROBE ERROR")
                    log("============================")
                    log(e.javaClass.name)
                    log(e.message ?: "")
                    log("")

                    runButton.text =
                        "RUN AGAIN"

                    runButton.isEnabled =
                        true
                }
            }
        }
    }

    private fun log(
        message: String
    ) {

        runOnUiThread {

            outputText.append(
                message
            )

            outputText.append(
                "\n"
            )

            scrollView.post {

                scrollView.fullScroll(
                    View.FOCUS_DOWN
                )
            }
        }
    }
}
