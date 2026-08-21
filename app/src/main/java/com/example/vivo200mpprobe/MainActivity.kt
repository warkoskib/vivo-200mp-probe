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
        private const val OBSERVE_MS = 5000L
    }

    private lateinit var cameraManager: CameraManager
    private lateinit var cameraThread: HandlerThread
    private lateinit var cameraHandler: Handler

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null

    private var rawReader: ImageReader? = null
    private var secondaryReader: ImageReader? = null

    private lateinit var output: TextView
    private lateinit var runButton: Button

    private var rawCount = 0
    private var secondaryCount = 0

    // =========================================================
    // CONFIRMED VIVO SESSION KEYS
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

    private val proRawKey =
        CaptureRequest.Key(
            "vivo.control.is_ProRaw_on",
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
    // BACKGROUND / VCF KEYS
    // =========================================================

    private val bgImageReaderIdKey =
        CaptureRequest.Key(
            "com.mediatek.bgservicefeature.imagereaderid",
            Int::class.javaObjectType
        )

    private val bgPrereleaseKey =
        CaptureRequest.Key(
            "com.mediatek.bgservicefeature.prerelease",
            Int::class.javaObjectType
        )

    private val proprietaryRequestKey =
        CaptureRequest.Key(
            "com.mediatek.configure.setting.proprietaryRequest",
            Int::class.javaObjectType
        )

    private val echoModeKey =
        CaptureRequest.Key(
            "vivo.control.echo.mode",
            Int::class.javaObjectType
        )

    private val reprocessModeKey =
        CaptureRequest.Key(
            "vivo.control.reprocessMode",
            Int::class.javaObjectType
        )

    // =========================================================
    // CAPTURE KEYS
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

    private val niceSensorModeKey =
        CaptureRequest.Key(
            "vivo.parameter.niceCaptureSensorMode",
            Int::class.javaObjectType
        )

    private val rawCaptureTypeKey =
        CaptureRequest.Key(
            "vivo.control.raw_capture_type",
            Int::class.javaObjectType
        )

    private val highResDngKey =
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

    private val snapshotKey =
        CaptureRequest.Key(
            "vivo.control.is_snapshot",
            Int::class.javaObjectType
        )

    // =========================================================
    // TEST CASE
    // =========================================================

    data class TestCase(
        val name: String,
        val format: Int,
        val readerId: Int
    )

    private val tests = listOf(
        TestCase(
            "RAW + JPEG / ID 0",
            ImageFormat.JPEG,
            0
        ),
        TestCase(
            "RAW + JPEG / ID 1",
            ImageFormat.JPEG,
            1
        ),
        TestCase(
            "RAW + YUV / ID 0",
            ImageFormat.YUV_420_888,
            0
        ),
        TestCase(
            "RAW + YUV / ID 1",
            ImageFormat.YUV_420_888,
            1
        )
    )

    // =========================================================
    // STARTUP
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startThread()
        buildUi()

        cameraManager =
            getSystemService(CAMERA_SERVICE) as CameraManager

        log("VIVO VCF BACKGROUND OUTPUT PROBE")
        log("================================")
        log("")
        log("Camera ID: $CAMERA_ID")
        log("")
        log("Primary:")
        log("4080 x 3072 RAW_SENSOR")
        log("")
        log("Secondary outputs tested:")
        log("4080 x 3072 JPEG")
        log("4080 x 3072 YUV")
        log("")
        log("Background ImageReader IDs:")
        log("0 and 1")
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
            openCamera()
        }
    }

    private fun buildUi() {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setPadding(20, 30, 20, 30)

        runButton = Button(this)
        runButton.text = "RUN BACKGROUND OUTPUT MATRIX"
        runButton.isEnabled = false

        runButton.setOnClickListener {
            runButton.isEnabled = false
            runTest(0)
        }

        root.addView(runButton)

        val copyButton = Button(this)
        copyButton.text = "COPY OUTPUT"

        copyButton.setOnClickListener {

            val clipboard =
                getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as ClipboardManager

            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    "VCF Background Probe",
                    output.text.toString()
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

        val scroll = ScrollView(this)

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

        log("OPENING CAMERA 3...")

        cameraManager.openCamera(
            CAMERA_ID,

            object : CameraDevice.StateCallback() {

                override fun onOpened(camera: CameraDevice) {

                    cameraDevice = camera

                    log("Camera 3 opened.")
                    log("")
                    log("Press RUN BACKGROUND OUTPUT MATRIX.")

                    runOnUiThread {
                        runButton.isEnabled = true
                    }
                }

                override fun onDisconnected(camera: CameraDevice) {

                    log("Camera disconnected.")

                    camera.close()
                    cameraDevice = null
                }

                override fun onError(
                    camera: CameraDevice,
                    error: Int
                ) {

                    log("CAMERA ERROR = $error")

                    camera.close()
                    cameraDevice = null
                }
            },

            cameraHandler
        )
    }

    // =========================================================
    // TEST MATRIX
    // =========================================================

    private fun runTest(index: Int) {

        if (index >= tests.size) {

            log("")
            log("")
            log("================================")
            log("TEST MATRIX COMPLETE")
            log("================================")
            log("")
            log("Press COPY OUTPUT.")

            runOnUiThread {
                runButton.isEnabled = true
            }

            return
        }

        val test = tests[index]

        closeReadersAndSession()

        rawCount = 0
        secondaryCount = 0

        log("")
        log("")
        log("################################")
        log("CASE ${index + 1}/${tests.size}")
        log(test.name)
        log("################################")

        try {

            rawReader =
                ImageReader.newInstance(
                    WIDTH,
                    HEIGHT,
                    ImageFormat.RAW_SENSOR,
                    4
                )

            secondaryReader =
                ImageReader.newInstance(
                    WIDTH,
                    HEIGHT,
                    test.format,
                    4
                )

            rawReader!!.setOnImageAvailableListener(
                { reader ->
                    consumeImages(
                        reader,
                        "RAW",
                        true
                    )
                },
                cameraHandler
            )

            secondaryReader!!.setOnImageAvailableListener(
                { reader ->
                    consumeImages(
                        reader,
                        if (test.format == ImageFormat.JPEG)
                            "SECONDARY JPEG"
                        else
                            "SECONDARY YUV",
                        false
                    )
                },
                cameraHandler
            )

        } catch (e: Throwable) {

            log("ImageReader creation failed:")
            log(e.toString())

            cameraHandler.postDelayed(
                { runTest(index + 1) },
                500
            )

            return
        }

        createSessionForTest(
            index,
            test
        )
    }

    private fun createSessionForTest(
        index: Int,
        test: TestCase
    ) {

        val camera =
            cameraDevice ?: return

        val rawSurface =
            rawReader!!.surface

        val secondarySurface =
            secondaryReader!!.surface

        val callback =
            object :
                CameraCaptureSession.StateCallback() {

                override fun onConfigured(
                    session: CameraCaptureSession
                ) {

                    captureSession = session

                    log("")
                    log("SESSION RESULT: CONFIGURED")

                    startCapture(
                        index,
                        test
                    )
                }

                override fun onConfigureFailed(
                    session: CameraCaptureSession
                ) {

                    log("")
                    log("SESSION RESULT: FAILED")

                    cameraHandler.postDelayed(
                        {
                            runTest(
                                index + 1
                            )
                        },
                        750
                    )
                }
            }

        try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.P
            ) {

                val outputs =
                    listOf(
                        OutputConfiguration(
                            rawSurface
                        ),
                        OutputConfiguration(
                            secondarySurface
                        )
                    )

                val config =
                    SessionConfiguration(
                        SessionConfiguration.SESSION_REGULAR,
                        outputs,
                        Executor { command ->
                            cameraHandler.post(command)
                        },
                        callback
                    )

                val sessionBuilder =
                    camera.createCaptureRequest(
                        CameraDevice.TEMPLATE_STILL_CAPTURE
                    )

                sessionBuilder.addTarget(
                    rawSurface
                )

                sessionBuilder.addTarget(
                    secondarySurface
                )

                log("")
                log("SESSION PARAMETERS")
                log("------------------")

                applySessionParameters(
                    sessionBuilder,
                    test.readerId
                )

                config.setSessionParameters(
                    sessionBuilder.build()
                )

                camera.createCaptureSession(
                    config
                )

            } else {

                @Suppress("DEPRECATION")
                camera.createCaptureSession(
                    listOf(
                        rawSurface,
                        secondarySurface
                    ),
                    callback,
                    cameraHandler
                )
            }

        } catch (e: Throwable) {

            log("")
            log("SESSION EXCEPTION")
            log(e.toString())

            cameraHandler.postDelayed(
                {
                    runTest(
                        index + 1
                    )
                },
                750
            )
        }
    }

    // =========================================================
    // SESSION PARAMETERS
    // =========================================================

    private fun applySessionParameters(
        builder: CaptureRequest.Builder,
        readerId: Int
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

        setInt(
            builder,
            proRawKey,
            1,
            "is_ProRaw_on"
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

        /*
         * Main variable being tested.
         */

        setInt(
            builder,
            bgImageReaderIdKey,
            readerId,
            "bgservice.imagereaderid"
        )

        /*
         * These are exploratory.
         * If their native type is not INT32,
         * builder.set() will simply log FAIL.
         */

        setInt(
            builder,
            bgPrereleaseKey,
            1,
            "bgservice.prerelease"
        )

        setInt(
            builder,
            proprietaryRequestKey,
            1,
            "proprietaryRequest"
        )

        setInt(
            builder,
            echoModeKey,
            1,
            "echo.mode"
        )

        setInt(
            builder,
            reprocessModeKey,
            1,
            "reprocessMode"
        )
    }

    // =========================================================
    // CAPTURE
    // =========================================================

    private fun startCapture(
        index: Int,
        test: TestCase
    ) {

        val camera =
            cameraDevice ?: return

        val session =
            captureSession ?: return

        val rawSurface =
            rawReader!!.surface

        val secondSurface =
            secondaryReader!!.surface

        try {

            val builder =
                camera.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE
                )

            builder.addTarget(
                rawSurface
            )

            builder.addTarget(
                secondSurface
            )

            builder.set(
                CaptureRequest.CONTROL_MODE,
                CameraMetadata.CONTROL_MODE_AUTO
            )

            builder.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON
            )

            applySessionParameters(
                builder,
                test.readerId
            )

            log("")
            log("CAPTURE PARAMETERS")
            log("------------------")

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
                niceSensorModeKey,
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
                highResDngKey,
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
                snapshotKey,
                1,
                "is_snapshot"
            )

            session.capture(
                builder.build(),

                object :
                    CameraCaptureSession.CaptureCallback() {

                    override fun onCaptureStarted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        timestamp: Long,
                        frameNumber: Long
                    ) {

                        log("")
                        log("CAPTURE STARTED")

                        log(
                            "Frame = $frameNumber"
                        )
                    }

                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {

                        log("")
                        log("CAPTURE RESULT")
                        log("--------------")

                        dumpInterestingResult(
                            result
                        )

                        log("")
                        log(
                            "Waiting ${OBSERVE_MS / 1000} seconds for secondary output..."
                        )

                        cameraHandler.postDelayed(
                            {

                                log("")
                                log("OBSERVATION RESULT")
                                log("------------------")

                                log(
                                    "RAW images = $rawCount"
                                )

                                log(
                                    "Secondary images = $secondaryCount"
                                )

                                if (secondaryCount > 0) {

                                    log("")
                                    log(
                                        "*** SECONDARY OUTPUT RECEIVED ***"
                                    )
                                }

                                runTest(
                                    index + 1
                                )

                            },
                            OBSERVE_MS
                        )
                    }

                    override fun onCaptureFailed(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        failure: CaptureFailure
                    ) {

                        log("")
                        log("CAPTURE FAILED")

                        log(
                            "Reason = ${failure.reason}"
                        )

                        cameraHandler.postDelayed(
                            {
                                runTest(
                                    index + 1
                                )
                            },
                            750
                        )
                    }
                },

                cameraHandler
            )

        } catch (e: Throwable) {

            log("")
            log("CAPTURE EXCEPTION")
            log(e.toString())

            cameraHandler.postDelayed(
                {
                    runTest(
                        index + 1
                    )
                },
                750
            )
        }
    }

    // =========================================================
    // IMAGE HANDLING
    // =========================================================

    private fun consumeImages(
        reader: ImageReader,
        label: String,
        isRaw: Boolean
    ) {

        while (true) {

            val image =
                try {
                    reader.acquireNextImage()
                } catch (_: Throwable) {
                    null
                }

            if (image == null) {
                break
            }

            try {

                if (isRaw) {
                    rawCount++
                } else {
                    secondaryCount++
                }

                log("")
                log("==============================")
                log("$label IMAGE RECEIVED")
                log("==============================")

                log(
                    "Timestamp = ${image.timestamp}"
                )

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
                    "Planes = ${image.planes.size}"
                )

                var total = 0L

                image.planes.forEachIndexed {
                        planeIndex,
                        plane ->

                    val bytes =
                        plane.buffer.remaining()

                    total += bytes

                    log("")
                    log(
                        "Plane $planeIndex"
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
                    "TOTAL BYTES = $total"
                )

                log(
                    String.format(
                        Locale.US,
                        "%.2f MB",
                        total /
                            1024.0 /
                            1024.0
                    )
                )

            } finally {

                try {
                    image.close()
                } catch (_: Throwable) {
                }
            }
        }
    }

    // =========================================================
    // RESULT DUMP
    // =========================================================

    private fun dumpInterestingResult(
        result: TotalCaptureResult
    ) {

        val terms =
            listOf(
                "imagereader",
                "bgservice",
                "echo",
                "reprocess",
                "vcf",
                "raw_capture",
                "highresolution",
                "200mp",
                "currentmode",
                "scene",
                "requestleft",
                "tuning",
                "sensor"
            )

        for (key in result.keys) {

            val name =
                key.name.lowercase(
                    Locale.US
                )

            if (
                terms.any {
                    name.contains(it)
                }
            ) {

                val value =
                    try {
                        result.get(key)
                    } catch (_: Throwable) {
                        "<READ ERROR>"
                    }

                log("")
                log(key.name)
                log(formatValue(value))
            }
        }
    }

    // =========================================================
    // SETTERS
    // =========================================================

    private fun setInt(
        builder: CaptureRequest.Builder,
        key: CaptureRequest.Key<Int>,
        value: Int,
        name: String
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
        builder: CaptureRequest.Builder,
        key: CaptureRequest.Key<Byte>,
        value: Int,
        name: String
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
        }
    }

    private fun setIntArray(
        builder: CaptureRequest.Builder,
        key: CaptureRequest.Key<IntArray>,
        value: IntArray,
        name: String
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
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

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

            is ByteArray ->
                value.contentToString()

            is FloatArray ->
                value.contentToString()

            is DoubleArray ->
                value.contentToString()

            is BooleanArray ->
                value.contentToString()

            else ->
                value.toString()
        }
    }

    private fun closeReadersAndSession() {

        try {
            captureSession?.close()
        } catch (_: Throwable) {
        }

        captureSession = null

        try {
            rawReader?.close()
        } catch (_: Throwable) {
        }

        rawReader = null

        try {
            secondaryReader?.close()
        } catch (_: Throwable) {
        }

        secondaryReader = null
    }

    private fun startThread() {

        cameraThread =
            HandlerThread(
                "VivoVcfBackgroundProbe"
            )

        cameraThread.start()

        cameraHandler =
            Handler(
                cameraThread.looper
            )
    }

    private fun log(text: String) {

        runOnUiThread {

            output.append(text)
            output.append("\n")
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

        if (
            requestCode ==
            CAMERA_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] ==
            PackageManager.PERMISSION_GRANTED
        ) {

            openCamera()
        }
    }

    override fun onDestroy() {

        closeReadersAndSession()

        try {
            cameraDevice?.close()
        } catch (_: Throwable) {
        }

        try {
            cameraThread.quitSafely()
        } catch (_: Throwable) {
        }

        super.onDestroy()
    }
}
