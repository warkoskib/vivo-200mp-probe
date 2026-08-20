package com.example.vivo200mpprobe

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val output = TextView(this)
        output.textSize = 16f
        output.setPadding(40, 60, 40, 60)

        val scrollView = ScrollView(this)
        scrollView.addView(output)
        setContentView(scrollView)

        try {
            val manager =
                getSystemService(Context.CAMERA_SERVICE) as CameraManager

            val cameraIds = manager.cameraIdList

            val text = StringBuilder()

            text.append("VIVO CAMERA HARDWARE PROBE\n")
            text.append("=========================\n\n")
            text.append("CAMERAS DETECTED: ${cameraIds.size}\n\n")

            for (id in cameraIds) {

                text.append("-------------------------\n")
                text.append("CAMERA ID: $id\n")

                try {
                    val characteristics =
                        manager.getCameraCharacteristics(id)

                    val facing =
                        characteristics.get(
                            CameraCharacteristics.LENS_FACING
                        )

                    val level =
                        characteristics.get(
                            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
                        )

                    text.append("Facing: ${facingName(facing)}\n")
                    text.append("Hardware Level: ${levelName(level)}\n")

                } catch (e: Exception) {
                    text.append("ERROR READING CAMERA: ${e.message}\n")
                }

                text.append("\n")
            }

            text.append("=========================\n")
            text.append("PROBE COMPLETED\n")

            output.text = text.toString()

        } catch (e: Exception) {

            output.text = """
                VIVO CAMERA HARDWARE PROBE

                PROBE FAILED

                ${e.javaClass.name}

                ${e.message}
            """.trimIndent()
        }
    }

    private fun facingName(value: Int?): String {

        return when (value) {

            CameraCharacteristics.LENS_FACING_BACK ->
                "BACK"

            CameraCharacteristics.LENS_FACING_FRONT ->
                "FRONT"

            CameraCharacteristics.LENS_FACING_EXTERNAL ->
                "EXTERNAL"

            else ->
                "UNKNOWN ($value)"
        }
    }

    private fun levelName(value: Int?): String {

        return when (value) {

            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY ->
                "LEGACY"

            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED ->
                "LIMITED"

            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL ->
                "FULL"

            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 ->
                "LEVEL 3"

            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL ->
                "EXTERNAL"

            else ->
                "UNKNOWN ($value)"
        }
    }
}
