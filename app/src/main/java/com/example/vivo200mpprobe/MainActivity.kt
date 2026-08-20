package com.example.vivocamera2probe

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var outputText: TextView
    private lateinit var scanButton: Button

    private val output = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createInterface()

        statusText.text = "APP STARTED SUCCESSFULLY"

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            statusText.text = "Camera permission required..."

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                100
            )
        } else {
            statusText.text = "Ready. Press START CAMERA PROBE."
        }
    }

    private fun createInterface() {

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
        }

        statusText = TextView(this).apply {
            textSize = 18f
            text = "Starting..."
            setPadding(0, 0, 0, 20)
        }

        scanButton = Button(this).apply {
            text = "START CAMERA PROBE"

            setOnClickListener {
                startProbe()
            }
        }

        outputText = TextView(this).apply {
            textSize = 12f
            setPadding(0, 25, 0, 25)
            setTextIsSelectable(true)
        }

        val scrollView = ScrollView(this)
        scrollView.addView(outputText)

        mainLayout.addView(statusText)
        mainLayout.addView(scanButton)

        mainLayout.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(mainLayout)
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

        if (
            requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            statusText.text =
                "Camera permission granted. Press START CAMERA PROBE."
        } else {
            statusText.text =
                "CAMERA PERMISSION DENIED"
        }
    }

    private fun startProbe() {

        scanButton.isEnabled = false

        output.clear()

        output.appendLine("VIVO CAMERA2 DEEP PROBE")
        output.appendLine("==============================")
        output.appendLine()

        updateOutput()

        statusText.text = "Starting camera scan..."

        thread {

            try {

                val cameraManager =
                    getSystemService(CAMERA_SERVICE) as CameraManager

                // ----------------------------------------------------
                // NORMAL CAMERA LIST
                // ----------------------------------------------------

                appendLineSafe("")
                appendLineSafe("==============================")
                appendLineSafe("ANDROID CAMERA ID LIST")
                appendLineSafe("==============================")

                val normalIds = try {
                    cameraManager.cameraIdList
                } catch (e: Throwable) {

                    appendLineSafe(
                        "ERROR getting camera list:"
                    )

                    appendLineSafe(
                        "${e.javaClass.simpleName}: ${e.message}"
                    )

                    emptyArray()
                }

                appendLineSafe(
                    "Number advertised: ${normalIds.size}"
                )

                for (id in normalIds) {

                    appendLineSafe("")
                    appendLineSafe("ADVERTISED CAMERA ID: $id")

                    analyzeCamera(
                        cameraManager,
                        id,
                        true
                    )
                }

                // ----------------------------------------------------
                // PHYSICAL IDS
                // ----------------------------------------------------

                appendLineSafe("")
                appendLineSafe("==============================")
                appendLineSafe("PHYSICAL CAMERA ID SEARCH")
                appendLineSafe("==============================")

                val physicalIds =
                    linkedSetOf<String>()

                for (logicalId in normalIds) {

                    try {

                        val characteristics =
                            cameraManager.getCameraCharacteristics(
                                logicalId
                            )

                        if (
                            android.os.Build.VERSION.SDK_INT >=
                            android.os.Build.VERSION_CODES.P
                        ) {

                            val ids =
                                characteristics.physicalCameraIds

                            for (id in ids) {
                                physicalIds.add(id)
                            }
                        }

                    } catch (e: Throwable) {

                        appendLineSafe(
                            "Could not inspect physical IDs for $logicalId"
                        )
                    }
                }

                if (physicalIds.isEmpty()) {

                    appendLineSafe(
                        "No physical camera IDs advertised."
                    )

                } else {

                    appendLineSafe(
                        "Physical IDs found: ${
                            physicalIds.joinToString(", ")
                        }"
                    )

                    for (id in physicalIds) {

                        appendLineSafe("")
                        appendLineSafe(
                            "PHYSICAL CAMERA ID: $id"
                        )

                        analyzeCamera(
                            cameraManager,
                            id,
                            false
                        )
                    }
                }

                // ----------------------------------------------------
                // HIDDEN NUMERIC ID TEST
                // ----------------------------------------------------

                appendLineSafe("")
                appendLineSafe("==============================")
                appendLineSafe("HIDDEN CAMERA ID TEST")
                appendLineSafe("==============================")
                appendLineSafe(
                    "Testing numeric IDs 0 through 30..."
                )

                var hiddenCount = 0

                for (number in 0..30) {

                    val id = number.toString()

                    runOnUiThread {
                        statusText.text =
                            "Testing Camera ID $id..."
                    }

                    if (
                        normalIds.contains(id) ||
                        physicalIds.contains(id)
                    ) {
                        continue
                    }

                    try {

                        cameraManager.getCameraCharacteristics(
                            id
                        )

                        hiddenCount++

                        appendLineSafe("")
                        appendLineSafe(
                            "*** HIDDEN CAMERA FOUND ***"
                        )

                        appendLineSafe(
                            "CAMERA ID: $id"
                        )

                        analyzeCamera(
                            cameraManager,
                            id,
                            false
                        )

                    } catch (_: IllegalArgumentException) {

                        // ID does not exist

                    } catch (_: SecurityException) {

                        appendLineSafe(
                            "ID $id = ACCESS BLOCKED BY ANDROID"
                        )

                    } catch (e: Throwable) {

                        // Important:
                        // don't crash the entire app if Vivo
                        // returns an unusual vendor exception.

                        appendLineSafe(
                            "ID $id response: " +
                                    e.javaClass.simpleName
                        )
                    }
                }

                appendLineSafe("")
                appendLineSafe("==============================")
                appendLineSafe("SCAN COMPLETE")
                appendLineSafe("==============================")

                appendLineSafe(
                    "Advertised cameras: ${normalIds.size}"
                )

                appendLineSafe(
                    "Physical IDs: ${physicalIds.size}"
                )

                appendLineSafe(
                    "Additional accessible hidden IDs: $hiddenCount"
                )

                runOnUiThread {

                    statusText.text = "SCAN COMPLETE"

                    scanButton.isEnabled = true

                    scanButton.text = "RUN SCAN AGAIN"
                }

            } catch (e: Throwable) {

                appendLineSafe("")
                appendLineSafe("==============================")
                appendLineSafe("FATAL SCAN ERROR")
                appendLineSafe("==============================")

                appendLineSafe(
                    e.javaClass.name
                )

                appendLineSafe(
                    e.message ?: "No error message"
                )

                runOnUiThread {

                    statusText.text =
                        "SCAN STOPPED - SEE ERROR BELOW"

                    scanButton.isEnabled = true
                }
            }
        }
    }

    private fun analyzeCamera(
        cameraManager: CameraManager,
        cameraId: String,
        advertised: Boolean
    ) {

        try {

            val c =
                cameraManager.getCameraCharacteristics(
                    cameraId
                )

            appendLineSafe("------------------------------")

            appendLineSafe(
                "Camera ID: $cameraId"
            )

            appendLineSafe(
                "Advertised: $advertised"
            )

            // ------------------------------------------------
            // FACING
            // ------------------------------------------------

            val facing =
                c.get(
                    CameraCharacteristics.LENS_FACING
                )

            val facingText =
                when (facing) {

                    CameraCharacteristics.LENS_FACING_BACK ->
                        "BACK"

                    CameraCharacteristics.LENS_FACING_FRONT ->
                        "FRONT"

                    CameraCharacteristics.LENS_FACING_EXTERNAL ->
                        "EXTERNAL"

                    else ->
                        "UNKNOWN"
                }

            appendLineSafe(
                "Facing: $facingText"
            )

            // ------------------------------------------------
            // SENSOR PIXEL ARRAY
            // ------------------------------------------------

            val pixelArray =
                c.get(
                    CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE
                )

            if (pixelArray != null) {

                val mp =
                    pixelArray.width.toDouble() *
                            pixelArray.height.toDouble() /
                            1_000_000.0

                appendLineSafe(
                    "Sensor Pixel Array: " +
                            "${pixelArray.width} x " +
                            "${pixelArray.height}"
                )

                appendLineSafe(
                    "Sensor Pixel Array MP: " +
                            "%.2f".format(mp)
                )
            }

            // ------------------------------------------------
            // ACTIVE ARRAY
            // ------------------------------------------------

            val activeArray =
                c.get(
                    CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE
                )

            if (activeArray != null) {

                val mp =
                    activeArray.width().toDouble() *
                            activeArray.height().toDouble() /
                            1_000_000.0

                appendLineSafe(
                    "Active Array: " +
                            "${activeArray.width()} x " +
                            "${activeArray.height()}"
                )

                appendLineSafe(
                    "Active Array MP: " +
                            "%.2f".format(mp)
                )
            }

            // ------------------------------------------------
            // SENSOR SIZE
            // ------------------------------------------------

            val sensorSize =
                c.get(
                    CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
                )

            if (sensorSize != null) {

                appendLineSafe(
                    "Physical Sensor: " +
                            "%.2f x %.2f mm".format(
                                sensorSize.width,
                                sensorSize.height
                            )
                )
            }

            // ------------------------------------------------
            // FOCAL LENGTH
            // ------------------------------------------------

            val focalLengths =
                c.get(
                    CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
                )

            if (focalLengths != null) {

                appendLineSafe(
                    "Focal Lengths: " +
                            focalLengths.joinToString(
                                ", "
                            ) {
                                "%.2f mm".format(it)
                            }
                )
            }

            // ------------------------------------------------
            // PHYSICAL CAMERA IDS
            // ------------------------------------------------

            if (
                android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.P
            ) {

                val ids =
                    c.physicalCameraIds

                if (ids.isEmpty()) {

                    appendLineSafe(
                        "Physical Camera IDs: NONE"
                    )

                } else {

                    appendLineSafe(
                        "Physical Camera IDs: " +
                                ids.joinToString(", ")
                    )
                }
            }

            // ------------------------------------------------
            // CAPABILITIES
            // ------------------------------------------------

            val capabilities =
                c.get(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
                )

            if (capabilities != null) {

                appendLineSafe(
                    "Capabilities:"
                )

                for (cap in capabilities) {

                    val capName =
                        when (cap) {

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
                                "CAPABILITY $cap"
                        }

                    appendLineSafe(
                        "  $capName"
                    )
                }
            }

            // ------------------------------------------------
            // OUTPUT RESOLUTIONS
            // ------------------------------------------------

            val map =
                c.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                )

            if (map != null) {

                checkOutputFormat(
                    map,
                    ImageFormat.JPEG,
                    "JPEG"
                )

                checkOutputFormat(
                    map,
                    ImageFormat.YUV_420_888,
                    "YUV"
                )

                checkOutputFormat(
                    map,
                    ImageFormat.RAW_SENSOR,
                    "RAW_SENSOR"
                )
            }

            // ------------------------------------------------
            // CHARACTERISTIC KEY SEARCH
            // ------------------------------------------------

            appendLineSafe("")
            appendLineSafe(
                "Interesting Camera Keys:"
            )

            val searchWords =
                listOf(
                    "vivo",
                    "vendor",
                    "pixel",
                    "quad",
                    "remosaic",
                    "resolution",
                    "high",
                    "super",
                    "binning",
                    "sensor",
                    "raw"
                )

            var interestingKeys = 0

            try {

                for (key in c.keys) {

                    val name = key.name

                    if (
                        searchWords.any {
                            name.contains(
                                it,
                                ignoreCase = true
                            )
                        }
                    ) {

                        interestingKeys++

                        appendLineSafe(
                            "  $name"
                        )
                    }
                }

            } catch (e: Throwable) {

                appendLineSafe(
                    "Could not enumerate keys."
                )
            }

            appendLineSafe(
                "Interesting keys found: $interestingKeys"
            )

        } catch (e: Throwable) {

            appendLineSafe(
                "Could not analyze Camera ID $cameraId"
            )

            appendLineSafe(
                "${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }

    private fun checkOutputFormat(
        map: android.hardware.camera2.params.StreamConfigurationMap,
        format: Int,
        name: String
    ) {

        try {

            val sizes =
                map.getOutputSizes(format)

            if (
                sizes == null ||
                sizes.isEmpty()
            ) {

                appendLineSafe(
                    "$name: NONE"
                )

                return
            }

            val largest =
                sizes.maxByOrNull {
                    it.width.toLong() *
                            it.height.toLong()
                }

            if (largest != null) {

                val mp =
                    largest.width.toDouble() *
                            largest.height.toDouble() /
                            1_000_000.0

                appendLineSafe(
                    "$name Max: " +
                            "${largest.width} x " +
                            "${largest.height} = " +
                            "%.2f MP".format(mp)
                )

                if (mp >= 40.0) {

                    appendLineSafe(
                        "*** HIGH RESOLUTION OUTPUT DETECTED ***"
                    )
                }

                if (mp >= 100.0) {

                    appendLineSafe(
                        "*** 100 MP+ OUTPUT DETECTED ***"
                    )
                }
            }

        } catch (e: Throwable) {

            appendLineSafe(
                "$name: ERROR ${e.javaClass.simpleName}"
            )
        }
    }

    @Synchronized
    private fun appendLineSafe(text: String) {

        output.appendLine(text)

        runOnUiThread {
            outputText.text =
                output.toString()
        }
    }

    private fun updateOutput() {

        runOnUiThread {
            outputText.text =
                output.toString()
        }
    }
}
