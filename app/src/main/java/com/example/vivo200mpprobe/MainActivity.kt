package com.example.vivo200mpprobe

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Size
import android.view.Gravity
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val cameraPermissionRequest = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                cameraPermissionRequest
            )
        } else {
            showCameraInfo()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == cameraPermissionRequest) {
            if (
                grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                showCameraInfo()
            } else {
                showText(
                    """
                    CAMERA PERMISSION DENIED

                    The app needs camera permission to inspect
                    the camera hardware exposed by Android.
                    """.trimIndent()
                )
            }
        }
    }

    private fun showCameraInfo() {

        val manager =
            getSystemService(CAMERA_SERVICE) as CameraManager

        val output = StringBuilder()

        output.append("VIVO CAMERA2 PROBE\n")
        output.append("==============================\n\n")

        try {

            val cameraIds = manager.cameraIdList

            output.append("CAMERAS DETECTED: ")
            output.append(cameraIds.size)
            output.append("\n\n")

            for (cameraId in cameraIds) {

                val c =
                    manager.getCameraCharacteristics(cameraId)

                output.append("================================\n")
                output.append("CAMERA ID: $cameraId\n")
                output.append("================================\n")

                val facing =
                    c.get(CameraCharacteristics.LENS_FACING)

                output.append(
                    "Facing: ${lensFacingName(facing)}\n"
                )

                val hardwareLevel =
                    c.get(
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
                    )

                output.append(
                    "Hardware Level: ${
                        hardwareLevelName(hardwareLevel)
                    }\n"
                )

                val capabilities =
                    c.get(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
                    )

                output.append("\nCAPABILITIES\n")
                output.append("------------------------------\n")

                if (capabilities != null) {

                    for (capability in capabilities) {
                        output.append(
                            capabilityName(capability)
                        )
                        output.append("\n")
                    }

                } else {
                    output.append("None reported\n")
                }

                val pixelArray =
                    c.get(
                        CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE
                    )

                val activeArray =
                    c.get(
                        CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE
                    )

                val physicalSize =
                    c.get(
                        CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
                    )

                output.append("\nSENSOR\n")
                output.append("------------------------------\n")

                if (pixelArray != null) {

                    output.append(
                        "Pixel Array: " +
                            "${pixelArray.width} x " +
                            "${pixelArray.height}\n"
                    )

                    output.append(
                        "Pixel Array MP: ${
                            megapixels(
                                pixelArray.width,
                                pixelArray.height
                            )
                        }\n"
                    )
                }

                if (activeArray != null) {

                    output.append(
                        "Active Array: " +
                            "${activeArray.width()} x " +
                            "${activeArray.height()}\n"
                    )

                    output.append(
                        "Active Array MP: ${
                            megapixels(
                                activeArray.width(),
                                activeArray.height()
                            )
                        }\n"
                    )
                }

                if (physicalSize != null) {
                    output.append(
                        "Physical Sensor Size: " +
                            String.format(
                                Locale.US,
                                "%.2f x %.2f mm\n",
                                physicalSize.width,
                                physicalSize.height
                            )
                    )
                }

                val focalLengths =
                    c.get(
                        CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
                    )

                if (focalLengths != null) {
                    output.append(
                        "Focal Lengths: " +
                            focalLengths.joinToString(
                                ", "
                            ) {
                                String.format(
                                    Locale.US,
                                    "%.2f mm",
                                    it
                                )
                            }
                    )
                    output.append("\n")
                }

                val streamMap =
                    c.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                    )

                if (streamMap == null) {

                    output.append(
                        "\nNo stream configuration map.\n\n"
                    )

                    continue
                }

                /*
                 * JPEG OUTPUTS
                 */

                val jpegSizes =
                    streamMap.getOutputSizes(
                        ImageFormat.JPEG
                    )

                output.append("\nJPEG OUTPUT SIZES\n")
                output.append("------------------------------\n")

                appendSizes(
                    output,
                    jpegSizes
                )

                /*
                 * YUV OUTPUTS
                 */

                val yuvSizes =
                    streamMap.getOutputSizes(
                        ImageFormat.YUV_420_888
                    )

                output.append("\nYUV OUTPUT SIZES\n")
                output.append("------------------------------\n")

                appendSizes(
                    output,
                    yuvSizes
                )

                /*
                 * RAW SENSOR
                 */

                val rawSizes =
                    try {
                        streamMap.getOutputSizes(
                            ImageFormat.RAW_SENSOR
                        )
                    } catch (e: Exception) {
                        null
                    }

                output.append("\nRAW_SENSOR OUTPUT SIZES\n")
                output.append("------------------------------\n")

                appendSizes(
                    output,
                    rawSizes
                )

                /*
                 * RAW10
                 */

                val raw10Sizes =
                    try {
                        streamMap.getOutputSizes(
                            ImageFormat.RAW10
                        )
                    } catch (e: Exception) {
                        null
                    }

                output.append("\nRAW10 OUTPUT SIZES\n")
                output.append("------------------------------\n")

                appendSizes(
                    output,
                    raw10Sizes
                )

                /*
                 * RAW12
                 */

                val raw12Sizes =
                    try {
                        streamMap.getOutputSizes(
                            ImageFormat.RAW12
                        )
                    } catch (e: Exception) {
                        null
                    }

                output.append("\nRAW12 OUTPUT SIZES\n")
                output.append("------------------------------\n")

                appendSizes(
                    output,
                    raw12Sizes
                )

                /*
                 * HIGH RESOLUTION JPEG
                 */

                val highResJpeg =
                    try {
                        streamMap.getHighResolutionOutputSizes(
                            ImageFormat.JPEG
                        )
                    } catch (e: Exception) {
                        null
                    }

                output.append("\nHIGH RESOLUTION JPEG\n")
                output.append("------------------------------\n")

                appendSizes(
                    output,
                    highResJpeg
                )

                /*
                 * HIGH RESOLUTION YUV
                 */

                val highResYuv =
                    try {
                        streamMap.getHighResolutionOutputSizes(
                            ImageFormat.YUV_420_888
                        )
                    } catch (e: Exception) {
                        null
                    }

                output.append("\nHIGH RESOLUTION YUV\n")
                output.append("------------------------------\n")

                appendSizes(
                    output,
                    highResYuv
                )

                /*
                 * HIGH RESOLUTION RAW
                 */

                val highResRaw =
                    try {
                        streamMap.getHighResolutionOutputSizes(
                            ImageFormat.RAW_SENSOR
                        )
                    } catch (e: Exception) {
                        null
                    }

                output.append("\nHIGH RESOLUTION RAW\n")
                output.append("------------------------------\n")

                appendSizes(
                    output,
                    highResRaw
                )

                /*
                 * MAXIMUM DISCOVERED RESOLUTION
                 */

                val allSizes =
                    mutableListOf<Size>()

                jpegSizes?.let {
                    allSizes.addAll(it)
                }

                yuvSizes?.let {
                    allSizes.addAll(it)
                }

                rawSizes?.let {
                    allSizes.addAll(it)
                }

                raw10Sizes?.let {
                    allSizes.addAll(it)
                }

                raw12Sizes?.let {
                    allSizes.addAll(it)
                }

                highResJpeg?.let {
                    allSizes.addAll(it)
                }

                highResYuv?.let {
                    allSizes.addAll(it)
                }

                highResRaw?.let {
                    allSizes.addAll(it)
                }

                if (allSizes.isNotEmpty()) {

                    val biggest =
                        allSizes.maxByOrNull {
                            it.width.toLong() *
                                it.height.toLong()
                        }

                    if (biggest != null) {

                        output.append(
                            "\n*** MAXIMUM EXPOSED RESOLUTION ***\n"
                        )

                        output.append(
                            "${biggest.width} x " +
                                "${biggest.height}\n"
                        )

                        output.append(
                            "${
                                megapixels(
                                    biggest.width,
                                    biggest.height
                                )
                            } MP\n"
                        )
                    }
                }

                /*
                 * CHECK SPECIFICALLY FOR > 100 MP
                 */

                val hugeSizes =
                    allSizes
                        .distinctBy {
                            "${it.width}x${it.height}"
                        }
                        .filter {
                            (
                                it.width.toLong() *
                                    it.height.toLong()
                            ) >= 100_000_000L
                        }
                        .sortedByDescending {
                            it.width.toLong() *
                                it.height.toLong()
                        }

                output.append(
                    "\n100 MP+ MODES DETECTED\n"
                )

                output.append(
                    "------------------------------\n"
                )

                if (hugeSizes.isEmpty()) {

                    output.append(
                        "NONE exposed through standard Camera2 outputs.\n"
                    )

                } else {

                    for (size in hugeSizes) {

                        output.append(
                            "${size.width} x " +
                                "${size.height}  =  "
                        )

                        output.append(
                            megapixels(
                                size.width,
                                size.height
                            )
                        )

                        output.append(" MP\n")
                    }
                }

                output.append("\n\n")
            }

        } catch (e: Exception) {

            output.append("\nERROR\n")
            output.append("==============================\n")
            output.append(e.javaClass.simpleName)
            output.append("\n")
            output.append(e.message ?: "Unknown error")
            output.append("\n")
        }

        showText(
            output.toString()
        )
    }

    private fun appendSizes(
        builder: StringBuilder,
        sizes: Array<Size>?
    ) {

        if (sizes == null || sizes.isEmpty()) {
            builder.append("None\n")
            return
        }

        val sorted =
            sizes.sortedByDescending {
                it.width.toLong() *
                    it.height.toLong()
            }

        for (size in sorted) {

            builder.append(
                String.format(
                    Locale.US,
                    "%d x %d   =   %.2f MP\n",
                    size.width,
                    size.height,
                    (
                        size.width.toDouble() *
                            size.height.toDouble()
                        ) / 1_000_000.0
                )
            )
        }
    }

    private fun megapixels(
        width: Int,
        height: Int
    ): String {

        val mp =
            (
                width.toDouble() *
                    height.toDouble()
                ) / 1_000_000.0

        return String.format(
            Locale.US,
            "%.2f",
            mp
        )
    }

    private fun lensFacingName(
        value: Int?
    ): String {

        return when (value) {

            CameraCharacteristics.LENS_FACING_BACK ->
                "BACK"

            CameraCharacteristics.LENS_FACING_FRONT ->
                "FRONT"

            CameraCharacteristics.LENS_FACING_EXTERNAL ->
                "EXTERNAL"

            else ->
                "UNKNOWN"
        }
    }

    private fun hardwareLevelName(
        value: Int?
    ): String {

        return when (value) {

            CameraCharacteristics
                .INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY ->
                "LEGACY"

            CameraCharacteristics
                .INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED ->
                "LIMITED"

            CameraCharacteristics
                .INFO_SUPPORTED_HARDWARE_LEVEL_FULL ->
                "FULL"

            CameraCharacteristics
                .INFO_SUPPORTED_HARDWARE_LEVEL_3 ->
                "LEVEL 3"

            CameraCharacteristics
                .INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL ->
                "EXTERNAL"

            else ->
                "UNKNOWN"
        }
    }

    private fun capabilityName(
        value: Int
    ): String {

        return when (value) {

            CameraCharacteristics
                .REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE ->
                "BACKWARD_COMPATIBLE"

            CameraCharacteristics
                .REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR ->
                "MANUAL_SENSOR"

            CameraCharacteristics
                .REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING ->
                "MANUAL_POST_PROCESSING"

            CameraCharacteristics
                .REQUEST_AVAILABLE_CAPABILITIES_RAW ->
                "RAW"

            CameraCharacteristics
                .REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING ->
                "PRIVATE_REPROCESSING"

            CameraCharacteristics
                .REQUEST_AVAILABLE_CAPABILITIES_READ_SENSOR_SETTINGS ->
                "READ_SENSOR_SETTINGS"

            CameraCharacteristics
                .REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE ->
                "BURST_CAPTURE"

            CameraCharacteristics
                .REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING ->
                "YUV_REPROCESSING"

            CameraCharacteristics
                .REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT ->
                "DEPTH_OUTPUT"

            CameraCharacteristics
                .REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO ->
                "HIGH_SPEED_VIDEO"

            CameraCharacteristics
                .REQUEST_AVAILABLE_CAPABILITIES_MOTION_TRACKING ->
                "MOTION_TRACKING"

            CameraCharacteristics
                .REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA ->
                "LOGICAL_MULTI_CAMERA"

            CameraCharacteristics
                .REQUEST_AVAILABLE_CAPABILITIES_MONOCHROME ->
                "MONOCHROME"

            CameraCharacteristics
                .REQUEST_AVAILABLE_CAPABILITIES_SECURE_IMAGE_DATA ->
                "SECURE_IMAGE_DATA"

            CameraCharacteristics
                .REQUEST_AVAILABLE_CAPABILITIES_SYSTEM_CAMERA ->
                "SYSTEM_CAMERA"

            CameraCharacteristics
                .REQUEST_AVAILABLE_CAPABILITIES_OFFLINE_PROCESSING ->
                "OFFLINE_PROCESSING"

            else ->
                "CAPABILITY $value"
        }
    }

    private fun showText(
        text: String
    ) {

        val textView =
            TextView(this).apply {

                this.text = text

                textSize = 14f

                setPadding(
                    28,
                    28,
                    28,
                    28
                )

                gravity = Gravity.START

                setTextIsSelectable(true)
            }

        val scrollView =
            ScrollView(this).apply {

                addView(textView)
            }

        setContentView(scrollView)
    }
}
