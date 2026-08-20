package com.example.vivocamera2probe

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var outputText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        outputText = TextView(this).apply {
            textSize = 13f
            setPadding(24, 24, 24, 24)
        }

        val scrollView = ScrollView(this)
        scrollView.addView(outputText)
        setContentView(scrollView)

        if (ContextCompat.checkSelfPermission(
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

        if (requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            runProbe()
        } else {
            outputText.text = "Camera permission denied."
        }
    }

    private fun runProbe() {

        val cameraManager =
            getSystemService(CAMERA_SERVICE) as CameraManager

        val sb = StringBuilder()

        sb.appendLine("VIVO HIDDEN CAMERA PROBE")
        sb.appendLine("==============================")
        sb.appendLine()

        val normalIds = try {
            cameraManager.cameraIdList.toList()
        } catch (e: Exception) {
            emptyList()
        }

        sb.appendLine("ANDROID ADVERTISED CAMERA IDs")
        sb.appendLine("------------------------------")

        if (normalIds.isEmpty()) {
            sb.appendLine("None")
        } else {
            normalIds.forEach {
                sb.appendLine("Camera ID: $it")
            }
        }

        sb.appendLine()
        sb.appendLine("==============================")
        sb.appendLine("BRUTE FORCE CAMERA ID TEST")
        sb.appendLine("==============================")
        sb.appendLine()

        val testIds = linkedSetOf<String>()

        // Standard numeric IDs
        for (i in 0..50) {
            testIds.add(i.toString())
        }

        // Some common OEM / Qualcomm / MediaTek style IDs
        val extraIds = listOf(
            "100",
            "101",
            "102",
            "103",
            "104",
            "105",
            "200",
            "201",
            "202",
            "203",
            "20",
            "21",
            "22",
            "23",
            "30",
            "31",
            "32",
            "40",
            "41",
            "50",
            "51"
        )

        testIds.addAll(extraIds)

        var discoveredCount = 0

        for (id in testIds) {

            try {

                val characteristics =
                    cameraManager.getCameraCharacteristics(id)

                discoveredCount++

                sb.appendLine("--------------------------------")
                sb.appendLine("ACCESSIBLE CAMERA ID: $id")

                if (normalIds.contains(id)) {
                    sb.appendLine("Status: ADVERTISED")
                } else {
                    sb.appendLine("*** Status: NOT IN cameraIdList ***")
                    sb.appendLine("*** POSSIBLE HIDDEN CAMERA ***")
                }

                appendCameraInfo(
                    sb,
                    id,
                    characteristics
                )

                sb.appendLine()

            } catch (_: IllegalArgumentException) {

                // Camera ID does not exist

            } catch (e: Exception) {

                sb.appendLine("--------------------------------")
                sb.appendLine("CAMERA ID: $id")
                sb.appendLine("Exists or responded, but access failed")
                sb.appendLine("Error: ${e.javaClass.simpleName}")
                sb.appendLine("Message: ${e.message}")
                sb.appendLine()

            }
        }

        sb.appendLine()
        sb.appendLine("==============================")
        sb.appendLine("RESULT")
        sb.appendLine("==============================")
        sb.appendLine()

        sb.appendLine("Advertised cameras: ${normalIds.size}")
        sb.appendLine("Accessible IDs found: $discoveredCount")

        if (discoveredCount > normalIds.size) {
            sb.appendLine()
            sb.appendLine("*** HIDDEN CAMERA ID DETECTED ***")
        } else {
            sb.appendLine()
            sb.appendLine("No additional hidden Camera2 IDs detected.")
        }

        outputText.text = sb.toString()
    }

    private fun appendCameraInfo(
        sb: StringBuilder,
        cameraId: String,
        characteristics: CameraCharacteristics
    ) {

        val facing =
            characteristics.get(
                CameraCharacteristics.LENS_FACING
            )

        val facingName = when (facing) {
            CameraCharacteristics.LENS_FACING_BACK -> "BACK"
            CameraCharacteristics.LENS_FACING_FRONT -> "FRONT"
            CameraCharacteristics.LENS_FACING_EXTERNAL -> "EXTERNAL"
            else -> "UNKNOWN"
        }

        sb.appendLine("Facing: $facingName")

        val hardwareLevel =
            characteristics.get(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
            )

        sb.appendLine(
            "Hardware Level: ${
                when (hardwareLevel) {
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

                    else -> "UNKNOWN"
                }
            }"
        )

        val pixelArray =
            characteristics.get(
                CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE
            )

        if (pixelArray != null) {

            val mp =
                pixelArray.width *
                        pixelArray.height /
                        1_000_000.0

            sb.appendLine(
                "Pixel Array: ${pixelArray.width} x ${pixelArray.height}"
            )

            sb.appendLine(
                "Pixel Array MP: %.2f".format(mp)
            )
        }

        val activeArray =
            characteristics.get(
                CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE
            )

        if (activeArray != null) {

            val mp =
                activeArray.width() *
                        activeArray.height() /
                        1_000_000.0

            sb.appendLine(
                "Active Array: ${activeArray.width()} x ${activeArray.height()}"
            )

            sb.appendLine(
                "Active Array MP: %.2f".format(mp)
            )
        }

        val physicalSize =
            characteristics.get(
                CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
            )

        if (physicalSize != null) {

            sb.appendLine(
                "Sensor Size: %.2f x %.2f mm".format(
                    physicalSize.width,
                    physicalSize.height
                )
            )
        }

        val focalLengths =
            characteristics.get(
                CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
            )

        if (focalLengths != null) {
            sb.appendLine(
                "Focal Lengths: ${
                    focalLengths.joinToString(", ") {
                        "%.2f mm".format(it)
                    }
                }"
            )
        }

        val physicalIds =
            if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.P
            ) {
                characteristics.physicalCameraIds
            } else {
                emptySet()
            }

        sb.appendLine(
            "Physical Camera IDs: ${
                if (physicalIds.isEmpty())
                    "NONE"
                else
                    physicalIds.joinToString(", ")
            }"
        )

        val capabilities =
            characteristics.get(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
            )

        if (capabilities != null) {

            sb.appendLine("Capabilities:")

            capabilities.forEach {

                val name = when (it) {

                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE ->
                        "BACKWARD_COMPATIBLE"

                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR ->
                        "MANUAL_SENSOR"

                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING ->
                        "MANUAL_POST_PROCESSING"

                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW ->
                        "RAW"

                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE ->
                        "BURST_CAPTURE"

                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA ->
                        "LOGICAL_MULTI_CAMERA"

                    else ->
                        "CAPABILITY $it"
                }

                sb.appendLine("  $name")
            }
        }

        val map =
            characteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
            )

        if (map != null) {

            val jpegSizes =
                map.getOutputSizes(
                    android.graphics.ImageFormat.JPEG
                )

            if (jpegSizes != null &&
                jpegSizes.isNotEmpty()
            ) {

                val largest =
                    jpegSizes.maxByOrNull {
                        it.width.toLong() *
                                it.height.toLong()
                    }

                if (largest != null) {

                    val mp =
                        largest.width *
                                largest.height /
                                1_000_000.0

                    sb.appendLine(
                        "Largest JPEG: ${largest.width} x ${largest.height}"
                    )

                    sb.appendLine(
                        "Largest JPEG MP: %.2f".format(mp)
                    )

                    if (mp >= 40) {
                        sb.appendLine(
                            "*** HIGH RESOLUTION SENSOR PATH FOUND ***"
                        )
                    }

                    if (mp >= 100) {
                        sb.appendLine(
                            "*** 100 MP+ CAMERA MODE FOUND ***"
                        )
                    }
                }
            }
        }

        // Dump vendor characteristic keys
        sb.appendLine()
        sb.appendLine("Vendor / Characteristic Keys:")

        try {

            val keys =
                characteristics.keys

            for (key in keys) {

                val name =
                    key.name

                if (
                    name.contains(
                        "vendor",
                        ignoreCase = true
                    ) ||
                    name.contains(
                        "vivo",
                        ignoreCase = true
                    ) ||
                    name.contains(
                        "sensor",
                        ignoreCase = true
                    ) ||
                    name.contains(
                        "pixel",
                        ignoreCase = true
                    ) ||
                    name.contains(
                        "resolution",
                        ignoreCase = true
                    ) ||
                    name.contains(
                        "quad",
                        ignoreCase = true
                    ) ||
                    name.contains(
                        "bin",
                        ignoreCase = true
                    ) ||
                    name.contains(
                        "super",
                        ignoreCase = true
                    ) ||
                    name.contains(
                        "high",
                        ignoreCase = true
                    )
                ) {

                    sb.appendLine("  $name")

                    try {

                        val value =
                            characteristics.get(key)

                        if (value != null) {
                            sb.appendLine(
                                "      = ${formatValue(value)}"
                            )
                        }

                    } catch (_: Exception) {
                    }
                }
            }

        } catch (e: Exception) {

            sb.appendLine(
                "Could not enumerate keys: ${e.message}"
            )
        }
    }

    private fun formatValue(value: Any): String {

        return when (value) {

            is IntArray ->
                value.joinToString(", ")

            is LongArray ->
                value.joinToString(", ")

            is FloatArray ->
                value.joinToString(", ")

            is DoubleArray ->
                value.joinToString(", ")

            is ByteArray ->
                value.take(50)
                    .joinToString(", ")

            is Array<*> ->
                value.joinToString(", ")

            else ->
                value.toString()
        }
    }
}
