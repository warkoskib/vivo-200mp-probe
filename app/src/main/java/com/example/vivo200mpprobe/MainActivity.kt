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
        output.textSize = 15f
        output.setPadding(30, 50, 30, 50)

        val scrollView = ScrollView(this)
        scrollView.addView(output)

        setContentView(scrollView)

        try {
            val manager =
                getSystemService(Context.CAMERA_SERVICE) as CameraManager

            val cameraIds = manager.cameraIdList

            val text = StringBuilder()

            text.append("VIVO CAMERA PHYSICAL-ID PROBE\n")
            text.append("================================\n\n")
            text.append("TOP-LEVEL CAMERAS DETECTED: ${cameraIds.size}\n\n")

            for (id in cameraIds) {

                text.append("================================\n")
                text.append("LOGICAL CAMERA ID: $id\n")
                text.append("================================\n")

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
                    text.append("Hardware Level: ${levelName(level)}\n\n")

                    val physicalIds =
                        characteristics.physicalCameraIds

                    text.append("PHYSICAL CAMERA IDS\n")
                    text.append("------------------------------\n")

                    if (physicalIds.isEmpty()) {
                        text.append("NONE EXPOSED\n")
                    } else {

                        text.append("Count: ${physicalIds.size}\n\n")

                        for (physicalId in physicalIds) {

                            text.append("Physical ID: $physicalId\n")

                            try {
                                val physicalCharacteristics =
                                    manager.getCameraCharacteristics(
                                        physicalId
                                    )

                                val physicalFacing =
                                    physicalCharacteristics.get(
                                        CameraCharacteristics.LENS_FACING
                                    )

                                val physicalLevel =
                                    physicalCharacteristics.get(
                                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
                                    )

                                val focalLengths =
                                    physicalCharacteristics.get(
                                        CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
                                    )

                                val sensorSize =
                                    physicalCharacteristics.get(
                                        CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
                                    )

                                val pixelArray =
                                    physicalCharacteristics.get(
                                        CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE
                                    )

                                val activeArray =
                                    physicalCharacteristics.get(
                                        CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE
                                    )

                                text.append(
                                    "  Facing: ${facingName(physicalFacing)}\n"
                                )

                                text.append(
                                    "  Hardware Level: ${levelName(physicalLevel)}\n"
                                )

                                if (focalLengths != null) {

                                    text.append("  Focal Lengths: ")

                                    focalLengths.forEachIndexed { index, value ->

                                        text.append(
                                            String.format(
                                                "%.2f mm",
                                                value
                                            )
                                        )

                                        if (index < focalLengths.size - 1) {
                                            text.append(", ")
                                        }
                                    }

                                    text.append("\n")
                                }

                                if (sensorSize != null) {
                                    text.append(
                                        "  Physical Sensor Size: " +
                                                String.format(
                                                    "%.2f x %.2f mm\n",
                                                    sensorSize.width,
                                                    sensorSize.height
                                                )
                                    )
                                }

                                if (pixelArray != null) {

                                    val mp =
                                        pixelArray.width.toDouble() *
                                                pixelArray.height.toDouble() /
                                                1_000_000.0

                                    text.append(
                                        "  Pixel Array: " +
                                                "${pixelArray.width} x ${pixelArray.height}"
                                    )

                                    text.append(
                                        " = ${String.format("%.2f", mp)} MP\n"
                                    )
                                }

                                if (activeArray != null) {

                                    val width =
                                        activeArray.width()

                                    val height =
                                        activeArray.height()

                                    val mp =
                                        width.toDouble() *
                                                height.toDouble() /
                                                1_000_000.0

                                    text.append(
                                        "  Active Array: " +
                                                "$width x $height"
                                    )

                                    text.append(
                                        " = ${String.format("%.2f", mp)} MP\n"
                                    )
                                }

                            } catch (e: Exception) {

                                text.append(
                                    "  Could not read physical camera characteristics\n"
                                )

                                text.append(
                                    "  ${e.javaClass.simpleName}: ${e.message}\n"
                                )
                            }

                            text.append("\n")
                        }
                    }

                    text.append("\n")

                    val focalLengths =
                        characteristics.get(
                            CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
                        )

                    text.append("LOGICAL CAMERA FOCAL LENGTHS\n")
                    text.append("------------------------------\n")

                    if (focalLengths == null) {

                        text.append("None\n")

                    } else {

                        focalLengths.forEach {

                            text.append(
                                String.format(
                                    "%.2f mm\n",
                                    it
                                )
                            )
                        }
                    }

                } catch (e: Exception) {

                    text.append(
                        "ERROR READING LOGICAL CAMERA\n"
                    )

                    text.append(
                        "${e.javaClass.name}\n"
                    )

                    text.append(
                        "${e.message}\n"
                    )
                }

                text.append("\n\n")
            }

            text.append("================================\n")
            text.append("PROBE COMPLETED\n")

            output.text = text.toString()

        } catch (e: Exception) {

            output.text = """
                VIVO CAMERA PHYSICAL-ID PROBE

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
