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
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_ID = "3"
        private const val WIDTH = 4080
        private const val HEIGHT = 3072
        private const val CAMERA_PERMISSION_REQUEST = 1001

        private const val CASE_CONTROL = 0
        private const val CASE_PRERELEASE = 1
        private const val CASE_PROPRIETARY = 2
        private const val CASE_ECHO = 3
        private const val CASE_REPROCESS = 4
        private const val CASE_IMAGEREADER_ID = 5
    }

    private lateinit var cameraManager: CameraManager
    private lateinit var cameraThread: HandlerThread
    private lateinit var cameraHandler: Handler

    private lateinit var output: TextView
    private lateinit var runButton: Button

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var rawReader: ImageReader? = null

    private var currentCase = 0
    private var stoppedByCameraError = false

    // =========================================================
    // CONFIRMED SW RAW16 SESSION KEYS
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
    // CONFIRMED CAPTURE KEYS
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
    // BACKGROUND / REPROCESS KEYS TO ISOLATE
    // =========================================================

    private val prereleaseKey =
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

    private val imageReaderIdKey =
        CaptureRequest.Key(
            "com.mediatek.bgservicefeature.imagereaderid",
            Int::class.javaObjectType
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startCameraThread()
        buildUi()

        cameraManager =
            getSystemService(CAMERA_SERVICE) as CameraManager

        log("VIVO BACKGROUND KEY ISOLATION PROBE")
        log("===================================")
        log("")
        log("Camera ID: $CAMERA_ID")
        log("RAW output: $WIDTH x $HEIGHT")
        log("")
        log("Cases:")
        log("0 = SW RAW16 control")
        log("1 = + prerelease")
        log("2 = + proprietaryRequest")
        log("3 = + echo.mode")
        log("4 = + reprocessMode")
        log("5 = + imagereaderid = 0")
        log("")
        log("The matrix stops immediately")
        log("if CAMERA ERROR 4 occurs.")
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
            30,
            20,
            30
        )

        runButton =
            Button(this)

        runButton.text =
            "RUN ISOLATION MATRIX"

        runButton.isEnabled =
            false

        runButton.setOnClickListener {

            currentCase =
                CASE_CONTROL

            stoppedByCameraError =
                false

            runButton.isEnabled =
                false

            startCurrentCase()
        }

        root.addView(runButton)

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
                    "Vivo Background Isolation Probe",
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
    // CAMERA OPEN
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

                    runOnUiThread {
                        runButton.isEnabled =
                            true
                    }
                }

                override fun onDisconnected(
                    camera: CameraDevice
                ) {

                    log("")
                    log("CAMERA DISCONNECTED")

                    camera.close()

                    cameraDevice =
                        null
                }

                override fun onError(
                    camera: CameraDevice,
                    error: Int
                ) {

                    log("")
                    log("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                    log("CAMERA ERROR = $error")
                    log("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")

                    log(
                        "Failure occurred during:"
                    )

                    log(
                        caseName(currentCase)
                    )

                    if (error == CameraDevice.StateCallback.ERROR_CAMERA_DEVICE) {

                        log("")
                        log(
                            "ERROR 4 = ERROR_CAMERA_DEVICE"
                        )

                        log(
                            "Isolation matrix STOPPED."
                        )

                        stoppedByCameraError =
                            true
                    }

                    camera.close()

                    cameraDevice =
                        null

                    runOnUiThread {
                        runButton.isEnabled =
                            true
                    }
                }
            },

            cameraHandler
        )
    }

    // =========================================================
    // TEST CASE
    // =========================================================

    private fun startCurrentCase() {

        val camera =
            cameraDevice

        if (camera == null) {

            log(
                "Camera is not currently open."
            )

            runOnUiThread {
                runButton.isEnabled =
                    true
            }

            return
        }

        closeSessionOnly()

        log("")
        log("")
        log("################################")
        log("CASE ${currentCase + 1}/6")
        log(caseName(currentCase))
        log("################################")

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

                        val image =
                            try {
                                reader.acquireNextImage()
                            } catch (_: Throwable) {
                                null
                            }

                        if (image != null) {

                            log("")
                            log("IMAGE RECEIVED")

                            log(
                                "${image.width} x ${image.height}"
                            )

                            log(
                                "Format = ${image.format}"
                            )

                            if (image.planes.isNotEmpty()) {

                                log(
                                    "Bytes = ${
                                        image.planes[0]
                                            .buffer
                                            .remaining()
                                    }"
                                )
                            }

                            image.close()
                        }
                    },

                    cameraHandler
                )

        } catch (e: Throwable) {

            log(
                "ImageReader error: $e"
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

                    log(
                        "SESSION RESULT: CONFIGURED"
                    )

                    performCapture(
                        session,
                        currentCase
                    )
                }

                override fun onConfigureFailed(
                    session:
                        CameraCaptureSession
                ) {

                    log(
                        "SESSION RESULT: FAILED"
                    )

                    advanceToNextCase()
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

                val sessionBuilder =
                    camera.createCaptureRequest(
                        CameraDevice.TEMPLATE_STILL_CAPTURE
                    )

                sessionBuilder.addTarget(
                    surface
                )

                log("")
                log("SESSION PARAMETERS")
                log("------------------")

                applyBaseSwRaw16(
                    sessionBuilder
                )

                applyIsolationKey(
                    sessionBuilder,
                    currentCase
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

            advanceToNextCase()
        }
    }

    // =========================================================
    // CAPTURE
    // =========================================================

    private fun performCapture(
        session:
            CameraCaptureSession,
        caseIndex:
            Int
    ) {

        val camera =
            cameraDevice ?: return

        val reader =
            rawReader ?: return

        try {

            val builder =
                camera.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE
                )

            builder.addTarget(
                reader.surface
            )

            applyBaseSwRaw16(
                builder
            )

            applyIsolationKey(
                builder,
                caseIndex
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

                        log(
                            "CAPTURE STARTED"
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

                        log(
                            "CAPTURE RESULT: COMPLETED"
                        )

                        cameraHandler.postDelayed(
                            {
                                advanceToNextCase()
                            },
                            1000
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

                        log(
                            "CAPTURE RESULT: FAILED"
                        )

                        log(
                            "Reason = ${failure.reason}"
                        )

                        cameraHandler.postDelayed(
                            {
                                advanceToNextCase()
                            },
                            1000
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
                e.javaClass.simpleName +
                    ": " +
                    (e.message ?: "")
            )

            advanceToNextCase()
        }
    }

    // =========================================================
    // BASE SW RAW16 CONFIG
    // =========================================================

    private fun applyBaseSwRaw16(
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

    // =========================================================
    // ONE EXTRA KEY PER TEST
    // =========================================================

    private fun applyIsolationKey(
        builder:
            CaptureRequest.Builder,
        testCase:
            Int
    ) {

        when (testCase) {

            CASE_CONTROL -> {

                log(
                    "EXTRA KEY: NONE"
                )
            }

            CASE_PRERELEASE -> {

                setInt(
                    builder,
                    prereleaseKey,
                    1,
                    "bgservice.prerelease"
                )
            }

            CASE_PROPRIETARY -> {

                setInt(
                    builder,
                    proprietaryRequestKey,
                    1,
                    "proprietaryRequest"
                )
            }

            CASE_ECHO -> {

                setInt(
                    builder,
                    echoModeKey,
                    1,
                    "echo.mode"
                )
            }

            CASE_REPROCESS -> {

                setInt(
                    builder,
                    reprocessModeKey,
                    1,
                    "reprocessMode"
                )
            }

            CASE_IMAGEREADER_ID -> {

                setInt(
                    builder,
                    imageReaderIdKey,
                    0,
                    "bgservice.imagereaderid"
                )
            }
        }
    }

    // =========================================================
    // NEXT CASE
    // =========================================================

    private fun advanceToNextCase() {

        if (stoppedByCameraError) {
            return
        }

        closeSessionOnly()

        if (
            currentCase >=
            CASE_IMAGEREADER_ID
        ) {

            log("")
            log("")
            log("==============================")
            log("ISOLATION MATRIX COMPLETE")
            log("==============================")

            log(
                "No fatal camera error occurred."
            )

            runOnUiThread {
                runButton.isEnabled =
                    true
            }

            return
        }

        currentCase++

        cameraHandler.postDelayed(
            {
                startCurrentCase()
            },
            750
        )
    }

    private fun caseName(
        value: Int
    ): String {

        return when (value) {

            CASE_CONTROL ->
                "CONTROL - SW RAW16 ONLY"

            CASE_PRERELEASE ->
                "+ bgservice.prerelease = 1"

            CASE_PROPRIETARY ->
                "+ proprietaryRequest = 1"

            CASE_ECHO ->
                "+ echo.mode = 1"

            CASE_REPROCESS ->
                "+ reprocessMode = 1"

            CASE_IMAGEREADER_ID ->
                "+ bgservice.imagereaderid = 0"

            else ->
                "UNKNOWN"
        }
    }

    // =========================================================
    // SET HELPERS
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
                e.javaClass.simpleName +
                    ": " +
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
                "VivoBackgroundIsolation"
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
