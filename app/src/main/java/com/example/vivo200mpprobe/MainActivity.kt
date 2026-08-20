package com.example.vivocamera2probe

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Size
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var outputText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        outputText = TextView(this).apply {
            textSize = 11f
            setPadding(24, 24, 24, 24)
            setTextIsSelectable(true)
        }

        setContentView(outputText)

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                100
            )
        } else {
            runProbe()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (
            requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            runProbe()
        } else {
            outputText.text = "Camera permission denied."
        }
    }

    private fun runProbe() {

        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        val report = StringBuilder()

        report.appendLine("VIVO CAMERA2 DEEP PROBE")
        report.appendLine("======================================")
        report.appendLine()
        report.appendLine("LOGICAL CAMERAS DETECTED: ${manager.cameraIdList.size}")
        report.appendLine()

        for (cameraId in manager.cameraIdList) {

            report.appendLine()
            report.appendLine("######################################")
            report.appendLine("CAMERA ID: $cameraId")
            report.appendLine("######################################")

            try {

                val chars = manager.getCameraCharacteristics(cameraId)

                val facing =
                    when (chars.get(CameraCharacteristics.LENS_FACING)) {
                        CameraCharacteristics.LENS_FACING_BACK -> "BACK"
                        CameraCharacteristics.LENS_FACING_FRONT -> "FRONT"
                        CameraCharacteristics.LENS_FACING_EXTERNAL -> "EXTERNAL"
                        else -> "UNKNOWN"
                    }

                report.appendLine("Facing: $facing")

                val level =
                    when (
                        chars.get(
                            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
                        )
                    ) {
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
                            "UNKNOWN"
                    }

                report.appendLine("Hardware Level: $level")
                report.appendLine()

                // ---------------------------------------------------
                // PHYSICAL CAMERA IDs
                // ---------------------------------------------------

                report.appendLine("PHYSICAL CAMERA IDS")
                report.appendLine("--------------------------------------")

                val physicalIds = chars.physicalCameraIds

                if (physicalIds.isEmpty()) {
                    report.appendLine("None")
                } else {

                    physicalIds.forEach { physicalId ->
                        report.appendLine("Physical Camera ID: $physicalId")
                    }
                }

                report.appendLine()

                // ---------------------------------------------------
                // SENSOR INFORMATION
                // ---------------------------------------------------

                report.appendLine("SENSOR INFORMATION")
                report.appendLine("--------------------------------------")

                val pixelArray =
                    chars.get(
                        CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE
                    )

                val activeArray =
                    chars.get(
                        CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE
                    )

                val physicalSize =
                    chars.get(
                        CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
                    )

                if (pixelArray != null) {

                    val mp =
                        pixelArray.width.toDouble() *
                                pixelArray.height.toDouble() /
                                1_000_000.0

                    report.appendLine(
                        "Pixel Array: ${pixelArray.width} x ${pixelArray.height}"
                    )

                    report.appendLine(
                        "Pixel Array MP: %.2f".format(mp)
                    )
                }

                if (activeArray != null) {

                    val mp =
                        activeArray.width().toDouble() *
                                activeArray.height().toDouble() /
                                1_000_000.0

                    report.appendLine(
                        "Active Array: ${activeArray.width()} x ${activeArray.height()}"
                    )

                    report.appendLine(
                        "Active Array MP: %.2f".format(mp)
                    )
                }

                if (physicalSize != null) {

                    report.appendLine(
                        "Physical Sensor Size: %.2f x %.2f mm".format(
                            physicalSize.width,
                            physicalSize.height
                        )
                    )
                }

                val focalLengths =
                    chars.get(
                        CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
                    )

                if (focalLengths != null) {

                    report.appendLine(
                        "Focal Lengths: ${
                            focalLengths.joinToString(", ") {
                                "%.2f mm".format(it)
                            }
                        }"
                    )
                }

                report.appendLine()

                // ---------------------------------------------------
                // CAPABILITIES
                // ---------------------------------------------------

                report.appendLine("CAPABILITIES")
                report.appendLine("--------------------------------------")

                val capabilities =
                    chars.get(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
                    )

                if (capabilities != null) {

                    capabilities.forEach {

                        report.appendLine(
                            capabilityName(it)
                        )
                    }
                }

                report.appendLine()

                // ---------------------------------------------------
                // ALL CHARACTERISTIC KEYS
                // ---------------------------------------------------

                report.appendLine("ALL CAMERA CHARACTERISTIC KEYS")
                report.appendLine("--------------------------------------")

                val keys =
                    chars.keys.sortedBy {
                        it.name.lowercase()
                    }

                for (key in keys) {

                    report.appendLine(key.name)

                    try {

                        val value =
                            chars.get(key)

                        report.appendLine(
                            "    VALUE: ${formatValue(value)}"
                        )

                    } catch (e: Exception) {

                        report.appendLine(
                            "    VALUE: <unable to read>"
                        )
                    }
                }

                report.appendLine()

                // ---------------------------------------------------
                // REQUEST KEYS
                // ---------------------------------------------------

                report.appendLine("AVAILABLE CAPTURE REQUEST KEYS")
                report.appendLine("--------------------------------------")

                chars.availableCaptureRequestKeys
                    .sortedBy {
                        it.name.lowercase()
                    }
                    .forEach {

                        report.appendLine(it.name)
                    }

                report.appendLine()

                // ---------------------------------------------------
                // RESULT KEYS
                // ---------------------------------------------------

                report.appendLine("AVAILABLE CAPTURE RESULT KEYS")
                report.appendLine("--------------------------------------")

                chars.availableCaptureResultKeys
                    .sortedBy {
                        it.name.lowercase()
                    }
                    .forEach {

                        report.appendLine(it.name)
                    }

                report.appendLine()

                // ---------------------------------------------------
                // SESSION KEYS
                // ---------------------------------------------------

                report.appendLine("AVAILABLE SESSION KEYS")
                report.appendLine("--------------------------------------")

                try {

                    chars.availableSessionKeys
                        .sortedBy {
                            it.name.lowercase()
                        }
                        .forEach {

                            report.appendLine(it.name)
                        }

                } catch (e: Exception) {

                    report.appendLine(
                        "Session keys unavailable."
                    )
                }

                report.appendLine()

                // ---------------------------------------------------
                // PHYSICAL CAMERA CHARACTERISTICS
                // ---------------------------------------------------

                if (physicalIds.isNotEmpty()) {

                    report.appendLine()
                    report.appendLine(
                        "======================================"
                    )

                    report.appendLine(
                        "PHYSICAL CAMERA DETAILS"
                    )

                    report.appendLine(
                        "======================================"
                    )

                    for (physicalId in physicalIds) {

                        report.appendLine()
                        report.appendLine(
                            "PHYSICAL CAMERA ID: $physicalId"
                        )

                        report.appendLine(
                            "--------------------------------------"
                        )

                        try {

                            val pChars =
                                manager.getCameraCharacteristics(
                                    physicalId
                                )

                            dumpPhysicalCamera(
                                physicalId,
                                pChars,
                                report
                            )

                        } catch (e: Exception) {

                            report.appendLine(
                                "Unable to access directly."
                            )

                            report.appendLine(
                                "Reason: ${e.message}"
                            )
                        }
                    }
                }

            } catch (e: Exception) {

                report.appendLine(
                    "ERROR READING CAMERA $cameraId"
                )

                report.appendLine(
                    e.stackTraceToString()
                )
            }

            report.appendLine()
        }

        // -----------------------------------------------------------
        // SEARCH HINT
        // -----------------------------------------------------------

        report.appendLine()
        report.appendLine("======================================")
        report.appendLine("SEARCH THESE TERMS IN THIS REPORT")
        report.appendLine("======================================")

        val terms =
            listOf(
                "vivo",
                "vendor",
                "remosaic",
                "remosaic",
                "highres",
                "high_resolution",
                "super",
                "ultra",
                "fullsize",
                "full_size",
                "fullpixel",
                "full_pixel",
                "200mp",
                "200m",
                "100mp",
                "50mp",
                "pixel",
                "quad",
                "bayer",
                "sensor",
                "raw",
                "maximum"
            )

        terms.forEach {

            report.appendLine(it)
        }

        outputText.text =
            report.toString()
    }

    private fun dumpPhysicalCamera(
        physicalId: String,
        chars: CameraCharacteristics,
        report: StringBuilder
    ) {

        val facing =
            when (
                chars.get(
                    CameraCharacteristics.LENS_FACING
                )
            ) {
                CameraCharacteristics.LENS_FACING_BACK ->
                    "BACK"

                CameraCharacteristics.LENS_FACING_FRONT ->
                    "FRONT"

                else ->
                    "UNKNOWN"
            }

        report.appendLine(
            "Facing: $facing"
        )

        val pixelArray =
            chars.get(
                CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE
            )

        if (pixelArray != null) {

            val mp =
                pixelArray.width.toDouble() *
                        pixelArray.height.toDouble() /
                        1_000_000.0

            report.appendLine(
                "Pixel Array: ${pixelArray.width} x ${pixelArray.height}"
            )

            report.appendLine(
                "Pixel Array MP: %.2f".format(mp)
            )
        }

        val active =
            chars.get(
                CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE
            )

        if (active != null) {

            val mp =
                active.width().toDouble() *
                        active.height().toDouble() /
                        1_000_000.0

            report.appendLine(
                "Active Array: ${active.width()} x ${active.height()}"
            )

            report.appendLine(
                "Active Array MP: %.2f".format(mp)
            )
        }

        val physicalSize =
            chars.get(
                CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
            )

        if (physicalSize != null) {

            report.appendLine(
                "Physical Sensor Size: %.2f x %.2f mm".format(
                    physicalSize.width,
                    physicalSize.height
                )
            )
        }

        val focalLengths =
            chars.get(
                CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
            )

        if (focalLengths != null) {

            report.appendLine(
                "Focal Lengths: ${
                    focalLengths.joinToString(", ") {
                        "%.2f mm".format(it)
                    }
                }"
            )
        }

        report.appendLine()
        report.appendLine(
            "CHARACTERISTIC KEYS"
        )

        report.appendLine(
            "--------------------------------------"
        )

        chars.keys
            .sortedBy {
                it.name.lowercase()
            }
            .forEach { key ->

                report.appendLine(
                    key.name
                )

                try {

                    val value =
                        chars.get(key)

                    report.appendLine(
                        "    VALUE: ${formatValue(value)}"
                    )

                } catch (e: Exception) {

                    report.appendLine(
                        "    VALUE: <unable to read>"
                    )
                }
            }
    }

    private fun capabilityName(
        capability: Int
    ): String {

        return when (capability) {

            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE ->
                "BACKWARD_COMPATIBLE"

            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR ->
                "MANUAL_SENSOR"

            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING ->
                "MANUAL_POST_PROCESSING"

            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW ->
                "RAW"

            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING ->
                "PRIVATE_REPROCESSING"

            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_READ_SENSOR_SETTINGS ->
                "READ_SENSOR_SETTINGS"

            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE ->
                "BURST_CAPTURE"

            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING ->
                "YUV_REPROCESSING"

            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT ->
                "DEPTH_OUTPUT"

            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO ->
                "HIGH_SPEED_VIDEO"

            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MOTION_TRACKING ->
                "MOTION_TRACKING"

            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA ->
                "LOGICAL_MULTI_CAMERA"

            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MONOCHROME ->
                "MONOCHROME"

            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_SECURE_IMAGE_DATA ->
                "SECURE_IMAGE_DATA"

            else ->
                "CAPABILITY $capability"
        }
    }

    private fun formatValue(
        value: Any?
    ): String {

        if (value == null) {
            return "null"
        }

        return when (value) {

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

            is Array<*> ->
                value.joinToString(
                    prefix = "[",
                    postfix = "]"
                )

            is Size ->
                "${value.width} x ${value.height}"

            else ->
                value.toString()
        }
    }
}
