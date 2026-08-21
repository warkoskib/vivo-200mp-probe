package com.example.vivo200mpprobe

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
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

    /*
     * These are the OEM keys we are especially interested in.
     *
     * Some may exist only as CaptureRequest / CaptureResult /
     * session keys rather than CameraCharacteristics keys.
     *
     * This probe tells us which ones are actually exposed through
     * CameraCharacteristics and prints their values when possible.
     */
    private val targetKeyNames = listOf(
        "vcf.parameter.sensorSizeList",
        "vcf.parameter.SnapshotJpegStreamMap",
        "vivo.control.snapshotYuvStreamMap",
        "vivo.control.snapJpegSize",
        "vivo.control.picturesize.value",
        "vivo.control.streamsUsage",
        "vivo.control.vcfStreamType",

        "vivo.control.raw_capture_type",
        "vivo.parameter.highResolutionDngType",
        "vivo.parameter.niceCaptureSensorMode",

        "vivo.control.sensorMode",
        "vivo.preview.sensorMode",

        "vivo.control.real200mp_switch_on",
        "vivo.control.ultra_highresolution",
        "vivo.control.portrait_high_resolution",
        "vivo.control.ai_highresolution",
        "vivo.control.forceSensorMode",

        "vivo.control.advance_fullsize",
        "vivo.control.EngineerRemosaicMode",
        "vivo.control.remosaic.capability",
        "vivo.control.seamless.remosaic.enable",
        "vivo.control.seamless.roiRemosaic",

        "com.mediatek.control.capture.remosaicenable",
        "com.mediatek.control.capture.seamless.remosaicenable",

        "com.mediatek.seamlessfeature.cameraScenario",
        "com.mediatek.seamlessfeature.sensorScenario",
        "com.mediatek.seamlessfeature.sensorScenarioCustomHint",
        "com.mediatek.seamlessfeature.sensorScenarioSwitchPolicy"
    )

    /*
     * Used to filter the enormous vendor-key list down to keys
     * that are likely to matter for full-resolution capture.
     */
    private val interestingTerms = listOf(
        "size",
        "stream",
        "snapshot",
        "jpeg",
        "yuv",
        "raw",
        "sensor",
        "remosaic",
        "full",
        "resolution",
        "picture",
        "vcf",
        "capture",
        "scenario",
        "200mp",
        "highres",
        "high_resolution",
        "highresolution"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraManager =
            getSystemService(CAMERA_SERVICE) as CameraManager

        buildUi()

        log("VIVO 200 MP STREAM MAP PROBE")
        log("==============================")
        log("")
        log("Camera ID: $CAMERA_ID")
        log("")
        log("This probe does NOT capture an image.")
        log("")
        log("It inspects:")
        log("- Public Camera2 stream sizes")
        log("- High-resolution stream sizes")
        log("- Input/reprocessing sizes")
        log("- Vivo vendor characteristic keys")
        log("- VCF stream-map metadata")
        log("- Sensor/full-resolution metadata")
        log("")
        log("Press RUN FULL STREAM PROBE.")
    }

    // =========================================================
    // UI
    // =========================================================

    private fun buildUi() {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setPadding(20, 30, 20, 30)

        // -----------------------------------------------------
        // RUN BUTTON
        // -----------------------------------------------------

        val runButton = Button(this)

        runButton.text = "RUN FULL STREAM PROBE"

        runButton.setOnClickListener {

            output.text = ""
            runButton.isEnabled = false

            Thread {

                try {

                    runProbe()

                } catch (e: Throwable) {

                    log("")
                    log("==============================")
                    log("FATAL PROBE ERROR")
                    log("==============================")
                    log("")
                    log(e.javaClass.name)
                    log(e.message ?: "No error message")

                    val stack =
                        e.stackTraceToString()

                    log("")
                    log(stack)

                } finally {

                    runOnUiThread {
                        runButton.isEnabled = true
                    }
                }

            }.start()
        }

        root.addView(runButton)

        // -----------------------------------------------------
        // COPY BUTTON
        // -----------------------------------------------------

        val copyButton = Button(this)

        copyButton.text = "COPY OUTPUT"

        copyButton.setOnClickListener {

            val text =
                output.text.toString()

            if (text.isBlank()) {

                Toast.makeText(
                    this,
                    "No output yet.",
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
                    "Vivo 200 MP Stream Probe",
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

        // -----------------------------------------------------
        // CLEAR BUTTON
        // -----------------------------------------------------

        val clearButton = Button(this)

        clearButton.text = "CLEAR"

        clearButton.setOnClickListener {
            output.text = ""
        }

        root.addView(clearButton)

        // -----------------------------------------------------
        // OUTPUT
        // -----------------------------------------------------

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

    // =========================================================
    // MAIN PROBE
    // =========================================================

    private fun runProbe() {

        log("VIVO 200 MP STREAM MAP PROBE")
        log("==============================")
        log("")
        log("Camera ID: $CAMERA_ID")

        // -----------------------------------------------------
        // CAMERA LIST
        // -----------------------------------------------------

        log("")
        log("==============================")
        log("AVAILABLE CAMERA IDS")
        log("==============================")

        try {

            val ids =
                cameraManager.cameraIdList

            for (id in ids) {
                log("Camera ID: $id")
            }

        } catch (e: Throwable) {

            log(
                "Could not enumerate cameras: " +
                    e.javaClass.simpleName
            )
        }

        // -----------------------------------------------------
        // CHARACTERISTICS
        // -----------------------------------------------------

        val chars =
            cameraManager.getCameraCharacteristics(
                CAMERA_ID
            )

        dumpSensorBasics(chars)

        dumpCapabilities(chars)

        // -----------------------------------------------------
        // PUBLIC STREAM MAP
        // -----------------------------------------------------

        val map =
            chars.get(
                CameraCharacteristics
                    .SCALER_STREAM_CONFIGURATION_MAP
            )

        if (map == null) {

            log("")
            log("==============================")
            log("NO STREAM CONFIGURATION MAP")
            log("==============================")
            log("")
            log("Camera 3 did not expose a public")
            log("SCALER_STREAM_CONFIGURATION_MAP.")

        } else {

            dumpAllFormats(map)

            dumpHighResolutionFormats(map)

            dumpInputFormats(map)

            searchPublicMapFor200MP(map)
        }

        // -----------------------------------------------------
        // OEM CHARACTERISTICS
        // -----------------------------------------------------

        dumpExactTargetCharacteristicKeys(
            chars
        )

        dumpInterestingCharacteristicKeys(
            chars
        )

        dumpAllVendorCharacteristicNames(
            chars
        )

        // -----------------------------------------------------
        // SUMMARY
        // -----------------------------------------------------

        log("")
        log("")
        log("==============================")
        log("PROBE COMPLETE")
        log("==============================")
        log("")
        log("SEARCH THE OUTPUT FOR:")
        log("")
        log("16320")
        log("12288")
        log("200")
        log("sensorSizeList")
        log("SnapshotJpegStreamMap")
        log("snapshotYuvStreamMap")
        log("snapJpegSize")
        log("picturesize")
        log("fullsize")
        log("remosaic")
        log("")
        log("Press COPY OUTPUT.")
    }

    // =========================================================
    // SENSOR BASICS
    // =========================================================

    private fun dumpSensorBasics(
        chars: CameraCharacteristics
    ) {

        log("")
        log("==============================")
        log("SENSOR BASICS")
        log("==============================")

        val pixelArray =
            chars.get(
                CameraCharacteristics
                    .SENSOR_INFO_PIXEL_ARRAY_SIZE
            )

        val preCorrection =
            chars.get(
                CameraCharacteristics
                    .SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE
            )

        val activeArray =
            chars.get(
                CameraCharacteristics
                    .SENSOR_INFO_ACTIVE_ARRAY_SIZE
            )

        val physicalSize =
            chars.get(
                CameraCharacteristics
                    .SENSOR_INFO_PHYSICAL_SIZE
            )

        val maxAnalogSensitivity =
            chars.get(
                CameraCharacteristics
                    .SENSOR_MAX_ANALOG_SENSITIVITY
            )

        val orientation =
            chars.get(
                CameraCharacteristics
                    .SENSOR_ORIENTATION
            )

        log("")
        log(
            "Pixel array: " +
                (pixelArray ?: "NOT EXPOSED")
        )

        log(
            "Pre-correction active array: " +
                (preCorrection ?: "NOT EXPOSED")
        )

        log(
            "Active array: " +
                (activeArray ?: "NOT EXPOSED")
        )

        log(
            "Physical size: " +
                (physicalSize ?: "NOT EXPOSED")
        )

        log(
            "Max analog sensitivity: " +
                (maxAnalogSensitivity ?: "NOT EXPOSED")
        )

        log(
            "Sensor orientation: " +
                (orientation ?: "NOT EXPOSED")
        )
    }

    // =========================================================
    // CAMERA CAPABILITIES
    // =========================================================

    private fun dumpCapabilities(
        chars: CameraCharacteristics
    ) {

        log("")
        log("==============================")
        log("CAMERA CAPABILITIES")
        log("==============================")

        val capabilities =
            chars.get(
                CameraCharacteristics
                    .REQUEST_AVAILABLE_CAPABILITIES
            )

        if (capabilities == null) {

            log("No capability array exposed.")
            return
        }

        for (capability in capabilities) {

            log(
                "$capability = " +
                    capabilityName(capability)
            )
        }
    }

    // =========================================================
    // STANDARD OUTPUT STREAM MAP
    // =========================================================

    private fun dumpAllFormats(
        map: StreamConfigurationMap
    ) {

        log("")
        log("==============================")
        log("STANDARD CAMERA2 STREAM MAP")
        log("==============================")

        val formats =
            map.outputFormats

        log("")
        log(
            "Output format count: " +
                formats.size
        )

        for (format in formats) {

            log("")
            log("--------------------------------")
            log(
                "FORMAT $format = " +
                    formatName(format)
            )
            log("--------------------------------")

            val sizes =
                try {

                    map.getOutputSizes(format)

                } catch (e: Throwable) {

                    log(
                        "getOutputSizes failed: " +
                            e.javaClass.simpleName
                    )

                    null
                }

            if (sizes == null) {

                log("No sizes exposed.")
                continue
            }

            val sorted =
                sortSizes(sizes)

            log(
                "Count: ${sorted.size}"
            )

            log("")

            for (size in sorted) {
                logSize(size)
            }

            val largest =
                sorted.firstOrNull()

            if (largest != null) {

                log("")
                log("LARGEST OUTPUT:")

                logSize(largest)
            }

            val huge =
                sorted.filter {
                    pixels(it) >=
                        100_000_000L
                }

            if (huge.isNotEmpty()) {

                log("")
                log(
                    "********************************"
                )
                log(
                    "VERY LARGE OUTPUT(S) FOUND"
                )
                log(
                    "********************************"
                )

                for (size in huge) {
                    logSize(size)
                }
            }
        }
    }

    // =========================================================
    // HIGH RESOLUTION OUTPUT STREAM MAP
    // =========================================================

    private fun dumpHighResolutionFormats(
        map: StreamConfigurationMap
    ) {

        log("")
        log("==============================")
        log("HIGH RESOLUTION STREAM MAP")
        log("==============================")

        var anythingFound = false

        val formats =
            map.outputFormats

        for (format in formats) {

            val highSizes =
                try {

                    map.getHighResolutionOutputSizes(
                        format
                    )

                } catch (_: Throwable) {

                    null
                }

            if (
                highSizes == null ||
                highSizes.isEmpty()
            ) {

                continue
            }

            anythingFound = true

            log("")
            log("--------------------------------")

            log(
                "HIGH RES FORMAT $format = " +
                    formatName(format)
            )

            log("--------------------------------")

            val sorted =
                sortSizes(highSizes)

            for (size in sorted) {

                logSize(size)

                if (
                    pixels(size) >=
                    100_000_000L
                ) {

                    log(
                        "  ***** >100 MP *****"
                    )
                }
            }
        }

        if (!anythingFound) {

            log("")
            log(
                "No high-resolution output sizes " +
                    "were exposed through the public API."
            )
        }
    }

    // =========================================================
    // INPUT / REPROCESSING STREAMS
    // =========================================================

    private fun dumpInputFormats(
        map: StreamConfigurationMap
    ) {

        log("")
        log("==============================")
        log("INPUT / REPROCESSING MAP")
        log("==============================")

        try {

            val formats =
                map.inputFormats

            if (formats.isEmpty()) {

                log("")
                log(
                    "No input formats exposed."
                )

                return
            }

            for (format in formats) {

                log("")
                log("--------------------------------")

                log(
                    "INPUT $format = " +
                        formatName(format)
                )

                log("--------------------------------")

                val sizes =
                    try {

                        map.getInputSizes(
                            format
                        )

                    } catch (_: Throwable) {

                        null
                    }

                if (sizes == null) {

                    log("No sizes.")

                } else {

                    for (
                        size in sortSizes(sizes)
                    ) {

                        logSize(size)
                    }
                }
            }

        } catch (e: Throwable) {

            log("")
            log(
                "Input map error: " +
                    e.javaClass.simpleName +
                    ": " +
                    (e.message ?: "")
            )
        }
    }

    // =========================================================
    // SPECIFICALLY SEARCH PUBLIC MAP FOR 200 MP
    // =========================================================

    private fun searchPublicMapFor200MP(
        map: StreamConfigurationMap
    ) {

        log("")
        log("==============================")
        log("200 MP PUBLIC STREAM SEARCH")
        log("==============================")

        var exactFound = false
        var hugeFound = false

        for (format in map.outputFormats) {

            val normal =
                try {
                    map.getOutputSizes(format)
                } catch (_: Throwable) {
                    null
                }

            if (normal != null) {

                for (size in normal) {

                    if (
                        size.width == 16320 &&
                        size.height == 12288
                    ) {

                        exactFound = true

                        log("")
                        log(
                            "***** EXACT 200 MP SIZE FOUND *****"
                        )

                        log(
                            "Format: $format = " +
                                formatName(format)
                        )

                        logSize(size)
                    }

                    if (
                        pixels(size) >=
                        150_000_000L
                    ) {

                        hugeFound = true

                        log("")
                        log(
                            "***** >=150 MP OUTPUT FOUND *****"
                        )

                        log(
                            "Format: $format = " +
                                formatName(format)
                        )

                        logSize(size)
                    }
                }
            }

            val high =
                try {
                    map.getHighResolutionOutputSizes(
                        format
                    )
                } catch (_: Throwable) {
                    null
                }

            if (high != null) {

                for (size in high) {

                    if (
                        size.width == 16320 &&
                        size.height == 12288
                    ) {

                        exactFound = true

                        log("")
                        log(
                            "***** EXACT 200 MP HIGH-RES SIZE FOUND *****"
                        )

                        log(
                            "Format: $format = " +
                                formatName(format)
                        )

                        logSize(size)
                    }

                    if (
                        pixels(size) >=
                        150_000_000L
                    ) {

                        hugeFound = true

                        log("")
                        log(
                            "***** >=150 MP HIGH-RES OUTPUT FOUND *****"
                        )

                        log(
                            "Format: $format = " +
                                formatName(format)
                        )

                        logSize(size)
                    }
                }
            }
        }

        log("")

        if (!exactFound) {

            log(
                "16320 x 12288 was NOT found " +
                    "in the public Camera2 stream map."
            )
        }

        if (!hugeFound) {

            log(
                "No >=150 MP public output " +
                    "was found."
            )
        }
    }

    // =========================================================
    // EXACT OEM TARGET CHARACTERISTIC KEYS
    // =========================================================

    private fun dumpExactTargetCharacteristicKeys(
        chars: CameraCharacteristics
    ) {

        log("")
        log("==============================")
        log("EXACT OEM TARGET KEYS")
        log("==============================")

        val byName =
            chars.keys.associateBy {
                it.name
            }

        for (target in targetKeyNames) {

            log("")
            log("--------------------------------")
            log(target)
            log("--------------------------------")

            val key =
                byName[target]

            if (key == null) {

                log(
                    "Not present as a " +
                        "CameraCharacteristics key."
                )

                continue
            }

            val value =
                try {

                    chars.get(key)

                } catch (e: Throwable) {

                    "<READ ERROR: " +
                        e.javaClass.simpleName +
                        ">"
                }

            log("VALUE:")

            log(
                safeFormatValue(value)
            )

            log("VALUE CLASS:")

            log(
                value?.javaClass?.name
                    ?: "null"
            )

            log("KEY JAVA CLASS:")

            log(
                key.javaClass.name
            )
        }
    }

    // =========================================================
    // INTERESTING OEM CHARACTERISTIC KEYS
    // =========================================================

    private fun dumpInterestingCharacteristicKeys(
        chars: CameraCharacteristics
    ) {

        log("")
        log("==============================")
        log("ALL INTERESTING CHARACTERISTIC KEYS")
        log("==============================")

        var count = 0

        val sortedKeys =
            chars.keys.sortedBy {
                it.name.lowercase(
                    Locale.US
                )
            }

        for (key in sortedKeys) {

            val lower =
                key.name.lowercase(
                    Locale.US
                )

            val interesting =
                interestingTerms.any {
                    lower.contains(it)
                }

            if (!interesting) {
                continue
            }

            count++

            val value =
                try {

                    chars.get(key)

                } catch (e: Throwable) {

                    "<READ ERROR: " +
                        e.javaClass.simpleName +
                        ">"
                }

            log("")
            log("--------------------------------")
            log(key.name)
            log("--------------------------------")

            log("VALUE:")

            log(
                truncateValue(
                    safeFormatValue(value)
                )
            )

            log("CLASS:")

            log(
                value?.javaClass?.name
                    ?: "null"
            )
        }

        log("")
        log(
            "Interesting characteristic key count: " +
                count
        )
    }

    // =========================================================
    // VENDOR KEY NAME INVENTORY
    // =========================================================

    private fun dumpAllVendorCharacteristicNames(
        chars: CameraCharacteristics
    ) {

        log("")
        log("==============================")
        log("VENDOR CHARACTERISTIC KEY NAMES")
        log("==============================")
        log("")
        log(
            "Names only. Values are omitted here to " +
                "avoid huge calibration/OTP dumps."
        )

        val vendorKeys =
            chars.keys
                .filter {
                    !it.name.startsWith(
                        "android."
                    )
                }
                .sortedBy {
                    it.name.lowercase(
                        Locale.US
                    )
                }

        log("")
        log(
            "Vendor characteristic key count: " +
                vendorKeys.size
        )

        for (key in vendorKeys) {

            log(key.name)
        }
    }

    // =========================================================
    // FORMAT NAME
    // =========================================================

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

            ImageFormat.PRIVATE ->
                "PRIVATE"

            ImageFormat.JPEG ->
                "JPEG"

            ImageFormat.YUV_420_888 ->
                "YUV_420_888"

            ImageFormat.YV12 ->
                "YV12"

            ImageFormat.NV21 ->
                "NV21"

            ImageFormat.YUY2 ->
                "YUY2"

            ImageFormat.DEPTH16 ->
                "DEPTH16"

            ImageFormat.DEPTH_POINT_CLOUD ->
                "DEPTH_POINT_CLOUD"

            ImageFormat.DEPTH_JPEG ->
                "DEPTH_JPEG"

            ImageFormat.HEIC ->
                "HEIC"

            54 ->
                "YCBCR_P010 / 10-BIT YUV"

            4101 ->
                "JPEG_R / HDR JPEG"

            else ->
                "UNKNOWN_OR_VENDOR_FORMAT"
        }
    }

    // =========================================================
    // CAPABILITY NAME
    // =========================================================

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
                "CONSTRAINED_HIGH_SPEED_VIDEO"

            else ->
                "CAPABILITY_$value"
        }
    }

    // =========================================================
    // SIZE HELPERS
    // =========================================================

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

        return (
            size.width.toLong() *
                size.height.toLong()
            )
    }

    private fun logSize(
        size: Size
    ) {

        val count =
            pixels(size)

        val mp =
            count.toDouble() /
                1_000_000.0

        log(
            "${size.width} x ${size.height}" +
                " = " +
                String.format(
                    Locale.US,
                    "%.2f MP",
                    mp
                )
        )
    }

    // =========================================================
    // VALUE FORMATTING
    // =========================================================

    private fun safeFormatValue(
        value: Any?
    ): String {

        if (value == null) {
            return "null"
        }

        return try {

            when (value) {

                is IntArray ->
                    value.contentToString()

                is LongArray ->
                    value.contentToString()

                is FloatArray ->
                    value.contentToString()

                is DoubleArray ->
                    value.contentToString()

                is ByteArray -> {

                    if (value.size > 512) {

                        "ByteArray(size=${value.size}) " +
                            value.take(128)
                                .joinToString(
                                    prefix = "[",
                                    postfix =
                                        ", ... TRUNCATED]"
                                )

                    } else {

                        value.contentToString()
                    }
                }

                is ShortArray ->
                    value.contentToString()

                is BooleanArray ->
                    value.contentToString()

                is CharArray ->
                    value.concatToString()

                is Array<*> ->
                    value.contentDeepToString()

                else ->
                    value.toString()
            }

        } catch (e: Throwable) {

            "<FORMAT ERROR: " +
                e.javaClass.simpleName +
                ">"
        }
    }

    private fun truncateValue(
        value: String
    ): String {

        val max =
            3000

        return if (
            value.length <= max
        ) {

            value

        } else {

            value.substring(
                0,
                max
            ) +
                "\n...[TRUNCATED " +
                (value.length - max) +
                " chars]"
        }
    }

    // =========================================================
    // LOG
    // =========================================================

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
        }
    }
}
