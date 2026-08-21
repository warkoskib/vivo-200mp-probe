package com.example.vivo200mpprobe

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.Locale
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    companion object {

        private const val CAMERA_ID = "3"

        private const val WIDTH = 4080
        private const val HEIGHT = 3072

        private const val CAMERA_PERMISSION_REQUEST = 1001

        /*
         * Android CameraMetadata sensor-pixel-mode constants.
         *
         * 0 = DEFAULT
         * 1 = MAXIMUM_RESOLUTION
         *
         * Using the integer values also avoids API-level
         * constant-name problems on some SDK installations.
         */
        private const val SENSOR_PIXEL_MODE_DEFAULT_VALUE = 0
        private const val SENSOR_PIXEL_MODE_MAX_VALUE = 1

        private const val CASE_CONTROL = 0
        private const val CASE_OUTPUT_MAX = 1
        private const val CASE_REQUEST_MAX = 2
        private const val CASE_OUTPUT_REQUEST_MAX = 3
        private const val CASE_VIVO_MAX = 4

        private const val TOTAL_CASES = 5
    }

    private lateinit var output: TextView
    private lateinit var runButton: Button

    private lateinit var cameraManager: CameraManager

    private lateinit var cameraThread: HandlerThread
    private lateinit var cameraHandler: Handler

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var rawReader: ImageReader? = null

    private var currentCase = 0

    private var caseImageReceived = false
    private var caseCaptureCompleted = false

    // =========================================================
    // VIVO SESSION KEYS
    // =========================================================

    private val ultraHighResolutionKey =
        CaptureRequest.Key(
            "vivo.control.ultra_highresolution",
            Int::class.javaObjectType
        )

    private val portraitHighResolutionKey =
        CaptureRequest.Key(
            "vivo.control.portrait_high_resolution",
            Byte::class.javaObjectType
        )

    private val forceSensorModeKey =
        CaptureRequest.Key(
            "vivo.control.forceSensorMode",
            Int::class.javaObjectType
        )

    private val engineerRemosaicModeKey =
        CaptureRequest.Key(
            "vivo.control.EngineerRemosaicMode",
            Int::class.javaObjectType
        )

    private val advanceFullsizeKey =
        CaptureRequest.Key(
            "vivo.control.advance_fullsize",
            Int::class.javaObjectType
        )

    private val proRawKey =
        CaptureRequest.Key(
            "vivo.control.is_ProRaw_on",
            Int::class.javaObjectType
        )

    private val cameraScenarioKey =
        CaptureRequest.Key(
            "com.mediatek.seamlessfeature.cameraScenario",
            Int::class.javaObjectType
        )

    private val sensorScenarioKey =
        CaptureRequest.Key(
            "com.mediatek.seamlessfeature.sensorScenario",
            Int::class.javaObjectType
        )

    private val sensorScenarioCustomHintKey =
        CaptureRequest.Key(
            "com.mediatek.seamlessfeature.sensorScenarioCustomHint",
            Int::class.javaObjectType
        )

    private val streamsUsageKey =
        CaptureRequest.Key(
            "vivo.control.streamsUsage",
            IntArray::class.java
        )

    private val vcfStreamTypeKey =
        CaptureRequest.Key(
            "vivo.control.vcfStreamType",
            IntArray::class.java
        )

    // =========================================================
    // VIVO CAPTURE KEYS
    // =========================================================

    private val real200mpKey =
        CaptureRequest.Key(
            "vivo.control.real200mp_switch_on",
            Int::class.javaObjectType
        )

    private val sensorModeKey =
        CaptureRequest.Key(
            "vivo.control.sensorMode",
            Int::class.javaObjectType
        )

    private val previewSensorModeKey =
        CaptureRequest.Key(
            "vivo.preview.sensorMode",
            Int::class.javaObjectType
        )

    private val niceCaptureSensorModeKey =
        CaptureRequest.Key(
            "vivo.parameter.niceCaptureSensorMode",
            Int::class.javaObjectType
        )

    private val rawCaptureTypeKey =
        CaptureRequest.Key(
            "vivo.control.raw_capture_type",
            Int::class.javaObjectType
        )

    private val highResolutionDngTypeKey =
        CaptureRequest.Key(
            "vivo.parameter.highResolutionDngType",
            Int::class.javaObjectType
        )

    private val nativeModeKey =
        CaptureRequest.Key(
            "vivo.control.isNativeMode",
            Int::class.javaObjectType
        )

    private val seamlessRemosaicKey =
        CaptureRequest.Key(
            "vivo.control.seamless.remosaic.enable",
            Int::class.javaObjectType
        )

    private val remosaicCapabilityKey =
        CaptureRequest.Key(
            "vivo.control.remosaic.capability",
            Int::class.javaObjectType
        )

    private val isCaptureKey =
        CaptureRequest.Key(
            "vivo.control.isCapture",
            Int::class.javaObjectType
        )

    private val isSnapshotKey =
        CaptureRequest.Key(
            "vivo.control.is_snapshot",
            Int::class.javaObjectType
        )

    // =========================================================
    // ACTIVITY
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startCameraThread()
        buildUi()

        cameraManager =
            getSystemService(CAMERA_SERVICE) as CameraManager

        log("VIVO RAW VIF OUTPUTCONFIG PROBE")
        log("================================")
        log("")
        log("Camera ID: $CAMERA_ID")
        log("")
        log("Physical ImageReader:")
        log("$WIDTH x $HEIGHT RAW_SENSOR")
        log("")
        log("Cases:")
        log("A = NORMAL CONTROL")
        log("B = OUTPUT MAXIMUM RESOLUTION")
        log("C = REQUEST MAXIMUM RESOLUTION")
        log("D = OUTPUT + REQUEST MAXIMUM")
        log("E = D + VIVO SW RAW16 / 200 MP")
        log("")

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )

        } else {

            initialize()
        }
    }

    // =========================================================
    // UI
    // =========================================================

    private fun buildUi() {

        val root =
            LinearLayout(this)

        root.orientation =
            LinearLayout.VERTICAL

        root.setPadding(
            20,
            25,
            20,
            25
        )

        runButton =
            Button(this)

        runButton.text =
            "RUN OUTPUTCONFIG MATRIX"

        runButton.isEnabled =
            false

        runButton.setOnClickListener {

            runButton.isEnabled =
                false

            currentCase =
                0

            log("")
            log("")
            log("STARTING TEST MATRIX")
            log("==============================")

            runCurrentCase()
        }

        root.addView(
            runButton
        )

        val copyButton =
            Button(this)

        copyButton.text =
            "COPY OUTPUT"

        copyButton.setOnClickListener {

            val clipboard =
                getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as ClipboardManager

            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    "Vivo RAW VIF OutputConfig Probe",
                    output.text.toString()
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
            150
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
    // INITIAL CHARACTERISTICS PROBE
    // =========================================================

    private fun initialize() {

        dumpMaximumResolutionCharacteristics()

        openCamera()
    }

    private fun dumpMaximumResolutionCharacteristics() {

        log("==============================")
        log("CAMERA CHARACTERISTICS")
        log("==============================")

        try {

            val chars =
                cameraManager.getCameraCharacteristics(
                    CAMERA_ID
                )

            val capabilities =
                chars.get(
                    CameraCharacteristics
                        .REQUEST_AVAILABLE_CAPABILITIES
                )

            log("")
            log("REQUEST_AVAILABLE_CAPABILITIES:")

            if (capabilities == null) {

                log("null")

            } else {

                log(
                    capabilities.contentToString()
                )

                log("")

                for (capability in capabilities) {

                    log(
                        "  $capability"
                    )
                }
            }

            // -------------------------------------------------
            // Normal sensor array
            // -------------------------------------------------

            log("")
            log("------------------------------")
            log("NORMAL SENSOR ARRAY")
            log("------------------------------")

            val normalPixelArray =
                chars.get(
                    CameraCharacteristics
                        .SENSOR_INFO_PIXEL_ARRAY_SIZE
                )

            log(
                "PIXEL_ARRAY_SIZE:"
            )

            log(
                normalPixelArray?.toString()
                    ?: "null"
            )

            val normalActive =
                chars.get(
                    CameraCharacteristics
                        .SENSOR_INFO_ACTIVE_ARRAY_SIZE
                )

            log(
                "ACTIVE_ARRAY_SIZE:"
            )

            log(
                normalActive?.toString()
                    ?: "null"
            )

            // -------------------------------------------------
            // Maximum-resolution characteristics
            // API 31+
            // -------------------------------------------------

            log("")
            log("------------------------------")
            log("MAXIMUM RESOLUTION SENSOR ARRAY")
            log("------------------------------")

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S
            ) {

                try {

                    val maxPixel =
                        chars.get(
                            CameraCharacteristics
                                .SENSOR_INFO_PIXEL_ARRAY_SIZE_MAXIMUM_RESOLUTION
                        )

                    log(
                        "PIXEL_ARRAY_SIZE_MAXIMUM_RESOLUTION:"
                    )

                    log(
                        maxPixel?.toString()
                            ?: "null"
                    )

                } catch (e: Throwable) {

                    log(
                        "PIXEL_ARRAY_SIZE_MAXIMUM_RESOLUTION:"
                    )

                    log(
                        "READ ERROR: ${e.javaClass.simpleName}"
                    )
                }

                try {

                    val maxActive =
                        chars.get(
                            CameraCharacteristics
                                .SENSOR_INFO_ACTIVE_ARRAY_SIZE_MAXIMUM_RESOLUTION
                        )

                    log(
                        "ACTIVE_ARRAY_SIZE_MAXIMUM_RESOLUTION:"
                    )

                    log(
                        maxActive?.toString()
                            ?: "null"
                    )

                } catch (e: Throwable) {

                    log(
                        "ACTIVE_ARRAY_SIZE_MAXIMUM_RESOLUTION:"
                    )

                    log(
                        "READ ERROR: ${e.javaClass.simpleName}"
                    )
                }

                try {

                    val maxPreCorrection =
                        chars.get(
                            CameraCharacteristics
                                .SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE_MAXIMUM_RESOLUTION
                        )

                    log(
                        "PRE_CORRECTION_ACTIVE_ARRAY_MAXIMUM:"
                    )

                    log(
                        maxPreCorrection?.toString()
                            ?: "null"
                    )

                } catch (e: Throwable) {

                    log(
                        "PRE_CORRECTION_ACTIVE_ARRAY_MAXIMUM:"
                    )

                    log(
                        "READ ERROR: ${e.javaClass.simpleName}"
                    )
                }

                // ---------------------------------------------
                // Maximum resolution stream map
                // ---------------------------------------------

                log("")
                log("------------------------------")
                log("MAXIMUM RESOLUTION STREAM MAP")
                log("------------------------------")

                try {

                    val maxMap =
                        chars.get(
                            CameraCharacteristics
                                .SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION
                        )

                    if (maxMap == null) {

                        log(
                            "MAXIMUM_RESOLUTION_STREAM_MAP = null"
                        )

                    } else {

                        log(
                            "MAXIMUM_RESOLUTION_STREAM_MAP PRESENT"
                        )

                        dumpMapFormat(
                            maxMap,
                            ImageFormat.RAW_SENSOR,
                            "RAW_SENSOR"
                        )

                        dumpMapFormat(
                            maxMap,
                            ImageFormat.RAW10,
                            "RAW10"
                        )

                        dumpMapFormat(
                            maxMap,
                            ImageFormat.RAW12,
                            "RAW12"
                        )

                        dumpMapFormat(
                            maxMap,
                            ImageFormat.JPEG,
                            "JPEG"
                        )

                        dumpMapFormat(
                            maxMap,
                            ImageFormat.YUV_420_888,
                            "YUV_420_888"
                        )
                    }

                } catch (e: Throwable) {

                    log(
                        "MAXIMUM_RESOLUTION_STREAM_MAP READ ERROR"
                    )

                    log(
                        e.javaClass.simpleName +
                            ": " +
                            (e.message ?: "")
                    )
                }

            } else {

                log(
                    "Android version is below API 31."
                )
            }

            // -------------------------------------------------
            // SENSOR_PIXEL_MODE availability
            // -------------------------------------------------

            log("")
            log("------------------------------")
            log("SENSOR_PIXEL_MODE KEY")
            log("------------------------------")

            try {

                val present =
                    chars.availableCaptureRequestKeys
                        .any {
                            it.name ==
                                "android.sensor.pixelMode"
                        }

                log(
                    "Capture request key present: $present"
                )

            } catch (e: Throwable) {

                log(
                    "Unable to inspect request keys."
                )
            }

            // -------------------------------------------------
            // Available stream use cases
            // -------------------------------------------------

            log("")
            log("------------------------------")
            log("STREAM USE CASES")
            log("------------------------------")

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {

                try {

                    val useCases =
                        chars.get(
                            CameraCharacteristics
                                .SCALER_AVAILABLE_STREAM_USE_CASES
                        )

                    if (useCases == null) {

                        log(
                            "SCALER_AVAILABLE_STREAM_USE_CASES = null"
                        )

                    } else {

                        log(
                            useCases.contentToString()
                        )
                    }

                } catch (e: Throwable) {

                    log(
                        "STREAM USE CASE READ ERROR: " +
                            e.javaClass.simpleName
                    )
                }

            } else {

                log(
                    "Stream-use-case characteristic not tested."
                )
            }

            log("")

        } catch (e: Throwable) {

            log("")
            log(
                "CHARACTERISTICS PROBE ERROR"
            )

            log(
                e.javaClass.name
            )

            log(
                e.message ?: ""
            )
        }
    }

    private fun dumpMapFormat(
        map:
            android.hardware.camera2.params.StreamConfigurationMap,
        format:
            Int,
        label:
            String
    ) {

        log("")
        log("$label:")

        try {

            val sizes =
                map.getOutputSizes(
                    format
                )

            if (
                sizes == null ||
                sizes.isEmpty()
            ) {

                log(
                    "  NONE"
                )

                return
            }

            for (size in sizes) {

                val mp =
                    size.width.toDouble() *
                        size.height.toDouble() /
                        1_000_000.0

                log(
                    String.format(
                        Locale.US,
                        "  %d x %d = %.2f MP",
                        size.width,
                        size.height,
                        mp
                    )
                )
            }

        } catch (e: Throwable) {

            log(
                "  READ ERROR: " +
                    e.javaClass.simpleName
            )
        }
    }

    // =========================================================
    // OPEN CAMERA
    // =========================================================

    private fun openCamera() {

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        log("==============================")
        log("OPENING CAMERA 3")
        log("==============================")

        try {

            cameraManager.openCamera(
                CAMERA_ID,

                object :
                    CameraDevice.StateCallback() {

                    override fun onOpened(
                        camera: CameraDevice
                    ) {

                        cameraDevice =
                            camera

                        log(
                            "Camera 3 opened."
                        )

                        log("")
                        log(
                            "Press RUN OUTPUTCONFIG MATRIX."
                        )

                        runOnUiThread {
                            runButton.isEnabled =
                                true
                        }
                    }

                    override fun onDisconnected(
                        camera: CameraDevice
                    ) {

                        log(
                            "Camera disconnected."
                        )

                        camera.close()

                        cameraDevice =
                            null
                    }

                    override fun onError(
                        camera: CameraDevice,
                        error: Int
                    ) {

                        log("")
                        log(
                            "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
                        )

                        log(
                            "CAMERA ERROR = $error"
                        )

                        log(
                            "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
                        )

                        if (error == 4) {

                            log(
                                "ERROR 4 = ERROR_CAMERA_DEVICE"
                            )
                        }

                        camera.close()

                        cameraDevice =
                            null

                        runOnUiThread {
                            runButton.isEnabled =
                                false
                        }
                    }
                },

                cameraHandler
            )

        } catch (e: Throwable) {

            log(
                "OPEN CAMERA EXCEPTION"
            )

            log(
                e.javaClass.name
            )

            log(
                e.message ?: ""
            )
        }
    }

    // =========================================================
    // MATRIX
    // =========================================================

    private fun runCurrentCase() {

        if (
            currentCase >=
            TOTAL_CASES
        ) {

            log("")
            log("")
            log("==============================")
            log("MATRIX COMPLETE")
            log("==============================")
            log("")
            log("Press COPY OUTPUT.")

            runOnUiThread {
                runButton.isEnabled =
                    true
            }

            return
        }

        closeSessionResources()

        caseImageReceived =
            false

        caseCaptureCompleted =
            false

        val caseNumber =
            currentCase + 1

        log("")
        log("")
        log("################################")
        log(
            "CASE $caseNumber/$TOTAL_CASES"
        )

        log(
            caseName(
                currentCase
            )
        )

        log("################################")

        createCaseSession(
            currentCase
        )
    }

    private fun caseName(
        caseId: Int
    ): String {

        return when (caseId) {

            CASE_CONTROL ->
                "A - NORMAL CONTROL"

            CASE_OUTPUT_MAX ->
                "B - OUTPUT MAXIMUM RESOLUTION"

            CASE_REQUEST_MAX ->
                "C - REQUEST MAXIMUM RESOLUTION"

            CASE_OUTPUT_REQUEST_MAX ->
                "D - OUTPUT + REQUEST MAXIMUM"

            CASE_VIVO_MAX ->
                "E - OUTPUT + REQUEST MAX + VIVO OEM"

            else ->
                "UNKNOWN"
        }
    }

    // =========================================================
    // CREATE SESSION
    // =========================================================

    private fun createCaseSession(
        caseId: Int
    ) {

        val camera =
            cameraDevice

        if (camera == null) {

            log(
                "Camera is not open."
            )

            return
        }

        try {

            rawReader =
                ImageReader.newInstance(
                    WIDTH,
                    HEIGHT,
                    ImageFormat.RAW_SENSOR,
                    3
                )

            rawReader!!
                .setOnImageAvailableListener(
                    { reader ->

                        handleRawImage(
                            reader
                        )
                    },

                    cameraHandler
                )

            log("")
            log(
                "ImageReader CREATED"
            )

            log(
                "$WIDTH x $HEIGHT RAW_SENSOR"
            )

        } catch (e: Throwable) {

            log(
                "ImageReader creation FAILED"
            )

            log(
                e.toString()
            )

            moveToNextCase()

            return
        }

        val surface =
            rawReader!!.surface

        val outputConfig =
            OutputConfiguration(
                surface
            )

        log("")
        log("OUTPUT CONFIGURATION")
        log("--------------------")

        log(
            "Surface size = $WIDTH x $HEIGHT"
        )

        log(
            "Surface format = RAW_SENSOR / 32"
        )

        val wantsOutputMax =
            caseId ==
                CASE_OUTPUT_MAX ||
                caseId ==
                CASE_OUTPUT_REQUEST_MAX ||
                caseId ==
                CASE_VIVO_MAX

        if (wantsOutputMax) {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S
            ) {

                try {

                    outputConfig
                        .addSensorPixelModeUsed(
                            SENSOR_PIXEL_MODE_MAX_VALUE
                        )

                    log(
                        "addSensorPixelModeUsed(MAXIMUM_RESOLUTION): ACCEPTED"
                    )

                } catch (e: Throwable) {

                    log(
                        "addSensorPixelModeUsed(MAXIMUM_RESOLUTION): REJECTED"
                    )

                    log(
                        e.javaClass.simpleName +
                            ": " +
                            (e.message ?: "")
                    )
                }

            } else {

                log(
                    "addSensorPixelModeUsed not available on this Android version."
                )
            }

        } else {

            log(
                "Sensor pixel mode on OutputConfiguration: DEFAULT / NOT OVERRIDDEN"
            )
        }

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {

            try {

                val modes =
                    outputConfig.sensorPixelModesUsed

                log(
                    "OutputConfiguration sensorPixelModesUsed:"
                )

                log(
                    modes.toString()
                )

            } catch (e: Throwable) {

                log(
                    "Unable to read sensorPixelModesUsed: " +
                        e.javaClass.simpleName
                )
            }
        }

        val callback =
            object :
                CameraCaptureSession.StateCallback() {

                override fun onConfigured(
                    session:
                        CameraCaptureSession
                ) {

                    captureSession =
                        session

                    log("")
                    log(
                        "SESSION RESULT: CONFIGURED"
                    )

                    startCaseCapture(
                        caseId
                    )
                }

                override fun onConfigureFailed(
                    session:
                        CameraCaptureSession
                ) {

                    log("")
                    log(
                        "SESSION RESULT: FAILED"
                    )

                    moveToNextCase()
                }
            }

        try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.P
            ) {

                val executor =
                    Executor { runnable ->
                        cameraHandler.post(
                            runnable
                        )
                    }

                val config =
                    SessionConfiguration(
                        SessionConfiguration
                            .SESSION_REGULAR,
                        listOf(
                            outputConfig
                        ),
                        executor,
                        callback
                    )

                /*
                 * Session parameters are only added
                 * for Case E.
                 */

                if (
                    caseId ==
                    CASE_VIVO_MAX
                ) {

                    val builder =
                        camera.createCaptureRequest(
                            CameraDevice
                                .TEMPLATE_STILL_CAPTURE
                        )

                    builder.addTarget(
                        surface
                    )

                    log("")
                    log("VIVO SESSION PARAMETERS")
                    log("-----------------------")

                    applyVivoSessionParameters(
                        builder
                    )

                    config.setSessionParameters(
                        builder.build()
                    )
                }

                camera.createCaptureSession(
                    config
                )

            } else {

                @Suppress("DEPRECATION")
                camera.createCaptureSession(
                    listOf(
                        surface
                    ),
                    callback,
                    cameraHandler
                )
            }

        } catch (e: Throwable) {

            log("")
            log(
                "SESSION CREATION EXCEPTION"
            )

            log(
                e.javaClass.name
            )

            log(
                e.message ?: ""
            )

            moveToNextCase()
        }
    }

    // =========================================================
    // CAPTURE
    // =========================================================

    private fun startCaseCapture(
        caseId: Int
    ) {

        val camera =
            cameraDevice ?: return

        val session =
            captureSession ?: return

        val reader =
            rawReader ?: return

        try {

            val builder =
                camera.createCaptureRequest(
                    CameraDevice
                        .TEMPLATE_STILL_CAPTURE
                )

            builder.addTarget(
                reader.surface
            )

            builder.set(
                CaptureRequest.CONTROL_MODE,
                CameraMetadata.CONTROL_MODE_AUTO
            )

            builder.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON
            )

            val wantsRequestMax =
                caseId ==
                    CASE_REQUEST_MAX ||
                    caseId ==
                    CASE_OUTPUT_REQUEST_MAX ||
                    caseId ==
                    CASE_VIVO_MAX

            log("")
            log("CAPTURE PARAMETERS")
            log("------------------")

            if (wantsRequestMax) {

                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.S
                ) {

                    try {

                        builder.set(
                            CaptureRequest.SENSOR_PIXEL_MODE,
                            SENSOR_PIXEL_MODE_MAX_VALUE
                        )

                        log(
                            "SENSOR_PIXEL_MODE = MAXIMUM_RESOLUTION"
                        )

                    } catch (e: Throwable) {

                        log(
                            "SENSOR_PIXEL_MODE MAX SET FAILED"
                        )

                        log(
                            e.javaClass.simpleName +
                                ": " +
                                (e.message ?: "")
                        )
                    }

                } else {

                    log(
                        "SENSOR_PIXEL_MODE unavailable on this Android version."
                    )
                }

            } else {

                log(
                    "SENSOR_PIXEL_MODE = DEFAULT / NOT SET"
                )
            }

            if (
                caseId ==
                CASE_VIVO_MAX
            ) {

                log("")
                log("VIVO CAPTURE PARAMETERS")
                log("-----------------------")

                applyVivoSessionParameters(
                    builder
                )

                applyVivoCaptureParameters(
                    builder
                )
            }

            session.capture(
                builder.build(),

                object :
                    CameraCaptureSession.CaptureCallback() {

                    override fun onCaptureStarted(
                        session:
                            CameraCaptureSession,
                        request:
                            CaptureRequest,
                        timestamp:
                            Long,
                        frameNumber:
                            Long
                    ) {

                        log("")
                        log(
                            "CAPTURE STARTED"
                        )

                        log(
                            "Frame = $frameNumber"
                        )

                        log(
                            "Timestamp = $timestamp"
                        )
                    }

                    override fun onCaptureCompleted(
                        session:
                            CameraCaptureSession,
                        request:
                            CaptureRequest,
                        result:
                            TotalCaptureResult
                    ) {

                        caseCaptureCompleted =
                            true

                        log("")
                        log(
                            "CAPTURE RESULT: COMPLETED"
                        )

                        dumpCaptureResult(
                            result
                        )

                        finishCaseSoon()
                    }

                    override fun onCaptureFailed(
                        session:
                            CameraCaptureSession,
                        request:
                            CaptureRequest,
                        failure:
                            CaptureFailure
                    ) {

                        log("")
                        log(
                            "CAPTURE RESULT: FAILED"
                        )

                        log(
                            "Reason = ${failure.reason}"
                        )

                        log(
                            "Frame = ${failure.frameNumber}"
                        )

                        finishCaseSoon()
                    }

                    override fun onCaptureSequenceCompleted(
                        session:
                            CameraCaptureSession,
                        sequenceId:
                            Int,
                        frameNumber:
                            Long
                    ) {

                        log(
                            "Capture sequence complete. Last frame = $frameNumber"
                        )
                    }
                },

                cameraHandler
            )

        } catch (e: Throwable) {

            log("")
            log(
                "CAPTURE EXCEPTION"
            )

            log(
                e.javaClass.name
            )

            log(
                e.message ?: ""
            )

            finishCaseSoon()
        }
    }

    // =========================================================
    // RAW IMAGE
    // =========================================================

    private fun handleRawImage(
        reader: ImageReader
    ) {

        var image:
            Image? =
            null

        try {

            image =
                reader.acquireNextImage()

            if (image == null) {
                return
            }

            caseImageReceived =
                true

            log("")
            log("==============================")
            log("RAW IMAGE RECEIVED")
            log("==============================")

            log(
                "Width = ${image.width}"
            )

            log(
                "Height = ${image.height}"
            )

            log(
                "Format = ${image.format}"
            )

            log(
                "Timestamp = ${image.timestamp}"
            )

            log(
                "Planes = ${image.planes.size}"
            )

            var totalBytes =
                0L

            image.planes
                .forEachIndexed {
                        index,
                        plane ->

                    val bytes =
                        plane.buffer.remaining()

                    totalBytes +=
                        bytes.toLong()

                    log("")
                    log(
                        "Plane $index"
                    )

                    log(
                        "Bytes = $bytes"
                    )

                    log(
                        "RowStride = ${plane.rowStride}"
                    )

                    log(
                        "PixelStride = ${plane.pixelStride}"
                    )
                }

            log("")
            log(
                "TOTAL BYTES = $totalBytes"
            )

            log(
                String.format(
                    Locale.US,
                    "%.2f MB",
                    totalBytes /
                        1024.0 /
                        1024.0
                )
            )

        } catch (e: Throwable) {

            log(
                "RAW IMAGE ERROR:"
            )

            log(
                e.toString()
            )

        } finally {

            try {
                image?.close()
            } catch (_: Throwable) {
            }
        }
    }

    // =========================================================
    // VIVO PARAMETERS
    // =========================================================

    private fun applyVivoSessionParameters(
        builder:
            CaptureRequest.Builder
    ) {

        setInt(
            builder,
            ultraHighResolutionKey,
            1,
            "ultra_highresolution"
        )

        setByte(
            builder,
            portraitHighResolutionKey,
            1,
            "portrait_high_resolution"
        )

        setInt(
            builder,
            forceSensorModeKey,
            0,
            "forceSensorMode"
        )

        setInt(
            builder,
            engineerRemosaicModeKey,
            1,
            "EngineerRemosaicMode"
        )

        setInt(
            builder,
            advanceFullsizeKey,
            0,
            "advance_fullsize"
        )

        setInt(
            builder,
            proRawKey,
            1,
            "is_ProRaw_on"
        )

        setInt(
            builder,
            cameraScenarioKey,
            3,
            "cameraScenario"
        )

        setInt(
            builder,
            sensorScenarioKey,
            3,
            "sensorScenario"
        )

        setInt(
            builder,
            sensorScenarioCustomHintKey,
            1,
            "sensorScenarioCustomHint"
        )

        setIntArray(
            builder,
            streamsUsageKey,
            intArrayOf(
                2,
                1,
                0
            ),
            "streamsUsage"
        )

        setIntArray(
            builder,
            vcfStreamTypeKey,
            intArrayOf(
                0,
                1
            ),
            "vcfStreamType"
        )
    }

    private fun applyVivoCaptureParameters(
        builder:
            CaptureRequest.Builder
    ) {

        setInt(
            builder,
            real200mpKey,
            1,
            "real200mp_switch_on"
        )

        setInt(
            builder,
            sensorModeKey,
            0,
            "sensorMode"
        )

        setInt(
            builder,
            previewSensorModeKey,
            0,
            "preview.sensorMode"
        )

        setInt(
            builder,
            niceCaptureSensorModeKey,
            0,
            "niceCaptureSensorMode"
        )

        setInt(
            builder,
            rawCaptureTypeKey,
            32,
            "raw_capture_type"
        )

        setInt(
            builder,
            highResolutionDngTypeKey,
            1,
            "highResolutionDngType"
        )

        setInt(
            builder,
            nativeModeKey,
            1,
            "isNativeMode"
        )

        setInt(
            builder,
            seamlessRemosaicKey,
            1,
            "seamless.remosaic.enable"
        )

        setInt(
            builder,
            remosaicCapabilityKey,
            1,
            "remosaic.capability"
        )

        setInt(
            builder,
            isCaptureKey,
            1,
            "isCapture"
        )

        setInt(
            builder,
            isSnapshotKey,
            1,
            "is_snapshot"
        )
    }

    // =========================================================
    // RESULT DUMP
    // =========================================================

    private fun dumpCaptureResult(
        result:
            TotalCaptureResult
    ) {

        log("")
        log("==============================")
        log("SENSOR / VIVO RESULT")
        log("==============================")

        // -----------------------------------------------------
        // Android SENSOR_PIXEL_MODE
        // -----------------------------------------------------

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {

            try {

                val value =
                    result.get(
                        CaptureResult.SENSOR_PIXEL_MODE
                    )

                log(
                    "android.sensor.pixelMode = " +
                        (value?.toString() ?: "null")
                )

            } catch (e: Throwable) {

                log(
                    "android.sensor.pixelMode = READ ERROR"
                )
            }
        }

        // -----------------------------------------------------
        // Dump relevant OEM values by name.
        // This avoids requiring guessed result-key types.
        // -----------------------------------------------------

        val terms =
            listOf(
                "sensorMode",
                "pixelMode",
                "raw_capture_type",
                "highResolutionDngType",
                "real200mp",
                "remosaic",
                "currentMode",
                "sceneMode",
                "fullsize",
                "upscale",
                "tuning",
                "RequestLeft"
            )

        for (key in result.keys) {

            val name =
                key.name

            if (
                terms.any {
                    name.contains(
                        it,
                        ignoreCase = true
                    )
                }
            ) {

                val value =
                    try {
                        result.get(
                            key
                        )
                    } catch (_: Throwable) {
                        "<READ ERROR>"
                    }

                log("")
                log(name)

                log(
                    formatValue(
                        value
                    )
                )
            }
        }
    }

    // =========================================================
    // CASE FINISH
    // =========================================================

    private var finishScheduled =
        false

    private fun finishCaseSoon() {

        if (finishScheduled) {
            return
        }

        finishScheduled =
            true

        cameraHandler.postDelayed(
            {

                log("")
                log("------------------------------")
                log("CASE SUMMARY")
                log("------------------------------")

                log(
                    "Image received = $caseImageReceived"
                )

                log(
                    "Capture completed = $caseCaptureCompleted"
                )

                finishScheduled =
                    false

                moveToNextCase()

            },

            1200L
        )
    }

    private fun moveToNextCase() {

        closeSessionResources()

        currentCase++

        cameraHandler.postDelayed(
            {
                runCurrentCase()
            },
            700L
        )
    }

    // =========================================================
    // SETTERS
    // =========================================================

    private fun setInt(
        builder:
            CaptureRequest.Builder,
        key:
            CaptureRequest.Key<Int>,
        value:
            Int,
        name:
            String
    ) {

        try {

            builder.set(
                key,
                value
            )

            log(
                "OK $name = $value"
            )

        } catch (e: Throwable) {

            log(
                "FAIL $name"
            )

            log(
                "  ${e.javaClass.simpleName}: " +
                    (e.message ?: "")
            )
        }
    }

    private fun setByte(
        builder:
            CaptureRequest.Builder,
        key:
            CaptureRequest.Key<Byte>,
        value:
            Int,
        name:
            String
    ) {

        try {

            builder.set(
                key,
                value.toByte()
            )

            log(
                "OK $name = $value"
            )

        } catch (e: Throwable) {

            log(
                "FAIL $name"
            )

            log(
                "  ${e.javaClass.simpleName}: " +
                    (e.message ?: "")
            )
        }
    }

    private fun setIntArray(
        builder:
            CaptureRequest.Builder,
        key:
            CaptureRequest.Key<IntArray>,
        value:
            IntArray,
        name:
            String
    ) {

        try {

            builder.set(
                key,
                value
            )

            log(
                "OK $name = " +
                    value.contentToString()
            )

        } catch (e: Throwable) {

            log(
                "FAIL $name"
            )

            log(
                "  ${e.javaClass.simpleName}: " +
                    (e.message ?: "")
            )
        }
    }

    // =========================================================
    // FORMAT RESULT VALUES
    // =========================================================

    private fun formatValue(
        value:
            Any?
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

    // =========================================================
    // CLEANUP
    // =========================================================

    private fun closeSessionResources() {

        try {

            captureSession?.close()

        } catch (_: Throwable) {
        }

        captureSession =
            null

        try {

            rawReader?.close()

        } catch (_: Throwable) {
        }

        rawReader =
            null
    }

    private fun startCameraThread() {

        cameraThread =
            HandlerThread(
                "VivoRawVifOutputConfig"
            )

        cameraThread.start()

        cameraHandler =
            Handler(
                cameraThread.looper
            )
    }

    private fun log(
        text:
            String
    ) {

        runOnUiThread {

            output.append(
                text
            )

            output.append(
                "\n"
            )
        }
    }

    // =========================================================
    // PERMISSION RESULT
    // =========================================================

    override fun onRequestPermissionsResult(
        requestCode:
            Int,
        permissions:
            Array<out String>,
        grantResults:
            IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode ==
            CAMERA_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] ==
            PackageManager.PERMISSION_GRANTED
        ) {

            initialize()
        }
    }

    // =========================================================
    // DESTROY
    // =========================================================

    override fun onDestroy() {

        closeSessionResources()

        try {

            cameraDevice?.close()

        } catch (_: Throwable) {
        }

        cameraDevice =
            null

        try {

            cameraThread.quitSafely()

        } catch (_: Throwable) {
        }

        super.onDestroy()
    }
}
