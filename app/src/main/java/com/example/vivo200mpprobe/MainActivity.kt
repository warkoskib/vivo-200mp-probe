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

        private const val NORMAL_WIDTH = 4080
        private const val NORMAL_HEIGHT = 3072

        private const val PERMISSION_REQUEST = 1001
    }

    private lateinit var output: TextView

    private lateinit var manager: CameraManager

    private lateinit var cameraThread: HandlerThread
    private lateinit var cameraHandler: Handler

    private var camera: CameraDevice? = null
    private var session: CameraCaptureSession? = null

    private var largeReader: ImageReader? = null
    private var normalReader: ImageReader? = null

    private var testIndex = 0

    private data class TestCase(
        val name: String,
        val width: Int,
        val height: Int
    )

    private val tests = listOf(

        TestCase(
            "A - RAW10 16320x12288 + RAW16 4080x3072",
            16320,
            12288
        ),

        TestCase(
            "B - RAW10 8160x6144 + RAW16 4080x3072",
            8160,
            6144
        ),

        TestCase(
            "C - RAW10 4080x3072 + RAW16 4080x3072",
            4080,
            3072
        )
    )

    // =========================================================
    // VIVO / MTK SESSION KEYS
    // =========================================================

    private val ultraHighResolution =
        CaptureRequest.Key(
            "vivo.control.ultra_highresolution",
            Int::class.javaObjectType
        )

    private val portraitHighResolution =
        CaptureRequest.Key(
            "vivo.control.portrait_high_resolution",
            Byte::class.javaObjectType
        )

    private val forceSensorMode =
        CaptureRequest.Key(
            "vivo.control.forceSensorMode",
            Int::class.javaObjectType
        )

    private val engineerRemosaicMode =
        CaptureRequest.Key(
            "vivo.control.EngineerRemosaicMode",
            Int::class.javaObjectType
        )

    private val advanceFullsize =
        CaptureRequest.Key(
            "vivo.control.advance_fullsize",
            Int::class.javaObjectType
        )

    private val proRaw =
        CaptureRequest.Key(
            "vivo.control.is_ProRaw_on",
            Int::class.javaObjectType
        )

    private val cameraScenario =
        CaptureRequest.Key(
            "com.mediatek.seamlessfeature.cameraScenario",
            Int::class.javaObjectType
        )

    private val sensorScenario =
        CaptureRequest.Key(
            "com.mediatek.seamlessfeature.sensorScenario",
            Int::class.javaObjectType
        )

    private val sensorScenarioHint =
        CaptureRequest.Key(
            "com.mediatek.seamlessfeature.sensorScenarioCustomHint",
            Int::class.javaObjectType
        )

    private val streamsUsage =
        CaptureRequest.Key(
            "vivo.control.streamsUsage",
            IntArray::class.java
        )

    private val vcfStreamType =
        CaptureRequest.Key(
            "vivo.control.vcfStreamType",
            IntArray::class.java
        )

    private val sensorSizeList =
        CaptureRequest.Key(
            "vcf.parameter.sensorSizeList",
            IntArray::class.java
        )

    // =========================================================
    // ACTIVITY
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        startThread()
        buildUi()

        manager =
            getSystemService(
                CAMERA_SERVICE
            ) as CameraManager

        log("VIVO OEM RAW VIF STRUCTURE PROBE")
        log("================================")
        log("")
        log("Camera ID: $CAMERA_ID")
        log("")
        log("OEM SW_RAW16_REMOSAIC structure:")
        log("")
        log("OUTPUT 0")
        log("  maxSensorModeSize")
        log("  RAW10 / format 37")
        log("")
        log("OUTPUT 1")
        log("  sensorSize")
        log("  RAW_SENSOR / format 32")
        log("")
        log("This probe performs SESSION")
        log("CONFIGURATION ONLY.")
        log("")
        log("No RAW image will be captured.")
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
                PERMISSION_REQUEST
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

        val runButton =
            Button(this)

        runButton.text =
            "RUN OEM RAW VIF MATRIX"

        runButton.setOnClickListener {

            testIndex = 0

            log("")
            log("")
            log("STARTING MATRIX")
            log("==============================")

            runNextTest()
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
                    "Vivo OEM RAW VIF Probe",
                    output.text.toString()
                )
            )

            Toast.makeText(
                this,
                "Copied",
                Toast.LENGTH_SHORT
            ).show()
        }

        root.addView(copyButton)

        val clear =
            Button(this)

        clear.text =
            "CLEAR"

        clear.setOnClickListener {
            output.text = ""
        }

        root.addView(clear)

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
            100
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

        manager.openCamera(

            CAMERA_ID,

            object :
                CameraDevice.StateCallback() {

                override fun onOpened(
                    cameraDevice: CameraDevice
                ) {

                    camera =
                        cameraDevice

                    log(
                        "Camera 3 opened."
                    )

                    dumpPublicRawSupport()
                }

                override fun onDisconnected(
                    cameraDevice: CameraDevice
                ) {

                    log(
                        "CAMERA DISCONNECTED"
                    )

                    cameraDevice.close()
                    camera = null
                }

                override fun onError(
                    cameraDevice: CameraDevice,
                    error: Int
                ) {

                    log("")
                    log(
                        "!!!!!!!!!!!!!!!!!!!!!!!!"
                    )

                    log(
                        "CAMERA ERROR = $error"
                    )

                    log(
                        "!!!!!!!!!!!!!!!!!!!!!!!!"
                    )

                    cameraDevice.close()
                    camera = null
                }
            },

            cameraHandler
        )
    }

    // =========================================================
    // SHOW PUBLIC MAP FOR REFERENCE
    // =========================================================

    private fun dumpPublicRawSupport() {

        try {

            val chars =
                manager.getCameraCharacteristics(
                    CAMERA_ID
                )

            val map =
                chars.get(
                    CameraCharacteristics
                        .SCALER_STREAM_CONFIGURATION_MAP
                )

            log("")
            log("==============================")
            log("PUBLIC STREAM MAP")
            log("==============================")

            val raw16 =
                map?.getOutputSizes(
                    ImageFormat.RAW_SENSOR
                )

            log("")
            log("RAW_SENSOR / 32:")

            if (
                raw16 == null ||
                raw16.isEmpty()
            ) {

                log("NONE")

            } else {

                raw16.forEach {
                    log(
                        "${it.width} x ${it.height}"
                    )
                }
            }

            val raw10 =
                map?.getOutputSizes(
                    ImageFormat.RAW10
                )

            log("")
            log("RAW10 / 37:")

            if (
                raw10 == null ||
                raw10.isEmpty()
            ) {

                log("NOT PUBLICLY EXPOSED")

            } else {

                raw10.forEach {
                    log(
                        "${it.width} x ${it.height}"
                    )
                }
            }

        } catch (e: Throwable) {

            log(
                "STREAM MAP ERROR:"
            )

            log(
                e.toString()
            )
        }
    }

    // =========================================================
    // TEST MATRIX
    // =========================================================

    private fun runNextTest() {

        if (
            testIndex >=
            tests.size
        ) {

            log("")
            log("")
            log("==============================")
            log("MATRIX COMPLETE")
            log("==============================")
            log("")
            log("Press COPY OUTPUT.")

            return
        }

        closeOutputs()

        val test =
            tests[testIndex]

        log("")
        log("")
        log("################################")
        log(
            "CASE ${testIndex + 1}/${tests.size}"
        )
        log(test.name)
        log("################################")

        log("")
        log("OUTPUT 0:")
        log(
            "${test.width} x ${test.height}"
        )
        log(
            "RAW10 / format ${ImageFormat.RAW10}"
        )

        log("")
        log("OUTPUT 1:")
        log(
            "$NORMAL_WIDTH x $NORMAL_HEIGHT"
        )
        log(
            "RAW_SENSOR / format ${ImageFormat.RAW_SENSOR}"
        )

        // -----------------------------------------------------
        // Construct RAW10 ImageReader
        // -----------------------------------------------------

        try {

            largeReader =
                ImageReader.newInstance(
                    test.width,
                    test.height,
                    ImageFormat.RAW10,
                    1
                )

            log("")
            log(
                "RAW10 ImageReader CREATED"
            )

        } catch (e: Throwable) {

            log("")
            log(
                "RAW10 ImageReader CREATION FAILED"
            )

            log(
                e.javaClass.name
            )

            log(
                e.message ?: ""
            )

            nextAfterDelay()
            return
        }

        // -----------------------------------------------------
        // Construct normal RAW16 ImageReader
        // -----------------------------------------------------

        try {

            normalReader =
                ImageReader.newInstance(
                    NORMAL_WIDTH,
                    NORMAL_HEIGHT,
                    ImageFormat.RAW_SENSOR,
                    1
                )

            log(
                "RAW16 ImageReader CREATED"
            )

        } catch (e: Throwable) {

            log("")
            log(
                "RAW16 ImageReader CREATION FAILED"
            )

            log(
                e.toString()
            )

            nextAfterDelay()
            return
        }

        createOemStyleSession(
            test
        )
    }

    // =========================================================
    // CREATE OEM-STYLE SESSION
    // =========================================================

    private fun createOemStyleSession(
        test: TestCase
    ) {

        val cameraDevice =
            camera

        if (cameraDevice == null) {

            log(
                "Camera is not open."
            )

            return
        }

        val raw10Surface =
            largeReader!!.surface

        val raw16Surface =
            normalReader!!.surface

        try {

            val output0 =
                OutputConfiguration(
                    raw10Surface
                )

            val output1 =
                OutputConfiguration(
                    raw16Surface
                )

            /*
             * Order is deliberate:
             *
             * INDEX 0 = REMOSAIC / RAW10
             * INDEX 1 = NORMAL / RAW16
             */

            val outputs =
                listOf(
                    output0,
                    output1
                )

            val executor =
                Executor {
                    runnable ->

                    cameraHandler.post(
                        runnable
                    )
                }

            val callback =
                object :
                    CameraCaptureSession.StateCallback() {

                    override fun onConfigured(
                        newSession:
                            CameraCaptureSession
                    ) {

                        session =
                            newSession

                        log("")
                        log(
                            "******************************"
                        )

                        log(
                            "SESSION RESULT: CONFIGURED"
                        )

                        log(
                            "******************************"
                        )

                        log("")
                        log(
                            "HAL ACCEPTED:"
                        )

                        log(
                            "RAW10 ${test.width}x${test.height}"
                        )

                        log(
                            "+"
                        )

                        log(
                            "RAW16 ${NORMAL_WIDTH}x${NORMAL_HEIGHT}"
                        )

                        log("")
                        log(
                            "*** IMPORTANT SUCCESS ***"
                        )

                        nextAfterDelay()
                    }

                    override fun onConfigureFailed(
                        failedSession:
                            CameraCaptureSession
                    ) {

                        log("")
                        log(
                            "SESSION RESULT: FAILED"
                        )

                        log(
                            "HAL rejected this two-surface combination."
                        )

                        nextAfterDelay()
                    }
                }

            val config =
                SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR,
                    outputs,
                    executor,
                    callback
                )

            // -------------------------------------------------
            // OEM SW RAW16 session request
            // -------------------------------------------------

            val builder =
                cameraDevice.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE
                )

            /*
             * Add BOTH surfaces, preserving OEM order.
             */

            builder.addTarget(
                raw10Surface
            )

            builder.addTarget(
                raw16Surface
            )

            log("")
            log("SESSION PARAMETERS")
            log("------------------")

            setInt(
                builder,
                ultraHighResolution,
                1,
                "ultra_highresolution"
            )

            setByte(
                builder,
                portraitHighResolution,
                1,
                "portrait_high_resolution"
            )

            setInt(
                builder,
                forceSensorMode,
                0,
                "forceSensorMode"
            )

            setInt(
                builder,
                engineerRemosaicMode,
                1,
                "EngineerRemosaicMode"
            )

            setInt(
                builder,
                advanceFullsize,
                0,
                "advance_fullsize"
            )

            setInt(
                builder,
                proRaw,
                1,
                "is_ProRaw_on"
            )

            setInt(
                builder,
                cameraScenario,
                3,
                "cameraScenario"
            )

            setInt(
                builder,
                sensorScenario,
                3,
                "sensorScenario"
            )

            setInt(
                builder,
                sensorScenarioHint,
                1,
                "sensorScenarioCustomHint"
            )

            setIntArray(
                builder,
                streamsUsage,
                intArrayOf(
                    2,
                    1,
                    0
                ),
                "streamsUsage"
            )

            setIntArray(
                builder,
                vcfStreamType,
                intArrayOf(
                    0,
                    1
                ),
                "vcfStreamType"
            )

            /*
             * Give VCF the same two dimensions represented by
             * our two physical outputs.
             */

            setIntArray(
                builder,
                sensorSizeList,
                intArrayOf(
                    test.width,
                    test.height,
                    NORMAL_WIDTH,
                    NORMAL_HEIGHT
                ),
                "sensorSizeList"
            )

            config.setSessionParameters(
                builder.build()
            )

            log("")
            log(
                "Creating two-surface OEM-style session..."
            )

            cameraDevice.createCaptureSession(
                config
            )

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

            nextAfterDelay()
        }
    }

    // =========================================================
    // NEXT TEST
    // =========================================================

    private fun nextAfterDelay() {

        cameraHandler.postDelayed(
            {

                try {
                    session?.close()
                } catch (_: Throwable) {
                }

                session =
                    null

                closeOutputs()

                testIndex++

                cameraHandler.postDelayed(
                    {
                        runNextTest()
                    },
                    800
                )

            },
            1200
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
                "FAIL $name: ${e.javaClass.simpleName}"
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
                "FAIL $name: ${e.javaClass.simpleName}"
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
                "FAIL $name: ${e.javaClass.simpleName}"
            )
        }
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    private fun closeOutputs() {

        try {
            largeReader?.close()
        } catch (_: Throwable) {
        }

        largeReader =
            null

        try {
            normalReader?.close()
        } catch (_: Throwable) {
        }

        normalReader =
            null
    }

    private fun startThread() {

        cameraThread =
            HandlerThread(
                "VivoRawVifProbe"
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
            PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] ==
            PackageManager.PERMISSION_GRANTED
        ) {

            openCamera()
        }
    }

    override fun onDestroy() {

        try {
            session?.close()
        } catch (_: Throwable) {
        }

        closeOutputs()

        try {
            camera?.close()
        } catch (_: Throwable) {
        }

        try {
            cameraThread.quitSafely()
        } catch (_: Throwable) {
        }

        super.onDestroy()
    }
}
