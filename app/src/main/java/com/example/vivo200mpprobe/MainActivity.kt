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

        private const val MODE_HW = 1
        private const val MODE_SW_RAW16 = 2
        private const val MODE_FULL_SIZE = 3
    }

    private lateinit var cameraManager: CameraManager

    private lateinit var cameraThread: HandlerThread
    private lateinit var cameraHandler: Handler

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var rawReader: ImageReader? = null

    private lateinit var output: TextView

    private lateinit var hwButton: Button
    private lateinit var swRaw16Button: Button
    private lateinit var fullSizeButton: Button
    private lateinit var captureButton: Button

    private var activeMode = 0

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

    private val upscaleKey =
        CaptureRequest.Key(
            "vivo.control.isUpscale",
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

        log("VIVO 3-MODE OEM REMOSAIC CLONE PROBE")
        log("====================================")
        log("")
        log("Camera ID: $CAMERA_ID")
        log("RAW output: $WIDTH x $HEIGHT")
        log("")
        log("MODE A - HW REMOSAIC CANDIDATE")
        log("MODE B - SW RAW16 REMOSAIC CANDIDATE")
        log("MODE C - FULL SIZE REMOSAIC CANDIDATE")
        log("")
        log("All modes use the known-valid RAW stream.")
        log("We compare HAL-returned metadata.")
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

        hwButton =
            Button(this)

        hwButton.text =
            "CREATE HW REMOSAIC SESSION"

        hwButton.isEnabled =
            false

        hwButton.setOnClickListener {
            createModeSession(
                MODE_HW
            )
        }

        root.addView(hwButton)

        swRaw16Button =
            Button(this)

        swRaw16Button.text =
            "CREATE SW RAW16 SESSION"

        swRaw16Button.isEnabled =
            false

        swRaw16Button.setOnClickListener {
            createModeSession(
                MODE_SW_RAW16
            )
        }

        root.addView(swRaw16Button)

        fullSizeButton =
            Button(this)

        fullSizeButton.text =
            "CREATE FULL SIZE SESSION"

        fullSizeButton.isEnabled =
            false

        fullSizeButton.setOnClickListener {
            createModeSession(
                MODE_FULL_SIZE
            )
        }

        root.addView(fullSizeButton)

        captureButton =
            Button(this)

        captureButton.text =
            "CAPTURE RAW"

        captureButton.isEnabled =
            false

        captureButton.setOnClickListener {
            captureCurrentMode()
        }

        root.addView(captureButton)

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
                    "Vivo 3 Mode Remosaic Probe",
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
            "CLEAR"

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

        output.setTextIsSelectable(
            true
        )

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

                    cameraDevice =
                        camera

                    log("Camera 3 opened.")
                    log("Choose a remosaic mode.")

                    enableModeButtons()
                }

                override fun onDisconnected(
                    camera: CameraDevice
                ) {

                    log("Camera disconnected.")

                    camera.close()

                    cameraDevice =
                        null
                }

                override fun onError(
                    camera: CameraDevice,
                    error: Int
                ) {

                    log("CAMERA ERROR: $error")

                    camera.close()

                    cameraDevice =
                        null
                }
            },

            cameraHandler
        )
    }

    // =========================================================
    // CREATE MODE SESSION
    // =========================================================

    private fun createModeSession(
        mode: Int
    ) {

        val camera =
            cameraDevice ?: return

        activeMode =
            mode

        disableButtons()

        closeCurrentSession()

        log("")
        log("")
        log("################################")
        log("CREATE ${modeName(mode)} SESSION")
        log("################################")

        log(
            "RAW_SENSOR $WIDTH x $HEIGHT"
        )

        try {

            rawReader =
                ImageReader.newInstance(
                    WIDTH,
                    HEIGHT,
                    ImageFormat.RAW_SENSOR,
                    2
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

        } catch (e: Throwable) {

            log("ImageReader FAILED")
            log(e.toString())

            enableModeButtons()

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
                        "${modeName(mode)} SESSION CONFIGURED"
                    )

                    log(
                        "HAL accepted session."
                    )

                    runOnUiThread {

                        captureButton.text =
                            "CAPTURE ${modeName(mode)}"

                        captureButton.isEnabled =
                            true

                        hwButton.isEnabled =
                            true

                        swRaw16Button.isEnabled =
                            true

                        fullSizeButton.isEnabled =
                            true
                    }
                }

                override fun onConfigureFailed(
                    session:
                        CameraCaptureSession
                ) {

                    log("")
                    log(
                        "${modeName(mode)} SESSION FAILED"
                    )

                    enableModeButtons()
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
                        SessionConfiguration.SESSION_REGULAR,
                        listOf(
                            OutputConfiguration(
                                surface
                            )
                        ),
                        executor,
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

                applyModeParameters(
                    sessionRequest,
                    mode,
                    true
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
            log("SESSION EXCEPTION")

            log(
                e.javaClass.simpleName +
                    ": " +
                    (e.message ?: "")
            )

            enableModeButtons()
        }
    }

    // =========================================================
    // MODE PRESETS
    // =========================================================

    private fun applyModeParameters(
        builder: CaptureRequest.Builder,
        mode: Int,
        sessionPhase: Boolean
    ) {

        /*
         * COMMON OEM HIGH-RES CONTROLS
         */

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

        when (mode) {

            // =================================================
            // MODE A
            // Hardware-remosaic candidate
            // =================================================

            MODE_HW -> {

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
                    sensorScenarioCustomHintKey,
                    1,
                    "sensorScenarioCustomHint"
                )

                setInt(
                    builder,
                    proRawKey,
                    0,
                    "is_ProRaw_on"
                )
            }

            // =================================================
            // MODE B
            // RAW16 software-remosaic candidate
            // =================================================

            MODE_SW_RAW16 -> {

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
            }

            // =================================================
            // MODE C
            // Full-size OEM candidate
            // =================================================

            MODE_FULL_SIZE -> {

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
            }
        }

        if (!sessionPhase) {

            log("")
            log("CAPTURE-ONLY PARAMETERS")
            log("-----------------------")

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

            when (mode) {

                MODE_HW -> {

                    setInt(
                        builder,
                        rawCaptureTypeKey,
                        0,
                        "raw_capture_type"
                    )

                    setInt(
                        builder,
                        highResolutionDngTypeKey,
                        0,
                        "highResolutionDngType"
                    )

                    setInt(
                        builder,
                        upscaleKey,
                        0,
                        "isUpscale"
                    )
                }

                MODE_SW_RAW16 -> {

                    /*
                     * The HAL previously returned 32 while
                     * the OEM-remosaic path was active.
                     * Test 32 explicitly here.
                     */

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
                        upscaleKey,
                        0,
                        "isUpscale"
                    )
                }

                MODE_FULL_SIZE -> {

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
                        upscaleKey,
                        1,
                        "isUpscale"
                    )
                }
            }
        }
    }

    // =========================================================
    // CAPTURE
    // =========================================================

    private fun captureCurrentMode() {

        val camera =
            cameraDevice ?: return

        val session =
            captureSession ?: return

        val reader =
            rawReader ?: return

        captureButton.isEnabled =
            false

        log("")
        log("")
        log("################################")
        log("${modeName(activeMode)} CAPTURE")
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

            applyModeParameters(
                builder,
                activeMode,
                false
            )

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
                            "Capture started."
                        )

                        log(
                            "Frame: $frameNumber"
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
                            "Capture completed."
                        )

                        dumpResult(
                            result
                        )

                        runOnUiThread {
                            captureButton.isEnabled =
                                true
                        }
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

                        runOnUiThread {
                            captureButton.isEnabled =
                                true
                        }
                    }
                },

                cameraHandler
            )

        } catch (e: Throwable) {

            log("")
            log("CAPTURE EXCEPTION")

            log(
                e.javaClass.simpleName +
                    ": " +
                    (e.message ?: "")
            )

            captureButton.isEnabled =
                true
        }
    }

    // =========================================================
    // RAW BUFFER
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

            log("")
            log("")
            log("==============================")
            log("RAW BUFFER RECEIVED")
            log("==============================")

            log(
                "Mode: ${modeName(activeMode)}"
            )

            log(
                "Image: ${image.width} x ${image.height}"
            )

            log(
                "Format: ${image.format}"
            )

            log(
                "Planes: ${image.planes.size}"
            )

            var total =
                0L

            image.planes.forEachIndexed {
                    index,
                    plane ->

                val count =
                    plane.buffer.remaining()

                total +=
                    count.toLong()

                log("")
                log(
                    "Plane $index"
                )

                log(
                    "Bytes: $count"
                )

                log(
                    "RowStride: ${plane.rowStride}"
                )

                log(
                    "PixelStride: ${plane.pixelStride}"
                )
            }

            log("")
            log(
                "TOTAL BYTES: $total"
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

            saveRaw(
                image
            )

        } catch (e: Throwable) {

            log(
                "RAW READ ERROR: $e"
            )

        } finally {

            try {
                image?.close()
            } catch (_: Throwable) {
            }
        }
    }

    private fun saveRaw(
        image: Image
    ) {

        try {

            val directory =
                getExternalFilesDir(
                    Environment.DIRECTORY_PICTURES
                ) ?: return

            directory.mkdirs()

            val timestamp =
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss_SSS",
                    Locale.US
                ).format(
                    Date()
                )

            val safeName =
                modeName(activeMode)
                    .replace(
                        " ",
                        "_"
                    )

            val file =
                File(
                    directory,
                    "${safeName}_" +
                        "${image.width}x${image.height}_" +
                        "$timestamp.raw"
                )

            FileOutputStream(
                file
            ).use {
                    output ->

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

                    output.write(
                        bytes
                    )
                }
            }

            log("")
            log("RAW SAVED:")

            log(
                file.absolutePath
            )

        } catch (e: Throwable) {

            log(
                "SAVE ERROR: $e"
            )
        }
    }

    // =========================================================
    // RESULT DUMP
    // =========================================================

    private fun dumpResult(
        result:
            TotalCaptureResult
    ) {

        log("")
        log("==============================")
        log("OEM / SENSOR RESULT")
        log("==============================")

        val terms =
            listOf(
                "remosaic",
                "200mp",
                "raw_capture",
                "sensor",
                "fullsize",
                "highresolution",
                "proraw",
                "native",
                "upscale",
                "scenario",
                "currentmode",
                "scenemode",
                "tuning",
                "requestleft",
                "stream",
                "vcf"
            )

        var count =
            0

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

                count++

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

        log("")
        log(
            "Matching result keys: $count"
        )

        log("")
        log("==============================")
        log("IMPORTANT COMPARISON TARGETS")
        log("==============================")

        log(
            "Watch for differences in:"
        )

        log(
            "raw_capture_type"
        )

        log(
            "currentModeEx"
        )

        log(
            "sceneMode"
        )

        log(
            "hintForIspTuning"
        )

        log(
            "hintForCustomTuning"
        )

        log(
            "RequestLeftInThisSnapshot"
        )

        log(
            "sensorMode"
        )

        log(
            "niceCaptureSensorMode"
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
    // HELPERS
    // =========================================================

    private fun modeName(
        mode: Int
    ): String {

        return when (mode) {

            MODE_HW ->
                "HW REMOSAIC"

            MODE_SW_RAW16 ->
                "SW RAW16 REMOSAIC"

            MODE_FULL_SIZE ->
                "FULL SIZE REMOSAIC"

            else ->
                "UNKNOWN"
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

    private fun closeCurrentSession() {

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

        captureButton.isEnabled =
            false
    }

    private fun disableButtons() {

        runOnUiThread {

            hwButton.isEnabled =
                false

            swRaw16Button.isEnabled =
                false

            fullSizeButton.isEnabled =
                false

            captureButton.isEnabled =
                false
        }
    }

    private fun enableModeButtons() {

        runOnUiThread {

            hwButton.isEnabled =
                true

            swRaw16Button.isEnabled =
                true

            fullSizeButton.isEnabled =
                true
        }
    }

    private fun startCameraThread() {

        cameraThread =
            HandlerThread(
                "VivoRemosaicClone"
            )

        cameraThread.start()

        cameraHandler =
            Handler(
                cameraThread.looper
            )
    }

    private fun log(
        text: String
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

        closeCurrentSession()

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
