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
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream
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
    private lateinit var startButton: Button

    private val advanceValues = intArrayOf(0, 1)
    private val remosaicValues = intArrayOf(0, 1)
    private val seamlessValues = intArrayOf(0, 1)
    private val engineerValues = intArrayOf(0, 1, 2)

    private var testIndex = 0
    private var testRunning = false
    private var waitingForImage = false

    private var currentAdvance = 0
    private var currentRemosaic = 0
    private var currentSeamless = 0
    private var currentEngineer = 0

    // ---------------------------------------------------------
    // Vivo controls
    // ---------------------------------------------------------

    private val aiHighResolutionKey =
        CaptureRequest.Key(
            "vivo.control.ai_highresolution",
            Int::class.javaObjectType
        )

    private val portraitHighResolutionKey =
        CaptureRequest.Key(
            "vivo.control.portrait_high_resolution",
            Byte::class.javaObjectType
        )

    private val ultraHighResolutionKey =
        CaptureRequest.Key(
            "vivo.control.ultra_highresolution",
            Int::class.javaObjectType
        )

    private val real200mpKey =
        CaptureRequest.Key(
            "vivo.control.real200mp_switch_on",
            Int::class.javaObjectType
        )

    private val streamsUsageKey =
        CaptureRequest.Key(
            "vivo.control.streamsUsage",
            IntArray::class.java
        )

    // ---------------------------------------------------------
    // Full-size / remosaic controls found in the stock dump
    // ---------------------------------------------------------

    private val advanceFullsizeKey =
        CaptureRequest.Key(
            "vivo.control.advance_fullsize",
            Int::class.javaObjectType
        )

    private val remosaicEnableKey =
        CaptureRequest.Key(
            "com.mediatek.control.capture.remosaicenable",
            Int::class.javaObjectType
        )

    private val seamlessRemosaicEnableKey =
        CaptureRequest.Key(
            "com.mediatek.control.capture.seamless.remosaicenable",
            Int::class.javaObjectType
        )

    private val engineerRemosaicModeKey =
        CaptureRequest.Key(
            "vivo.control.EngineerRemosaicMode",
            Int::class.javaObjectType
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createUi()
        startBackgroundThread()

        cameraManager =
            getSystemService(CAMERA_SERVICE) as CameraManager

        log("VIVO 200 MP REMOSAIC MATRIX TEST")
        log("==============================")
        log("")
        log("Camera ID: $CAMERA_ID")
        log("Requested JPEG: $WIDTH x $HEIGHT")
        log("")
        log("24 combinations:")
        log("advance_fullsize = [0, 1]")
        log("remosaicenable = [0, 1]")
        log("seamless.remosaicenable = [0, 1]")
        log("EngineerRemosaicMode = [0, 1, 2]")
        log("")

        startButton.isEnabled = false

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

    private fun createUi() {

        val root =
            LinearLayout(this)

        root.orientation =
            LinearLayout.VERTICAL

        root.setPadding(
            20,
            20,
            20,
            20
        )

        startButton =
            Button(this)

        startButton.text =
            "START REMOSAIC TEST"

        startButton.setOnClickListener {

            if (!testRunning) {
                startMatrixTest()
            }
        }

        root.addView(
            startButton
        )

        scrollView =
            ScrollView(this)

        logText =
            TextView(this)

        logText.textSize =
            15f

        logText.setPadding(
            0,
            20,
            0,
            120
        )

        scrollView.addView(
            logText
        )

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
    }

    private fun initializeCamera() {

        log("STEP 1")
        log("Creating 200 MP ImageReader...")
        log("")

        createImageReader()
        openCamera()
    }

    private fun createImageReader() {

        try {

            jpegReader =
                ImageReader.newInstance(
                    WIDTH,
                    HEIGHT,
                    ImageFormat.JPEG,
                    2
                )

            jpegReader
                ?.setOnImageAvailableListener(
                    { reader ->

                        var image: Image? = null

                        try {

                            image =
                                reader.acquireNextImage()

                            if (image == null) {

                                log(
                                    "ImageReader returned null."
                                )

                                onTestFinished(
                                    false,
                                    0,
                                    0
                                )

                                return@setOnImageAvailableListener
                            }

                            processImage(
                                image
                            )

                        } catch (
                            e: Throwable
                        ) {

                            log("")
                            log("IMAGE ERROR")
                            log(
                                e.javaClass.simpleName
                            )
                            log(
                                e.message ?: ""
                            )

                            onTestFinished(
                                false,
                                0,
                                0
                            )

                        } finally {

                            try {
                                image?.close()
                            } catch (_: Throwable) {
                            }
                        }

                    },

                    backgroundHandler
                )

            log(
                "ImageReader created:"
            )

            log(
                "$WIDTH x $HEIGHT"
            )

        } catch (
            e: Throwable
        ) {

            log(
                "ImageReader failed."
            )

            log(
                e.toString()
            )
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
        log("STEP 2")
        log("Opening Camera 3...")

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

                        log(
                            "SUCCESS: Camera 3 opened."
                        )

                        createCaptureSession()
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

                        log("")
                        log(
                            "CAMERA ERROR: $error"
                        )

                        camera.close()

                        cameraDevice =
                            null
                    }
                },

                backgroundHandler
            )

        } catch (
            e: Throwable
        ) {

            log(
                "Open camera failed."
            )

            log(
                e.toString()
            )
        }
    }

    private fun createCaptureSession() {

        val camera =
            cameraDevice ?: return

        val reader =
            jpegReader ?: return

        log("")
        log("STEP 3")
        log("Creating still session...")

        try {

            camera.createCaptureSession(
                listOf(
                    reader.surface
                ),

                object :
                    CameraCaptureSession.StateCallback() {

                    override fun onConfigured(
                        session: CameraCaptureSession
                    ) {

                        captureSession =
                            session

                        log("")
                        log(
                            "SESSION CONFIGURED"
                        )

                        log("")
                        log(
                            "Ready to test 24 combinations."
                        )

                        runOnUiThread {

                            startButton
                                .isEnabled =
                                true
                        }
                    }

                    override fun onConfigureFailed(
                        session: CameraCaptureSession
                    ) {

                        log(
                            "SESSION CONFIGURATION FAILED"
                        )
                    }
                },

                backgroundHandler
            )

        } catch (
            e: Throwable
        ) {

            log(
                "Session creation error."
            )

            log(
                e.toString()
            )
        }
    }

    private fun startMatrixTest() {
        testIndex = 0
        testRunning = true
        waitingForImage = false
        runOnUiThread { startButton.isEnabled = false }
        log("")
        log("==============================")
        log("STARTING REMOSAIC MATRIX")
        log("==============================")
        runCurrentCombination()
    }

    private fun runCurrentCombination() {
        if (!testRunning) return
        if (testIndex >= 24) { finishMatrix(); return }

        var n = testIndex
        currentEngineer = engineerValues[n % engineerValues.size]
        n /= engineerValues.size
        currentSeamless = seamlessValues[n % seamlessValues.size]
        n /= seamlessValues.size
        currentRemosaic = remosaicValues[n % remosaicValues.size]
        n /= remosaicValues.size
        currentAdvance = advanceValues[n % advanceValues.size]

        log("")
        log("================================")
        log("TEST ${testIndex + 1}/24")
        log("advance_fullsize = $currentAdvance")
        log("remosaicenable = $currentRemosaic")
        log("seamless.remosaicenable = $currentSeamless")
        log("EngineerRemosaicMode = $currentEngineer")
        log("================================")
        captureCombination()
    }

    private fun captureCombination() {
        val camera = cameraDevice
        val session = captureSession
        val reader = jpegReader
        if (camera == null || session == null || reader == null) { nextCombination(); return }

        try {
            val builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            builder.addTarget(reader.surface)
            builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            builder.set(CaptureRequest.JPEG_QUALITY, 100.toByte())

            applyVivoControls(builder)
            setIntKey(builder, advanceFullsizeKey, currentAdvance, "advance_fullsize")
            setIntKey(builder, remosaicEnableKey, currentRemosaic, "remosaicenable")
            setIntKey(builder, seamlessRemosaicEnableKey, currentSeamless, "seamless.remosaicenable")
            setIntKey(builder, engineerRemosaicModeKey, currentEngineer, "EngineerRemosaicMode")

            waitingForImage = true
            session.capture(builder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureStarted(session: CameraCaptureSession, request: CaptureRequest, timestamp: Long, frameNumber: Long) {
                    log("Capture started. Frame: $frameNumber")
                }
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    log("Capture request completed.")
                }
                override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) {
                    log("CAPTURE FAILED: ${failure.reason}")
                    waitingForImage = false
                    backgroundHandler.postDelayed({ nextCombination() }, 500)
                }
            }, backgroundHandler)
        } catch (e: Throwable) {
            log("CAPTURE EXCEPTION: ${e.javaClass.simpleName}: ${e.message ?: ""}")
            waitingForImage = false
            backgroundHandler.postDelayed({ nextCombination() }, 500)
        }
    }

    private fun setIntKey(builder: CaptureRequest.Builder, key: CaptureRequest.Key<Int>, value: Int, label: String) {
        try {
            builder.set(key, value)
            log("$label = $value")
        } catch (e: Throwable) {
            log("$label FAILED: ${e.message ?: ""}")
        }
    }

    private fun applyVivoControls(
        builder:
            CaptureRequest.Builder
    ) {

        try {

            builder.set(
                aiHighResolutionKey,
                0
            )

            log(
                "ai_highresolution = 0"
            )

        } catch (
            e: Throwable
        ) {

            log(
                "ai_highresolution FAILED"
            )
        }

        try {

            builder.set(
                portraitHighResolutionKey,
                1.toByte()
            )

            log(
                "portrait_high_resolution = 1"
            )

        } catch (
            e: Throwable
        ) {

            log(
                "portrait_high_resolution FAILED"
            )
        }

        try {

            builder.set(
                ultraHighResolutionKey,
                1
            )

            log(
                "ultra_highresolution = 1"
            )

        } catch (
            e: Throwable
        ) {

            log(
                "ultra_highresolution FAILED"
            )
        }

        try {

            builder.set(
                real200mpKey,
                1
            )

            log(
                "real200mp_switch_on = 1"
            )

        } catch (
            e: Throwable
        ) {

            log(
                "real200mp_switch_on FAILED"
            )
        }

        try {

            builder.set(
                streamsUsageKey,
                intArrayOf(
                    2,
                    1,
                    0
                )
            )

            log(
                "streamsUsage = [2, 1, 0]"
            )

        } catch (
            e: Throwable
        ) {

            log(
                "streamsUsage FAILED"
            )
        }
    }

    private fun processImage(
        image: Image
    ) {

        if (
            !waitingForImage
        ) {

            log(
                "Unexpected image received."
            )

            return
        }

        waitingForImage =
            false

        val buffer =
            image
                .planes[0]
                .buffer

        val bytes =
            ByteArray(
                buffer.remaining()
            )

        buffer.get(
            bytes
        )

        val dims =
            readJpegDimensions(
                bytes
            )

        log("")
        log(
            "JPEG RESULT"
        )

        log(
            "ImageReader = " +
                "${image.width} x ${image.height}"
        )

        log(
            "Bytes = ${bytes.size}"
        )

        if (
            dims == null
        ) {

            log(
                "JPEG SOF could not be parsed."
            )

            saveTestImage(
                bytes,
                currentAdvance,
                currentRemosaic,
                currentSeamless,
                currentEngineer,
                0,
                0
            )

            onTestFinished(
                false,
                0,
                0
            )

            return
        }

        val width =
            dims.first

        val height =
            dims.second

        val mp =
            width.toDouble() *
                height.toDouble() /
                1_000_000.0

        log(
            "JPEG SOF = $width x $height"
        )

        log(
            "JPEG MP = ${
                String.format(
                    Locale.US,
                    "%.2f",
                    mp
                )
            }"
        )

        saveTestImage(
            bytes,
            currentAdvance,
            currentRemosaic,
            currentSeamless,
            currentEngineer,
            width,
            height
        )

        val success =
            width == WIDTH &&
                height == HEIGHT

        if (
            success
        ) {

            log("")
            log(
                "********************************"
            )

            log(
                "FOUND REAL 200 MP MODE"
            )

            log(
                "********************************"
            )

            log("advance_fullsize = $currentAdvance")
            log("remosaicenable = $currentRemosaic")
            log("seamless.remosaicenable = $currentSeamless")
            log("EngineerRemosaicMode = $currentEngineer")

            log(
                "$width x $height"
            )

            testRunning =
                false

            runOnUiThread {

                startButton.text =
                    "200 MP FOUND"

                startButton
                    .isEnabled =
                    false
            }

        } else {

            onTestFinished(
                false,
                width,
                height
            )
        }
    }

    private fun onTestFinished(
        success: Boolean,
        width: Int,
        height: Int
    ) {

        if (
            success
        ) {
            return
        }

        backgroundHandler.postDelayed(
            {
                nextCombination()
            },
            750
        )
    }

    private fun nextCombination() {
        testIndex++
        if (testIndex >= 24) finishMatrix() else runCurrentCombination()
    }

    private fun finishMatrix() {

        testRunning =
            false

        log("")
        log("")
        log("==============================")
        log("MATRIX COMPLETE")
        log("==============================")

        log("")
        log(
            "No 16320 x 12288 JPEG was found"
        )

        log(
            "in the tested combinations."
        )

        runOnUiThread {

            startButton.text =
                "TEST COMPLETE"

            startButton
                .isEnabled =
                true
        }
    }

    private fun saveTestImage(
        bytes:
            ByteArray,
        advance:
            Int,
        remosaic:
            Int,
        seamless:
            Int,
        engineer:
            Int,
        width:
            Int,
        height:
            Int
    ) {

        try {

            val directory =
                getExternalFilesDir(
                    Environment
                        .DIRECTORY_PICTURES
                )
                    ?: return

            if (
                !directory.exists()
            ) {

                directory.mkdirs()
            }

            val timestamp =
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss_SSS",
                    Locale.US
                ).format(
                    Date()
                )

            val file =
                File(
                    directory,
                    "A${advance}_R${remosaic}_S${seamless}_E${engineer}_${width}x${height}_$timestamp.jpg"
                )

            FileOutputStream(
                file
            ).use {

                it.write(
                    bytes
                )
            }

            log(
                "Saved: ${file.name}"
            )

        } catch (
            e: Throwable
        ) {

            log(
                "Save failed: ${e.message}"
            )
        }
    }

    private fun readJpegDimensions(
        data:
            ByteArray
    ): Pair<Int, Int>? {

        if (
            data.size < 4
        ) {
            return null
        }

        if (
            (data[0].toInt() and 0xFF) != 0xFF ||
            (data[1].toInt() and 0xFF) != 0xD8
        ) {
            return null
        }

        var offset = 2

        while (
            offset <
            data.size - 1
        ) {

            if (
                (data[offset].toInt() and 0xFF) !=
                0xFF
            ) {

                offset++
                continue
            }

            while (
                offset <
                data.size &&
                (data[offset].toInt() and 0xFF) ==
                0xFF
            ) {

                offset++
            }

            if (
                offset >=
                data.size
            ) {
                break
            }

            val marker =
                data[offset]
                    .toInt() and
                    0xFF

            offset++

            if (
                marker == 0xD8 ||
                marker == 0xD9 ||
                marker in
                    0xD0..0xD7 ||
                marker == 0x01
            ) {
                continue
            }

            if (
                marker == 0xDA
            ) {
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
                    (
                        data[offset]
                            .toInt() and
                            0xFF
                        ) shl 8
                    ) or
                    (
                        data[offset + 1]
                            .toInt() and
                            0xFF
                        )

            if (
                length < 2
            ) {
                return null
            }

            if (
                isSofMarker(
                    marker
                )
            ) {

                if (
                    offset + 6 >=
                    data.size
                ) {
                    return null
                }

                val height =
                    (
                        (
                            data[offset + 3]
                                .toInt() and
                                0xFF
                            ) shl 8
                        ) or
                        (
                            data[offset + 4]
                                .toInt() and
                                0xFF
                            )

                val width =
                    (
                        (
                            data[offset + 5]
                                .toInt() and
                                0xFF
                            ) shl 8
                        ) or
                        (
                            data[offset + 6]
                                .toInt() and
                                0xFF
                            )

                return Pair(
                    width,
                    height
                )
            }

            offset +=
                length
        }

        return null
    }

    private fun isSofMarker(
        marker:
            Int
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

    private fun log(
        message:
            String
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

    private fun startBackgroundThread() {

        backgroundThread =
            HandlerThread(
                "VivoSensorMatrix"
            )

        backgroundThread.start()

        backgroundHandler =
            Handler(
                backgroundThread.looper
            )
    }

    private fun stopBackgroundThread() {

        try {

            backgroundThread
                .quitSafely()

            backgroundThread
                .join()

        } catch (
            _: Throwable
        ) {
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

        super
            .onRequestPermissionsResult(
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

        testRunning =
            false

        try {
            captureSession?.close()
        } catch (_: Throwable) {
        }

        try {
            cameraDevice?.close()
        } catch (_: Throwable) {
        }

        try {
            jpegReader?.close()
        } catch (_: Throwable) {
        }

        stopBackgroundThread()

        super.onDestroy()
    }
}
