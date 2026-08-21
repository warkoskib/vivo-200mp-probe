package com.example.vivo200mpprobe

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
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
    private lateinit var scroll: ScrollView
    private lateinit var cameraManager: CameraManager

    // =========================================================
    // VIVO CHARACTERISTIC KEYS FOUND IN STOCK Camera.apk
    // =========================================================

    private val sensorModeSizeListKey =
        CameraCharacteristics.Key(
            "com.vivo.sensorModeSizeList",
            IntArray::class.java
        )

    private val overrideSensorSizeKey =
        CameraCharacteristics.Key(
            "com.vivo.chi.override.SensorSize",
            IntArray::class.java
        )

    private val remosaicTypeIntKey =
        CameraCharacteristics.Key(
            "com.vivo.RemosaicType",
            Int::class.javaObjectType
        )

    private val remosaicTypeArrayKey =
        CameraCharacteristics.Key(
            "com.vivo.RemosaicType",
            IntArray::class.java
        )

    private val actualSizeKey =
        CameraCharacteristics.Key(
            "com.vivo.ActualSize",
            IntArray::class.java
        )

    private val mtkActualSizeKey =
        CameraCharacteristics.Key(
            "com.vivo.mtk.ActualSize",
            IntArray::class.java
        )

    private val jpegNeedUpscaleIntKey =
        CameraCharacteristics.Key(
            "com.vivo.JPEGNeedUpscale",
            Int::class.javaObjectType
        )

    private val jpegNeedUpscaleArrayKey =
        CameraCharacteristics.Key(
            "com.vivo.JPEGNeedUpscale",
            IntArray::class.java
        )

    private val supportHalDownscaleRemosaicIntKey =
        CameraCharacteristics.Key(
            "com.vivo.SupportHalDownscaleRemosaic",
            Int::class.javaObjectType
        )

    private val supportHalDownscaleRemosaicArrayKey =
        CameraCharacteristics.Key(
            "com.vivo.SupportHalDownscaleRemosaic",
            IntArray::class.java
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraManager =
            getSystemService(CAMERA_SERVICE) as CameraManager

        buildUi()

        log("VIVO HIDDEN SENSOR SIZE PROBE")
        log("==============================")
        log("")
        log("Keys extracted from stock Vivo Camera.apk:")
        log("")
        log("com.vivo.sensorModeSizeList")
        log("com.vivo.chi.override.SensorSize")
        log("com.vivo.RemosaicType")
        log("com.vivo.ActualSize")
        log("com.vivo.mtk.ActualSize")
        log("com.vivo.JPEGNeedUpscale")
        log("com.vivo.SupportHalDownscaleRemosaic")
        log("")
        log("Press PROBE CAMERA IDS 0-7.")
    }

    private fun buildUi() {

        val root = LinearLayout(this)

        root.orientation =
            LinearLayout.VERTICAL

        root.setPadding(
            20,
            30,
            20,
            30
        )

        val probeButton =
            Button(this)

        probeButton.text =
            "PROBE CAMERA IDS 0-7"

        probeButton.setOnClickListener {

            output.text = ""

            probeButton.isEnabled =
                false

            Thread {

                try {
                    runProbe()
                } finally {

                    runOnUiThread {
                        probeButton.isEnabled =
                            true
                    }
                }

            }.start()
        }

        root.addView(
            probeButton
        )

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
                    "Vivo Hidden Sensor Size Probe",
                    text
                )
            )

            Toast.makeText(
                this,
                "Output copied",
                Toast.LENGTH_SHORT
            ).show()
        }

        root.addView(
            copyButton
        )

        val clearButton =
            Button(this)

        clearButton.text =
            "CLEAR OUTPUT"

        clearButton.setOnClickListener {
            output.text = ""
        }

        root.addView(
            clearButton
        )

        scroll =
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

        scroll.addView(
            output
        )

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(
            root
        )
    }

    // =========================================================
    // MAIN PROBE
    // =========================================================

    private fun runProbe() {

        log("VIVO HIDDEN SENSOR SIZE PROBE")
        log("==============================")
        log("")

        try {

            val publicIds =
                cameraManager.cameraIdList

            log("==============================")
            log("PUBLIC CAMERA IDS")
            log("==============================")

            for (id in publicIds) {
                log(id)
            }

            log("")

        } catch (e: Throwable) {

            log(
                "Unable to enumerate public IDs:"
            )

            log(
                e.toString()
            )
        }

        var validCount =
            0

        var hiddenSizeCount =
            0

        for (number in 0..7) {

            val id =
                number.toString()

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

                validCount++

                dumpStandardSensorSize(
                    chars
                )

                log("")
                log("==============================")
                log("VIVO HIDDEN CHARACTERISTICS")
                log("==============================")

                val sensorModes =
                    readIntArrayKey(
                        chars,
                        sensorModeSizeListKey,
                        "com.vivo.sensorModeSizeList"
                    )

                if (
                    sensorModes != null &&
                    sensorModes.isNotEmpty()
                ) {

                    hiddenSizeCount++

                    dumpPossibleSizePairs(
                        sensorModes,
                        "sensorModeSizeList"
                    )
                }

                val overrideSize =
                    readIntArrayKey(
                        chars,
                        overrideSensorSizeKey,
                        "com.vivo.chi.override.SensorSize"
                    )

                if (
                    overrideSize != null &&
                    overrideSize.isNotEmpty()
                ) {

                    dumpPossibleSizePairs(
                        overrideSize,
                        "override.SensorSize"
                    )
                }

                val actualSize =
                    readIntArrayKey(
                        chars,
                        actualSizeKey,
                        "com.vivo.ActualSize"
                    )

                if (
                    actualSize != null &&
                    actualSize.isNotEmpty()
                ) {

                    dumpPossibleSizePairs(
                        actualSize,
                        "ActualSize"
                    )
                }

                val mtkActualSize =
                    readIntArrayKey(
                        chars,
                        mtkActualSizeKey,
                        "com.vivo.mtk.ActualSize"
                    )

                if (
                    mtkActualSize != null &&
                    mtkActualSize.isNotEmpty()
                ) {

                    dumpPossibleSizePairs(
                        mtkActualSize,
                        "mtk.ActualSize"
                    )
                }

                readFlexibleInt(
                    chars,
                    "com.vivo.RemosaicType",
                    remosaicTypeIntKey,
                    remosaicTypeArrayKey
                )

                readFlexibleInt(
                    chars,
                    "com.vivo.JPEGNeedUpscale",
                    jpegNeedUpscaleIntKey,
                    jpegNeedUpscaleArrayKey
                )

                readFlexibleInt(
                    chars,
                    "com.vivo.SupportHalDownscaleRemosaic",
                    supportHalDownscaleRemosaicIntKey,
                    supportHalDownscaleRemosaicArrayKey
                )

                searchCharacteristicNames(
                    chars
                )

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
        log("FINAL SUMMARY")
        log("==============================")

        log(
            "Valid IDs: $validCount"
        )

        log(
            "IDs returning sensorModeSizeList: $hiddenSizeCount"
        )

        log("")
        log(
            "Look specifically for:"
        )

        log(
            "16320 x 12288"
        )

        log(
            "8160 x 6144"
        )

        log(
            "200.54 MP"
        )

        log(
            "50.14 MP"
        )

        log("")
        log("==============================")
        log("PROBE COMPLETE")
        log("==============================")

        log("")
        log(
            "Press COPY OUTPUT."
        )
    }

    // =========================================================
    // STANDARD SENSOR DATA
    // =========================================================

    private fun dumpStandardSensorSize(
        chars:
            CameraCharacteristics
    ) {

        val pixel =
            chars.get(
                CameraCharacteristics
                    .SENSOR_INFO_PIXEL_ARRAY_SIZE
            )

        val active =
            chars.get(
                CameraCharacteristics
                    .SENSOR_INFO_ACTIVE_ARRAY_SIZE
            )

        log(
            "STATUS: VALID"
        )

        log("")
        log(
            "PUBLIC PIXEL ARRAY:"
        )

        if (pixel == null) {

            log(
                "NOT EXPOSED"
            )

        } else {

            logSize(
                pixel.width,
                pixel.height
            )
        }

        log("")
        log(
            "PUBLIC ACTIVE ARRAY:"
        )

        log(
            active?.toString()
                ?: "NOT EXPOSED"
        )
    }

    // =========================================================
    // DIRECT INT[] KEY QUERY
    // =========================================================

    private fun readIntArrayKey(
        chars:
            CameraCharacteristics,
        key:
            CameraCharacteristics.Key<IntArray>,
        name:
            String
    ): IntArray? {

        log("")
        log("--------------------------------")
        log(name)
        log("--------------------------------")

        return try {

            val value =
                chars.get(
                    key
                )

            if (value == null) {

                log(
                    "VALUE: null"
                )

                null

            } else {

                log(
                    "TYPE: IntArray"
                )

                log(
                    "LENGTH: ${value.size}"
                )

                log(
                    "RAW VALUE:"
                )

                log(
                    value.contentToString()
                )

                value
            }

        } catch (e: Throwable) {

            log(
                "READ FAILED"
            )

            log(
                e.javaClass.simpleName +
                    ": " +
                    (e.message ?: "")
            )

            null
        }
    }

    // =========================================================
    // FLEXIBLE INT / INT ARRAY QUERY
    // =========================================================

    private fun readFlexibleInt(
        chars:
            CameraCharacteristics,
        name:
            String,
        intKey:
            CameraCharacteristics.Key<Int>,
        arrayKey:
            CameraCharacteristics.Key<IntArray>
    ) {

        log("")
        log("--------------------------------")
        log(name)
        log("--------------------------------")

        var success =
            false

        try {

            val value =
                chars.get(
                    intKey
                )

            if (value != null) {

                log(
                    "TYPE: Integer"
                )

                log(
                    "VALUE: $value"
                )

                success =
                    true
            }

        } catch (_: Throwable) {
        }

        if (success) {
            return
        }

        try {

            val value =
                chars.get(
                    arrayKey
                )

            if (value != null) {

                log(
                    "TYPE: IntArray"
                )

                log(
                    "LENGTH: ${value.size}"
                )

                log(
                    "VALUE:"
                )

                log(
                    value.contentToString()
                )

                success =
                    true
            }

        } catch (_: Throwable) {
        }

        if (!success) {

            log(
                "No value returned using Integer or IntArray."
            )
        }
    }

    // =========================================================
    // INTERPRET INT ARRAYS AS POSSIBLE SIZE PAIRS
    // =========================================================

    private fun dumpPossibleSizePairs(
        data:
            IntArray,
        label:
            String
    ) {

        log("")
        log(
            "POSSIBLE SIZE PAIRS FROM $label"
        )

        log(
            "--------------------------------"
        )

        if (
            data.size < 2
        ) {

            log(
                "Not enough values."
            )

            return
        }

        var found =
            0

        var i =
            0

        while (
            i + 1 <
            data.size
        ) {

            val width =
                data[i]

            val height =
                data[i + 1]

            if (
                width > 100 &&
                height > 100 &&
                width < 50000 &&
                height < 50000
            ) {

                found++

                val mp =
                    width.toDouble() *
                        height.toDouble() /
                        1_000_000.0

                val marker =
                    when {

                        width == 16320 &&
                            height == 12288 ->
                            "  <<< EXACT 200 MP TARGET"

                        width == 12288 &&
                            height == 16320 ->
                            "  <<< EXACT 200 MP ROTATED"

                        mp >= 150.0 ->
                            "  <<< 150+ MP"

                        mp >= 40.0 ->
                            "  <<< 40+ MP"

                        else ->
                            ""
                    }

                log(
                    "$i/${i + 1}: " +
                        "$width x $height = " +
                        String.format(
                            Locale.US,
                            "%.2f MP",
                            mp
                        ) +
                        marker
                )
            }

            i +=
                2
        }

        if (
            found == 0
        ) {

            log(
                "No plausible sequential width/height pairs."
            )
        }

        /*
         * Also scan every adjacent pair because the array
         * may contain headers/flags between size entries.
         */

        log("")
        log(
            "ADJACENT-PAIR SEARCH:"
        )

        var adjacentFound =
            0

        for (
            index in
            0 until
                data.size - 1
        ) {

            val width =
                data[index]

            val height =
                data[index + 1]

            if (
                width >= 500 &&
                height >= 500 &&
                width <= 50000 &&
                height <= 50000
            ) {

                val mp =
                    width.toDouble() *
                        height.toDouble() /
                        1_000_000.0

                if (
                    mp >= 20.0
                ) {

                    adjacentFound++

                    val marker =
                        when {

                            width == 16320 &&
                                height == 12288 ->
                                " <<< EXACT TARGET"

                            mp >= 150.0 ->
                                " <<< 150+ MP"

                            mp >= 40.0 ->
                                " <<< 40+ MP"

                            else ->
                                ""
                        }

                    log(
                        "[$index,${
                            index + 1
                        }] $width x $height = " +
                            String.format(
                                Locale.US,
                                "%.2f MP",
                                mp
                            ) +
                            marker
                    )
                }
            }
        }

        if (
            adjacentFound == 0
        ) {

            log(
                "No >=20 MP adjacent pairs found."
            )
        }
    }

    // =========================================================
    // SEARCH VISIBLE CHARACTERISTIC NAMES
    // =========================================================

    private fun searchCharacteristicNames(
        chars:
            CameraCharacteristics
    ) {

        log("")
        log("==============================")
        log("VISIBLE RELATED KEY NAMES")
        log("==============================")

        val terms =
            listOf(
                "vivo",
                "sensor",
                "remosaic",
                "actualsize",
                "size",
                "upscale"
            )

        var count =
            0

        for (
            key in
            chars.keys
        ) {

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

                log(
                    key.name
                )
            }
        }

        log(
            "Visible related key count: $count"
        )
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private fun logSize(
        width:
            Int,
        height:
            Int
    ) {

        val mp =
            width.toDouble() *
                height.toDouble() /
                1_000_000.0

        log(
            "$width x $height"
        )

        log(
            String.format(
                Locale.US,
                "%.2f MP",
                mp
            )
        )
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
