package com.example.vivo200mpprobe

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_ID = "3"
    }

    private lateinit var output: TextView
    private lateinit var scroll: ScrollView
    private lateinit var cameraManager: CameraManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraManager =
            getSystemService(CAMERA_SERVICE) as CameraManager

        buildUi()

        log("CAMERA 3 RAW PATH PROBE")
        log("==============================")
        log("")
        log("Camera ID: $CAMERA_ID")
        log("Device: ${Build.MODEL}")
        log("Android: ${Build.VERSION.RELEASE}")
        log("SDK: ${Build.VERSION.SDK_INT}")
        log("")
        log("Press SCAN RAW PATHS.")
    }

    private fun buildUi() {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setPadding(20, 30, 20, 30)

        // -------------------------------------------------
        // SCAN BUTTON
        // -------------------------------------------------

        val scanButton = Button(this)

        scanButton.text = "SCAN RAW PATHS"

        scanButton.setOnClickListener {

            output.text = ""

            scanButton.isEnabled = false

            Thread {

                try {
                    runProbe()
                } finally {

                    runOnUiThread {
                        scanButton.isEnabled = true
                    }
                }

            }.start()
        }

        root.addView(scanButton)

        // -------------------------------------------------
        // COPY BUTTON
        // -------------------------------------------------

        val copyButton = Button(this)

        copyButton.text = "COPY OUTPUT"

        copyButton.setOnClickListener {

            val text = output.text.toString()

            if (text.isBlank()) {

                Toast.makeText(
                    this,
                    "No output to copy.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val clipboard =
                getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as ClipboardManager

            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    "Camera 3 RAW Probe",
                    text
                )
            )

            Toast.makeText(
                this,
                "Output copied",
                Toast.LENGTH_SHORT
            ).show()
        }

        root.addView(copyButton)

        // -------------------------------------------------
        // CLEAR BUTTON
        // -------------------------------------------------

        val clearButton = Button(this)

        clearButton.text = "CLEAR OUTPUT"

        clearButton.setOnClickListener {
            output.text = ""
        }

        root.addView(clearButton)

        // -------------------------------------------------
        // OUTPUT
        // -------------------------------------------------

        scroll = ScrollView(this)

        output = TextView(this)

        output.textSize = 13f
        output.setTextIsSelectable(true)

        output.setPadding(
            0,
            20,
            0,
            120
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

    // =====================================================
    // MAIN PROBE
    // =====================================================

    private fun runProbe() {

        log("CAMERA 3 RAW PATH PROBE")
        log("==============================")
        log("")

        try {

            val chars =
                cameraManager.getCameraCharacteristics(
                    CAMERA_ID
                )

            dumpSensorInfo(chars)
            dumpCapabilities(chars)

            val map =
                chars.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                )

            if (map == null) {

                log("")
                log("NO STREAM CONFIGURATION MAP")
                return
            }

            dumpOutputFormats(map)

            dumpSpecificFormat(
                map,
                ImageFormat.RAW_SENSOR,
                "RAW_SENSOR / RAW16"
            )

            dumpSpecificFormat(
                map,
                ImageFormat.RAW10,
                "RAW10"
            )

            dumpSpecificFormat(
                map,
                ImageFormat.RAW12,
                "RAW12"
            )

            dumpSpecificFormat(
                map,
                ImageFormat.RAW_PRIVATE,
                "RAW_PRIVATE"
            )

            dumpSpecificFormat(
                map,
                ImageFormat.PRIVATE,
                "PRIVATE / OPAQUE"
            )

            dumpSpecificFormat(
                map,
                ImageFormat.JPEG,
                "JPEG"
            )

            dumpSpecificFormat(
                map,
                ImageFormat.YUV_420_888,
                "YUV_420_888"
            )

            dumpHighResolution(map)

            dumpInputFormats(map)

            dumpReprocessingCapabilities(chars)

            dumpPhysicalCameraIds(chars)

            dumpAllCharacteristicKeys(chars)

            log("")
            log("")
            log("==============================")
            log("PROBE COMPLETE")
            log("==============================")
            log("")
            log("Press COPY OUTPUT.")

        } catch (e: Throwable) {

            log("")
            log("==============================")
            log("PROBE ERROR")
            log("==============================")

            log(e.javaClass.name)
            log(e.message ?: "")
        }
    }

    // =====================================================
    // SENSOR INFORMATION
    // =====================================================

    private fun dumpSensorInfo(
        chars: CameraCharacteristics
    ) {

        log("==============================")
        log("SENSOR INFORMATION")
        log("==============================")

        val pixelArray =
            chars.get(
                CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE
            )

        val activeArray =
            chars.get(
                CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE
            )

        val preCorrection =
            chars.get(
                CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE
            )

        val physicalSize =
            chars.get(
                CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
            )

        val maxAnalog =
            chars.get(
                CameraCharacteristics.SENSOR_MAX_ANALOG_SENSITIVITY
            )

        val sensitivity =
            chars.get(
                CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE
            )

        val exposure =
            chars.get(
                CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE
            )

        val CFA =
            chars.get(
                CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT
            )

        log("")
        log("Pixel array:")
        log(pixelArray?.toString() ?: "NOT EXPOSED")

        if (pixelArray != null) {
            logMP(
                pixelArray.width,
                pixelArray.height
            )
        }

        log("")
        log("Active array:")
        log(activeArray?.toString() ?: "NOT EXPOSED")

        if (activeArray != null) {

            logMP(
                activeArray.width(),
                activeArray.height()
            )
        }

        log("")
        log("Pre-correction active array:")
        log(
            preCorrection?.toString()
                ?: "NOT EXPOSED"
        )

        log("")
        log("Physical sensor size:")
        log(
            physicalSize?.toString()
                ?: "NOT EXPOSED"
        )

        log("")
        log("Color filter arrangement:")
        log(
            cfaName(CFA)
        )

        log("")
        log("Sensitivity range:")
        log(
            sensitivity?.toString()
                ?: "NOT EXPOSED"
        )

        log("")
        log("Max analog sensitivity:")
        log(
            maxAnalog?.toString()
                ?: "NOT EXPOSED"
        )

        log("")
        log("Exposure range:")
        log(
            exposure?.toString()
                ?: "NOT EXPOSED"
        )

        log("")
    }

    // =====================================================
    // CAPABILITIES
    // =====================================================

    private fun dumpCapabilities(
        chars: CameraCharacteristics
    ) {

        log("==============================")
        log("CAMERA CAPABILITIES")
        log("==============================")

        val caps =
            chars.get(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
            )

        if (caps == null) {

            log("No capability list.")
            return
        }

        for (cap in caps) {

            log(
                "$cap = ${capabilityName(cap)}"
            )
        }

        log("")
    }

    // =====================================================
    // OUTPUT FORMATS
    // =====================================================

    private fun dumpOutputFormats(
        map: StreamConfigurationMap
    ) {

        log("==============================")
        log("ALL OUTPUT FORMATS")
        log("==============================")

        try {

            val formats =
                map.outputFormats

            for (format in formats) {

                log("")
                log(
                    "$format = ${formatName(format)}"
                )

                val sizes =
                    try {
                        map.getOutputSizes(format)
                    } catch (_: Throwable) {
                        null
                    }

                if (sizes == null) {

                    log("  No sizes exposed.")
                    continue
                }

                log(
                    "  Sizes: ${sizes.size}"
                )

                for (size in sortSizes(sizes)) {

                    logSize(
                        "  ",
                        size
                    )
                }
            }

        } catch (e: Throwable) {

            log(
                "Output format error: " +
                    e.toString()
            )
        }

        log("")
    }

    // =====================================================
    // SPECIFIC FORMAT
    // =====================================================

    private fun dumpSpecificFormat(
        map: StreamConfigurationMap,
        format: Int,
        label: String
    ) {

        log("==============================")
        log(label)
        log("==============================")

        try {

            val sizes =
                map.getOutputSizes(format)

            if (sizes == null) {

                log("NOT EXPOSED")
                log("")
                return
            }

            log(
                "Format integer: $format"
            )

            log(
                "Number of sizes: ${sizes.size}"
            )

            var largest: Size? = null

            for (size in sortSizes(sizes)) {

                logSize(
                    "",
                    size
                )

                if (
                    largest == null ||
                    pixels(size) >
                    pixels(largest)
                ) {

                    largest = size
                }
            }

            if (largest != null) {

                log("")
                log("LARGEST:")
                logSize(
                    "",
                    largest
                )
            }

        } catch (e: Throwable) {

            log(
                "ERROR: " +
                    e.javaClass.simpleName +
                    ": " +
                    (e.message ?: "")
            )
        }

        log("")
    }

    // =====================================================
    // HIGH RESOLUTION
    // =====================================================

    private fun dumpHighResolution(
        map: StreamConfigurationMap
    ) {

        log("==============================")
        log("HIGH-RESOLUTION OUTPUT SIZES")
        log("==============================")

        val formats = listOf(
            Pair(
                ImageFormat.RAW_SENSOR,
                "RAW_SENSOR"
            ),
            Pair(
                ImageFormat.RAW10,
                "RAW10"
            ),
            Pair(
                ImageFormat.RAW12,
                "RAW12"
            ),
            Pair(
                ImageFormat.RAW_PRIVATE,
                "RAW_PRIVATE"
            ),
            Pair(
                ImageFormat.JPEG,
                "JPEG"
            ),
            Pair(
                ImageFormat.YUV_420_888,
                "YUV"
            )
        )

        for ((format, name) in formats) {

            log("")
            log("$name:")

            try {

                val sizes =
                    map.getHighResolutionOutputSizes(
                        format
                    )

                if (sizes == null) {

                    log("  NONE")
                    continue
                }

                if (sizes.isEmpty()) {

                    log("  EMPTY")
                    continue
                }

                for (size in sortSizes(sizes)) {

                    logSize(
                        "  ",
                        size
                    )
                }

            } catch (e: Throwable) {

                log(
                    "  ERROR: " +
                        e.javaClass.simpleName
                )
            }
        }

        log("")
    }

    // =====================================================
    // INPUT / REPROCESSING
    // =====================================================

    private fun dumpInputFormats(
        map: StreamConfigurationMap
    ) {

        log("==============================")
        log("INPUT / REPROCESSING FORMATS")
        log("==============================")

        try {

            val formats =
                map.inputFormats

            if (formats.isEmpty()) {

                log(
                    "No input formats exposed."
                )

                log("")
                return
            }

            for (format in formats) {

                log("")
                log(
                    "$format = ${formatName(format)}"
                )

                try {

                    val sizes =
                        map.getInputSizes(format)

                    if (sizes == null) {

                        log(
                            "  No input sizes."
                        )

                    } else {

                        for (size in sortSizes(sizes)) {

                            logSize(
                                "  ",
                                size
                            )
                        }
                    }

                } catch (e: Throwable) {

                    log(
                        "  Size query error."
                    )
                }
            }

        } catch (e: Throwable) {

            log(
                "Input format query error: " +
                    e.toString()
            )
        }

        log("")
    }

    private fun dumpReprocessingCapabilities(
        chars: CameraCharacteristics
    ) {

        log("==============================")
        log("REPROCESSING CHECK")
        log("==============================")

        val caps =
            chars.get(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
            ) ?: intArrayOf()

        val privateReprocess =
            caps.contains(
                CameraCharacteristics
                    .REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING
            )

        val yuvReprocess =
            caps.contains(
                CameraCharacteristics
                    .REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING
            )

        val raw =
            caps.contains(
                CameraCharacteristics
                    .REQUEST_AVAILABLE_CAPABILITIES_RAW
            )

        log(
            "RAW capability: $raw"
        )

        log(
            "PRIVATE reprocessing: $privateReprocess"
        )

        log(
            "YUV reprocessing: $yuvReprocess"
        )

        log("")
    }

    // =====================================================
    // PHYSICAL CAMERA IDs
    // =====================================================

    private fun dumpPhysicalCameraIds(
        chars: CameraCharacteristics
    ) {

        log("==============================")
        log("PHYSICAL CAMERA IDS")
        log("==============================")

        try {

            val ids =
                chars.physicalCameraIds

            if (ids.isEmpty()) {

                log(
                    "No physical IDs exposed."
                )

            } else {

                for (id in ids) {
                    log(id)
                }
            }

        } catch (e: Throwable) {

            log(
                "Physical camera query failed."
            )
        }

        log("")
    }

    // =====================================================
    // CHARACTERISTIC KEY SEARCH
    // =====================================================

    private fun dumpAllCharacteristicKeys(
        chars: CameraCharacteristics
    ) {

        log("==============================")
        log("RAW / SENSOR / STREAM KEYS")
        log("==============================")

        val terms = listOf(
            "raw",
            "sensor",
            "stream",
            "resolution",
            "high",
            "remosaic",
            "full",
            "pixel",
            "scenario",
            "vivo",
            "mediatek",
            "mtk",
            "quad",
            "camera"
        )

        var count = 0

        for (key in chars.keys) {

            val lower =
                key.name.lowercase(
                    Locale.US
                )

            if (
                terms.any {
                    lower.contains(it)
                }
            ) {

                count++

                val value =
                    try {
                        chars.get(key)
                    } catch (_: Throwable) {
                        "<READ ERROR>"
                    }

                log("")
                log(
                    "*** ${key.name}"
                )

                log(
                    "    ${formatValue(value)}"
                )
            }
        }

        log("")
        log(
            "Matching characteristic keys: $count"
        )

        log("")
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private fun sortSizes(
        sizes: Array<Size>
    ): List<Size> {

        return sizes.sortedByDescending {
            pixels(it)
        }
    }

    private fun pixels(
        size: Size
    ): Long {

        return size.width.toLong() *
            size.height.toLong()
    }

    private fun logSize(
        prefix: String,
        size: Size
    ) {

        val mp =
            pixels(size).toDouble() /
                1_000_000.0

        log(
            prefix +
                size.width +
                " x " +
                size.height +
                " = " +
                String.format(
                    Locale.US,
                    "%.2f MP",
                    mp
                )
        )
    }

    private fun logMP(
        width: Int,
        height: Int
    ) {

        val mp =
            width.toDouble() *
                height.toDouble() /
                1_000_000.0

        log(
            String.format(
                Locale.US,
                "%.2f MP",
                mp
            )
        )
    }

    private fun cfaName(
        value: Int?
    ): String {

        if (value == null) {
            return "NOT EXPOSED"
        }

        return when (value) {

            CameraCharacteristics
                .SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_RGGB ->
                "$value = RGGB"

            CameraCharacteristics
                .SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GRBG ->
                "$value = GRBG"

            CameraCharacteristics
                .SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GBRG ->
                "$value = GBRG"

            CameraCharacteristics
                .SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_BGGR ->
                "$value = BGGR"

            CameraCharacteristics
                .SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_RGB ->
                "$value = RGB"

            else ->
                "$value = UNKNOWN"
        }
    }

    private fun capabilityName(
        cap: Int
    ): String {

        return when (cap) {

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
                .REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA ->
                "LOGICAL_MULTI_CAMERA"

            else ->
                "CAPABILITY_$cap"
        }
    }

    private fun formatName(
        format: Int
    ): String {

        return when (format) {

            ImageFormat.RAW_SENSOR ->
                "RAW_SENSOR"

            ImageFormat.RAW10 ->
                "RAW10"

            ImageFormat.RAW12 ->
                "RAW12"

            ImageFormat.RAW_PRIVATE ->
                "RAW_PRIVATE"

            ImageFormat.JPEG ->
                "JPEG"

            ImageFormat.YUV_420_888 ->
                "YUV_420_888"

            ImageFormat.PRIVATE ->
                "PRIVATE"

            ImageFormat.DEPTH16 ->
                "DEPTH16"

            ImageFormat.DEPTH_POINT_CLOUD ->
                "DEPTH_POINT_CLOUD"

            else ->
                "UNKNOWN/VENDOR FORMAT"
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
                value.contentToString()

            is LongArray ->
                value.contentToString()

            is FloatArray ->
                value.contentToString()

            is DoubleArray ->
                value.contentToString()

            is ByteArray ->
                value.contentToString()

            is BooleanArray ->
                value.contentToString()

            is Array<*> ->
                value.contentDeepToString()

            else ->
                value.toString()
        }
    }

    private fun log(
        message: String
    ) {

        runOnUiThread {

            output.append(message)
            output.append("\n")
        }
    }
}
