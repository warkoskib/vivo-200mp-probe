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
        output.textSize = 13f
        output.setPadding(24, 48, 24, 48)

        val scroll = ScrollView(this)
        scroll.addView(output)
        setContentView(scroll)

        val text = StringBuilder()

        try {
            val manager =
                getSystemService(Context.CAMERA_SERVICE) as CameraManager

            val targetId = "2"

            text.append("VIVO PHYSICAL CAMERA DEEP PROBE\n")
            text.append("====================================\n\n")
            text.append("TARGET PHYSICAL CAMERA ID: $targetId\n\n")

            val characteristics =
                manager.getCameraCharacteristics(targetId)

            appendBasicInfo(text, characteristics)

            text.append("\n")
            text.append("====================================\n")
            text.append("ALL CAMERA CHARACTERISTIC KEYS\n")
            text.append("====================================\n\n")

            val keys = characteristics.keys

            text.append("TOTAL KEYS: ${keys.size}\n\n")

            for (key in keys.sortedBy { it.name.lowercase() }) {

                text.append("------------------------------------\n")
                text.append("KEY: ${key.name}\n")

                try {
                    val value = characteristics.get(key)

                    text.append("VALUE:\n")
                    text.append(formatValue(value))
                    text.append("\n")

                } catch (e: Throwable) {

                    text.append("READ ERROR: ")
                    text.append(e.javaClass.simpleName)
                    text.append(": ")
                    text.append(e.message)
                    text.append("\n")
                }
            }

            text.append("\n")
            text.append("====================================\n")
            text.append("INTERESTING KEYS\n")
            text.append("====================================\n\n")

            val interestingWords = listOf(
                "sensor",
                "pixel",
                "resolution",
                "quad",
                "remosaic",
                "bayer",
                "binning",
                "high",
                "super",
                "vivo",
                "raw",
                "stream",
                "size"
            )

            var interestingCount = 0

            for (key in keys.sortedBy { it.name.lowercase() }) {

                val lower = key.name.lowercase()

                if (interestingWords.any { lower.contains(it) }) {

                    interestingCount++

                    text.append("------------------------------------\n")
                    text.append("KEY: ${key.name}\n")

                    try {
                        val value = characteristics.get(key)

                        text.append("VALUE:\n")
                        text.append(formatValue(value))
                        text.append("\n")

                    } catch (e: Throwable) {

                        text.append("READ ERROR: ")
                        text.append(e.javaClass.simpleName)
                        text.append(": ")
                        text.append(e.message)
                        text.append("\n")
                    }
                }
            }

            text.append("\nInteresting keys found: $interestingCount\n")

            text.append("\n")
            text.append("====================================\n")
            text.append("PROBE COMPLETED\n")
            text.append("====================================\n")

        } catch (e: Throwable) {

            text.append("\n")
            text.append("FATAL PROBE ERROR\n")
            text.append("============================\n")
            text.append(e.javaClass.name)
            text.append("\n")
            text.append(e.message)
            text.append("\n")
        }

        output.text = text.toString()
    }

    private fun appendBasicInfo(
        text: StringBuilder,
        c: CameraCharacteristics
    ) {

        text.append("BASIC CAMERA INFORMATION\n")
        text.append("------------------------------------\n")

        try {
            val facing =
                c.get(CameraCharacteristics.LENS_FACING)

            text.append("Facing: ${facingName(facing)}\n")
        } catch (_: Throwable) {
        }

        try {
            val level =
                c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)

            text.append("Hardware Level: ${levelName(level)}\n")
        } catch (_: Throwable) {
        }

        try {
            val pixelArray =
                c.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)

            if (pixelArray != null) {

                val mp =
                    pixelArray.width.toDouble() *
                            pixelArray.height.toDouble() /
                            1_000_000.0

                text.append(
                    "Pixel Array: ${pixelArray.width} x ${pixelArray.height}"
                )

                text.append(
                    " = ${String.format("%.2f", mp)} MP\n"
                )
            }
        } catch (_: Throwable) {
        }

        try {
            val preCorrection =
                c.get(
                    CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE
                )

            if (preCorrection != null) {

                val mp =
                    preCorrection.width().toDouble() *
                            preCorrection.height().toDouble() /
                            1_000_000.0

                text.append(
                    "Pre-Correction Active Array: " +
                            "${preCorrection.width()} x ${preCorrection.height()}"
                )

                text.append(
                    " = ${String.format("%.2f", mp)} MP\n"
                )
            }
        } catch (_: Throwable) {
        }

        try {
            val active =
                c.get(
                    CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE
                )

            if (active != null) {

                val mp =
                    active.width().toDouble() *
                            active.height().toDouble() /
                            1_000_000.0

                text.append(
                    "Active Array: ${active.width()} x ${active.height()}"
                )

                text.append(
                    " = ${String.format("%.2f", mp)} MP\n"
                )
            }
        } catch (_: Throwable) {
        }

        try {
            val sensorSize =
                c.get(
                    CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
                )

            if (sensorSize != null) {

                text.append(
                    "Physical Sensor Size: " +
                            String.format(
                                "%.3f x %.3f mm\n",
                                sensorSize.width,
                                sensorSize.height
                            )
                )
            }
        } catch (_: Throwable) {
        }

        try {
            val focalLengths =
                c.get(
                    CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
                )

            if (focalLengths != null) {

                text.append("Focal Lengths: ")

                focalLengths.forEachIndexed { index, value ->

                    text.append(
                        String.format("%.3f mm", value)
                    )

                    if (index < focalLengths.size - 1) {
                        text.append(", ")
                    }
                }

                text.append("\n")
            }
        } catch (_: Throwable) {
        }

        try {
            val capabilities =
                c.get(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
                )

            if (capabilities != null) {

                text.append("Capabilities: ")

                capabilities.forEachIndexed { index, value ->

                    text.append(value)

                    if (index < capabilities.size - 1) {
                        text.append(", ")
                    }
                }

                text.append("\n")
            }
        } catch (_: Throwable) {
        }

        try {
            val physicalIds =
                c.physicalCameraIds

            text.append(
                "Nested Physical IDs: " +
                        if (physicalIds.isEmpty()) {
                            "NONE"
                        } else {
                            physicalIds.joinToString(", ")
                        }
            )

            text.append("\n")

        } catch (_: Throwable) {
        }
    }

    private fun formatValue(value: Any?): String {

        if (value == null) {
            return "null"
        }

        return try {

            when (value) {

                is IntArray ->
                    value.joinToString(
                        prefix = "[",
                        postfix = "]"
                    )

                is LongArray ->
                    value.joinToString(
                        prefix = "[",
                        postfix = "]"
                    )

                is FloatArray ->
                    value.joinToString(
                        prefix = "[",
                        postfix = "]"
                    )

                is DoubleArray ->
                    value.joinToString(
                        prefix = "[",
                        postfix = "]"
                    )

                is BooleanArray ->
                    value.joinToString(
                        prefix = "[",
                        postfix = "]"
                    )

                is ByteArray ->
                    value.joinToString(
                        prefix = "[",
                        postfix = "]"
                    ) {
                        String.format("0x%02X", it)
                    }

                is Array<*> ->
                    value.joinToString(
                        prefix = "[",
                        postfix = "]"
                    )

                else ->
                    value.toString()
            }

        } catch (e: Throwable) {

            "Could not format value: ${e.message}"
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
