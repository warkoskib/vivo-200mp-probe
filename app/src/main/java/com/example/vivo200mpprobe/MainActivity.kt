package com.example.vivo200mpprobe

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
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

    private lateinit var output: TextView
    private lateinit var cameraManager: CameraManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraManager =
            getSystemService(CAMERA_SERVICE) as CameraManager

        buildUi()

        log("VIVO PHYSICAL / MAX RESOLUTION PROBE")
        log("====================================")
        log("")
        log("Device: ${Build.MODEL}")
        log("Android: ${Build.VERSION.RELEASE}")
        log("SDK: ${Build.VERSION.SDK_INT}")
        log("")
        log("Press RUN PROBE.")
    }

    private fun buildUi() {

        val root =
            LinearLayout(this)

        root.orientation =
            LinearLayout.VERTICAL

        root.setPadding(
            20,
            30,
            20,
            30
        )

        val runButton =
            Button(this)

        runButton.text =
            "RUN PROBE"

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

        val copyButton =
            Button(this)

        copyButton.text =
            "COPY OUTPUT"

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
                    "Vivo Physical Max Resolution Probe",
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

        val scroll =
            ScrollView(this)

        output =
            TextView(this)

        output.textSize =
            13f

        output.setTextIsSelectable(
            true
        )

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

    private fun runProbe() {

        log("VIVO PHYSICAL / MAX RESOLUTION PROBE")
        log("====================================")
        log("")

        try {

            log("==============================")
            log("PUBLIC CAMERA ID LIST")
            log("==============================")

            val ids =
                cameraManager.cameraIdList

            for (id in ids) {
                log("PUBLIC: $id")
            }

        } catch (e: Throwable) {

            log(
                "Public camera enumeration failed:"
            )

            log(
                e.toString()
            )
        }

        for (i in 0..7) {

            val id =
                i.toString()

            log("")
            log("")
            log("################################")
            log("CAMERA ID $id")
            log("################################")

            try {

                val chars =
                    cameraManager
                        .getCameraCharacteristics(
                            id
                        )

                log("STATUS: VALID")

                dumpLogicalMultiCamera(chars)
                dumpPhysicalIds(chars)
                dumpPhysicalRequestKeys(chars)
                dumpPixelModes(chars)

                dumpStandardMap(chars)
                dumpMaximumResolutionMap(chars)

            } catch (e: Throwable) {

                log(
                    "STATUS: INVALID / INACCESSIBLE"
                )

                log(
                    e.javaClass.simpleName +
                        ": " +
                        (e.message ?: "")
                )
            }
        }

        log("")
        log("")
        log("==============================")
        log("PROBE COMPLETE")
        log("==============================")
        log("")
        log("Search the output for:")
        log("16320")
        log("12288")
        log("8160")
        log("6144")
        log("MAXIMUM")
        log("PHYSICAL")
        log("")
        log("Press COPY OUTPUT.")
    }

    private fun dumpLogicalMultiCamera(
        chars:
            CameraCharacteristics
    ) {

        log("")
        log("==============================")
        log("LOGICAL MULTI CAMERA")
        log("==============================")

        val caps =
            chars.get(
                CameraCharacteristics
                    .REQUEST_AVAILABLE_CAPABILITIES
            ) ?: intArrayOf()

        val logical =
            caps.contains(
                CameraCharacteristics
                    .REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA
            )

        log(
            "Logical multi-camera: $logical"
        )

        log(
            "Capabilities: ${
                caps.contentToString()
            }"
        )
    }

    private fun dumpPhysicalIds(
        chars:
            CameraCharacteristics
    ) {

        log("")
        log("==============================")
        log("PHYSICAL CAMERA IDS")
        log("==============================")

        try {

            val ids =
                chars.physicalCameraIds

            if (ids.isEmpty()) {

                log(
                    "NONE"
                )

            } else {

                for (id in ids) {
                    log(id)
                }
            }

        } catch (e: Throwable) {

            log(
                "Physical ID query failed:"
            )

            log(
                e.toString()
            )
        }
    }

    private fun dumpPhysicalRequestKeys(
        chars:
            CameraCharacteristics
    ) {

        log("")
        log("==============================")
        log("PHYSICAL CAMERA REQUEST KEYS")
        log("==============================")

        try {

            val keys =
                chars.availablePhysicalCameraRequestKeys

            if (keys == null) {

                log(
                    "NONE / null"
                )

                return
            }

            if (keys.isEmpty()) {

                log(
                    "EMPTY"
                )

                return
            }

            for (key in keys) {

                log(
                    key.name
                )
            }

        } catch (e: Throwable) {

            log(
                "Physical request-key query failed:"
            )

            log(
                e.toString()
            )
        }
    }

    private fun dumpPixelModes(
        chars:
            CameraCharacteristics
    ) {

        log("")
        log("==============================")
        log("SENSOR PIXEL MODE SUPPORT")
        log("==============================")

        try {

            val modes =
                chars.get(
                    CameraCharacteristics
                        .SENSOR_INFO_PIXEL_ARRAY_SIZE
                )

            log(
                "Standard pixel array: ${
                    modes?.toString()
                        ?: "null"
                }"
            )

        } catch (_: Throwable) {
        }

        try {

            val maxArray =
                chars.get(
                    CameraCharacteristics
                        .SENSOR_INFO_PIXEL_ARRAY_SIZE_MAXIMUM_RESOLUTION
                )

            if (maxArray == null) {

                log(
                    "Maximum-resolution pixel array: null"
                )

            } else {

                log(
                    "Maximum-resolution pixel array:"
                )

                logSize(
                    maxArray
                )
            }

        } catch (e: Throwable) {

            log(
                "Maximum pixel-array query error:"
            )

            log(
                e.javaClass.simpleName +
                    ": " +
                    (e.message ?: "")
            )
        }
    }

    private fun dumpStandardMap(
        chars:
            CameraCharacteristics
    ) {

        log("")
        log("==============================")
        log("STANDARD STREAM MAP")
        log("==============================")

        val map =
            chars.get(
                CameraCharacteristics
                    .SCALER_STREAM_CONFIGURATION_MAP
            )

        if (map == null) {

            log(
                "NO STANDARD STREAM MAP"
            )

            return
        }

        dumpMap(
            map,
            "STANDARD"
        )
    }

    private fun dumpMaximumResolutionMap(
        chars:
            CameraCharacteristics
    ) {

        log("")
        log("==============================")
        log("MAXIMUM RESOLUTION STREAM MAP")
        log("==============================")

        try {

            val map =
                chars.get(
                    CameraCharacteristics
                        .SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION
                )

            if (map == null) {

                log(
                    "NOT EXPOSED / null"
                )

                return
            }

            dumpMap(
                map,
                "MAXIMUM"
            )

        } catch (e: Throwable) {

            log(
                "MAXIMUM RESOLUTION MAP QUERY FAILED"
            )

            log(
                e.javaClass.simpleName +
                    ": " +
                    (e.message ?: "")
            )
        }
    }

    private fun dumpMap(
        map:
            StreamConfigurationMap,
        label:
            String
    ) {

        val formats =
            try {
                map.outputFormats
            } catch (e: Throwable) {

                log(
                    "$label outputFormats error:"
                )

                log(
                    e.toString()
                )

                return
            }

        log(
            "$label output format count: ${
                formats.size
            }"
        )

        for (format in formats) {

            log("")
            log("--------------------------------")
            log(
                "$label FORMAT $format = ${
                    formatName(format)
                }"
            )
            log("--------------------------------")

            val sizes =
                try {
                    map.getOutputSizes(
                        format
                    )
                } catch (_: Throwable) {
                    null
                }

            if (sizes == null) {

                log(
                    "NO SIZES"
                )

                continue
            }

            log(
                "Count: ${sizes.size}"
            )

            val sorted =
                sizes.sortedByDescending {
                    pixels(it)
                }

            for (size in sorted) {

                val marker =
                    markerForSize(
                        size
                    )

                log(
                    size.width.toString() +
                        " x " +
                        size.height.toString() +
                        " = " +
                        String.format(
                            Locale.US,
                            "%.2f MP",
                            pixels(size) /
                                1_000_000.0
                        ) +
                        marker
                )
            }

            if (sorted.isNotEmpty()) {

                log("")
                log(
                    "LARGEST:"
                )

                logSize(
                    sorted.first()
                )
            }
        }

        log("")
        log(
            "$label HIGH RESOLUTION OUTPUTS"
        )

        val importantFormats =
            listOf(
                ImageFormat.RAW_SENSOR,
                ImageFormat.RAW10,
                ImageFormat.RAW12,
                ImageFormat.RAW_PRIVATE,
                ImageFormat.JPEG,
                ImageFormat.YUV_420_888,
                ImageFormat.PRIVATE,
                ImageFormat.HEIC
            )

        for (format in importantFormats) {

            log("")
            log(
                "${formatName(format)}:"
            )

            try {

                val sizes =
                    map.getHighResolutionOutputSizes(
                        format
                    )

                if (sizes == null) {

                    log(
                        "  NONE"
                    )

                    continue
                }

                if (sizes.isEmpty()) {

                    log(
                        "  EMPTY"
                    )

                    continue
                }

                for (
                    size in
                    sizes.sortedByDescending {
                        pixels(it)
                    }
                ) {

                    log(
                        "  ${size.width} x ${size.height}" +
                            markerForSize(size)
                    )
                }

            } catch (e: Throwable) {

                log(
                    "  query error: ${
                        e.javaClass.simpleName
                    }"
                )
            }
        }
    }

    private fun markerForSize(
        size:
            Size
    ): String {

        val mp =
            pixels(size) /
                1_000_000.0

        return when {

            size.width == 16320 &&
                size.height == 12288 ->
                "  <<< EXACT 200 MP TARGET"

            size.width == 12288 &&
                size.height == 16320 ->
                "  <<< EXACT 200 MP ROTATED"

            size.width == 8160 &&
                size.height == 6144 ->
                "  <<< 50 MP TARGET"

            size.width == 8192 &&
                size.height == 6144 ->
                "  <<< ~50 MP TARGET"

            mp >= 150.0 ->
                "  <<< 150+ MP"

            mp >= 40.0 ->
                "  <<< 40+ MP"

            else ->
                ""
        }
    }

    private fun pixels(
        size:
            Size
    ): Double {

        return size.width.toDouble() *
            size.height.toDouble()
    }

    private fun logSize(
        size:
            Size
    ) {

        log(
            "${size.width} x ${size.height}"
        )

        log(
            String.format(
                Locale.US,
                "%.2f MP",
                pixels(size) /
                    1_000_000.0
            ) +
                markerForSize(size)
        )
    }

    private fun formatName(
        format:
            Int
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

            ImageFormat.HEIC ->
                "HEIC"

            54 ->
                "YCBCR_P010 / 10-BIT YUV"

            4101 ->
                "JPEG_R / HDR JPEG"

            842094169 ->
                "YV12"

            else ->
                "UNKNOWN / VENDOR"
        }
    }

    private fun log(
        message:
            String
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
