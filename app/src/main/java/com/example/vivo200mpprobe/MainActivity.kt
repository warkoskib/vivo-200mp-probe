package com.example.vivo200mpprobe

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
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
    }

    private lateinit var cameraManager: CameraManager

    private lateinit var cameraThread: HandlerThread
    private lateinit var cameraHandler: Handler

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null

    private lateinit var output: TextView

    private lateinit var sensorSizeButton: Button
    private lateinit var jpegMapButton: Button
    private lateinit var imageReaderIdButton: Button

    // =========================================================
    // TARGET KEY NAMES
    // =========================================================

    private val sensorSizeListName =
        "vcf.parameter.sensorSizeList"

    private val snapshotJpegMapName =
        "vcf.parameter.SnapshotJpegStreamMap"

    private val bgImageReaderIdName =
        "com.mediatek.bgservicefeature.imagereaderid"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startCameraThread()
        buildUi()

        cameraManager =
            getSystemService(CAMERA_SERVICE) as CameraManager

        log("VIVO VCF SESSION MAP TYPE PROBE")
        log("================================")
        log("")
        log("Camera ID: $CAMERA_ID")
        log("")
        log("Supported output used for every test:")
        log("$WIDTH x $HEIGHT RAW_SENSOR")
        log("")
        log("No image capture is performed.")
        log("Each test creates a new Camera2 session.")
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

        sensorSizeButton =
            Button(this)

        sensorSizeButton.text =
            "TEST SENSOR SIZE LIST"

        sensorSizeButton.isEnabled =
            false

        sensorSizeButton.setOnClickListener {
            runSensorSizeListTests()
        }

        root.addView(
            sensorSizeButton
        )

        jpegMapButton =
            Button(this)

        jpegMapButton.text =
            "TEST SNAPSHOT JPEG MAP"

        jpegMapButton.isEnabled =
            false

        jpegMapButton.setOnClickListener {
            runSnapshotJpegMapTests()
        }

        root.addView(
            jpegMapButton
        )

        imageReaderIdButton =
            Button(this)

        imageReaderIdButton.text =
            "TEST BG IMAGE READER ID"

        imageReaderIdButton.isEnabled =
            false

        imageReaderIdButton.setOnClickListener {
            runImageReaderIdTests()
        }

        root.addView(
            imageReaderIdButton
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
                    "VCF Session Map Probe",
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

        try {

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
                        log("")
                        log("Choose a test.")

                        enableButtons()
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

        } catch (e: Throwable) {

            log("OPEN ERROR")
            log(e.toString())
        }
    }

    // =========================================================
    // SENSOR SIZE LIST TESTS
    // =========================================================

    private fun runSensorSizeListTests() {

        disableButtons()

        log("")
        log("")
        log("################################")
        log("TEST: vcf.parameter.sensorSizeList")
        log("################################")

        val tests =
            listOf(
                TestValue(
                    "IntArray 4080x3072",
                    Type.INT_ARRAY,
                    intArrayOf(
                        4080,
                        3072
                    )
                ),

                TestValue(
                    "IntArray 8160x6144",
                    Type.INT_ARRAY,
                    intArrayOf(
                        8160,
                        6144
                    )
                ),

                TestValue(
                    "IntArray 16320x12288",
                    Type.INT_ARRAY,
                    intArrayOf(
                        16320,
                        12288
                    )
                ),

                TestValue(
                    "IntArray MULTI SIZE",
                    Type.INT_ARRAY,
                    intArrayOf(
                        4080,
                        3072,
                        8160,
                        6144,
                        16320,
                        12288
                    )
                ),

                TestValue(
                    "LongArray 16320x12288",
                    Type.LONG_ARRAY,
                    longArrayOf(
                        16320L,
                        12288L
                    )
                ),

                TestValue(
                    "ByteArray test",
                    Type.BYTE_ARRAY,
                    byteArrayOf(
                        1,
                        2,
                        3,
                        4
                    )
                ),

                TestValue(
                    "Integer test",
                    Type.INT,
                    1
                ),

                TestValue(
                    "Long test",
                    Type.LONG,
                    1L
                )
            )

        runTestSequence(
            sensorSizeListName,
            tests,
            0
        )
    }

    // =========================================================
    // JPEG STREAM MAP TESTS
    // =========================================================

    private fun runSnapshotJpegMapTests() {

        disableButtons()

        log("")
        log("")
        log("################################")
        log("TEST: vcf.parameter.SnapshotJpegStreamMap")
        log("################################")

        val tests =
            listOf(

                TestValue(
                    "IntArray [0]",
                    Type.INT_ARRAY,
                    intArrayOf(
                        0
                    )
                ),

                TestValue(
                    "IntArray [1]",
                    Type.INT_ARRAY,
                    intArrayOf(
                        1
                    )
                ),

                TestValue(
                    "IntArray [0,1]",
                    Type.INT_ARRAY,
                    intArrayOf(
                        0,
                        1
                    )
                ),

                TestValue(
                    "IntArray size only",
                    Type.INT_ARRAY,
                    intArrayOf(
                        4080,
                        3072
                    )
                ),

                TestValue(
                    "IntArray size + format",
                    Type.INT_ARRAY,
                    intArrayOf(
                        4080,
                        3072,
                        256
                    )
                ),

                TestValue(
                    "IntArray stream + size + JPEG",
                    Type.INT_ARRAY,
                    intArrayOf(
                        0,
                        4080,
                        3072,
                        256
                    )
                ),

                TestValue(
                    "IntArray 200MP candidate",
                    Type.INT_ARRAY,
                    intArrayOf(
                        0,
                        16320,
                        12288,
                        256
                    )
                ),

                TestValue(
                    "LongArray map",
                    Type.LONG_ARRAY,
                    longArrayOf(
                        0,
                        4080,
                        3072,
                        256
                    )
                ),

                TestValue(
                    "ByteArray map",
                    Type.BYTE_ARRAY,
                    byteArrayOf(
                        0,
                        1,
                        2,
                        3
                    )
                ),

                TestValue(
                    "Integer map",
                    Type.INT,
                    1
                ),

                TestValue(
                    "Long map",
                    Type.LONG,
                    1L
                )
            )

        runTestSequence(
            snapshotJpegMapName,
            tests,
            0
        )
    }

    // =========================================================
    // IMAGE READER ID TESTS
    // =========================================================

    private fun runImageReaderIdTests() {

        disableButtons()

        log("")
        log("")
        log("################################")
        log("TEST: com.mediatek.bgservicefeature.imagereaderid")
        log("################################")

        val tests =
            listOf(

                TestValue(
                    "Integer 0",
                    Type.INT,
                    0
                ),

                TestValue(
                    "Integer 1",
                    Type.INT,
                    1
                ),

                TestValue(
                    "Integer 10",
                    Type.INT,
                    10
                ),

                TestValue(
                    "Long 0",
                    Type.LONG,
                    0L
                ),

                TestValue(
                    "Long 1",
                    Type.LONG,
                    1L
                ),

                TestValue(
                    "IntArray [0]",
                    Type.INT_ARRAY,
                    intArrayOf(
                        0
                    )
                ),

                TestValue(
                    "IntArray [1]",
                    Type.INT_ARRAY,
                    intArrayOf(
                        1
                    )
                ),

                TestValue(
                    "LongArray [1]",
                    Type.LONG_ARRAY,
                    longArrayOf(
                        1L
                    )
                )
            )

        runTestSequence(
            bgImageReaderIdName,
            tests,
            0
        )
    }

    // =========================================================
    // SEQUENTIAL TEST RUNNER
    // =========================================================

    private fun runTestSequence(
        keyName: String,
        tests: List<TestValue>,
        index: Int
    ) {

        if (
            index >=
            tests.size
        ) {

            log("")
            log("==============================")
            log("TEST SET COMPLETE")
            log("==============================")

            enableButtons()

            return
        }

        val test =
            tests[index]

        log("")
        log("--------------------------------")
        log("TEST ${index + 1}/${tests.size}")
        log("--------------------------------")

        log(
            "Key: $keyName"
        )

        log(
            "Value test: ${test.label}"
        )

        createSessionWithTestValue(
            keyName,
            test
        ) {

            cameraHandler.postDelayed(
                {

                    runTestSequence(
                        keyName,
                        tests,
                        index + 1
                    )

                },
                500L
            )
        }
    }

    // =========================================================
    // CREATE SESSION
    // =========================================================

    private fun createSessionWithTestValue(
        keyName: String,
        test: TestValue,
        finished: () -> Unit
    ) {

        val camera =
            cameraDevice

        if (camera == null) {

            log("Camera not open.")

            finished()

            return
        }

        closeCurrentSession()

        try {

            imageReader =
                ImageReader.newInstance(
                    WIDTH,
                    HEIGHT,
                    ImageFormat.RAW_SENSOR,
                    2
                )

        } catch (e: Throwable) {

            log(
                "ImageReader creation failed."
            )

            log(
                e.toString()
            )

            finished()

            return
        }

        val surface =
            imageReader!!.surface

        val builder =
            try {

                camera.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE
                )

            } catch (e: Throwable) {

                log(
                    "createCaptureRequest failed."
                )

                finished()

                return
            }

        builder.addTarget(
            surface
        )

        // -----------------------------------------------------
        // ATTEMPT TYPE SET
        // -----------------------------------------------------

        val setSuccess =
            attemptSet(
                builder,
                keyName,
                test
            )

        if (!setSuccess) {

            log(
                "RESULT: builder.set() rejected this type/value."
            )

            closeCurrentSession()

            finished()

            return
        }

        log(
            "builder.set(): ACCEPTED"
        )

        // -----------------------------------------------------
        // SESSION CREATION
        // -----------------------------------------------------

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

                    log(
                        "HAL accepted this value during session creation."
                    )

                    cameraHandler.postDelayed(
                        {

                            closeCurrentSession()

                            finished()

                        },
                        300L
                    )
                }

                override fun onConfigureFailed(
                    session:
                        CameraCaptureSession
                ) {

                    log(
                        "SESSION RESULT: CONFIGURE FAILED"
                    )

                    log(
                        "HAL rejected configuration after builder.set()."
                    )

                    closeCurrentSession()

                    finished()
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
                        Executor {
                                runnable ->

                            cameraHandler.post(
                                runnable
                            )
                        },
                        callback
                    )

                config.setSessionParameters(
                    builder.build()
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
                "SESSION CREATION EXCEPTION"
            )

            log(
                e.javaClass.simpleName +
                    ": " +
                    (e.message ?: "")
            )

            closeCurrentSession()

            finished()
        }
    }

    // =========================================================
    // SET DIFFERENT JAVA TYPES
    // =========================================================

    private fun attemptSet(
        builder: CaptureRequest.Builder,
        keyName: String,
        test: TestValue
    ): Boolean {

        return try {

            when (test.type) {

                Type.INT_ARRAY -> {

                    val key =
                        CaptureRequest.Key(
                            keyName,
                            IntArray::class.java
                        )

                    builder.set(
                        key,
                        test.value as IntArray
                    )
                }

                Type.LONG_ARRAY -> {

                    val key =
                        CaptureRequest.Key(
                            keyName,
                            LongArray::class.java
                        )

                    builder.set(
                        key,
                        test.value as LongArray
                    )
                }

                Type.BYTE_ARRAY -> {

                    val key =
                        CaptureRequest.Key(
                            keyName,
                            ByteArray::class.java
                        )

                    builder.set(
                        key,
                        test.value as ByteArray
                    )
                }

                Type.INT -> {

                    val key =
                        CaptureRequest.Key(
                            keyName,
                            Int::class.javaObjectType
                        )

                    builder.set(
                        key,
                        test.value as Int
                    )
                }

                Type.LONG -> {

                    val key =
                        CaptureRequest.Key(
                            keyName,
                            Long::class.javaObjectType
                        )

                    builder.set(
                        key,
                        test.value as Long
                    )
                }
            }

            true

        } catch (e: Throwable) {

            log(
                "builder.set() EXCEPTION"
            )

            log(
                e.javaClass.simpleName +
                    ": " +
                    (e.message ?: "")
            )

            false
        }
    }

    // =========================================================
    // DATA CLASS
    // =========================================================

    private data class TestValue(
        val label: String,
        val type: Type,
        val value: Any
    )

    private enum class Type {
        INT_ARRAY,
        LONG_ARRAY,
        BYTE_ARRAY,
        INT,
        LONG
    }

    // =========================================================
    // HELPERS
    // =========================================================

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

    private fun disableButtons() {

        runOnUiThread {

            sensorSizeButton.isEnabled =
                false

            jpegMapButton.isEnabled =
                false

            imageReaderIdButton.isEnabled =
                false
        }
    }

    private fun enableButtons() {

        runOnUiThread {

            sensorSizeButton.isEnabled =
                true

            jpegMapButton.isEnabled =
                true

            imageReaderIdButton.isEnabled =
                true
        }
    }

    private fun startCameraThread() {

        cameraThread =
            HandlerThread(
                "VcfSessionMapProbe"
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
