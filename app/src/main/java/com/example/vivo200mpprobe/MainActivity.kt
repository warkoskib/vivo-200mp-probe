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
        private const val REQUEST_CAMERA = 1001

        private const val STANDARD_WIDTH = 4080
        private const val STANDARD_HEIGHT = 3072

        private const val FULL_WIDTH = 16320
        private const val FULL_HEIGHT = 12288
    }

    private lateinit var output: TextView
    private lateinit var scroll: ScrollView

    private lateinit var standardSessionButton: Button
    private lateinit var fullSessionButton: Button
    private lateinit var captureButton: Button

    private lateinit var cameraManager: CameraManager
    private lateinit var cameraThread: HandlerThread
    private lateinit var cameraHandler: Handler

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var rawReader: ImageReader? = null

    private var currentWidth = 0
    private var currentHeight = 0
    private var currentIsFull = false

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildUi()
        startCameraThread()

        cameraManager =
            getSystemService(CAMERA_SERVICE) as CameraManager

        log("VIVO SINGLE RAW OUTPUT TEST")
        log("==============================")
        log("")
        log("Camera ID: $CAMERA_ID")
        log("")
        log("TEST A:")
        log("$STANDARD_WIDTH x $STANDARD_HEIGHT RAW_SENSOR")
        log("")
        log("TEST B:")
        log("$FULL_WIDTH x $FULL_HEIGHT RAW_SENSOR")
        log("")
        log("Only ONE RAW output is used per session.")
        log("")

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA
            )
        }
    }

    private fun buildUi() {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setPadding(20, 30, 20, 30)

        standardSessionButton = Button(this)
        standardSessionButton.text = "CREATE STANDARD RAW SESSION"
        standardSessionButton.isEnabled = false

        standardSessionButton.setOnClickListener {
            createRawSession(
                STANDARD_WIDTH,
                STANDARD_HEIGHT,
                false
            )
        }

        root.addView(standardSessionButton)

        fullSessionButton = Button(this)
        fullSessionButton.text = "CREATE 200 MP RAW SESSION"
        fullSessionButton.isEnabled = false

        fullSessionButton.setOnClickListener {
            createRawSession(
                FULL_WIDTH,
                FULL_HEIGHT,
                true
            )
        }

        root.addView(fullSessionButton)

        captureButton = Button(this)
        captureButton.text = "CAPTURE RAW"
        captureButton.isEnabled = false

        captureButton.setOnClickListener {
            captureRaw()
        }

        root.addView(captureButton)

        val copyButton = Button(this)
        copyButton.text = "COPY OUTPUT"

        copyButton.setOnClickListener {

            val clipboard =
                getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as ClipboardManager

            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    "Single RAW Test",
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

        cameraManager.openCamera(
            CAMERA_ID,

            object : CameraDevice.StateCallback() {

                override fun onOpened(
                    camera: CameraDevice
                ) {

                    cameraDevice = camera

                    log("Camera 3 opened.")
                    log("")
                    log("Choose which RAW session to create.")

                    runOnUiThread {
                        standardSessionButton.isEnabled = true
                        fullSessionButton.isEnabled = true
                    }
                }

                override fun onDisconnected(
                    camera: CameraDevice
                ) {

                    log("Camera disconnected.")

                    camera.close()
                    cameraDevice = null
                }

                override fun onError(
                    camera: CameraDevice,
                    error: Int
                ) {

                    log("CAMERA ERROR: $error")

                    camera.close()
                    cameraDevice = null
                }
            },

            cameraHandler
        )
    }

    // =========================================================
    // CREATE ONE-OUTPUT SESSION
    // =========================================================

    private fun createRawSession(
        width: Int,
        height: Int,
        full: Boolean
    ) {

        val camera =
            cameraDevice ?: return

        runOnUiThread {
            standardSessionButton.isEnabled = false
            fullSessionButton.isEnabled = false
            captureButton.isEnabled = false
        }

        try {
            captureSession?.close()
        } catch (_: Throwable) {
        }

        try {
            rawReader?.close()
        } catch (_: Throwable) {
        }

        captureSession = null
        rawReader = null

        currentWidth = width
        currentHeight = height
        currentIsFull = full

        log("")
        log("")
        log("################################")
        log(
            if (full)
                "CREATE 200 MP RAW SESSION"
            else
                "CREATE STANDARD RAW SESSION"
        )
        log("################################")

        log("")
        log("Requested RAW_SENSOR:")
        log("$width x $height")

        try {

            rawReader =
                ImageReader.newInstance(
                    width,
                    height,
                    ImageFormat.RAW_SENSOR,
                    1
                )

            rawReader?.setOnImageAvailableListener(
                { reader ->
                    handleRawImage(reader)
                },
                cameraHandler
            )

            log("ImageReader creation: SUCCESS")

        } catch (e: Throwable) {

            log("ImageReader creation: FAILED")
            log(e.javaClass.name)
            log(e.message ?: "")

            enableSessionButtons()
            return
        }

        val reader =
            rawReader ?: return

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.P
        ) {

            try {

                val outputConfiguration =
                    OutputConfiguration(
                        reader.surface
                    )

                val executor =
                    Executor { runnable ->
                        cameraHandler.post(runnable)
                    }

                val configuration =
                    SessionConfiguration(
                        SessionConfiguration.SESSION_REGULAR,
                        listOf(outputConfiguration),
                        executor,

                        object :
                            CameraCaptureSession.StateCallback() {

                            override fun onConfigured(
                                session: CameraCaptureSession
                            ) {

                                captureSession = session

                                log("")
                                log("==============================")
                                log("SESSION CONFIGURED")
                                log("==============================")

                                log(
                                    "$currentWidth x $currentHeight RAW_SENSOR"
                                )

                                log("")
                                log("Press CAPTURE RAW.")

                                runOnUiThread {
                                    captureButton.isEnabled = true
                                    standardSessionButton.isEnabled = true
                                    fullSessionButton.isEnabled = true
                                }
                            }

                            override fun onConfigureFailed(
                                session: CameraCaptureSession
                            ) {

                                log("")
                                log("==============================")
                                log("SESSION CONFIGURATION FAILED")
                                log("==============================")

                                log(
                                    "$currentWidth x $currentHeight RAW_SENSOR"
                                )

                                if (currentIsFull) {

                                    log("")
                                    log(
                                        "HAL explicitly rejected the"
                                    )

                                    log(
                                        "single 200 MP RAW output."
                                    )
                                }

                                enableSessionButtons()
                            }
                        }
                    )

                val sessionBuilder =
                    camera.createCaptureRequest(
                        CameraDevice.TEMPLATE_STILL_CAPTURE
                    )

                sessionBuilder.addTarget(
                    reader.surface
                )

                log("")
                log("==============================")
                log("SESSION PARAMETERS")
                log("==============================")

                applySessionParameters(
                    sessionBuilder,
                    full
                )

                configuration.setSessionParameters(
                    sessionBuilder.build()
                )

                camera.createCaptureSession(
                    configuration
                )

            } catch (e: Throwable) {

                log("")
                log("SESSION CREATE EXCEPTION")
                log(e.javaClass.name)
                log(e.message ?: "")

                enableSessionButtons()
            }

        } else {

            log(
                "This test requires Android 9+."
            )

            enableSessionButtons()
        }
    }

    private fun applySessionParameters(
        builder: CaptureRequest.Builder,
        full: Boolean
    ) {

        setInt(
            builder,
            ultraHighResolutionKey,
            if (full) 1 else 0,
            "ultra_highresolution"
        )

        setByte(
            builder,
            portraitHighResolutionKey,
            if (full) 1 else 0,
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
            forceSensorModeKey,
            if (full) 0 else 1,
            "forceSensorMode"
        )

        setInt(
            builder,
            engineerRemosaicModeKey,
            if (full) 1 else 0,
            "EngineerRemosaicMode"
        )

        setInt(
            builder,
            advanceFullsizeKey,
            if (full) 1 else 0,
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
            if (full) 1 else 0,
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

        setInt(
            builder,
            proRawKey,
            if (full) 1 else 0,
            "is_ProRaw_on"
        )
    }

    // =========================================================
    // CAPTURE
    // =========================================================

    private fun captureRaw() {

        val camera =
            cameraDevice ?: return

        val session =
            captureSession ?: return

        val reader =
            rawReader ?: return

        runOnUiThread {
            captureButton.isEnabled = false
        }

        log("")
        log("")
        log("==============================")
        log("RAW CAPTURE")
        log("==============================")

        log(
            "Target: $currentWidth x $currentHeight"
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

            applySessionParameters(
                builder,
                currentIsFull
            )

            log("")
            log("CAPTURE PARAMETERS")

            setInt(
                builder,
                real200mpKey,
                if (currentIsFull) 1 else 0,
                "real200mp_switch_on"
            )

            setInt(
                builder,
                sensorModeKey,
                if (currentIsFull) 0 else 1,
                "sensorMode"
            )

            setInt(
                builder,
                niceCaptureSensorModeKey,
                if (currentIsFull) 0 else 1,
                "niceCaptureSensorMode"
            )

            setInt(
                builder,
                rawCaptureTypeKey,
                1,
                "raw_capture_type"
            )

            setInt(
                builder,
                highResolutionDngTypeKey,
                if (currentIsFull) 1 else 0,
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
                if (currentIsFull) 1 else 0,
                "seamless.remosaic.enable"
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

                        log(
                            "Capture started."
                        )

                        log(
                            "Frame: $frameNumber"
                        )
                    }

                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {

                        log(
                            "Capture request completed."
                        )

                        log(
                            "Waiting for RAW image..."
                        )

                        runOnUiThread {
                            captureButton.isEnabled = true
                        }
                    }

                    override fun onCaptureFailed(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        failure: CaptureFailure
                    ) {

                        log("")
                        log(
                            "CAPTURE FAILED"
                        )

                        log(
                            "Reason: ${failure.reason}"
                        )

                        runOnUiThread {
                            captureButton.isEnabled = true
                        }
                    }
                },

                cameraHandler
            )

        } catch (e: Throwable) {

            log("")
            log("CAPTURE EXCEPTION")
            log(e.javaClass.name)
            log(e.message ?: "")

            runOnUiThread {
                captureButton.isEnabled = true
            }
        }
    }

    // =========================================================
    // RAW IMAGE
    // =========================================================

    private fun handleRawImage(
        reader: ImageReader
    ) {

        var image: Image? = null

        try {

            image =
                reader.acquireNextImage()

            if (image == null) {

                log(
                    "ImageReader returned null."
                )

                return
            }

            log("")
            log("")
            log("********************************")
            log("RAW IMAGE RECEIVED")
            log("********************************")

            log(
                "Width: ${image.width}"
            )

            log(
                "Height: ${image.height}"
            )

            val mp =
                image.width.toDouble() *
                    image.height.toDouble() /
                    1_000_000.0

            log(
                "MP: ${
                    String.format(
                        Locale.US,
                        "%.2f",
                        mp
                    )
                }"
            )

            log(
                "Format: ${image.format}"
            )

            var totalBytes =
                0L

            image.planes.forEachIndexed {
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
                    "Bytes: $bytes"
                )

                log(
                    "Row stride: ${plane.rowStride}"
                )

                log(
                    "Pixel stride: ${plane.pixelStride}"
                )
            }

            log("")
            log(
                "Total bytes: $totalBytes"
            )

            log(
                "Approx MB: ${
                    String.format(
                        Locale.US,
                        "%.2f",
                        totalBytes /
                            1024.0 /
                            1024.0
                    )
                }"
            )

            saveRawImage(
                image
            )

            if (
                image.width == FULL_WIDTH &&
                image.height == FULL_HEIGHT
            ) {

                log("")
                log(
                    "********************************"
                )

                log(
                    "REAL 200 MP RAW FRAME RECEIVED"
                )

                log(
                    "********************************"
                )
            }

        } catch (e: Throwable) {

            log("")
            log("RAW IMAGE ERROR")
            log(e.javaClass.name)
            log(e.message ?: "")

        } finally {

            try {
                image?.close()
            } catch (_: Throwable) {
            }
        }
    }

    private fun saveRawImage(
        image: Image
    ) {

        try {

            val directory =
                getExternalFilesDir(
                    Environment.DIRECTORY_PICTURES
                ) ?: return

            if (!directory.exists()) {
                directory.mkdirs()
            }

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
                    "RAW_${image.width}x${image.height}_$stamp.bin"
                )

            FileOutputStream(
                file
            ).use { stream ->

                image.planes.forEach { plane ->

                    val buffer =
                        plane.buffer.duplicate()

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
            log("Saved:")
            log(file.absolutePath)

            log(
                "File size: ${
                    String.format(
                        Locale.US,
                        "%.2f MB",
                        file.length() /
                            1024.0 /
                            1024.0
                    )
                }"
            )

        } catch (e: Throwable) {

            log("")
            log("SAVE ERROR")
            log(e.javaClass.name)
            log(e.message ?: "")
        }
    }

    // =========================================================
    // SETTERS
    // =========================================================

    private fun setInt(
        builder: CaptureRequest.Builder,
        key: CaptureRequest.Key<Int>,
        value: Int,
        label: String
    ) {

        try {

            builder.set(
                key,
                value
            )

            log(
                "OK $label = $value"
            )

        } catch (e: Throwable) {

            log(
                "FAIL $label"
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
        label: String
    ) {

        try {

            builder.set(
                key,
                value.toByte()
            )

            log(
                "OK $label = $value"
            )

        } catch (e: Throwable) {

            log(
                "FAIL $label"
            )

            log(
                "  ${e.javaClass.simpleName}: " +
                    (e.message ?: "")
            )
        }
    }

    private fun setIntArray(
        builder: CaptureRequest.Builder,
        key: CaptureRequest.Key<IntArray>,
        value: IntArray,
        label: String
    ) {

        try {

            builder.set(
                key,
                value
            )

            log(
                "OK $label = " +
                    value.contentToString()
            )

        } catch (e: Throwable) {

            log(
                "FAIL $label"
            )

            log(
                "  ${e.javaClass.simpleName}: " +
                    (e.message ?: "")
            )
        }
    }

    private fun enableSessionButtons() {

        runOnUiThread {
            standardSessionButton.isEnabled = true
            fullSessionButton.isEnabled = true
            captureButton.isEnabled = false
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

    private fun startCameraThread() {

        cameraThread =
            HandlerThread(
                "VivoSingleRawTest"
            )

        cameraThread.start()

        cameraHandler =
            Handler(
                cameraThread.looper
            )
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
            requestCode == REQUEST_CAMERA &&
            grantResults.isNotEmpty() &&
            grantResults[0] ==
            PackageManager.PERMISSION_GRANTED
        ) {

            openCamera()
        }
    }

    override fun onDestroy() {

        try {
            captureSession?.close()
        } catch (_: Throwable) {
        }

        try {
            cameraDevice?.close()
        } catch (_: Throwable) {
        }

        try {
            rawReader?.close()
        } catch (_: Throwable) {
        }

        try {
            cameraThread.quitSafely()
        } catch (_: Throwable) {
        }

        super.onDestroy()
    }
}
