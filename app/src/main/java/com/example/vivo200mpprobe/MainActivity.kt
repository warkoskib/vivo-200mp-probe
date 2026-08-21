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
import java.lang.reflect.Method
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_ID = "3"
    }

    private lateinit var output: TextView
    private lateinit var scroll: ScrollView
    private lateinit var cameraManager: CameraManager

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
        "vivo.control.advance_fullsize",
        "vivo.control.EngineerRemosaicMode",
        "vivo.control.remosaic.capability",
        "vivo.control.seamless.remosaic.enable",
        "com.mediatek.seamlessfeature.cameraScenario",
        "com.mediatek.seamlessfeature.sensorScenario",
        "com.mediatek.seamlessfeature.sensorScenarioCustomHint"
    )

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
        "highres"
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
        log("No image will be captured.")
        log("Press RUN FULL STREAM PROBE.")
    }

    private fun buildUi() {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setPadding(20, 30, 20, 30)

        val runButton = Button(this)

        runButton.text = "RUN FULL STREAM PROBE"

        runButton.setOnClickListener {

            output.text = ""
            runButton.isEnabled = false

            Thread {

                try {
                    runProbe()
                } finally {

                    runOnUiThread {
                        runButton.isEnabled = true
                    }
                }

            }.start()
        }

        root.addView(runButton)

        val copyButton = Button(this)

        copyButton.text = "COPY OUTPUT"

        copyButton.setOnClickListener {

            val text = output.text.toString()

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

        val clearButton = Button(this)

        clearButton.text = "CLEAR"

        clearButton.setOnClickListener {
            output.text = ""
        }

        root.addView(clearButton)

        scroll = ScrollView(this)

        output = TextView(this)

        output.textSize = 13f
        output.setTextIsSelectable(true)
        output.setPadding(0, 20, 0, 120)

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

    private fun runProbe() {

        log("VIVO 200 MP STREAM MAP PROBE")
        log("==============================")

        try {

            val chars =
                cameraManager.getCameraCharacteristics(
                    CAMERA_ID
                )

            dumpSensorBasics(chars)

            val map =
                chars.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                )

            if (map == null) {

                log("")
                log("NO STREAM CONFIGURATION MAP")
                return
            }

            dumpAllFormats(map)
            dumpHighResolutionFormats(map)
            dumpInputFormats(map)

            dumpExactTargetCharacteristicKeys(chars)

            dumpInterestingCharacteristicKeys(chars)

            dumpKeyTypeHints(chars)

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

        val pixel =
            chars.get(
                CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE
            )

        val active =
            chars.get(
                CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE
            )

        log(
            "Pixel array: ${pixel ?: "NOT EXPOSED"}"
        )

        log(
            "Active array: ${active ?: "NOT EXPOSED"}"
        )

        log("")
    }

    // =========================================================
    // ALL STANDARD OUTPUT FORMATS
    // =========================================================

    private fun dumpAllFormats(
        map: StreamConfigurationMap
    ) {

        log("==============================")
        log("STANDARD CAMERA2 STREAM MAP")
        log("==============================")

        val formats =
            map.outputFormats

        for (format in formats) {

            log("")
            log("--------------------------------")
            log(
                "FORMAT $format = ${formatName(format)}"
            )
            log("--------------------------------")

            val sizes =
                try {
                    map.getOutputSizes(format)
                } catch (_: Throwable) {
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

            for (size in sorted) {

                logSize(size)
            }

            val largest =
                sorted.firstOrNull()

            if (largest != null) {

                log("")
                log("LARGEST:")

                logSize(
                    largest
                )
            }

            val possible200 =
                sorted.filter {
                    pixels(it) >=
                        150_000_000L
                }

            if (possible200.isNotEmpty()) {

                log("")
                log(
                    "***** VERY LARGE OUTPUT FOUND *****"
                )

                for (size in possible200) {
                    logSize(size)
                }
            }
        }

        log("")
    }

    // =========================================================
    // HIGH RESOLUTION
    // =========================================================

    private fun dumpHighResolutionFormats(
        map: StreamConfigurationMap
    ) {

        log("==============================")
        log("HIGH RESOLUTION STREAM MAP")
        log("==============================")

        val formatsToCheck =
            map.outputFormats.toList()

        for (format in formatsToCheck) {

            val high =
                try {
                    map.getHighResolutionOutputSizes(
                        format
                    )
                } catch (_: Throwable) {
                    null
                }

            if (
                high == null ||
                high.isEmpty()
            ) {
                continue
            }

            log("")
            log("--------------------------------")

            log(
                "HIGH RES FORMAT $format = " +
                    formatName(format)
            )

            log("--------------------------------")

            for (size in sortSizes(high)) {

                logSize(size)
            }
        }

        log("")
    }

    // =========================================================
    // INPUT FORMATS
    // =========================================================

    private fun dumpInputFormats(
        map: StreamConfigurationMap
    ) {

        log("==============================")
        log("INPUT / REPROCESSING MAP")
        log("==============================")

        try {

            val formats =
                map.inputFormats

            if (formats.isEmpty()) {

                log(
                    "No input formats exposed."
                )

            } else {

                for (format in formats) {

                    log("")
                    log(
                        "INPUT $format = ${formatName(format)}"
                    )

                    val sizes =
                        try {
                            map.getInputSizes(format)
                        } catch (_: Throwable) {
                            null
                        }

                    if (sizes == null) {

                        log(
                            "No sizes."
                        )

                    } else {

                        for (size in sortSizes(sizes)) {

                            logSize(size)
                        }
                    }
                }
            }

        } catch (e: Throwable) {

            log(
                "Input map error: ${e.message ?: ""}"
            )
        }

        log("")
    }

    // =========================================================
    // EXACT TARGET CHARACTERISTIC KEYS
    // =========================================================

    private fun dumpExactTargetCharacteristicKeys(
        chars: CameraCharacteristics
    ) {

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
                    "Not present as CameraCharacteristics key."
                )

                continue
            }

            val value =
                try {
                    chars.get(key)
                } catch (e: Throwable) {
                    "<READ ERROR: ${e.javaClass.simpleName}>"
                }

            log(
                "VALUE:"
            )

            log(
                formatValue(value)
            )

            log(
                "VALUE CLASS:"
            )

            log(
                value?.javaClass?.name
                    ?: "null"
            )

            log(
                "KEY OBJECT:"
            )

            log(
                key.toString()
            )
        }
    }

    // =========================================================
    // ALL INTERESTING CHARACTERISTIC KEYS
    // =========================================================

    private fun dumpInterestingCharacteristicKeys(
        chars: CameraCharacteristics
    ) {

        log("")
        log("==============================")
        log("ALL INTERESTING CHARACTERISTIC KEYS")
        log("==============================")

        var count = 0

        for (key in chars.keys) {

            val lower =
                key.name.lowercase(
                    Locale.US
                )

            if (
                interestingTerms.any {
                    lower.contains(it)
                }
            ) {

                count++

                val value =
                    try {
                        chars.get(key)
                    } catch (e: Throwable) {
                        "<READ ERROR>"
                    }

                log("")
                log(
                    "*** ${key.name}"
                )

                log(
                    "VALUE:"
                )

                log(
                    truncateValue(
                        formatValue(value)
                    )
                )

                log(
                    "CLASS:"
                )

                log(
                    value?.javaClass?.name
                        ?: "null"
                )
            }
        }

        log("")
        log(
            "Interesting characteristic key count: $count"
        )

        log("")
    }

    // =========================================================
    // KEY TYPE HINTS
    // =========================================================

    private fun dumpKeyTypeHints(
        chars: CameraCharacteristics
    ) {

        log("==============================")
        log("KEY TYPE / REFLECTION HINTS")
        log("==============================")

        val allKeys =
            chars.keys

        for (key in allKeys) {

            if (
                !targetKeyNames.contains(
                    key.name
                )
            ) {
                continue
            }

            log("")
            log("--------------------------------")
            log(key.name)
            log("--------------------------------")

            log(
                "Key class: ${key.javaClass.name}"
            )

            try {

                val nativeKeyField =
                    key.javaClass.declaredFields
                        .firstOrNull {
                            it.name.contains(
                                "mKey"
                            )
                        }

                if (nativeKeyField != null) {

                    nativeKeyField.isAccessible =
                        true

                    val nativeKey =
                        nativeKeyField.get(
                            key
                        )

                    log(
                        "Internal key object:"
                    )

                    log(
                        nativeKey?.toString()
                            ?: "null"
                    )

                    if (nativeKey != null) {

                        dumpMethods(
                            nativeKey
                        )
                    }
                }

            } catch (e: Throwable) {

                log(
                    "Reflection unavailable: " +
                        e.javaClass.simpleName
                )
            }
        }

        log("")
    }

    private fun dumpMethods(
        obj: Any
    ) {

        try {

            val methods =
                obj.javaClass.declaredMethods

            for (method in methods) {

                val name =
                    method.name.lowercase(
                        Locale.US
                    )

                if (
                    name.contains("type") ||
                    name.contains("name") ||
                    name.contains("tag")
                ) {

                    try {

                        method.isAccessible =
                            true

                        if (
                            method.parameterTypes.isEmpty()
                        ) {

                            val result =
                                method.invoke(obj)

                            log(
                                "  ${method.name}() = " +
                                    (result?.toString()
                                        ?: "null")
                            )
                        }

                    } catch (_: Throwable) {
                    }
                }
            }

        } catch (_: Throwable) {
        }
    }

    // =========================================================
    // HELPERS
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

        return size.width.toLong() *
            size.height.toLong()
    }

    private fun logSize(
        size: Size
    ) {

        val mp =
            pixels(size).toDouble() /
                1_000_000.0

        log(
            "${size.width} x ${size.height} = " +
                String.format(
                    Locale.US,
                    "%.2f MP",
                    mp
                )
        )
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

            ImageFormat.PRIVATE ->
                "PRIVATE"

            ImageFormat.JPEG ->
                "JPEG"

            ImageFormat.YUV_420_888 ->
                "YUV_420_888"

            ImageFormat.YV12 ->
                "YV12"

            ImageFormat.HEIC ->
                "HEIC"

            54 ->
                "YCBCR_P010 / 10-bit YUV"

            4101 ->
                "JPEG/R"

            else ->
                "UNKNOWN/VENDOR"
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

            is ShortArray ->
                value.contentToString()

            is BooleanArray ->
                value.contentToString()

            is Array<*> ->
                value.contentDeepToString()

            else ->
                value.toString()
        }
    }

    private fun truncateValue(
        value: String
    ): String {

        val max =
            2000

        return if (
            value.length <= max
        ) {

            value

        } else {

            value.substring(
                0,
                max
            ) +
                "\n...[TRUNCATED ${value.length - max} chars]"
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
        }
    }
}
