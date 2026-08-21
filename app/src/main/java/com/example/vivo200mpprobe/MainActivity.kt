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
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_ID = "3"

        private const val WIDTH = 4080
        private const val HEIGHT = 3072

        private const val REQUEST_CAMERA = 1001

        private const val CASE_DELAY_MS = 1500L
    }

    private lateinit var output: TextView
    private lateinit var runButton: Button

    private lateinit var cameraManager: CameraManager

    private lateinit var cameraThread: HandlerThread
    private lateinit var cameraHandler: Handler

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null

    private var currentCaseIndex = -1

    private val results =
        mutableListOf<CaseResult>()

    // =========================================================
    // TEST CASE
    // =========================================================

    data class TestCase(
        val name: String,
        val sensorSizeList: IntArray?,
        val snapshotJpegStreamMap: IntArray?
    )

    data class CaseResult(
        val name: String,
        var configured: Boolean = false,

        var imageWidth: Int = -1,
        var imageHeight: Int = -1,
        var imageFormat: Int = -1,
        var imageBytes: Long = -1,

        var snapJpegSize: String = "null",
        var pictureSize: String = "null",
        var snapshotYuvMap: String = "null",
        var rawCaptureType: String = "null",
        var highResolutionDngType: String = "null",
        var currentModeEx: String = "null",
        var isUpscale: String = "null",
        var advanceFullsize: String = "null",
        var real200mp: String = "null",
        var requestLeft: String = "null"
    )

    private val testCases =
        listOf(

            TestCase(
                "A - NO VCF MAP",
                null,
                null
            ),

            TestCase(
                "B - sensorSizeList 4080x3072",
                intArrayOf(
                    4080,
                    3072
                ),
                null
            ),

            TestCase(
                "C - sensorSizeList 8160x6144",
                intArrayOf(
                    8160,
                    6144
                ),
                null
            ),

            TestCase(
                "D - sensorSizeList 16320x12288",
                intArrayOf(
                    16320,
                    12288
                ),
                null
            ),

            TestCase(
                "E - SnapshotJpegMap 4080x3072",
                null,
                intArrayOf(
                    4080,
                    3072
                )
            ),

            TestCase(
                "F - SnapshotJpegMap 16320x12288",
                null,
                intArrayOf(
                    16320,
                    12288
                )
            ),

            TestCase(
                "G - BOTH 16320x12288",
                intArrayOf(
                    16320,
                    12288
                ),
                intArrayOf(
                    16320,
                    12288
                )
            )
        )

    // =========================================================
    // VCF SESSION KEYS
    // =========================================================

    private val sensorSizeListKey =
        CaptureRequest.Key(
            "vcf.parameter.sensorSizeList",
            IntArray::class.java
        )

    private val snapshotJpegStreamMapKey =
        CaptureRequest.Key(
            "vcf.parameter.SnapshotJpegStreamMap",
            IntArray::class.java
        )

    // =========================================================
    // OEM SESSION KEYS
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
    // CAPTURE KEYS
    // =========================================================

    private val real200mpKey =
        CaptureRequest.Key(
            "vivo.control.real200mp_switch_on",
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

    private val seamlessRemosaicKey =
        CaptureRequest.Key(
            "vivo.control.seamless.remosaic.enable",
            Int::class.javaObjectType
        )

    private val nativeModeKey =
        CaptureRequest.Key(
            "vivo.control.isNativeMode",
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

    private val snapJpegSizeResult =
        CaptureResult.Key(
            "vivo.control.snapJpegSize",
            IntArray::class.java
        )

    private val pictureSizeResult =
        CaptureResult.Key(
            "vivo.control.picturesize.value",
            IntArray::class.java
        )

    private val snapshotYuvMapResult =
        CaptureResult.Key(
            "vivo.control.snapshotYuvStreamMap",
            IntArray::class.java
        )

    private val rawCaptureTypeResult =
        CaptureResult.Key(
            "vivo.control.raw_capture_type",
            IntArray::class.java
        )

    private val highResolutionDngTypeResult =
        CaptureResult.Key(
            "vivo.parameter.highResolutionDngType",
            IntArray::class.java
        )

    private val currentModeExResult =
        CaptureResult.Key(
            "vivo.control.currentModeEx",
            IntArray::class.java
        )

    private val isUpscaleResult =
        CaptureResult.Key(
            "vivo.control.isUpscale",
            IntArray::class.java
        )

    private val advanceFullsizeResult =
        CaptureResult.Key(
            "vivo.control.advance_fullsize",
            IntArray::class.java
        )

    private val real200mpResult =
        CaptureResult.Key(
            "vivo.control.real200mp_switch_on",
            IntArray::class.java
        )

    private val requestLeftResult =
        CaptureResult.Key(
            "vivo.control.RequestLeftInThisSnapshot",
            IntArray::class.java
        )

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startCameraThread()
        buildUi()

        cameraManager =
            getSystemService(
                CAMERA_SERVICE
            ) as CameraManager

        log(
            "VIVO VCF 200 MP BEHAVIORAL PROBE"
        )

        log(
            "================================"
        )

        log("")
        log(
            "Camera ID: $CAMERA_ID"
        )

        log(
            "Physical output:"
        )

        log(
            "$WIDTH x $HEIGHT RAW_SENSOR"
        )

        log("")
        log(
            "Seven VCF map cases will run automatically."
        )

        log("")

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.CAMERA
                ),
                REQUEST_CAMERA
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
            30,
            20,
            30
        )

        runButton =
            Button(this)

        runButton.text =
            "RUN 7-CASE VCF TEST"

        runButton.isEnabled =
            false

        runButton.setOnClickListener {

            runButton.isEnabled =
                false

            results.clear()

            currentCaseIndex =
                -1

            log("")
            log("")
            log(
                "STARTING TEST MATRIX..."
            )

            runNextCase()
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
                    "VCF Behavioral Probe",
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
            "CLEAR"

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

        log(
            "OPENING CAMERA 3..."
        )

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
                        runButton.isEnabled =
                            true
                    }
                }

                override fun onDisconnected(
                    camera: CameraDevice
                ) {

                    camera.close()

                    cameraDevice =
                        null

                    log(
                        "Camera disconnected."
                    )
                }

                override fun onError(
                    camera: CameraDevice,
                    error: Int
                ) {

                    camera.close()

                    cameraDevice =
                        null

                    log(
                        "CAMERA ERROR: $error"
                    )
                }
            },

            cameraHandler
        )
    }

    // =========================================================
    // RUN NEXT CASE
    // =========================================================

    private fun runNextCase() {

        currentCaseIndex++

        if (
            currentCaseIndex >=
            testCases.size
        ) {

            printSummary()

            runOnUiThread {
                runButton.isEnabled =
                    true
            }

            return
        }

        val testCase =
            testCases[
                currentCaseIndex
            ]

        val result =
            CaseResult(
                testCase.name
            )

        results.add(
            result
        )

        log("")
        log("")
        log(
            "################################"
        )

        log(
            "CASE ${currentCaseIndex + 1}/" +
                "${testCases.size}"
        )

        log(
            testCase.name
        )

        log(
            "################################"
        )

        createSessionForCase(
            testCase,
            result
        )
    }

    // =========================================================
    // SESSION CREATION
    // =========================================================

    private fun createSessionForCase(
        testCase: TestCase,
        result: CaseResult
    ) {

        val camera =
            cameraDevice
                ?: return

        closeCurrentSession()

        try {

            imageReader =
                ImageReader.newInstance(
                    WIDTH,
                    HEIGHT,
                    ImageFormat.RAW_SENSOR,
                    2
                )

            imageReader!!
                .setOnImageAvailableListener(
                    { reader ->

                        handleImage(
                            reader,
                            result
                        )
                    },

                    cameraHandler
                )

        } catch (e: Throwable) {

            log(
                "ImageReader FAILED:"
            )

            log(
                e.toString()
            )

            scheduleNextCase()

            return
        }

        val surface =
            imageReader!!.surface

        val callback =
            object :
                CameraCaptureSession.StateCallback() {

                override fun onConfigured(
                    session:
                        CameraCaptureSession
                ) {

                    captureSession =
                        session

                    result.configured =
                        true

                    log(
                        "SESSION CONFIGURED"
                    )

                    performCapture(
                        testCase,
                        result
                    )
                }

                override fun onConfigureFailed(
                    session:
                        CameraCaptureSession
                ) {

                    log(
                        "SESSION FAILED"
                    )

                    scheduleNextCase()
                }
            }

        try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.P
            ) {

                val requestBuilder =
                    camera.createCaptureRequest(
                        CameraDevice.TEMPLATE_STILL_CAPTURE
                    )

                requestBuilder.addTarget(
                    surface
                )

                applyCommonSessionKeys(
                    requestBuilder
                )

                if (
                    testCase.sensorSizeList
                    != null
                ) {

                    requestBuilder.set(
                        sensorSizeListKey,
                        testCase.sensorSizeList
                    )

                    log(
                        "sensorSizeList = " +
                            testCase.sensorSizeList
                                .contentToString()
                    )
                }

                if (
                    testCase.snapshotJpegStreamMap
                    != null
                ) {

                    requestBuilder.set(
                        snapshotJpegStreamMapKey,
                        testCase.snapshotJpegStreamMap
                    )

                    log(
                        "SnapshotJpegStreamMap = " +
                            testCase.snapshotJpegStreamMap
                                .contentToString()
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

                        Executor {
                                runnable ->

                            cameraHandler.post(
                                runnable
                            )
                        },

                        callback
                    )

                config.setSessionParameters(
                    requestBuilder.build()
                )

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

            log(
                "SESSION EXCEPTION"
            )

            log(
                e.javaClass.simpleName +
                    ": " +
                    (e.message ?: "")
            )

            scheduleNextCase()
        }
    }

    // =========================================================
    // COMMON SESSION KEYS
    // =========================================================

    private fun applyCommonSessionKeys(
        builder:
            CaptureRequest.Builder
    ) {

        setInt(
            builder,
            ultraHighResolutionKey,
            1
        )

        setByte(
            builder,
            portraitHighResolutionKey,
            1
        )

        setInt(
            builder,
            forceSensorModeKey,
            0
        )

        setInt(
            builder,
            engineerRemosaicModeKey,
            1
        )

        setInt(
            builder,
            advanceFullsizeKey,
            0
        )

        setInt(
            builder,
            proRawKey,
            1
        )

        setInt(
            builder,
            sensorScenarioKey,
            3
        )

        setInt(
            builder,
            sensorScenarioCustomHintKey,
            1
        )

        setIntArray(
            builder,
            streamsUsageKey,
            intArrayOf(
                2,
                1,
                0
            )
        )

        setIntArray(
            builder,
            vcfStreamTypeKey,
            intArrayOf(
                0,
                1
            )
        )
    }

    // =========================================================
    // CAPTURE
    // =========================================================

    private fun performCapture(
        testCase: TestCase,
        caseResult: CaseResult
    ) {

        val camera =
            cameraDevice
                ?: return

        val session =
            captureSession
                ?: return

        val reader =
            imageReader
                ?: return

        try {

            val builder =
                camera.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE
                )

            builder.addTarget(
                reader.surface
            )

            applyCommonSessionKeys(
                builder
            )

            if (
                testCase.sensorSizeList
                != null
            ) {

                builder.set(
                    sensorSizeListKey,
                    testCase.sensorSizeList
                )
            }

            if (
                testCase.snapshotJpegStreamMap
                != null
            ) {

                builder.set(
                    snapshotJpegStreamMapKey,
                    testCase.snapshotJpegStreamMap
                )
            }

            setInt(
                builder,
                real200mpKey,
                1
            )

            setInt(
                builder,
                rawCaptureTypeKey,
                32
            )

            setInt(
                builder,
                highResolutionDngTypeKey,
                1
            )

            setInt(
                builder,
                sensorModeKey,
                0
            )

            setInt(
                builder,
                previewSensorModeKey,
                0
            )

            setInt(
                builder,
                niceCaptureSensorModeKey,
                0
            )

            setInt(
                builder,
                seamlessRemosaicKey,
                1
            )

            setInt(
                builder,
                nativeModeKey,
                1
            )

            setInt(
                builder,
                isCaptureKey,
                1
            )

            setInt(
                builder,
                isSnapshotKey,
                1
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

                        readResults(
                            result,
                            caseResult
                        )

                        log(
                            "CAPTURE COMPLETE"
                        )

                        scheduleNextCase()
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
                            "CAPTURE FAILED"
                        )

                        log(
                            "Reason = ${
                                failure.reason
                            }"
                        )

                        scheduleNextCase()
                    }
                },

                cameraHandler
            )

        } catch (e: Throwable) {

            log(
                "CAPTURE EXCEPTION"
            )

            log(
                e.toString()
            )

            scheduleNextCase()
        }
    }

    // =========================================================
    // IMAGE
    // =========================================================

    private fun handleImage(
        reader:
            ImageReader,
        result:
            CaseResult
    ) {

        var image:
            Image? =
            null

        try {

            image =
                reader.acquireNextImage()

            if (
                image == null
            ) {
                return
            }

            result.imageWidth =
                image.width

            result.imageHeight =
                image.height

            result.imageFormat =
                image.format

            var total =
                0L

            for (
                plane in
                image.planes
            ) {

                total +=
                    plane.buffer
                        .remaining()
                        .toLong()
            }

            result.imageBytes =
                total

            log(
                "IMAGE:"
            )

            log(
                "${image.width} x " +
                    "${image.height}"
            )

            log(
                "Format = ${
                    image.format
                }"
            )

            log(
                "Bytes = $total"
            )

        } catch (e: Throwable) {

            log(
                "IMAGE ERROR:"
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
    // RESULT READ
    // =========================================================

    private fun readResults(
        result:
            TotalCaptureResult,
        out:
            CaseResult
    ) {

        out.snapJpegSize =
            getArray(
                result,
                snapJpegSizeResult
            )

        out.pictureSize =
            getArray(
                result,
                pictureSizeResult
            )

        out.snapshotYuvMap =
            getArray(
                result,
                snapshotYuvMapResult
            )

        out.rawCaptureType =
            getArray(
                result,
                rawCaptureTypeResult
            )

        out.highResolutionDngType =
            getArray(
                result,
                highResolutionDngTypeResult
            )

        out.currentModeEx =
            getArray(
                result,
                currentModeExResult
            )

        out.isUpscale =
            getArray(
                result,
                isUpscaleResult
            )

        out.advanceFullsize =
            getArray(
                result,
                advanceFullsizeResult
            )

        out.real200mp =
            getArray(
                result,
                real200mpResult
            )

        out.requestLeft =
            getArray(
                result,
                requestLeftResult
            )

        log("")
        log(
            "RETURNED VALUES:"
        )

        log(
            "snapJpegSize = " +
                out.snapJpegSize
        )

        log(
            "picturesize.value = " +
                out.pictureSize
        )

        log(
            "snapshotYuvStreamMap = " +
                out.snapshotYuvMap
        )

        log(
            "raw_capture_type = " +
                out.rawCaptureType
        )

        log(
            "highResolutionDngType = " +
                out.highResolutionDngType
        )

        log(
            "currentModeEx = " +
                out.currentModeEx
        )

        log(
            "isUpscale = " +
                out.isUpscale
        )

        log(
            "advance_fullsize = " +
                out.advanceFullsize
        )

        log(
            "real200mp = " +
                out.real200mp
        )

        log(
            "RequestLeft = " +
                out.requestLeft
        )
    }

    private fun getArray(
        result:
            CaptureResult,
        key:
            CaptureResult.Key<IntArray>
    ): String {

        return try {

            val value =
                result.get(
                    key
                )

            value?.contentToString()
                ?: "null"

        } catch (_: Throwable) {

            "READ ERROR"
        }
    }

    // =========================================================
    // NEXT CASE
    // =========================================================

    private fun scheduleNextCase() {

        cameraHandler.postDelayed(
            {

                runNextCase()

            },
            CASE_DELAY_MS
        )
    }

    // =========================================================
    // FINAL SUMMARY
    // =========================================================

    private fun printSummary() {

        log("")
        log("")
        log(
            "================================"
        )

        log(
            "FINAL COMPARISON"
        )

        log(
            "================================"
        )

        for (
            result in
            results
        ) {

            log("")
            log(
                result.name
            )

            log(
                "Configured: " +
                    result.configured
            )

            log(
                "Image: " +
                    result.imageWidth +
                    " x " +
                    result.imageHeight
            )

            log(
                "Bytes: " +
                    result.imageBytes
            )

            log(
                "snapJpegSize: " +
                    result.snapJpegSize
            )

            log(
                "pictureSize: " +
                    result.pictureSize
            )

            log(
                "snapshotYuvMap: " +
                    result.snapshotYuvMap
            )

            log(
                "rawCaptureType: " +
                    result.rawCaptureType
            )

            log(
                "highResDng: " +
                    result.highResolutionDngType
            )

            log(
                "currentModeEx: " +
                    result.currentModeEx
            )

            log(
                "isUpscale: " +
                    result.isUpscale
            )

            log(
                "advanceFullsize: " +
                    result.advanceFullsize
            )

            log(
                "real200mp: " +
                    result.real200mp
            )

            log(
                "RequestLeft: " +
                    result.requestLeft
            )
        }

        log("")
        log(
            "================================"
        )

        log(
            "TEST MATRIX COMPLETE"
        )

        log(
            "================================"
        )

        log("")
        log(
            "Press COPY OUTPUT."
        )
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private fun setInt(
        builder:
            CaptureRequest.Builder,
        key:
            CaptureRequest.Key<Int>,
        value:
            Int
    ) {

        try {

            builder.set(
                key,
                value
            )

        } catch (_: Throwable) {
        }
    }

    private fun setByte(
        builder:
            CaptureRequest.Builder,
        key:
            CaptureRequest.Key<Byte>,
        value:
            Int
    ) {

        try {

            builder.set(
                key,
                value.toByte()
            )

        } catch (_: Throwable) {
        }
    }

    private fun setIntArray(
        builder:
            CaptureRequest.Builder,
        key:
            CaptureRequest.Key<IntArray>,
        value:
            IntArray
    ) {

        try {

            builder.set(
                key,
                value
            )

        } catch (_: Throwable) {
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
            imageReader?.close()
        } catch (_: Throwable) {
        }

        imageReader =
            null
    }

    private fun startCameraThread() {

        cameraThread =
            HandlerThread(
                "VivoVCFBehavior"
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
            REQUEST_CAMERA &&
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
