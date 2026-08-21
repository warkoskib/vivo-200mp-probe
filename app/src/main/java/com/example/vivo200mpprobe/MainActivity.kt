package com.example.vivo200mpprobe

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
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
    private lateinit var cameraManager: CameraManager

    /*
     * These are the terms we're especially interested in after
     * examining VivoCamera.apk.
     */
    private val interestingTerms = listOf(
        "vivo",
        "mtk",
        "mediatek",
        "remosaic",
        "remosa",
        "sensor",
        "mode",
        "quad",
        "full",
        "resolution",
        "highresolution",
        "high_resolution",
        "200mp",
        "100mp",
        "capture",
        "raw",
        "pixel",
        "scenario",
        "stream",
        "session",
        "size"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraManager =
            getSystemService(CAMERA_SERVICE) as CameraManager

        buildUi()

        log("CAMERA 3 VENDOR KEY PROBE")
        log("==============================")
        log("")
        log("Camera ID: $CAMERA_ID")
        log("Device: ${Build.MODEL}")
        log("Android: ${Build.VERSION.RELEASE}")
        log("SDK: ${Build.VERSION.SDK_INT}")
        log("")
        log("Press SCAN CAMERA KEYS.")
    }

    private fun buildUi() {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setPadding(20, 30, 20, 30)

        // -------------------------------------------------
        // SCAN
        // -------------------------------------------------

        val scanButton = Button(this)

        scanButton.text = "SCAN CAMERA KEYS"

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
        // COPY
        // -------------------------------------------------

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
                    "Camera 3 Vendor Key Probe",
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
        // CLEAR
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

        val scroll = ScrollView(this)

        output = TextView(this)

        output.textSize = 12f
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

    // =====================================================
    // MAIN PROBE
    // =====================================================

    private fun runProbe() {

        log("CAMERA 3 VENDOR KEY PROBE")
        log("==============================")
        log("")

        try {

            val chars =
                cameraManager.getCameraCharacteristics(
                    CAMERA_ID
                )

            // -------------------------------------------------
            // SUMMARY FIRST
            // -------------------------------------------------

            dumpInterestingSummary(chars)

            // -------------------------------------------------
            // COMPLETE LISTS
            // -------------------------------------------------

            dumpCaptureRequestKeys(chars)

            dumpCaptureResultKeys(chars)

            dumpSessionKeys(chars)

            dumpCharacteristicKeys(chars)

            if (Build.VERSION.SDK_INT >= 35) {
                dumpSessionCharacteristicKeys(chars)
            }

            dumpPhysicalRequestKeys(chars)

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
    // INTERESTING SUMMARY
    // =====================================================

    private fun dumpInterestingSummary(
        chars: CameraCharacteristics
    ) {

        log("==============================")
        log("IMPORTANT / VENDOR KEY SUMMARY")
        log("==============================")
        log("")

        val requestKeys =
            try {
                chars.availableCaptureRequestKeys
            } catch (_: Throwable) {
                emptyList()
            }

        val resultKeys =
            try {
                chars.availableCaptureResultKeys
            } catch (_: Throwable) {
                emptyList()
            }

        val sessionKeys =
            if (Build.VERSION.SDK_INT >= 28) {

                try {
                    chars.availableSessionKeys ?: emptyList()
                } catch (_: Throwable) {
                    emptyList()
                }

            } else {
                emptyList()
            }

        val characteristicKeys =
            try {
                chars.keys
            } catch (_: Throwable) {
                emptyList()
            }

        val interestingRequests =
            requestKeys.filter {
                isInteresting(it.name)
            }

        val interestingResults =
            resultKeys.filter {
                isInteresting(it.name)
            }

        val interestingSessions =
            sessionKeys.filter {
                isInteresting(it.name)
            }

        val interestingCharacteristics =
            characteristicKeys.filter {
                isInteresting(it.name)
            }

        log("*** INTERESTING REQUEST KEYS ***")

        if (interestingRequests.isEmpty()) {
            log("NONE")
        } else {

            interestingRequests
                .sortedBy { it.name }
                .forEach {

                    log("")
                    log(it.name)
                }
        }

        log("")
        log("*** INTERESTING RESULT KEYS ***")

        if (interestingResults.isEmpty()) {
            log("NONE")
        } else {

            interestingResults
                .sortedBy { it.name }
                .forEach {

                    log("")
                    log(it.name)
                }
        }

        log("")
        log("*** INTERESTING SESSION KEYS ***")

        if (interestingSessions.isEmpty()) {
            log("NONE")
        } else {

            interestingSessions
                .sortedBy { it.name }
                .forEach {

                    log("")
                    log("!!! SESSION KEY !!!")
                    log(it.name)
                }
        }

        log("")
        log("*** INTERESTING CHARACTERISTIC KEYS ***")

        if (interestingCharacteristics.isEmpty()) {
            log("NONE")
        } else {

            interestingCharacteristics
                .sortedBy { it.name }
                .forEach {

                    log("")
                    log(it.name)

                    try {

                        val value =
                            chars.get(it)

                        log(
                            "VALUE: ${formatValue(value)}"
                        )

                    } catch (e: Throwable) {

                        log(
                            "VALUE READ ERROR: " +
                                e.javaClass.simpleName
                        )
                    }
                }
        }

        log("")
    }

    // =====================================================
    // REQUEST KEYS
    // =====================================================

    private fun dumpCaptureRequestKeys(
        chars: CameraCharacteristics
    ) {

        log("==============================")
        log("AVAILABLE CAPTURE REQUEST KEYS")
        log("==============================")

        try {

            val keys =
                chars.availableCaptureRequestKeys

            log("COUNT: ${keys.size}")
            log("")

            keys
                .sortedBy { it.name }
                .forEachIndexed { index, key ->

                    val marker =
                        if (isInteresting(key.name)) {
                            "***"
                        } else {
                            "   "
                        }

                    log(
                        "$marker [$index] ${key.name}"
                    )
                }

        } catch (e: Throwable) {

            log("ERROR:")
            log(e.toString())
        }

        log("")
    }

    // =====================================================
    // RESULT KEYS
    // =====================================================

    private fun dumpCaptureResultKeys(
        chars: CameraCharacteristics
    ) {

        log("==============================")
        log("AVAILABLE CAPTURE RESULT KEYS")
        log("==============================")

        try {

            val keys =
                chars.availableCaptureResultKeys

            log("COUNT: ${keys.size}")
            log("")

            keys
                .sortedBy { it.name }
                .forEachIndexed { index, key ->

                    val marker =
                        if (isInteresting(key.name)) {
                            "***"
                        } else {
                            "   "
                        }

                    log(
                        "$marker [$index] ${key.name}"
                    )
                }

        } catch (e: Throwable) {

            log("ERROR:")
            log(e.toString())
        }

        log("")
    }

    // =====================================================
    // SESSION KEYS
    // =====================================================

    private fun dumpSessionKeys(
        chars: CameraCharacteristics
    ) {

        log("==============================")
        log("AVAILABLE SESSION KEYS")
        log("==============================")

        if (Build.VERSION.SDK_INT < 28) {

            log(
                "Requires Android API 28+."
            )

            log("")
            return
        }

        try {

            val keys =
                chars.availableSessionKeys

            if (keys == null) {

                log("NULL / NONE")
                log("")
                return
            }

            log("COUNT: ${keys.size}")
            log("")

            keys
                .sortedBy { it.name }
                .forEachIndexed { index, key ->

                    val marker =
                        if (isInteresting(key.name)) {
                            ">>>"
                        } else {
                            "   "
                        }

                    log(
                        "$marker [$index] ${key.name}"
                    )
                }

        } catch (e: Throwable) {

            log("ERROR:")
            log(e.toString())
        }

        log("")
    }

    // =====================================================
    // CHARACTERISTIC KEYS
    // =====================================================

    private fun dumpCharacteristicKeys(
        chars: CameraCharacteristics
    ) {

        log("==============================")
        log("ALL CAMERA CHARACTERISTIC KEYS")
        log("==============================")

        try {

            val keys =
                chars.keys

            log("COUNT: ${keys.size}")
            log("")

            keys
                .sortedBy { it.name }
                .forEachIndexed { index, key ->

                    val marker =
                        if (isInteresting(key.name)) {
                            "***"
                        } else {
                            "   "
                        }

                    log(
                        "$marker [$index] ${key.name}"
                    )
                }

        } catch (e: Throwable) {

            log("ERROR:")
            log(e.toString())
        }

        log("")
    }

    // =====================================================
    // SESSION CHARACTERISTIC KEYS
    // Android 15 / API 35+
    // =====================================================

    private fun dumpSessionCharacteristicKeys(
        chars: CameraCharacteristics
    ) {

        log("==============================")
        log("SESSION CHARACTERISTIC KEYS")
        log("==============================")

        try {

            val keys =
                chars.availableSessionCharacteristicsKeys

            log("COUNT: ${keys.size}")
            log("")

            keys
                .sortedBy { it.name }
                .forEachIndexed { index, key ->

                    val marker =
                        if (isInteresting(key.name)) {
                            "***"
                        } else {
                            "   "
                        }

                    log(
                        "$marker [$index] ${key.name}"
                    )
                }

        } catch (e: Throwable) {

            log("ERROR:")
            log(e.toString())
        }

        log("")
    }

    // =====================================================
    // PHYSICAL CAMERA REQUEST KEYS
    // =====================================================

    private fun dumpPhysicalRequestKeys(
        chars: CameraCharacteristics
    ) {

        log("==============================")
        log("PHYSICAL CAMERA REQUEST KEYS")
        log("==============================")

        if (Build.VERSION.SDK_INT < 28) {

            log("Requires API 28+.")
            log("")
            return
        }

        try {

            val keys =
                chars.availablePhysicalCameraRequestKeys

            if (keys == null) {

                log("NULL / NONE")
                log("")
                return
            }

            log("COUNT: ${keys.size}")
            log("")

            keys
                .sortedBy { it.name }
                .forEachIndexed { index, key ->

                    val marker =
                        if (isInteresting(key.name)) {
                            "***"
                        } else {
                            "   "
                        }

                    log(
                        "$marker [$index] ${key.name}"
                    )
                }

        } catch (e: Throwable) {

            log("ERROR:")
            log(e.toString())
        }

        log("")
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private fun isInteresting(
        name: String
    ): Boolean {

        val lower =
            name.lowercase(
                Locale.US
            )

        return interestingTerms.any {
            lower.contains(it)
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
