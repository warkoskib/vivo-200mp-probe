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

        private const val NORMAL_WIDTH = 4080
        private const val NORMAL_HEIGHT = 3072

        private const val FULL_WIDTH = 16320
        private const val FULL_HEIGHT = 12288
    }

    private lateinit var output: TextView
    private lateinit var scroll: ScrollView

    private lateinit var standardButton: Button
    private lateinit var fullButton: Button

    private lateinit var cameraThread: HandlerThread
    private lateinit var cameraHandler: Handler

    private lateinit var cameraManager: CameraManager

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null

    private var normalRawReader: ImageReader? = null
    private var fullRawReader: ImageReader? = null

    private var currentMode = 0

    // =========================================================
    // KNOWN / SUSPECTED VIVO SESSION KEYS
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

    private val sensorScenarioKey =
        CaptureRequest.Key(
            "com.mediatek.seamlessfeature.sensorScenario",
            Int::class.javaObjectType
        )

    private val cameraScenarioKey =
        CaptureRequest.Key(
            "com.mediatek.seamlessfeature.cameraScenario",
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
    // CAPTURE-TIME KEYS
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildUi()
        startCameraThread()

        cameraManager =
            getSystemService(CAMERA_SERVICE) as CameraManager

        log("VIVO CAMERA 3 FULL SENSOR RAW PROBE")
        log("===================================")
        log("")
        log("Camera ID: $CAMERA_ID")
        log("")
        log("Known public RAW:")
        log("$NORMAL_WIDTH x $NORMAL_HEIGHT")
        log("")
        log("Experimental full RAW:")
        log("$FULL_WIDTH x $FULL_HEIGHT")
        log("")
        log("This probe configures ProRAW/full-size")
        log("controls BEFORE session creation.")
        log("")

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            initializeCamera()
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

        standardButton = Button(this)
        standardButton.text = "CAPTURE STANDARD RAW"
        standardButton.isEnabled = false

        standardButton.setOnClickListener {
            currentMode = 1
            captureRaw(false)
        }

        root.addView(standardButton)

        fullButton = Button(this)
        fullButton.text = "TRY 200 MP RAW"
        fullButton.isEnabled = false

        fullButton.setOnClickListener {
            currentMode = 2
            captureRaw(true)
        }

        root.addView(fullButton)

        val copyButton = Button(this)
        copyButton.text = "COPY OUTPUT"

        copyButton.setOnClickListener {

            val clipboard =
                getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as ClipboardManager

            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    "Vivo RAW Probe",
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

        val recreateButton = Button(this)
        recreateButton.text = "RECREATE SESSION"

        recreateButton.setOnClickListener {
            recreateSession()
        }

        root.addView(recreateButton)

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
    // CAMERA INITIALIZATION
    // =========================================================

    private fun initializeCamera() {

        log("==============================")
        log("STEP 1 - CREATE RAW READERS")
        log("==============================")

        createNormalRawReader()
        createFullRawReader()

        openCamera()
    }

    private fun createNormalRawReader() {

        try {

            normalRawReader =
                ImageReader.newInstance(
                    NORMAL_WIDTH,
                    NORMAL_HEIGHT,
                    ImageFormat.RAW_SENSOR,
                    2
                )

            normalRawReader?.setOnImageAvailableListener(
                { reader ->
                    handleRawImage(
                        reader,
                        "STANDARD"
                    )
                },
                cameraHandler
            )

            log("")
            log("STANDARD RAW reader created:")
            log("$NORMAL_WIDTH x $NORMAL_HEIGHT")

        } catch (e: Throwable) {

            log("")
            log("STANDARD RAW reader FAILED")
            log(e.javaClass.name)
            log(e.message ?: "")
        }
    }

    private fun createFullRawReader() {

        try {

            fullRawReader =
                ImageReader.newInstance(
                    FULL_WIDTH,
                    FULL_HEIGHT,
                    ImageFormat.RAW_SENSOR,
                    1
                )

            fullRawReader?.setOnImageAvailableListener(
                { reader ->
                    handleRawImage(
                        reader,
                        "FULL"
                    )
                },
                cameraHandler
            )

            log("")
            log("EXPERIMENTAL RAW reader CREATED:")
            log("$FULL_WIDTH x $FULL_HEIGHT")

        } catch (e: Throwable) {

            log("")
            log("EXPERIMENTAL RAW reader FAILED")
            log(e.javaClass.name)
            log(e.message ?: "")
        }
    }

    private fun openCamera() {

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        log("")
        log("==============================")
        log("STEP 2 - OPEN CAMERA 3")
        log("==============================")

        cameraManager.openCamera(
            CAMERA_ID,

            object : CameraDevice.StateCallback() {

                override fun onOpened(
                    camera: CameraDevice
                ) {

                    cameraDevice = camera

                    log("Camera 3 opened.")

                    createRawSession()
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

                    log("")
                    log("CAMERA ERROR: $error")

                    camera.close()
                    cameraDevice = null
                }
            },

            cameraHandler
        )
    }

    // =========================================================
    // SESSION CREATION
    // =========================================================

    private fun createRawSession() {

        val camera =
            cameraDevice ?: return

        val normal =
            normalRawReader ?: return

        val surfaces =
            mutableListOf(
                normal.surface
            )

        val full =
            fullRawReader

        if (full != null) {

            surfaces.add(
                full.surface
            )
        }

        log("")
        log("==============================")
        log("STEP 3 - CREATE RAW SESSION")
        log("==============================")

        log("")
        log("Outputs requested:")

        log(
            "RAW_SENSOR $NORMAL_WIDTH x $NORMAL_HEIGHT"
        )

        if (full != null) {

            log(
                "RAW_SENSOR $FULL_WIDTH x $FULL_HEIGHT"
            )
        }

        try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.P
            ) {

                val outputs =
                    surfaces.map {
                        OutputConfiguration(it)
                    }

                val executor =
                    Executor { runnable ->
                        cameraHandler.post(runnable)
                    }

                val configuration =
                    SessionConfiguration(
                        SessionConfiguration.SESSION_REGULAR,
                        outputs,
                        executor,

                        object :
                            CameraCaptureSession.StateCallback() {

                            override fun onConfigured(
                                session: CameraCaptureSession
                            ) {

                                captureSession =
                                    session

                                log("")
                                log(
                                    "RAW SESSION CONFIGURED"
                                )

                                log("")
                                log(
                                    "Both capture buttons enabled."
                                )

                                runOnUiThread {
                                    standardButton.isEnabled = true
                                    fullButton.isEnabled = true
                                }
                            }

                            override fun onConfigureFailed(
                                session: CameraCaptureSession
                            ) {

                                log("")
                                log(
                                    "RAW SESSION CONFIGURATION FAILED"
                                )

                                log("")
                                log(
                                    "The HAL rejected the output combination."
                                )

                                log("")
                                log(
                                    "Press RECREATE SESSION to try"
                                )

                                log(
                                    "again after changing configuration."
                                )
                            }
                        }
                    )

                val sessionBuilder =
                    camera.createCaptureRequest(
                        CameraDevice.TEMPLATE_STILL_CAPTURE
                    )

                sessionBuilder.addTarget(
                    normal.surface
                )

                log("")
                log("==============================")
                log("SESSION PARAMETERS")
                log("==============================")

                applySessionParameters(
                    sessionBuilder
                )

                configuration.setSessionParameters(
                    sessionBuilder.build()
                )

                camera.createCaptureSession(
                    configuration
                )

            } else {

                @Suppress("DEPRECATION")
                camera.createCaptureSession(
                    surfaces,

                    object :
                        CameraCaptureSession.StateCallback() {

                        override fun onConfigured(
                            session: CameraCaptureSession
                        ) {

                            captureSession =
                                session

                            runOnUiThread {
                                standardButton.isEnabled = true
                                fullButton.isEnabled = true
                            }

                            log(
                                "RAW SESSION CONFIGURED"
                            )
                        }

                        override fun onConfigureFailed(
                            session: CameraCaptureSession
                        ) {

                            log(
                                "RAW SESSION CONFIGURATION FAILED"
                            )
                        }
                    },

                    cameraHandler
                )
            }

        } catch (e: Throwable) {

            log("")
            log("SESSION CREATION EXCEPTION")
            log(e.javaClass.name)
            log(e.message ?: "")
        }
    }

    private fun applySessionParameters(
        builder: CaptureRequest.Builder
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
            1,
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

        /*
         * vcfStreamType type is not yet proven.
         * We try IntArray first and log rejection.
         */

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
            proRawKey,
            1,
            "is_ProRaw_on"
        )
    }

    // =========================================================
    // CAPTURE
    // =========================================================

    private fun captureRaw(
        fullResolution: Boolean
    ) {

        val camera =
            cameraDevice ?: return

        val session =
            captureSession ?: return

        val reader =
            if (fullResolution) {
                fullRawReader
            } else {
                normalRawReader
            }

        if (reader == null) {

            log(
                "Requested ImageReader does not exist."
            )

            return
        }

        runOnUiThread {
            standardButton.isEnabled = false
            fullButton.isEnabled = false
        }

        log("")
        log("")
        log("################################")
        log(
            if (fullResolution)
                "FULL 200 MP RAW REQUEST"
            else
                "STANDARD RAW REQUEST"
        )
        log("################################")

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

            /*
             * Re-apply session-level controls to
             * capture request as well.
             */
            applySessionParameters(
                builder
            )

            log("")
            log("==============================")
            log("CAPTURE PARAMETERS")
            log("==============================")

            setInt(
                builder,
                real200mpKey,
                1,
                "real200mp_switch_on"
            )

            setInt(
                builder,
                sensorModeKey,
                if (fullResolution) 0 else 1,
                "sensorMode"
            )

            setInt(
                builder,
                previewSensorModeKey,
                if (fullResolution) 0 else 1,
                "preview.sensorMode"
            )

            setInt(
                builder,
                niceCaptureSensorModeKey,
                if (fullResolution) 0 else 1,
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
                if (fullResolution) 1 else 0,
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
                if (fullResolution) 1 else 0,
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

                        log("")
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

                        log("")
                        log(
                            "Capture request completed."
                        )

                        dumpInterestingResults(
                            result
                        )

                        log("")
                        log(
                            "Waiting for RAW ImageReader..."
                        )

                        runOnUiThread {
                            standardButton.isEnabled = true
                            fullButton.isEnabled = true
                        }
                    }

                    override fun onCaptureFailed(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        failure: CaptureFailure
                    ) {

                        log("")
                        log("CAPTURE FAILED")

                        log(
                            "Reason: ${failure.reason}"
                        )

                        log(
                            "Frame: ${failure.frameNumber}"
                        )

                        runOnUiThread {
                            standardButton.isEnabled = true
                            fullButton.isEnabled = true
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
                standardButton.isEnabled = true
                fullButton.isEnabled = true
            }
        }
    }

    // =========================================================
    // RAW IMAGE HANDLER
    // =========================================================

    private fun handleRawImage(
        reader: ImageReader,
        label: String
    ) {

        var image: Image? = null

        try {

            image =
                reader.acquireNextImage()

            if (image == null) {

                log(
                    "$label ImageReader returned null."
                )

                return
            }

            log("")
            log("")
            log("================================")
            log("$label RAW IMAGE RECEIVED")
            log("================================")

            log(
                "Image width: ${image.width}"
            )

            log(
                "Image height: ${image.height}"
            )

            val mp =
                image.width.toDouble() *
                    image.height.toDouble() /
                    1_000_000.0

            log(
                "Image MP: ${
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

            log(
                "Planes: ${image.planes.size}"
            )

            var totalBytes = 0L

            image.planes.forEachIndexed {
                    index,
                    plane ->

                val buffer =
                    plane.buffer

                val size =
                    buffer.remaining()

                totalBytes +=
                    size.toLong()

                log("")
                log(
                    "Plane $index"
                )

                log(
                    "Bytes: $size"
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
                "TOTAL RAW BYTES: $totalBytes"
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

            saveRaw(
                image,
                label
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
                    "FULL 200 MP RAW FRAME RECEIVED"
                )

                log(
                    "********************************"
                )
            }

        } catch (e: Throwable) {

            log("")
            log("$label RAW READ ERROR")

            log(e.javaClass.name)
            log(e.message ?: "")

        } finally {

            try {
                image?.close()
            } catch (_: Throwable) {
            }
        }
    }

    private fun saveRaw(
        image: Image,
        label: String
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
                    "RAW_${label}_" +
                        "${image.width}x${image.height}_" +
                        "$stamp.bin"
                )

            FileOutputStream(
                file
            ).use { stream ->

                image.planes.forEach {
                        plane ->

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
            log("RAW FILE SAVED:")

            log(
                file.absolutePath
            )

            log(
                "Saved size: ${
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
            log("RAW SAVE ERROR")

            log(e.javaClass.name)
            log(e.message ?: "")
        }
    }

    // =========================================================
    // RESULT INSPECTION
    // =========================================================

    private fun dumpInterestingResults(
        result: TotalCaptureResult
    ) {

        log("")
        log("==============================")
        log("RETURNED RAW / SENSOR KEYS")
        log("==============================")

        val terms =
            listOf(
                "raw",
                "sensor",
                "remosaic",
                "200mp",
                "highresolution",
                "highres",
                "dng",
                "proraw",
                "native",
                "scenario",
                "fullsize",
                "capture_type"
            )

        for (key in result.keys) {

            val lower =
                key.name.lowercase(
                    Locale.US
                )

            if (
                terms.any {
                    lower.contains(it)
                }
            ) {

                val value =
                    try {
                        result.get(key)
                    } catch (_: Throwable) {
                        "<READ ERROR>"
                    }

                log("")
                log(
                    key.name
                )

                log(
                    formatValue(value)
                )
            }
        }
    }

    // =========================================================
    // KEY SETTERS
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

    // =========================================================
    // RECREATE SESSION
    // =========================================================

    private fun recreateSession() {

        runOnUiThread {
            standardButton.isEnabled = false
            fullButton.isEnabled = false
        }

        try {
            captureSession?.close()
        } catch (_: Throwable) {
        }

        captureSession = null

        createRawSession()
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

            is FloatArray ->
                value.contentToString()

            is DoubleArray ->
                value.contentToString()

            is ByteArray ->
                value.contentToString()

            else ->
                value.toString()
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

    private fun startCameraThread() {

        cameraThread =
            HandlerThread(
                "VivoFullRawProbe"
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
            requestCode ==
            REQUEST_CAMERA &&
            grantResults.isNotEmpty() &&
            grantResults[0] ==
            PackageManager.PERMISSION_GRANTED
        ) {

            initializeCamera()
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
            normalRawReader?.close()
        } catch (_: Throwable) {
        }

        try {
            fullRawReader?.close()
        } catch (_: Throwable) {
        }

        try {
            cameraThread.quitSafely()
        } catch (_: Throwable) {
        }

        super.onDestroy()
    }
}
