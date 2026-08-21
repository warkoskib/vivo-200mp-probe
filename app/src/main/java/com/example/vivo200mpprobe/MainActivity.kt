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
        private const val FIRST_ID = 0
        private const val LAST_ID = 30

        private const val TARGET_WIDTH = 16320
        private const val TARGET_HEIGHT = 12288

        private const val BIG_MP_THRESHOLD = 40.0
    }

    private lateinit var output: TextView
    private lateinit var scroll: ScrollView
    private lateinit var cameraManager: CameraManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraManager =
            getSystemService(CAMERA_SERVICE) as CameraManager

        buildUi()

        log("VIVO HIDDEN CAMERA ID PROBE")
        log("==============================")
        log("")
        log("Directly probing camera IDs:")
        log("$FIRST_ID through $LAST_ID")
        log("")
        log("This does NOT rely on cameraIdList.")
        log("")
        log("Press PROBE IDS 0-30.")
    }

    private fun buildUi() {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setPadding(20, 30, 20, 30)

        val probeButton = Button(this)

        probeButton.text = "PROBE IDS 0-30"

        probeButton.setOnClickListener {

            output.text = ""
            probeButton.isEnabled = false

            Thread {

                try {
                    runProbe()
                } finally {

                    runOnUiThread {
                        probeButton.isEnabled = true
                    }
                }

            }.start()
        }

        root.addView(probeButton)

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
                    "Vivo Hidden Camera Probe",
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

        clearButton.text = "CLEAR OUTPUT"

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

        log("VIVO HIDDEN CAMERA ID PROBE")
        log("==============================")
        log("")

        log("==============================")
        log("PUBLIC CAMERA ID LIST")
        log("==============================")

        try {

            val publicIds =
                cameraManager.cameraIdList

            if (publicIds.isEmpty()) {

                log("No public IDs returned.")

            } else {

                for (id in publicIds) {
                    log("PUBLIC: $id")
                }
            }

        } catch (e: Throwable) {

            log("cameraIdList error:")
            log(e.javaClass.name)
            log(e.message ?: "")
        }

        log("")
        log("==============================")
        log("DIRECT ID PROBE")
        log("==============================")

        var validCount = 0
        var hiddenCount = 0
        var largeSensorCount = 0

        val publicIdSet =
            try {
                cameraManager.cameraIdList.toSet()
            } catch (_: Throwable) {
                emptySet()
            }

        for (idNumber in FIRST_ID..LAST_ID) {

            val id =
                idNumber.toString()

            log("")
            log("")
            log("################################")
            log("CAMERA ID $id")
            log("################################")

            try {

                val chars =
                    cameraManager.getCameraCharacteristics(
                        id
                    )

                validCount++

                val hidden =
                    !publicIdSet.contains(id)

                if (hidden) {

                    hiddenCount++

                    log("*** HIDDEN / NOT PUBLICLY ENUMERATED ***")
                }

                log("STATUS: VALID")

                dumpCameraBasics(
                    id,
                    chars
                )

                val map =
                    chars.get(
                        CameraCharacteristics
                            .SCALER_STREAM_CONFIGURATION_MAP
                    )

                if (map == null) {

                    log("")
                    log("NO STREAM CONFIGURATION MAP")

                } else {

                    val foundLarge =
                        dumpStreamSummary(
                            map
                        )

                    if (foundLarge) {
                        largeSensorCount++
                    }
                }

            } catch (e: Throwable) {

                log("STATUS: INVALID / INACCESSIBLE")

                log(
                    "${e.javaClass.simpleName}: " +
                        (e.message ?: "")
                )
            }
        }

        log("")
        log("")
        log("==============================")
        log("FINAL SUMMARY")
        log("==============================")

        log(
            "IDs tested: ${LAST_ID - FIRST_ID + 1}"
        )

        log(
            "Valid IDs: $validCount"
        )

        log(
            "Hidden valid IDs: $hiddenCount"
        )

        log(
            "IDs with >=40 MP sensor/output clue: $largeSensorCount"
        )

        log("")
        log("==============================")
        log("PROBE COMPLETE")
        log("==============================")

        log("")
        log("Search output for:")
        log("16320")
        log("12288")
        log("200 MP")
        log(">=40 MP")
        log("HIDDEN")
    }

    private fun dumpCameraBasics(
        id: String,
        chars: CameraCharacteristics
    ) {

        val facing =
            chars.get(
                CameraCharacteristics.LENS_FACING
            )

        val hardwareLevel =
            chars.get(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
            )

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

        val capabilities =
            chars.get(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
            )

        log("")
        log("Lens facing:")
        log(
            facingName(facing)
        )

        log("")
        log("Hardware level:")
        log(
            hardwareLevelName(
                hardwareLevel
            )
        )

        log("")
        log("Pixel array:")

        if (pixelArray == null) {

            log("NOT EXPOSED")

        } else {

            log(
                "${pixelArray.width} x ${pixelArray.height}"
            )

            val mp =
                megapixels(
                    pixelArray.width,
                    pixelArray.height
                )

            log(
                String.format(
                    Locale.US,
                    "%.2f MP",
                    mp
                )
            )

            if (
                pixelArray.width ==
                TARGET_WIDTH &&
                pixelArray.height ==
                TARGET_HEIGHT
            ) {

                log("")
                log("********************************")
                log("EXACT 200 MP SENSOR FOUND")
                log("CAMERA ID: $id")
                log("16320 x 12288")
                log("********************************")
            }

            if (
                mp >=
                BIG_MP_THRESHOLD
            ) {

                log(
                    "*** >=40 MP SENSOR ARRAY ***"
                )
            }
        }

        log("")
        log("Active array:")
        log(
            activeArray?.toString()
                ?: "NOT EXPOSED"
        )

        log("")
        log("Physical size:")
        log(
            physicalSize?.toString()
                ?: "NOT EXPOSED"
        )

        val rawCapable =
            capabilities?.contains(
                CameraCharacteristics
                    .REQUEST_AVAILABLE_CAPABILITIES_RAW
            ) == true

        log("")
        log(
            "RAW capability: $rawCapable"
        )
    }

    private fun dumpStreamSummary(
        map: StreamConfigurationMap
    ): Boolean {

        log("")
        log("==============================")
        log("STREAM SUMMARY")
        log("==============================")

        var anyLarge =
            false

        val formats =
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

        for (format in formats) {

            log("")
            log(
                formatName(format)
            )

            val sizes =
                try {
                    map.getOutputSizes(format)
                } catch (_: Throwable) {
                    null
                }

            if (
                sizes == null ||
                sizes.isEmpty()
            ) {

                log("  NOT EXPOSED")
                continue
            }

            val largest =
                sizes.maxByOrNull {
                    pixels(it)
                }

            if (largest == null) {

                log("  NO SIZE")
                continue
            }

            val mp =
                megapixels(
                    largest.width,
                    largest.height
                )

            log(
                "  Largest: " +
                    "${largest.width} x ${largest.height}"
            )

            log(
                String.format(
                    Locale.US,
                    "  %.2f MP",
                    mp
                )
            )

            if (
                largest.width ==
                TARGET_WIDTH &&
                largest.height ==
                TARGET_HEIGHT
            ) {

                log(
                    "  *** EXACT 16320 x 12288 OUTPUT FOUND ***"
                )

                anyLarge =
                    true
            }

            if (
                mp >=
                BIG_MP_THRESHOLD
            ) {

                log(
                    "  *** >=40 MP OUTPUT ***"
                )

                anyLarge =
                    true
            }
        }

        log("")
        log("HIGH RES JPEG:")

        try {

            val highRes =
                map.getHighResolutionOutputSizes(
                    ImageFormat.JPEG
                )

            if (
                highRes == null ||
                highRes.isEmpty()
            ) {

                log(
                    "  NONE"
                )

            } else {

                val largestHigh =
                    highRes.maxByOrNull {
                        pixels(it)
                    }

                if (largestHigh != null) {

                    val mp =
                        megapixels(
                            largestHigh.width,
                            largestHigh.height
                        )

                    log(
                        "  Largest: " +
                            "${largestHigh.width} x ${largestHigh.height}"
                    )

                    log(
                        String.format(
                            Locale.US,
                            "  %.2f MP",
                            mp
                        )
                    )

                    if (
                        largestHigh.width ==
                        TARGET_WIDTH &&
                        largestHigh.height ==
                        TARGET_HEIGHT
                    ) {

                        log(
                            "  *** EXACT 200 MP HIGH-RES OUTPUT ***"
                        )

                        anyLarge =
                            true
                    }

                    if (
                        mp >=
                        BIG_MP_THRESHOLD
                    ) {

                        log(
                            "  *** >=40 MP HIGH-RES OUTPUT ***"
                        )

                        anyLarge =
                            true
                    }
                }
            }

        } catch (e: Throwable) {

            log(
                "  Query failed: " +
                    e.javaClass.simpleName
            )
        }

        return anyLarge
    }

    private fun pixels(
        size: Size
    ): Long {

        return size.width.toLong() *
            size.height.toLong()
    }

    private fun megapixels(
        width: Int,
        height: Int
    ): Double {

        return width.toDouble() *
            height.toDouble() /
            1_000_000.0
    }

    private fun facingName(
        value: Int?
    ): String {

        return when (value) {

            CameraCharacteristics
                .LENS_FACING_FRONT ->
                "$value = FRONT"

            CameraCharacteristics
                .LENS_FACING_BACK ->
                "$value = BACK"

            CameraCharacteristics
                .LENS_FACING_EXTERNAL ->
                "$value = EXTERNAL"

            null ->
                "NOT EXPOSED"

            else ->
                "$value = UNKNOWN"
        }
    }

    private fun hardwareLevelName(
        value: Int?
    ): String {

        return when (value) {

            CameraCharacteristics
                .INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY ->
                "$value = LEGACY"

            CameraCharacteristics
                .INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED ->
                "$value = LIMITED"

            CameraCharacteristics
                .INFO_SUPPORTED_HARDWARE_LEVEL_FULL ->
                "$value = FULL"

            CameraCharacteristics
                .INFO_SUPPORTED_HARDWARE_LEVEL_3 ->
                "$value = LEVEL_3"

            CameraCharacteristics
                .INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL ->
                "$value = EXTERNAL"

            null ->
                "NOT EXPOSED"

            else ->
                "$value = UNKNOWN"
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

            ImageFormat.HEIC ->
                "HEIC"

            else ->
                "FORMAT $format"
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
