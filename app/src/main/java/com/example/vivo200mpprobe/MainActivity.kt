package com.example.vivo200mpprobe

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_ID = "3"

        private const val WIDTH = 16320
        private const val HEIGHT = 12288

        private const val REQUEST_CAMERA = 1001
    }

    private lateinit var cameraManager: CameraManager

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var jpegReader: ImageReader? = null

    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler

    private lateinit var logText: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var captureButton: Button

    // ---------------------------------------------------------
    // Vendor key names
    // ---------------------------------------------------------

    private val KEY_AI_HIGH_RES =
        "com.vivo.camera.ai_highresolution"

    private val KEY_PORTRAIT_HIGH_RES =
        "com.vivo.camera.portrait_high_resolution"

    private val KEY_ULTRA_HIGH_RES =
        "com.vivo.camera.ultra_highresolution"

    private val KEY_REAL_200MP =
        "com.vivo.camera.real200mp_switch_on"

    private val KEY_STREAM_USAGE =
        "com.vivo.camera.streamsUsage"

    private val KEY_CAMERA_ID =
        "com.vivo.camera.camera_id"

    // MediaTek seamless sensor controls
    private val KEY_SENSOR_SCENARIO =
        "com.mediatek.seamlessfeature.sensorScenario"

    private val KEY_FORCE_SENSOR_MODE =
        "com.mediatek.seamlessfeature.forceSensorMode"

    // ---------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createUi()

        cameraManager =
            getSystemService(CAMERA_SERVICE) as CameraManager

        startBackgroundThread()

        log("")
        log("VIVO 200 MP SENSOR MODE TEST")
        log("============================")
        log("")
        log("Camera ID: $CAMERA_ID")
        log("Target: $WIDTH x $HEIGHT")
        log("Target MP: %.2f".format(WIDTH * HEIGHT / 1_000_000.0))
        log("")
        log("NEW TEST:")
        log("MediaTek sensorScenario + forceSensorMode")
        log("")

        captureButton.isEnabled = false

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA
            )
        } else {
            initializeCamera()
        }
    }

    // =========================================================
    // UI
    // =========================================================

    private fun createUi() {

        val root =
            android.widget.LinearLayout(this)

        root.orientation =
            android.widget.LinearLayout.VERTICAL

        root.setPadding(20, 20, 20, 20)

        captureButton = Button(this)
        captureButton.text = "CAPTURE 200 MP"

        captureButton.setOnClickListener {
            capture200MP()
        }

        root.addView(
            captureButton,
            android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        scrollView = ScrollView(this)

        logText = TextView(this)

        logText.textSize = 16f
        logText.setPadding(0, 20, 0, 100)

        scrollView.addView(logText)

        root.addView(
            scrollView,
            android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
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

        log("STEP 1 - CAMERA CHARACTERISTICS")
        log("-------------------------------")

        try {

            val characteristics =
                cameraManager.getCameraCharacteristics(CAMERA_ID)

            log("Camera $CAMERA_ID found.")

            val focal =
                characteristics.get(
                    CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
                )

            if (focal != null) {
                log(
                    "Focal lengths: ${
                        focal.joinToString()
                    }"
                )
            }

            val map =
                characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                )

            val publicSizes =
                map?.getOutputSizes(ImageFormat.JPEG)

            if (publicSizes != null) {

                val largest =
                    publicSizes.maxByOrNull {
                        it.width.toLong() * it.height
                    }

                if (largest != null) {
                    log(
                        "Largest PUBLIC JPEG: " +
                            "${largest.width} x ${largest.height}"
                    )
                }
            }

            log("")
            log("Creating forced ImageReader:")
            log("$WIDTH x $HEIGHT JPEG")

            createImageReader()

            openCamera()

        } catch (e: Exception) {

            log("")
            log("INITIALIZATION ERROR")
            log(e.javaClass.simpleName)
            log(e.message ?: "unknown")
        }
    }

    // =========================================================
    // IMAGE READER
    // =========================================================

    private fun createImageReader() {

        try {

            jpegReader =
                ImageReader.newInstance(
                    WIDTH,
                    HEIGHT,
                    ImageFormat.JPEG,
                    2
                )

            jpegReader?.setOnImageAvailableListener(
                { reader ->

                    var image: Image? = null

                    try {

                        image =
                            reader.acquireLatestImage()

                        if (image == null) {
                            log("ImageReader returned null image.")
                            return@setOnImageAvailableListener
                        }

                        processImage(image)

                    } catch (e: Exception) {

                        log("")
                        log("IMAGE ERROR:")
                        log(e.toString())

                    } finally {

                        image?.close()
                    }

                },
                backgroundHandler
            )

            log("ImageReader CREATED.")
            log("$WIDTH x $HEIGHT")
            log("")

        } catch (e: Exception) {

            log("ImageReader creation FAILED.")
            log(e.toString())
        }
    }

    // =========================================================
    // OPEN CAMERA
    // =========================================================

    private fun openCamera() {

        log("STEP 2 - OPEN CAMERA 3")
        log("----------------------")

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        try {

            cameraManager.openCamera(
                CAMERA_ID,

                object : CameraDevice.StateCallback() {

                    override fun onOpened(camera: CameraDevice) {

                        cameraDevice = camera

                        log("SUCCESS: Camera 3 opened.")
                        log("")

                        createCaptureSession()
                    }

                    override fun onDisconnected(camera: CameraDevice) {

                        log("CAMERA DISCONNECTED")

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

                backgroundHandler
            )

        } catch (e: Exception) {

            log("OPEN CAMERA FAILED")
            log(e.toString())
        }
    }

    // =========================================================
    // SESSION
    // =========================================================

    private fun createCaptureSession() {

        log("STEP 3 - CREATE 200 MP SESSION")
        log("------------------------------")

        val camera =
            cameraDevice ?: return

        val reader =
            jpegReader ?: return

        try {

            val outputConfiguration =
                android.hardware.camera2.params.OutputConfiguration(
                    reader.surface
                )

            val sessionConfiguration =
                android.hardware.camera2.params.SessionConfiguration(
                    android.hardware.camera2.params.SessionConfiguration.SESSION_REGULAR,
                    listOf(outputConfiguration),
                    mainExecutor,

                    object :
                        CameraCaptureSession.StateCallback() {

                        override fun onConfigured(
                            session: CameraCaptureSession
                        ) {

                            captureSession = session

                            log("")
                            log("============================")
                            log("SESSION CONFIGURED")
                            log("============================")
                            log("")

                            log("Output:")
                            log("$WIDTH x $HEIGHT JPEG")
                            log("")

                            captureButton.isEnabled = true

                            log("Press CAPTURE 200 MP.")
                        }

                        override fun onConfigureFailed(
                            session: CameraCaptureSession
                        ) {

                            log("")
                            log("SESSION CONFIGURATION FAILED")
                        }
                    }
                )

            // -------------------------------------------------
            // SESSION PARAMETERS
            // -------------------------------------------------

            val sessionBuilder =
                camera.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE
                )

            sessionBuilder.addTarget(reader.surface)

            log("")
            log("SESSION PARAMETERS")
            log("------------------")

            applyCommonVivoKeys(sessionBuilder)

            /*
             * First MediaTek sensor test.
             *
             * Stock metadata showed:
             *
             * sensorScenario = [3, 1]
             *
             * We begin with scenario 3.
             */

            setIntVendorKey(
                sessionBuilder,
                KEY_SENSOR_SCENARIO,
                3
            )

            /*
             * forceSensorMode is the key we specifically
             * want to test now.
             *
             * Start with sensor mode 0 because the sensor
             * mode size list begins with:
             *
             * 0 -> 16320 x 12288
             */

            setIntVendorKey(
                sessionBuilder,
                KEY_FORCE_SENSOR_MODE,
                0
            )

            sessionConfiguration.sessionParameters =
                sessionBuilder.build()

            log("")
            log("Creating session...")

            camera.createCaptureSession(
                sessionConfiguration
            )

        } catch (e: Exception) {

            log("")
            log("SESSION EXCEPTION")
            log(e.javaClass.simpleName)
            log(e.message ?: "unknown")
        }
    }

    // =========================================================
    // CAPTURE
    // =========================================================

    private fun capture200MP() {

        val camera =
            cameraDevice

        val session =
            captureSession

        val reader =
            jpegReader

        if (
            camera == null ||
            session == null ||
            reader == null
        ) {

            log("Camera/session/reader not ready.")
            return
        }

        log("")
        log("")
        log("============================")
        log("200 MP SENSOR MODE REQUEST")
        log("============================")
        log("")

        try {

            val builder =
                camera.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE
                )

            builder.addTarget(reader.surface)

            builder.set(
                CaptureRequest.CONTROL_MODE,
                CameraMetadata.CONTROL_MODE_AUTO
            )

            builder.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )

            builder.set(
                CaptureRequest.JPEG_QUALITY,
                100.toByte()
            )

            log("CAPTURE VIVO CONTROLS")
            log("---------------------")

            applyCommonVivoKeys(builder)

            log("")
            log("MEDIATEK SENSOR CONTROLS")
            log("------------------------")

            setIntVendorKey(
                builder,
                KEY_SENSOR_SCENARIO,
                3
            )

            setIntVendorKey(
                builder,
                KEY_FORCE_SENSOR_MODE,
                0
            )

            log("")
            log("Expected sensor mode:")
            log("Mode 0")
            log("$WIDTH x $HEIGHT")
            log("")

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

                        log("Capture started.")
                        log("Frame: $frameNumber")
                    }

                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {

                        log("")
                        log("Capture request completed.")

                        inspectCaptureResult(result)

                        log("Waiting for JPEG...")
                    }

                    override fun onCaptureFailed(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        failure: CaptureFailure
                    ) {

                        log("")
                        log("CAPTURE FAILED")
                        log("Reason: ${failure.reason}")
                    }
                },

                backgroundHandler
            )

        } catch (e: Exception) {

            log("")
            log("CAPTURE EXCEPTION")
            log(e.javaClass.simpleName)
            log(e.message ?: "unknown")
        }
    }

    // =========================================================
    // COMMON VIVO KEYS
    // =========================================================

    private fun applyCommonVivoKeys(
        builder: CaptureRequest.Builder
    ) {

        setIntVendorKey(
            builder,
            KEY_AI_HIGH_RES,
            0
        )

        setIntVendorKey(
            builder,
            KEY_PORTRAIT_HIGH_RES,
            1
        )

        setIntVendorKey(
            builder,
            KEY_ULTRA_HIGH_RES,
            1
        )

        setIntVendorKey(
            builder,
            KEY_REAL_200MP,
            1
        )

        setIntArrayVendorKey(
            builder,
            KEY_STREAM_USAGE,
            intArrayOf(2, 1, 0)
        )

        setIntVendorKey(
            builder,
            KEY_CAMERA_ID,
            3
        )
    }

    // =========================================================
    // VENDOR KEY HELPERS
    // =========================================================

    private fun setIntVendorKey(
        builder: CaptureRequest.Builder,
        name: String,
        value: Int
    ) {

        try {

            val key =
                CaptureRequest.Key(
                    name,
                    Int::class.javaObjectType
                )

            builder.set(
                key,
                value
            )

            log("OK $name = $value")

        } catch (e: Exception) {

            log("FAIL $name")
            log("  ${e.javaClass.simpleName}")
            log("  ${e.message}")
        }
    }

    private fun setIntArrayVendorKey(
        builder: CaptureRequest.Builder,
        name: String,
        value: IntArray
    ) {

        try {

            val key =
                CaptureRequest.Key(
                    name,
                    IntArray::class.java
                )

            builder.set(
                key,
                value
            )

            log(
                "OK $name = " +
                    value.contentToString()
            )

        } catch (e: Exception) {

            log("FAIL $name")
            log("  ${e.javaClass.simpleName}")
            log("  ${e.message}")
        }
    }

    // =========================================================
    // CAPTURE RESULT
    // =========================================================

    private fun inspectCaptureResult(
        result: TotalCaptureResult
    ) {

        log("")
        log("============================")
        log("CAPTURE RESULT")
        log("============================")

        try {

            for (key in result.keys) {

                val name =
                    key.name

                if (
                    name.contains(
                        "sensorScenario",
                        true
                    ) ||
                    name.contains(
                        "forceSensorMode",
                        true
                    ) ||
                    name.contains(
                        "remosaic",
                        true
                    ) ||
                    name.contains(
                        "highresolution",
                        true
                    ) ||
                    name.contains(
                        "200mp",
                        true
                    )
                ) {

                    try {

                        @Suppress("UNCHECKED_CAST")
                        val value =
                            result.get(
                                key as CaptureResult.Key<Any>
                            )

                        log("$name = ${formatValue(value)}")

                    } catch (_: Exception) {
                    }
                }
            }

        } catch (e: Exception) {

            log("Result inspection error:")
            log(e.toString())
        }
    }

    // =========================================================
    // IMAGE PROCESSING
    // =========================================================

    private fun processImage(
        image: Image
    ) {

        log("")
        log("")
        log("============================")
        log("JPEG RECEIVED")
        log("============================")

        log(
            "ImageReader: " +
                "${image.width} x ${image.height}"
        )

        val buffer =
            image.planes[0].buffer

        val bytes =
            ByteArray(buffer.remaining())

        buffer.get(bytes)

        log("Bytes: ${bytes.size}")

        val dimensions =
            readJpegDimensions(bytes)

        log("")

        if (dimensions != null) {

            val jpegWidth =
                dimensions.first

            val jpegHeight =
                dimensions.second

            val mp =
                jpegWidth.toLong() *
                    jpegHeight.toLong() /
                    1_000_000.0

            log("JPEG SOF:")
            log("$jpegWidth x $jpegHeight")
            log("%.2f MP".format(mp))

            log("")

            if (
                jpegWidth == WIDTH &&
                jpegHeight == HEIGHT
            ) {

                log("============================")
                log("*** REAL 200 MP JPEG ***")
                log("============================")

            } else {

                log("============================")
                log("NOT 200 MP")
                log(
                    "HAL returned " +
                        "$jpegWidth x $jpegHeight"
                )
                log("============================")
            }

        } else {

            log("Could not locate JPEG SOF marker.")
        }

        saveJpeg(bytes)
    }

    // =========================================================
    // SAVE JPEG
    // =========================================================

    private fun saveJpeg(
        bytes: ByteArray
    ) {

        try {

            val directory =
                getExternalFilesDir(
                    Environment.DIRECTORY_PICTURES
                )

            if (directory == null) {

                log("Picture directory unavailable.")
                return
            }

            if (!directory.exists()) {
                directory.mkdirs()
            }

            val timestamp =
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.US
                ).format(Date())

            val file =
                File(
                    directory,
                    "Vivo_SensorMode_200MP_$timestamp.jpg"
                )

            FileOutputStream(file).use {
                it.write(bytes)
            }

            log("")
            log("============================")
            log("JPEG SAVED")
            log("============================")
            log(file.absolutePath)

            log(
                "File size: %.3f MB".format(
                    file.length() /
                        1024.0 /
                        1024.0
                )
            )

        } catch (e: Exception) {

            log("")
            log("SAVE FAILED")
            log(e.toString())
        }
    }

    // =========================================================
    // JPEG SOF PARSER
    // =========================================================

    private fun readJpegDimensions(
        data: ByteArray
    ): Pair<Int, Int>? {

        if (data.size < 4) {
            return null
        }

        if (
            data[0].toInt() and 0xFF != 0xFF ||
            data[1].toInt() and 0xFF != 0xD8
        ) {
            return null
        }

        var offset = 2

        while (
            offset + 9 <
            data.size
        ) {

            if (
                data[offset].toInt() and 0xFF
                != 0xFF
            ) {

                offset++
                continue
            }

            while (
                offset < data.size &&
                data[offset].toInt() and 0xFF ==
                0xFF
            ) {
                offset++
            }

            if (offset >= data.size) {
                break
            }

            val marker =
                data[offset].toInt() and 0xFF

            offset++

            if (
                marker == 0xD8 ||
                marker == 0xD9
            ) {
                continue
            }

            if (marker == 0xDA) {
                break
            }

            if (
                offset + 1 >=
                data.size
            ) {
                break
            }

            val length =
                (
                    (data[offset].toInt() and 0xFF)
                        shl 8
                    ) or
                    (
                        data[offset + 1].toInt()
                            and 0xFF
                        )

            if (length < 2) {
                break
            }

            if (isSofMarker(marker)) {

                if (
                    offset + 7 >=
                    data.size
                ) {
                    return null
                }

                val height =
                    (
                        (
                            data[offset + 3].toInt()
                                and 0xFF
                            ) shl 8
                        ) or
                        (
                            data[offset + 4].toInt()
                                and 0xFF
                            )

                val width =
                    (
                        (
                            data[offset + 5].toInt()
                                and 0xFF
                            ) shl 8
                        ) or
                        (
                            data[offset + 6].toInt()
                                and 0xFF
                            )

                return Pair(
                    width,
                    height
                )
            }

            offset += length
        }

        return null
    }

    private fun isSofMarker(
        marker: Int
    ): Boolean {

        return marker == 0xC0 ||
            marker == 0xC1 ||
            marker == 0xC2 ||
            marker == 0xC3 ||
            marker == 0xC5 ||
            marker == 0xC6 ||
            marker == 0xC7 ||
            marker == 0xC9 ||
            marker == 0xCA ||
            marker == 0xCB ||
            marker == 0xCD ||
            marker == 0xCE ||
            marker == 0xCF
    }

    // =========================================================
    // VALUE FORMATTER
    // =========================================================

    private fun formatValue(
        value: Any?
    ): String {

        return when (value) {

            null ->
                "null"

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

    // =========================================================
    // LOGGING
    // =========================================================

    private fun log(
        message: String
    ) {

        runOnUiThread {

            logText.append(
                message + "\n"
            )

            scrollView.post {
                scrollView.fullScroll(
                    View.FOCUS_DOWN
                )
            }
        }
    }

    // =========================================================
    // BACKGROUND THREAD
    // =========================================================

    private fun startBackgroundThread() {

        backgroundThread =
            HandlerThread(
                "Vivo200MPCamera"
            )

        backgroundThread.start()

        backgroundHandler =
            Handler(
                backgroundThread.looper
            )
    }

    private fun stopBackgroundThread() {

        try {

            backgroundThread.quitSafely()
            backgroundThread.join()

        } catch (_: Exception) {
        }
    }

    // =========================================================
    // PERMISSION
    // =========================================================

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
            REQUEST_CAMERA
        ) {

            if (
                grantResults.isNotEmpty() &&
                grantResults[0] ==
                PackageManager.PERMISSION_GRANTED
            ) {

                initializeCamera()

            } else {

                log("Camera permission denied.")
            }
        }
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    override fun onDestroy() {

        try {
            captureSession?.close()
        } catch (_: Exception) {
        }

        try {
            cameraDevice?.close()
        } catch (_: Exception) {
        }

        try {
            jpegReader?.close()
        } catch (_: Exception) {
        }

        stopBackgroundThread()

        super.onDestroy()
    }
}
