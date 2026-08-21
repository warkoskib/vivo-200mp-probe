package com.example.vivo200mpprobe

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
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
        private const val CAMERA_PERMISSION_REQUEST = 1001

        private const val CAMERA_ID = "3"

        private const val FULL_WIDTH = 16320
        private const val FULL_HEIGHT = 12288
    }

    private lateinit var statusText: TextView
    private lateinit var captureButton: Button

    private lateinit var cameraThread: HandlerThread
    private lateinit var cameraHandler: Handler

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var fullResReader: ImageReader? = null

    private var sessionReady = false

    private val ultraHighResolutionKey =
        CaptureRequest.Key(
            "vivo.control.ultra_highresolution",
            Int::class.javaObjectType
        )

    private val aiHighResolutionKey =
        CaptureRequest.Key(
            "vivo.control.ai_highresolution",
            Int::class.javaObjectType
        )

    private val real200mpKey =
        CaptureRequest.Key(
            "vivo.control.real200mp_switch_on",
            Int::class.javaObjectType
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildUi()
        startCameraThread()

        log("VIVO 200 MP - NO PREVIEW TEST")
        log("==============================")
        log("")
        log("Camera ID: $CAMERA_ID")
        log("Capture: $FULL_WIDTH x $FULL_HEIGHT")
        log("")

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startTest()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        }
    }

    private fun buildUi() {

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(24, 40, 24, 40)

        captureButton = Button(this)
        captureButton.text = "CAPTURE 200 MP"
        captureButton.isEnabled = false

        statusText = TextView(this)
        statusText.textSize = 13f

        captureButton.setOnClickListener {
            capture200Mp()
        }

        root.addView(captureButton)

        val scroll = ScrollView(this)
        scroll.addView(statusText)

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

    private fun log(message: String) {

        runOnUiThread {
            statusText.append(message)
            statusText.append("\n")
        }
    }

    private fun startCameraThread() {

        cameraThread = HandlerThread("Vivo200MpThread")
        cameraThread.start()

        cameraHandler = Handler(cameraThread.looper)
    }

    private fun startTest() {

        log("STEP 1 - CREATE 200 MP READER")
        log("------------------------------")

        try {

            fullResReader =
                ImageReader.newInstance(
                    FULL_WIDTH,
                    FULL_HEIGHT,
                    ImageFormat.JPEG,
                    1
                )

            fullResReader?.setOnImageAvailableListener(
                { reader ->

                    var image: android.media.Image? = null

                    try {

                        image = reader.acquireNextImage()

                        if (image == null) {
                            log("ImageReader returned null.")
                            return@setOnImageAvailableListener
                        }

                        log("")
                        log("******************************")
                        log("IMAGE RECEIVED")
                        log("******************************")

                        log(
                            "Dimensions: ${image.width} x ${image.height}"
                        )

                        val buffer = image.planes[0].buffer

                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)

                        log("Bytes: ${bytes.size}")

                        saveImage(bytes)

                    } catch (e: Throwable) {

                        log("")
                        log("IMAGE READ ERROR")
                        log(e.javaClass.name)
                        log(e.message ?: "")

                    } finally {

                        try {
                            image?.close()
                        } catch (_: Throwable) {
                        }
                    }
                },
                cameraHandler
            )

            log(
                "SUCCESS: ImageReader $FULL_WIDTH x $FULL_HEIGHT created."
            )

        } catch (e: Throwable) {

            log("")
            log("IMAGE READER FAILED")
            log(e.javaClass.name)
            log(e.message ?: "")
            return
        }

        openCamera()
    }

    private fun openCamera() {

        val manager =
            getSystemService(Context.CAMERA_SERVICE) as CameraManager

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        log("")
        log("STEP 2 - OPEN CAMERA 3")
        log("------------------------------")

        try {

            manager.openCamera(
                CAMERA_ID,
                object : CameraDevice.StateCallback() {

                    override fun onOpened(camera: CameraDevice) {

                        cameraDevice = camera

                        log("SUCCESS: Camera 3 opened.")

                        createStillOnlySession(camera)
                    }

                    override fun onDisconnected(camera: CameraDevice) {

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

                        when (error) {

                            ERROR_CAMERA_IN_USE ->
                                log("ERROR_CAMERA_IN_USE")

                            ERROR_MAX_CAMERAS_IN_USE ->
                                log("ERROR_MAX_CAMERAS_IN_USE")

                            ERROR_CAMERA_DISABLED ->
                                log("ERROR_CAMERA_DISABLED")

                            ERROR_CAMERA_DEVICE ->
                                log("ERROR_CAMERA_DEVICE")

                            ERROR_CAMERA_SERVICE ->
                                log("ERROR_CAMERA_SERVICE")
                        }

                        camera.close()
                        cameraDevice = null
                    }
                },
                cameraHandler
            )

        } catch (e: Throwable) {

            log("")
            log("OPEN CAMERA FAILED")
            log(e.javaClass.name)
            log(e.message ?: "")
        }
    }

    private fun createStillOnlySession(camera: CameraDevice) {

        val surface =
            fullResReader?.surface ?: return

        log("")
        log("STEP 3 - CREATE STILL-ONLY SESSION")
        log("-----------------------------------")

        val callback =
            object : CameraCaptureSession.StateCallback() {

                override fun onConfigured(
                    session: CameraCaptureSession
                ) {

                    captureSession = session
                    sessionReady = true

                    log("")
                    log("******************************")
                    log("STILL-ONLY SESSION CONFIGURED")
                    log("******************************")
                    log("")
                    log("No preview request was started.")
                    log("Press CAPTURE 200 MP.")

                    runOnUiThread {
                        captureButton.isEnabled = true
                    }
                }

                override fun onConfigureFailed(
                    session: CameraCaptureSession
                ) {

                    sessionReady = false

                    log("")
                    log("SESSION CONFIGURATION FAILED")
                }
            }

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

                val outputs =
                    listOf(
                        OutputConfiguration(surface)
                    )

                val executor =
                    Executor { runnable ->
                        cameraHandler.post(runnable)
                    }

                val configuration =
                    SessionConfiguration(
                        SessionConfiguration.SESSION_REGULAR,
                        outputs,
                        executor,
                        callback
                    )

                try {

                    val builder =
                        camera.createCaptureRequest(
                            CameraDevice.TEMPLATE_STILL_CAPTURE
                        )

                    builder.addTarget(surface)

                    applyVivoKeys(builder, "SESSION")

                    configuration.setSessionParameters(
                        builder.build()
                    )

                    log("Vivo session parameters attached.")

                } catch (e: Throwable) {

                    log("Session parameter error:")
                    log(e.javaClass.name)
                    log(e.message ?: "")
                }

                camera.createCaptureSession(configuration)

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
            log("CREATE SESSION EXCEPTION")
            log(e.javaClass.name)
            log(e.message ?: "")
        }
    }

    private fun capture200Mp() {

        val camera =
            cameraDevice

        val session =
            captureSession

        val surface =
            fullResReader?.surface

        if (
            camera == null ||
            session == null ||
            surface == null
        ) {

            log("")
            log("Capture cannot start.")
            log("Camera/session/surface missing.")
            return
        }

        log("")
        log("==============================")
        log("SENDING STILL CAPTURE")
        log("==============================")

        captureButton.isEnabled = false

        try {

            val builder =
                camera.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE
                )

            builder.addTarget(surface)

            builder.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON
            )

            builder.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )

            builder.set(
                CaptureRequest.JPEG_QUALITY,
                100.toByte()
            )

            builder.set(
                CaptureRequest.JPEG_ORIENTATION,
                90
            )

            applyVivoKeys(builder, "CAPTURE")

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
                        log("Waiting for 200 MP image...")

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
                        log("CAPTURE FAILED")
                        log("Reason: ${failure.reason}")
                        log("Frame: ${failure.frameNumber}")

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

    private fun applyVivoKeys(
        builder: CaptureRequest.Builder,
        stage: String
    ) {

        log("")
        log("$stage VIVO KEYS")
        log("------------------------------")

        try {
            builder.set(aiHighResolutionKey, 0)
            log("OK: ai_highresolution = 0")
        } catch (e: Throwable) {
            log("FAILED ai_highresolution")
        }

        try {
            builder.set(ultraHighResolutionKey, 1)
            log("OK: ultra_highresolution = 1")
        } catch (e: Throwable) {
            log("FAILED ultra_highresolution")
        }

        try {
            builder.set(real200mpKey, 1)
            log("OK: real200mp_switch_on = 1")
        } catch (e: Throwable) {
            log("FAILED real200mp_switch_on")
        }
    }

    private fun saveImage(data: ByteArray) {

        try {

            val dir =
                getExternalFilesDir(
                    android.os.Environment.DIRECTORY_PICTURES
                ) ?: return

            if (!dir.exists()) {
                dir.mkdirs()
            }

            val timestamp =
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.US
                ).format(Date())

            val file =
                File(
                    dir,
                    "Vivo_200MP_$timestamp.jpg"
                )

            FileOutputStream(file).use {
                it.write(data)
            }

            log("")
            log("******************************")
            log("IMAGE SAVED")
            log("******************************")
            log(file.absolutePath)

            val mb =
                file.length().toDouble() /
                    1024.0 /
                    1024.0

            log(
                "Size: ${
                    String.format(
                        Locale.US,
                        "%.2f MB",
                        mb
                    )
                }"
            )

        } catch (e: Throwable) {

            log("")
            log("SAVE FAILED")
            log(e.javaClass.name)
            log(e.message ?: "")
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
            requestCode == CAMERA_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] ==
            PackageManager.PERMISSION_GRANTED
        ) {

            startTest()

        } else {

            log("Camera permission denied.")
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
            fullResReader?.close()
        } catch (_: Throwable) {
        }

        try {
            cameraThread.quitSafely()
        } catch (_: Throwable) {
        }

        super.onDestroy()
    }
}
