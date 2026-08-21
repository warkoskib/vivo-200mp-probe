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
    }

    private lateinit var cameraManager: CameraManager
    private lateinit var cameraThread: HandlerThread
    private lateinit var cameraHandler: Handler

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var rawReader: ImageReader? = null

    private lateinit var output: TextView
    private lateinit var runButton: Button

    private val testPhysicalIds = listOf(
        null,
        "0",
        "2",
        "3",
        "5",
        "7"
    )

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

    // =========================================================
    // RESULT KEYS
    // =========================================================

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

    private val requestLeftResultKey =
        CaptureResult.Key(
            "vivo.control.RequestLeftInThisSnapshot",
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

    private val ispHintResultKey =
        CaptureResult.Key(
            "com.mediatek.control.capture.hintForIspTuning",
            IntArray::class.java
        )

    private val customHintResultKey =
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

        log("VIVO PHYSICAL RAW VIF PROBE")
        log("==============================")
        log("")
        log("Logical camera: 3")
        log("RAW surface: 4080 x 3072")
        log("")
        log("Physical bindings tested:")
        log("NONE, 0, 2, 3, 5, 7")
        log("")
        log("Every case uses the confirmed")
        log("SW RAW16 / high-resolution DNG controls.")
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

        runButton =
            Button(this)

        runButton.text =
            "RUN PHYSICAL RAW VIF MATRIX"

        runButton.isEnabled =
            false

        runButton.setOnClickListener {

            runButton.isEnabled =
                false

            Thread {
                runMatrix()
            }.start()
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
                    "Vivo Physical RAW VIF Probe",
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

                    log("Camera 3 opened.")

                    try {

                        val chars =
                            cameraManager.getCameraCharacteristics(
                                CAMERA_ID
                            )

                        log("")
                        log(
                            "Public physicalCameraIds:"
                        )

                        val ids =
                            chars.physicalCameraIds

                        if (ids.isEmpty()) {

                            log("NONE")

                        } else {

                            for (id in ids) {
                                log(id)
                            }
                        }

                    } catch (e: Throwable) {

                        log(
                            "physicalCameraIds read error: $e"
                        )
                    }

                    runOnUiThread {
                        runButton.isEnabled =
                            true
                    }
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
    // MATRIX
    // =========================================================

    private fun runMatrix() {

        log("")
        log("")
        log("STARTING PHYSICAL-ID MATRIX")
        log("==============================")

        runCase(0)
    }

    private fun runCase(
        index: Int
    ) {

        if (
            index >=
            testPhysicalIds.size
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

        val physicalId =
            testPhysicalIds[index]

        val label =
            physicalId ?: "NONE"

        log("")
        log("")
        log("################################")
        log(
            "CASE ${index + 1}/${testPhysicalIds.size}"
        )
        log("PHYSICAL ID = $label")
        log("################################")

        createSessionForCase(
            physicalId,
            index
        )
    }

    // =========================================================
    // SESSION CREATION
    // =========================================================

    private fun createSessionForCase(
        physicalId: String?,
        caseIndex: Int
    ) {

        val camera =
            cameraDevice ?: return

        closeCurrentSession()

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
                            reader.acquireNextImage()

                        if (image != null) {

                            try {

                                log("")
                                log("IMAGE RECEIVED")

                                log(
                                    "Width = ${image.width}"
                                )

                                log(
                                    "Height = ${image.height}"
                                )

                                log(
                                    "Format = ${image.format}"
                                )

                                var total =
                                    0L

                                image.planes.forEachIndexed {
                                        planeIndex,
                                        plane ->

                                    val bytes =
                                        plane.buffer.remaining()

                                    total +=
                                        bytes.toLong()

                                    log(
                                        "Plane $planeIndex bytes = $bytes"
                                    )

                                    log(
                                        "RowStride = ${plane.rowStride}"
                                    )

                                    log(
                                        "PixelStride = ${plane.pixelStride}"
                                    )
                                }

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

                                image.close()
                            }
                        }
                    },

                    cameraHandler
                )

        } catch (e: Throwable) {

            log(
                "ImageReader creation failed:"
            )

            log(
                e.toString()
            )

            cameraHandler.postDelayed(
                {
                    runCase(
                        caseIndex + 1
                    )
                },
                500
            )

            return
        }

        val outputConfig =
            OutputConfiguration(
                rawReader!!.surface
            )

        if (physicalId != null) {

            log("")
            log(
                "Calling OutputConfiguration.setPhysicalCameraId(\"$physicalId\")"
            )

            try {

                outputConfig.setPhysicalCameraId(
                    physicalId
                )

                log(
                    "setPhysicalCameraId(): ACCEPTED"
                )

            } catch (e: Throwable) {

                log(
                    "setPhysicalCameraId(): EXCEPTION"
                )

                log(
                    e.javaClass.simpleName +
                        ": " +
                        (e.message ?: "")
                )

                closeCurrentSession()

                cameraHandler.postDelayed(
                    {
                        runCase(
                            caseIndex + 1
                        )
                    },
                    500
                )

                return
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

                    captureCase(
                        physicalId,
                        caseIndex
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

                    closeCurrentSession()

                    cameraHandler.postDelayed(
                        {
                            runCase(
                                caseIndex + 1
                            )
                        },
                        700
                    )
                }
            }

        try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.P
            ) {

                val sessionConfig =
                    SessionConfiguration(
                        SessionConfiguration.SESSION_REGULAR,
                        listOf(
                            outputConfig
                        ),
                        Executor { runnable ->
                            cameraHandler.post(
                                runnable
                            )
                        },
                        callback
                    )

                val builder =
                    camera.createCaptureRequest(
                        CameraDevice.TEMPLATE_STILL_CAPTURE
                    )

                builder.addTarget(
                    rawReader!!.surface
                )

                log("")
                log(
                    "SESSION PARAMETERS"
                )
                log(
                    "------------------"
                )

                applySwRaw16Session(
                    builder
                )

                sessionConfig.setSessionParameters(
                    builder.build()
                )

                camera.createCaptureSession(
                    sessionConfig
                )

            } else {

                log(
                    "Android version too old for this probe."
                )
            }

        } catch (e: Throwable) {

            log("")
            log(
                "SESSION CREATION EXCEPTION"
            )

            log(
                e.javaClass.simpleName +
                    ": " +
                    (e.message ?: "")
            )

            closeCurrentSession()

            cameraHandler.postDelayed(
                {
                    runCase(
                        caseIndex + 1
                    )
                },
                700
            )
        }
    }

    // =========================================================
    // CAPTURE
    // =========================================================

    private fun captureCase(
        physicalId: String?,
        caseIndex: Int
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
                    CameraDevice.TEMPLATE_STILL_CAPTURE
                )

            builder.addTarget(
                reader.surface
            )

            applySwRaw16Session(
                builder
            )

            log("")
            log(
                "CAPTURE PARAMETERS"
            )
            log(
                "------------------"
            )

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
                            "CAPTURE RESULT"
                        )
                        log(
                            "--------------"
                        )

                        logResult(
                            result,
                            rawCaptureTypeResultKey,
                            "raw_capture_type"
                        )

                        logResult(
                            result,
                            highResolutionDngTypeResultKey,
                            "highResolutionDngType"
                        )

                        logResult(
                            result,
                            currentModeExResultKey,
                            "currentModeEx"
                        )

                        logResult(
                            result,
                            sceneModeResultKey,
                            "sceneMode"
                        )

                        logResult(
                            result,
                            requestLeftResultKey,
                            "RequestLeft"
                        )

                        logResult(
                            result,
                            sensorModeResultKey,
                            "sensorMode"
                        )

                        logResult(
                            result,
                            niceCaptureSensorModeResultKey,
                            "niceCaptureSensorMode"
                        )

                        logResult(
                            result,
                            ispHintResultKey,
                            "hintForIspTuning"
                        )

                        logResult(
                            result,
                            customHintResultKey,
                            "hintForCustomTuning"
                        )

                        cameraHandler.postDelayed(
                            {

                                closeCurrentSession()

                                runCase(
                                    caseIndex + 1
                                )

                            },
                            800
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
                            "Reason = ${failure.reason}"
                        )

                        closeCurrentSession()

                        cameraHandler.postDelayed(
                            {
                                runCase(
                                    caseIndex + 1
                                )
                            },
                            700
                        )
                    }
                },

                cameraHandler
            )

        } catch (e: Throwable) {

            log(
                "Capture exception:"
            )

            log(
                e.toString()
            )

            closeCurrentSession()

            cameraHandler.postDelayed(
                {
                    runCase(
                        caseIndex + 1
                    )
                },
                700
            )
        }
    }

    // =========================================================
    // SW RAW16 SESSION
    // =========================================================

    private fun applySwRaw16Session(
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
    // HELPERS
    // =========================================================

    private fun logResult(
        result:
            CaptureResult,
        key:
            CaptureResult.Key<IntArray>,
        name:
            String
    ) {

        try {

            val value =
                result.get(key)

            log(
                "$name = " +
                    (
                        value?.contentToString()
                            ?: "null"
                        )
            )

        } catch (e: Throwable) {

            log(
                "$name = READ ERROR"
            )
        }
    }

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
    }

    private fun startCameraThread() {

        cameraThread =
            HandlerThread(
                "VivoPhysicalRawVif"
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
