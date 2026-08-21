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

        private const val WAIT_AFTER_CAPTURE_MS = 10000L
    }

    private lateinit var cameraManager: CameraManager

    private lateinit var cameraThread: HandlerThread
    private lateinit var cameraHandler: Handler

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var rawReader: ImageReader? = null

    private lateinit var output: TextView

    private lateinit var createSessionButton: Button
    private lateinit var captureButton: Button

    private var imageCounter = 0
    private var resultCounter = 0

    private var captureStartTime = 0L

    // =========================================================
    // SESSION KEYS
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

    private val aiHighResolutionKey =
        CaptureRequest.Key(
            "vivo.control.ai_highresolution",
            Int::class.javaObjectType
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

    private val proRawKey =
        CaptureRequest.Key(
            "vivo.control.is_ProRaw_on",
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

    private val upscaleKey =
        CaptureRequest.Key(
            "vivo.control.isUpscale",
            Int::class.javaObjectType
        )

    // =========================================================
    // RESULT KEYS
    // =========================================================

    private val requestLeftResultKey =
        CaptureResult.Key(
            "vivo.control.RequestLeftInThisSnapshot",
            IntArray::class.java
        )

    private val rawCaptureTypeResultKey =
        CaptureResult.Key(
            "vivo.control.raw_capture_type",
            IntArray::class.java
        )

    private val highResolutionDngTypeResultKey =
        CaptureResult.Key(
            "vivo.parameter.highResolutionDngType",
            IntArray::class.java
        )

    private val currentModeExResultKey =
        CaptureResult.Key(
            "vivo.control.currentModeEx",
            IntArray::class.java
        )

    private val sceneModeResultKey =
        CaptureResult.Key(
            "vivo.control.sceneMode",
            IntArray::class.java
        )

    private val imageEchoResultKey =
        CaptureResult.Key(
            "vcf.parameter.ImageEcho",
            IntArray::class.java
        )

    private val isCaptureResultKey =
        CaptureResult.Key(
            "vivo.control.isCapture",
            IntArray::class.java
        )

    private val isSnapshotResultKey =
        CaptureResult.Key(
            "vivo.control.is_snapshot",
            IntArray::class.java
        )

    private val sensorModeResultKey =
        CaptureResult.Key(
            "vivo.control.sensorMode",
            IntArray::class.java
        )

    private val niceCaptureSensorModeResultKey =
        CaptureResult.Key(
            "vivo.parameter.niceCaptureSensorMode",
            IntArray::class.java
        )

    private val tuningHintResultKey =
        CaptureResult.Key(
            "com.mediatek.control.capture.hintForIspTuning",
            IntArray::class.java
        )

    private val customTuningHintResultKey =
        CaptureResult.Key(
            "com.mediatek.control.capture.hintForCustomTuning",
            IntArray::class.java
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startCameraThread()
        buildUi()

        cameraManager =
            getSystemService(CAMERA_SERVICE) as CameraManager

        log("VIVO SW RAW16 MULTI-FRAME PROBE")
        log("================================")
        log("")
        log("Camera ID: $CAMERA_ID")
        log("RAW output: $WIDTH x $HEIGHT")
        log("")
        log("Purpose:")
        log("Keep the OEM SW RAW16 session alive")
        log("and record every result + RAW image")
        log("for 10 seconds after capture.")
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

        createSessionButton =
            Button(this)

        createSessionButton.text =
            "CREATE SW RAW16 SESSION"

        createSessionButton.isEnabled =
            false

        createSessionButton.setOnClickListener {
            createSwRaw16Session()
        }

        root.addView(
            createSessionButton
        )

        captureButton =
            Button(this)

        captureButton.text =
            "START SW RAW16 CAPTURE"

        captureButton.isEnabled =
            false

        captureButton.setOnClickListener {
            startSwRaw16Capture()
        }

        root.addView(
            captureButton
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
                    "Vivo SW RAW16 Multi Frame Probe",
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

                    runOnUiThread {
                        createSessionButton.isEnabled =
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

                    log(
                        "CAMERA ERROR: $error"
                    )

                    camera.close()

                    cameraDevice =
                        null
                }
            },

            cameraHandler
        )
    }

    // =========================================================
    // CREATE SW RAW16 SESSION
    // =========================================================

    private fun createSwRaw16Session() {

        val camera =
            cameraDevice ?: return

        closeSessionOnly()

        captureButton.isEnabled =
            false

        imageCounter =
            0

        resultCounter =
            0

        log("")
        log("")
        log("################################")
        log("CREATE SW RAW16 SESSION")
        log("################################")

        try {

            rawReader =
                ImageReader.newInstance(
                    WIDTH,
                    HEIGHT,
                    ImageFormat.RAW_SENSOR,
                    12
                )

            rawReader!!
                .setOnImageAvailableListener(
                    { reader ->
                        handleEveryImage(
                            reader
                        )
                    },
                    cameraHandler
                )

            log(
                "ImageReader created."
            )

            log(
                "maxImages = 12"
            )

        } catch (e: Throwable) {

            log(
                "ImageReader FAILED"
            )

            log(
                e.toString()
            )

            return
        }

        val surface =
            rawReader!!.surface

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
                        "SW RAW16 SESSION CONFIGURED"
                    )

                    log(
                        "HAL accepted the session."
                    )

                    runOnUiThread {
                        captureButton.isEnabled =
                            true
                    }
                }

                override fun onConfigureFailed(
                    session:
                        CameraCaptureSession
                ) {

                    log("")
                    log(
                        "SW RAW16 SESSION FAILED"
                    )
                }
            }

        try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.P
            ) {

                val config =
                    SessionConfiguration(
                        SessionConfiguration.SESSION_REGULAR,
                        listOf(
                            OutputConfiguration(
                                surface
                            )
                        ),
                        Executor { runnable ->
                            cameraHandler.post(
                                runnable
                            )
                        },
                        callback
                    )

                val sessionRequest =
                    camera.createCaptureRequest(
                        CameraDevice.TEMPLATE_STILL_CAPTURE
                    )

                sessionRequest.addTarget(
                    surface
                )

                log("")
                log("SESSION PARAMETERS")
                log("------------------")

                applySwRaw16SessionParameters(
                    sessionRequest
                )

                config.setSessionParameters(
                    sessionRequest.build()
                )

                camera.createCaptureSession(
                    config
                )

            } else {

                @Suppress("DEPRECATION")
                camera.createCaptureSession(
                    listOf(surface),
                    callback,
                    cameraHandler
                )
            }

        } catch (e: Throwable) {

            log("")
            log(
                "SESSION EXCEPTION"
            )

            log(
                e.javaClass.simpleName +
                    ": " +
                    (e.message ?: "")
            )
        }
    }

    // =========================================================
    // SESSION PARAMETERS
    // =========================================================

    private fun applySwRaw16SessionParameters(
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
            aiHighResolutionKey,
            0,
            "ai_highresolution"
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

    // =========================================================
    // CAPTURE
    // =========================================================

    private fun startSwRaw16Capture() {

        val camera =
            cameraDevice ?: return

        val session =
            captureSession ?: return

        val reader =
            rawReader ?: return

        imageCounter =
            0

        resultCounter =
            0

        captureStartTime =
            System.currentTimeMillis()

        runOnUiThread {
            captureButton.isEnabled =
                false
        }

        log("")
        log("")
        log("################################")
        log("START SW RAW16 MULTI-FRAME TEST")
        log("################################")

        log(
            "Observation window: 10 seconds"
        )

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

            applySwRaw16SessionParameters(
                builder
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
                upscaleKey,
                0,
                "isUpscale"
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

            val request =
                builder.build()

            session.capture(
                request,

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
                            "frameNumber = $frameNumber"
                        )

                        log(
                            "timestamp = $timestamp"
                        )
                    }

                    override fun onCaptureProgressed(
                        session:
                            CameraCaptureSession,
                        request:
                            CaptureRequest,
                        partialResult:
                            CaptureResult
                    ) {

                        dumpOneResult(
                            partialResult,
                            "PARTIAL"
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

                        dumpOneResult(
                            result,
                            "TOTAL"
                        )

                        log("")
                        log(
                            "Camera2 says capture request completed."
                        )

                        log(
                            "Keeping session + reader alive for 10 seconds..."
                        )

                        cameraHandler.postDelayed(
                            {

                                log("")
                                log("")
                                log("================================")
                                log("10 SECOND OBSERVATION COMPLETE")
                                log("================================")

                                log(
                                    "Capture results observed: $resultCounter"
                                )

                                log(
                                    "RAW images observed: $imageCounter"
                                )

                                log("")
                                log(
                                    "Session remains open."
                                )

                                log(
                                    "Press COPY OUTPUT."
                                )

                                runOnUiThread {
                                    captureButton.isEnabled =
                                        true
                                }

                            },
                            WAIT_AFTER_CAPTURE_MS
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
                            "CAPTURE FAILED"
                        )

                        log(
                            "Reason: ${failure.reason}"
                        )

                        log(
                            "Frame: ${failure.frameNumber}"
                        )

                        runOnUiThread {
                            captureButton.isEnabled =
                                true
                        }
                    }

                    override fun onCaptureSequenceCompleted(
                        session:
                            CameraCaptureSession,
                        sequenceId:
                            Int,
                        frameNumber:
                            Long
                    ) {

                        log("")
                        log(
                            "CAPTURE SEQUENCE COMPLETED"
                        )

                        log(
                            "sequenceId = $sequenceId"
                        )

                        log(
                            "lastFrame = $frameNumber"
                        )
                    }

                    override fun onCaptureSequenceAborted(
                        session:
                            CameraCaptureSession,
                        sequenceId:
                            Int
                    ) {

                        log("")
                        log(
                            "CAPTURE SEQUENCE ABORTED"
                        )

                        log(
                            "sequenceId = $sequenceId"
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

            runOnUiThread {
                captureButton.isEnabled =
                    true
            }
        }
    }

    // =========================================================
    // EVERY RESULT
    // =========================================================

    private fun dumpOneResult(
        result:
            CaptureResult,
        type:
            String
    ) {

        resultCounter++

        log("")
        log("")
        log("================================")
        log("$type RESULT #$resultCounter")
        log("================================")

        log(
            "Frame number: ${result.frameNumber}"
        )

        val sensorTimestamp =
            try {
                result.get(
                    CaptureResult.SENSOR_TIMESTAMP
                )
            } catch (_: Throwable) {
                null
            }

        log(
            "Sensor timestamp: ${sensorTimestamp ?: "null"}"
        )

        logResultArray(
            result,
            requestLeftResultKey,
            "RequestLeftInThisSnapshot"
        )

        logResultArray(
            result,
            rawCaptureTypeResultKey,
            "raw_capture_type"
        )

        logResultArray(
            result,
            highResolutionDngTypeResultKey,
            "highResolutionDngType"
        )

        logResultArray(
            result,
            currentModeExResultKey,
            "currentModeEx"
        )

        logResultArray(
            result,
            sceneModeResultKey,
            "sceneMode"
        )

        logResultArray(
            result,
            imageEchoResultKey,
            "ImageEcho"
        )

        logResultArray(
            result,
            isCaptureResultKey,
            "isCapture"
        )

        logResultArray(
            result,
            isSnapshotResultKey,
            "is_snapshot"
        )

        logResultArray(
            result,
            sensorModeResultKey,
            "sensorMode"
        )

        logResultArray(
            result,
            niceCaptureSensorModeResultKey,
            "niceCaptureSensorMode"
        )

        logResultArray(
            result,
            tuningHintResultKey,
            "hintForIspTuning"
        )

        logResultArray(
            result,
            customTuningHintResultKey,
            "hintForCustomTuning"
        )
    }

    // =========================================================
    // EVERY IMAGE
    // =========================================================

    private fun handleEveryImage(
        reader:
            ImageReader
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

            imageCounter++

            try {

                log("")
                log("")
                log("********************************")
                log("RAW IMAGE #$imageCounter")
                log("********************************")

                log(
                    "Timestamp: ${image.timestamp}"
                )

                log(
                    "Width: ${image.width}"
                )

                log(
                    "Height: ${image.height}"
                )

                log(
                    "Format: ${image.format}"
                )

                log(
                    "Planes: ${image.planes.size}"
                )

                var totalBytes =
                    0L

                image.planes.forEachIndexed {
                        index,
                        plane ->

                    val count =
                        plane.buffer.remaining()

                    totalBytes +=
                        count.toLong()

                    log("")
                    log(
                        "Plane $index"
                    )

                    log(
                        "Bytes = $count"
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
                    "Total bytes = $totalBytes"
                )

                log(
                    String.format(
                        Locale.US,
                        "Approx %.2f MB",
                        totalBytes /
                            1024.0 /
                            1024.0
                    )
                )

                saveImage(
                    image,
                    imageCounter
                )

            } catch (e: Throwable) {

                log(
                    "IMAGE HANDLER ERROR:"
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
    }

    // =========================================================
    // SAVE EVERY RAW IMAGE
    // =========================================================

    private fun saveImage(
        image:
            Image,
        index:
            Int
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
                    "SW_RAW16_FRAME_" +
                        String.format(
                            Locale.US,
                            "%02d",
                            index
                        ) +
                        "_" +
                        "${image.width}x${image.height}_" +
                        "$stamp.raw"
                )

            FileOutputStream(
                file
            ).use {
                    stream ->

                image.planes.forEach {
                        plane ->

                    val buffer =
                        plane.buffer
                            .duplicate()

                    val bytes =
                        ByteArray(
                            buffer.remaining()
                        )

                    buffer.get(
                        bytes
                    )

                    stream.write(
                        bytes
                    )
                }
            }

            log("")
            log(
                "Saved RAW image #$index:"
            )

            log(
                file.absolutePath
            )

            log(
                String.format(
                    Locale.US,
                    "Saved %.2f MB",
                    file.length() /
                        1024.0 /
                        1024.0
                )
            )

        } catch (e: Throwable) {

            log(
                "RAW SAVE ERROR:"
            )

            log(
                e.toString()
            )
        }
    }

    // =========================================================
    // RESULT HELPER
    // =========================================================

    private fun logResultArray(
        result:
            CaptureResult,
        key:
            CaptureResult.Key<IntArray>,
        name:
            String
    ) {

        try {

            val value =
                result.get(
                    key
                )

            if (value == null) {

                log(
                    "$name = null"
                )

            } else {

                log(
                    "$name = " +
                        value.contentToString()
                )
            }

        } catch (e: Throwable) {

            log(
                "$name = READ ERROR"
            )
        }
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
        }
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    private fun closeSessionOnly() {

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
                "VivoRaw16MultiFrame"
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

        closeSessionOnly()

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
