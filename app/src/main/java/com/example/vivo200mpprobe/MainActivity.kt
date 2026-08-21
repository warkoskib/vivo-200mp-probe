package com.example.vivo200mpprobe

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.params.InputConfiguration
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.Image
import android.media.ImageReader
import android.media.ImageWriter
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    companion object {

        private const val CAMERA_ID = "3"

        private const val WIDTH = 4080
        private const val HEIGHT = 3072

        private const val CAMERA_PERMISSION_REQUEST = 1001

        private const val CASE_A = 0
        private const val CASE_B = 1
        private const val CASE_C = 2
        private const val CASE_D = 3
    }

    private lateinit var cameraManager: CameraManager

    private lateinit var cameraThread: HandlerThread
    private lateinit var cameraHandler: Handler

    private var cameraDevice: CameraDevice? = null
    private var session: CameraCaptureSession? = null

    private var sourceReader: ImageReader? = null
    private var jpegReader: ImageReader? = null
    private var imageWriter: ImageWriter? = null

    private var pendingSourceImage: Image? = null
    private var pendingSourceResult: TotalCaptureResult? = null

    private var activeCase = CASE_A
    private var reprocessStarted = false

    private lateinit var output: TextView

    private lateinit var caseAButton: Button
    private lateinit var caseBButton: Button
    private lateinit var caseCButton: Button
    private lateinit var caseDButton: Button

    // =========================================================
    // VIVO / MTK SESSION KEYS
    // =========================================================

    private val reprocessModeKey =
        CaptureRequest.Key(
            "vivo.control.reprocessMode",
            Int::class.javaObjectType
        )

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
    // CAPTURE / REPROCESS KEYS
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startCameraThread()
        buildUi()

        cameraManager =
            getSystemService(CAMERA_SERVICE) as CameraManager

        log("VIVO REAL REPROCESS SESSION PROBE")
        log("=================================")
        log("")
        log("Camera ID: $CAMERA_ID")
        log("")
        log("INPUT:")
        log("$WIDTH x $HEIGHT YUV_420_888")
        log("")
        log("OUTPUT:")
        log("$WIDTH x $HEIGHT JPEG")
        log("")
        log("A = real reprocess / reprocessMode 0")
        log("B = real reprocess / reprocessMode 1")
        log("C = B + SW RAW16 controls")
        log("D = C + real200mp switch")
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

    // =========================================================
    // UI
    // =========================================================

    private fun buildUi() {

        val root = LinearLayout(this)

        root.orientation =
            LinearLayout.VERTICAL

        root.setPadding(
            20,
            25,
            20,
            25
        )

        caseAButton =
            makeButton(
                "CASE A - REPROCESS CONTROL"
            ) {
                startCase(CASE_A)
            }

        root.addView(caseAButton)

        caseBButton =
            makeButton(
                "CASE B - REPROCESSMODE = 1"
            ) {
                startCase(CASE_B)
            }

        root.addView(caseBButton)

        caseCButton =
            makeButton(
                "CASE C - SW RAW16 REPROCESS"
            ) {
                startCase(CASE_C)
            }

        root.addView(caseCButton)

        caseDButton =
            makeButton(
                "CASE D - SW RAW16 + REAL 200MP"
            ) {
                startCase(CASE_D)
            }

        root.addView(caseDButton)

        setCaseButtons(false)

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
                    "Vivo Real Reprocess Probe",
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

        val clearButton =
            Button(this)

        clearButton.text =
            "CLEAR OUTPUT"

        clearButton.setOnClickListener {
            output.text = ""
        }

        root.addView(clearButton)

        val scroll =
            ScrollView(this)

        output =
            TextView(this)

        output.textSize =
            13f

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

    private fun makeButton(
        text: String,
        action: () -> Unit
    ): Button {

        return Button(this).apply {

            this.text = text

            setOnClickListener {
                action()
            }
        }
    }

    // =========================================================
    // CAMERA
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

                override fun onOpened(
                    camera: CameraDevice
                ) {

                    cameraDevice = camera

                    log("Camera 3 opened.")
                    log("")
                    log("Choose a test case.")

                    setCaseButtons(true)
                }

                override fun onDisconnected(
                    camera: CameraDevice
                ) {

                    log("")
                    log("CAMERA DISCONNECTED")

                    camera.close()

                    cameraDevice = null

                    setCaseButtons(false)
                }

                override fun onError(
                    camera: CameraDevice,
                    error: Int
                ) {

                    log("")
                    log("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                    log("CAMERA ERROR = $error")
                    log("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")

                    if (error == CameraDevice.StateCallback.ERROR_CAMERA_DEVICE) {

                        log(
                            "ERROR 4 = ERROR_CAMERA_DEVICE"
                        )
                    }

                    camera.close()

                    cameraDevice = null

                    setCaseButtons(false)
                }
            },

            cameraHandler
        )
    }

    // =========================================================
    // START CASE
    // =========================================================

    private fun startCase(
        testCase: Int
    ) {

        val camera =
            cameraDevice ?: return

        activeCase = testCase

        setCaseButtons(false)

        cleanupSession()

        pendingSourceImage = null
        pendingSourceResult = null
        reprocessStarted = false

        log("")
        log("")
        log("################################")
        log(caseName(testCase))
        log("################################")

        // -----------------------------------------------------
        // SOURCE YUV READER
        // -----------------------------------------------------

        sourceReader =
            ImageReader.newInstance(
                WIDTH,
                HEIGHT,
                ImageFormat.YUV_420_888,
                3
            )

        sourceReader!!
            .setOnImageAvailableListener(
                { reader ->
                    onSourceImage(reader)
                },
                cameraHandler
            )

        // -----------------------------------------------------
        // JPEG OUTPUT READER
        // -----------------------------------------------------

        jpegReader =
            ImageReader.newInstance(
                WIDTH,
                HEIGHT,
                ImageFormat.JPEG,
                3
            )

        jpegReader!!
            .setOnImageAvailableListener(
                { reader ->
                    onJpegImage(reader)
                },
                cameraHandler
            )

        val sourceSurface =
            sourceReader!!.surface

        val jpegSurface =
            jpegReader!!.surface

        val outputs =
            listOf(
                OutputConfiguration(sourceSurface),
                OutputConfiguration(jpegSurface)
            )

        val executor =
            Executor { runnable ->
                cameraHandler.post(runnable)
            }

        val callback =
            object :
                CameraCaptureSession.StateCallback() {

                override fun onConfigured(
                    configuredSession:
                        CameraCaptureSession
                ) {

                    session =
                        configuredSession

                    log("")
                    log(
                        "REPROCESS SESSION CONFIGURED"
                    )

                    log(
                        "session.isReprocessable = " +
                            configuredSession.isReprocessable
                    )

                    val inputSurface =
                        configuredSession.inputSurface

                    if (inputSurface == null) {

                        log("")
                        log(
                            "ERROR: inputSurface = null"
                        )

                        setCaseButtons(true)

                        return
                    }

                    try {

                        imageWriter =
                            ImageWriter.newInstance(
                                inputSurface,
                                3
                            )

                        log(
                            "ImageWriter created."
                        )

                    } catch (e: Throwable) {

                        log(
                            "ImageWriter creation FAILED"
                        )

                        log(
                            e.javaClass.simpleName +
                                ": " +
                                (e.message ?: "")
                        )

                        setCaseButtons(true)

                        return
                    }

                    captureSourceYuv()
                }

                override fun onConfigureFailed(
                    configuredSession:
                        CameraCaptureSession
                ) {

                    log("")
                    log(
                        "REPROCESS SESSION CONFIGURATION FAILED"
                    )

                    setCaseButtons(true)
                }
            }

        try {

            val config =
                SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR,
                    outputs,
                    executor,
                    callback
                )

            config.setInputConfiguration(
                InputConfiguration(
                    WIDTH,
                    HEIGHT,
                    ImageFormat.YUV_420_888
                )
            )

            // =================================================
            // SESSION PARAMETERS
            // =================================================

            val sessionBuilder =
                camera.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE
                )

            sessionBuilder.addTarget(
                sourceSurface
            )

            log("")
            log("SESSION PARAMETERS")
            log("------------------")

            applySessionParameters(
                sessionBuilder,
                testCase
            )

            config.setSessionParameters(
                sessionBuilder.build()
            )

            log("")
            log(
                "Creating REAL YUV reprocessable session..."
            )

            camera.createCaptureSession(
                config
            )

        } catch (e: Throwable) {

            log("")
            log("SESSION CREATION EXCEPTION")

            log(
                e.javaClass.name
            )

            log(
                e.message ?: ""
            )

            setCaseButtons(true)
        }
    }

    // =========================================================
    // SESSION PARAMETERS
    // =========================================================

    private fun applySessionParameters(
        builder:
            CaptureRequest.Builder,
        testCase:
            Int
    ) {

        setInt(
            builder,
            reprocessModeKey,
            if (testCase == CASE_A) 0 else 1,
            "reprocessMode"
        )

        if (
            testCase == CASE_C ||
            testCase == CASE_D
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
        }
    }

    // =========================================================
    // SOURCE CAPTURE
    // =========================================================

    private fun captureSourceYuv() {

        val camera =
            cameraDevice ?: return

        val currentSession =
            session ?: return

        val reader =
            sourceReader ?: return

        log("")
        log("==============================")
        log("CAPTURING SOURCE YUV")
        log("==============================")

        try {

            val builder =
                camera.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE
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

            currentSession.capture(
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
                            "SOURCE CAPTURE STARTED"
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

                        log("")
                        log(
                            "SOURCE CAPTURE RESULT RECEIVED"
                        )

                        log(
                            "Frame = ${result.frameNumber}"
                        )

                        val timestamp =
                            result.get(
                                CaptureResult.SENSOR_TIMESTAMP
                            )

                        log(
                            "Sensor timestamp = $timestamp"
                        )

                        pendingSourceResult =
                            result

                        attemptReprocess()
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
                            "SOURCE CAPTURE FAILED"
                        )

                        log(
                            "Reason = ${failure.reason}"
                        )

                        setCaseButtons(true)
                    }
                },

                cameraHandler
            )

        } catch (e: Throwable) {

            log("")
            log(
                "SOURCE CAPTURE EXCEPTION"
            )

            log(
                e.javaClass.name
            )

            log(
                e.message ?: ""
            )

            setCaseButtons(true)
        }
    }

    // =========================================================
    // SOURCE IMAGE CALLBACK
    // =========================================================

    private fun onSourceImage(
        reader:
            ImageReader
    ) {

        val image =
            try {
                reader.acquireNextImage()
            } catch (e: Throwable) {
                null
            }

        if (image == null) {
            return
        }

        // If something unexpected sends another source frame,
        // close it instead of leaking it.
        if (pendingSourceImage != null) {

            try {
                image.close()
            } catch (_: Throwable) {
            }

            return
        }

        pendingSourceImage =
            image

        log("")
        log(
            "SOURCE YUV IMAGE RECEIVED"
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
            "Timestamp = ${image.timestamp}"
        )

        log(
            "Planes = ${image.planes.size}"
        )

        image.planes.forEachIndexed {
                index,
                plane ->

            log(
                "Plane $index: " +
                    "bytes=${plane.buffer.remaining()} " +
                    "rowStride=${plane.rowStride} " +
                    "pixelStride=${plane.pixelStride}"
            )
        }

        attemptReprocess()
    }

    // =========================================================
    // WHEN IMAGE + RESULT ARE BOTH READY
    // =========================================================

    private fun attemptReprocess() {

        if (reprocessStarted) {
            return
        }

        val image =
            pendingSourceImage ?: return

        val result =
            pendingSourceResult ?: return

        val writer =
            imageWriter ?: return

        reprocessStarted =
            true

        log("")
        log("==============================")
        log("SOURCE PAIR READY")
        log("==============================")

        log(
            "Image timestamp = ${image.timestamp}"
        )

        log(
            "Result timestamp = " +
                result.get(
                    CaptureResult.SENSOR_TIMESTAMP
                )
        )

        try {

            log("")
            log(
                "Queueing YUV image into ImageWriter..."
            )

            writer.queueInputImage(
                image
            )

            /*
             * Ownership transfers to ImageWriter here.
             * DO NOT close image ourselves afterward.
             */

            pendingSourceImage =
                null

            log(
                "IMAGE QUEUED SUCCESSFULLY"
            )

        } catch (e: Throwable) {

            log("")
            log(
                "queueInputImage FAILED"
            )

            log(
                e.javaClass.name
            )

            log(
                e.message ?: ""
            )

            try {
                image.close()
            } catch (_: Throwable) {
            }

            pendingSourceImage =
                null

            setCaseButtons(true)

            return
        }

        performReprocess(
            result
        )
    }

    // =========================================================
    // REAL REPROCESS REQUEST
    // =========================================================

    private fun performReprocess(
        sourceResult:
            TotalCaptureResult
    ) {

        val camera =
            cameraDevice ?: return

        val currentSession =
            session ?: return

        val outputReader =
            jpegReader ?: return

        log("")
        log("==============================")
        log("CREATE REPROCESS REQUEST")
        log("==============================")

        try {

            val builder =
                camera.createReprocessCaptureRequest(
                    sourceResult
                )

            log(
                "createReprocessCaptureRequest(): SUCCESS"
            )

            builder.addTarget(
                outputReader.surface
            )

            log("")
            log("REPROCESS PARAMETERS")
            log("--------------------")

            setInt(
                builder,
                reprocessModeKey,
                if (activeCase == CASE_A) 0 else 1,
                "reprocessMode"
            )

            if (
                activeCase == CASE_C ||
                activeCase == CASE_D
            ) {

                applySwRaw16Controls(
                    builder,
                    activeCase == CASE_D
                )
            }

            log("")
            log(
                "Submitting reprocess request..."
            )

            currentSession.capture(
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
                            "REPROCESS STARTED"
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

                        log("")
                        log(
                            "REPROCESS RESULT COMPLETED"
                        )

                        log(
                            "Frame = ${result.frameNumber}"
                        )

                        dumpVendorResults(
                            result
                        )

                        /*
                         * JPEG callback may occur before or
                         * after this result.
                         */

                        cameraHandler.postDelayed(
                            {
                                setCaseButtons(true)
                            },
                            1500
                        )
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
                            "REPROCESS CAPTURE FAILED"
                        )

                        log(
                            "Reason = ${failure.reason}"
                        )

                        log(
                            "Frame = ${failure.frameNumber}"
                        )

                        setCaseButtons(true)
                    }
                },

                cameraHandler
            )

        } catch (e: Throwable) {

            log("")
            log(
                "REPROCESS EXCEPTION"
            )

            log(
                e.javaClass.name
            )

            log(
                e.message ?: ""
            )

            setCaseButtons(true)
        }
    }

    // =========================================================
    // SW RAW16 / 200 MP CONTROLS
    // =========================================================

    private fun applySwRaw16Controls(
        builder:
            CaptureRequest.Builder,
        enableReal200mp:
            Boolean
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

        setInt(
            builder,
            real200mpKey,
            if (enableReal200mp) 1 else 0,
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
    // JPEG OUTPUT
    // =========================================================

    private fun onJpegImage(
        reader:
            ImageReader
    ) {

        val image =
            try {
                reader.acquireNextImage()
            } catch (_: Throwable) {
                null
            }

        if (image == null) {
            return
        }

        try {

            val buffer =
                image.planes[0].buffer

            val bytes =
                ByteArray(
                    buffer.remaining()
                )

            buffer.get(
                bytes
            )

            log("")
            log("")
            log("********************************")
            log("JPEG RECEIVED")
            log("********************************")

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
                "Bytes = ${bytes.size}"
            )

            saveJpeg(
                bytes
            )

        } catch (e: Throwable) {

            log("")
            log(
                "JPEG READ ERROR"
            )

            log(
                e.toString()
            )

        } finally {

            try {
                image.close()
            } catch (_: Throwable) {
            }
        }
    }

    private fun saveJpeg(
        bytes:
            ByteArray
    ) {

        try {

            val directory =
                getExternalFilesDir(
                    Environment.DIRECTORY_PICTURES
                ) ?: return

            directory.mkdirs()

            val stamp =
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss_SSS",
                    Locale.US
                ).format(
                    Date()
                )

            val file =
                File(
                    directory,
                    "REPROCESS_" +
                        caseShortName(activeCase) +
                        "_" +
                        stamp +
                        ".jpg"
                )

            FileOutputStream(
                file
            ).use {
                it.write(bytes)
            }

            log("")
            log(
                "JPEG SAVED:"
            )

            log(
                file.absolutePath
            )

            log(
                String.format(
                    Locale.US,
                    "%.2f MB",
                    file.length() /
                        1024.0 /
                        1024.0
                )
            )

        } catch (e: Throwable) {

            log(
                "SAVE JPEG ERROR: $e"
            )
        }
    }

    // =========================================================
    // RESULT DUMP
    // =========================================================

    private fun dumpVendorResults(
        result:
            TotalCaptureResult
    ) {

        log("")
        log("==============================")
        log("VIVO / VCF / MTK RESULTS")
        log("==============================")

        var count =
            0

        for (key in result.keys) {

            val name =
                key.name

            val lower =
                name.lowercase(
                    Locale.US
                )

            if (
                lower.startsWith("vivo.") ||
                lower.startsWith("com.vivo.") ||
                lower.startsWith("vcf.") ||
                lower.startsWith("com.mediatek.")
            ) {

                val value =
                    try {
                        result.get(key)
                    } catch (_: Throwable) {
                        "<READ ERROR>"
                    }

                if (value != null) {

                    count++

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

        log("")
        log(
            "Non-null OEM result keys: $count"
        )
    }

    private fun formatValue(
        value:
            Any
    ): String {

        return when (value) {

            is IntArray ->
                limitArray(
                    value.contentToString()
                )

            is LongArray ->
                limitArray(
                    value.contentToString()
                )

            is FloatArray ->
                limitArray(
                    value.contentToString()
                )

            is DoubleArray ->
                limitArray(
                    value.contentToString()
                )

            is ByteArray ->
                "ByteArray(${value.size})"

            is Array<*> ->
                limitArray(
                    value.contentDeepToString()
                )

            else ->
                value.toString()
        }
    }

    private fun limitArray(
        text:
            String
    ): String {

        return if (
            text.length > 1000
        ) {

            text.take(1000) +
                "... [TRUNCATED]"

        } else {

            text
        }
    }

    // =========================================================
    // KEY SETTERS
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
                "FAIL $name: " +
                    e.javaClass.simpleName
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
                "FAIL $name: " +
                    e.javaClass.simpleName
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
                "FAIL $name: " +
                    e.javaClass.simpleName
            )
        }
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    private fun cleanupSession() {

        try {

            pendingSourceImage?.close()

        } catch (_: Throwable) {
        }

        pendingSourceImage =
            null

        pendingSourceResult =
            null

        try {
            imageWriter?.close()
        } catch (_: Throwable) {
        }

        imageWriter =
            null

        try {
            session?.close()
        } catch (_: Throwable) {
        }

        session =
            null

        try {
            sourceReader?.close()
        } catch (_: Throwable) {
        }

        sourceReader =
            null

        try {
            jpegReader?.close()
        } catch (_: Throwable) {
        }

        jpegReader =
            null
    }

    private fun startCameraThread() {

        cameraThread =
            HandlerThread(
                "VivoRealReprocess"
            )

        cameraThread.start()

        cameraHandler =
            Handler(
                cameraThread.looper
            )
    }

    private fun setCaseButtons(
        enabled:
            Boolean
    ) {

        runOnUiThread {

            caseAButton.isEnabled =
                enabled

            caseBButton.isEnabled =
                enabled

            caseCButton.isEnabled =
                enabled

            caseDButton.isEnabled =
                enabled
        }
    }

    private fun caseName(
        value:
            Int
    ): String {

        return when (value) {

            CASE_A ->
                "CASE A - REAL REPROCESS / MODE 0"

            CASE_B ->
                "CASE B - REAL REPROCESS / MODE 1"

            CASE_C ->
                "CASE C - SW RAW16 REPROCESS"

            CASE_D ->
                "CASE D - SW RAW16 + REAL 200MP"

            else ->
                "UNKNOWN CASE"
        }
    }

    private fun caseShortName(
        value:
            Int
    ): String {

        return when (value) {

            CASE_A -> "A"
            CASE_B -> "B"
            CASE_C -> "C"
            CASE_D -> "D"

            else -> "X"
        }
    }

    private fun log(
        text:
            String
    ) {

        runOnUiThread {

            output.append(text)
            output.append("\n")
        }
    }

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

            openCamera()
        }
    }

    override fun onDestroy() {

        cleanupSession()

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
