package com.example.vivo200mpprobe

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
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
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 1001

        private const val CAMERA_ID = "3"

        private const val FULL_WIDTH = 16320
        private const val FULL_HEIGHT = 12288

        private const val PREVIEW_WIDTH = 1440
        private const val PREVIEW_HEIGHT = 1080
    }

    private lateinit var statusText: TextView
    private lateinit var captureButton: Button

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null

    private var previewReader: ImageReader? = null
    private var fullResReader: ImageReader? = null

    private lateinit var cameraThread: HandlerThread
    private lateinit var cameraHandler: Handler

    private var sessionReady = false

    /*
     * Vivo vendor keys seen in the stock camera dump.
     */
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

        log("VIVO REAL 200 MP TEST")
        log("==============================")
        log("")
        log("Target Camera ID: $CAMERA_ID")
        log("Target capture: $FULL_WIDTH x $FULL_HEIGHT")
        log("Pixels: 200.54 MP")
        log("")

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            log("Camera permission already granted.")
            start200MpTest()
        } else {
            log("Requesting camera permission...")

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
        statusText.setPadding(10, 20, 10, 40)

        captureButton.setOnClickListener {
            capture200Mp()
        }

        root.addView(
            captureButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val scrollView = ScrollView(this)
        scrollView.addView(statusText)

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

    private fun log(message: String) {

        runOnUiThread {

            statusText.append(message)
            statusText.append("\n")

            val parent = statusText.parent

            if (parent is ScrollView) {
                parent.post {
                    parent.fullScroll(View.FOCUS_DOWN)
                }
            }
        }
    }

    private fun startCameraThread() {

        cameraThread =
            HandlerThread("Vivo200MpCamera")

        cameraThread.start()

        cameraHandler =
            Handler(cameraThread.looper)
    }

    private fun start200MpTest() {

        log("")
        log("STEP 1 - CHECK CAMERA")
        log("------------------------------")

        val manager =
            getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {

            val ids =
                manager.cameraIdList.joinToString(", ")

            log("Public Camera2 IDs: $ids")

            log("")
            log("Attempting Camera ID $CAMERA_ID...")

            val characteristics =
                manager.getCameraCharacteristics(CAMERA_ID)

            val facing =
                characteristics.get(
                    CameraCharacteristics.LENS_FACING
                )

            log(
                "Lens facing value: ${facing ?: "unknown"}"
            )

            val focalLengths =
                characteristics.get(
                    CameraCharacteristics
                        .LENS_INFO_AVAILABLE_FOCAL_LENGTHS
                )

            if (focalLengths != null) {

                log(
                    "Focal lengths: ${
                        focalLengths.joinToString(", ")
                    } mm"
                )
            }

            val hardwareLevel =
                characteristics.get(
                    CameraCharacteristics
                        .INFO_SUPPORTED_HARDWARE_LEVEL
                )

            log(
                "Hardware Level: ${hardwareLevel ?: "unknown"}"
            )

            val publicSizes =
                characteristics.get(
                    CameraCharacteristics
                        .SCALER_STREAM_CONFIGURATION_MAP
                )
                    ?.getOutputSizes(ImageFormat.JPEG)

            if (publicSizes != null) {

                val largest =
                    publicSizes.maxByOrNull {
                        it.width.toLong() * it.height.toLong()
                    }

                if (largest != null) {

                    log(
                        "Largest PUBLIC JPEG: " +
                            "${largest.width} x ${largest.height}"
                    )
                }
            }

            log("")
            log("We are intentionally bypassing the")
            log("public size list and requesting:")
            log("")
            log("$FULL_WIDTH x $FULL_HEIGHT")
            log("")

            createImageReaders()
            openCamera(manager)

        } catch (e: Throwable) {

            log("")
            log("CAMERA CHECK FAILED")
            log("==============================")
            log(e.javaClass.name)
            log(e.message ?: "No error message")
        }
    }

    private fun createImageReaders() {

        log("")
        log("STEP 2 - CREATE IMAGE READERS")
        log("------------------------------")

        try {

            previewReader =
                ImageReader.newInstance(
                    PREVIEW_WIDTH,
                    PREVIEW_HEIGHT,
                    ImageFormat.YUV_420_888,
                    2
                )

            previewReader?.setOnImageAvailableListener(
                { reader ->

                    try {
                        reader.acquireLatestImage()?.close()
                    } catch (_: Throwable) {
                    }

                },
                cameraHandler
            )

            log(
                "Preview reader created: " +
                    "$PREVIEW_WIDTH x $PREVIEW_HEIGHT"
            )

        } catch (e: Throwable) {

            log("")
            log("PREVIEW READER FAILED")
            log(e.javaClass.name)
            log(e.message ?: "")
            return
        }

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

                        image =
                            reader.acquireNextImage()

                        if (image == null) {
                            log("ImageReader returned null image.")
                            return@setOnImageAvailableListener
                        }

                        log("")
                        log("********************************")
                        log("200 MP IMAGE RECEIVED")
                        log("********************************")

                        log(
                            "Image dimensions: " +
                                "${image.width} x ${image.height}"
                        )

                        val buffer =
                            image.planes[0].buffer

                        val bytes =
                            ByteArray(buffer.remaining())

                        buffer.get(bytes)

                        log(
                            "Image data: ${bytes.size} bytes"
                        )

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

            log("")
            log("200 MP reader CREATED:")
            log("$FULL_WIDTH x $FULL_HEIGHT")
            log("")

        } catch (e: Throwable) {

            log("")
            log("200 MP IMAGE READER FAILED")
            log("==============================")
            log(e.javaClass.name)
            log(e.message ?: "")
        }
    }

    private fun openCamera(
        manager: CameraManager
    ) {

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            log("Camera permission missing.")
            return
        }

        log("")
        log("STEP 3 - OPEN CAMERA 3")
        log("------------------------------")

        try {

            manager.openCamera(
                CAMERA_ID,
                object : CameraDevice.StateCallback() {

                    override fun onOpened(
                        camera: CameraDevice
                    ) {

                        cameraDevice = camera

                        log("")
                        log("SUCCESS: Camera 3 opened.")

                        create200MpSession(camera)
                    }

                    override fun onDisconnected(
                        camera: CameraDevice
                    ) {

                        log("")
                        log("Camera 3 disconnected.")

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

                            CameraDevice.StateCallback.ERROR_CAMERA_IN_USE ->
                                log("ERROR_CAMERA_IN_USE")

                            CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE ->
                                log("ERROR_MAX_CAMERAS_IN_USE")

                            CameraDevice.StateCallback.ERROR_CAMERA_DISABLED ->
                                log("ERROR_CAMERA_DISABLED")

                            CameraDevice.StateCallback.ERROR_CAMERA_DEVICE ->
                                log("ERROR_CAMERA_DEVICE")

                            CameraDevice.StateCallback.ERROR_CAMERA_SERVICE ->
                                log("ERROR_CAMERA_SERVICE")

                            else ->
                                log("UNKNOWN CAMERA ERROR")
                        }

                        camera.close()

                        cameraDevice = null
                    }
                },
                cameraHandler
            )

        } catch (e: Throwable) {

            log("")
            log("OPEN CAMERA EXCEPTION")
            log("==============================")
            log(e.javaClass.name)
            log(e.message ?: "No error message")
        }
    }

    private fun create200MpSession(
        camera: CameraDevice
    ) {

        val previewSurface =
            previewReader?.surface

        val fullSurface =
            fullResReader?.surface

        if (
            previewSurface == null ||
            fullSurface == null
        ) {

            log("One or more ImageReader surfaces are missing.")
            return
        }

        log("")
        log("STEP 4 - REQUEST 200 MP SESSION")
        log("--------------------------------")
        log("")
        log(
            "Preview output: " +
                "$PREVIEW_WIDTH x $PREVIEW_HEIGHT YUV"
        )

        log(
            "Capture output: " +
                "$FULL_WIDTH x $FULL_HEIGHT JPEG"
        )

        val stateCallback =
            object :
                CameraCaptureSession.StateCallback() {

                override fun onConfigured(
                    session: CameraCaptureSession
                ) {

                    captureSession = session
                    sessionReady = true

                    log("")
                    log("********************************")
                    log("200 MP SESSION CONFIGURED!")
                    log("********************************")

                    runOnUiThread {
                        captureButton.isEnabled = true
                    }

                    startPreview(session)
                }

                override fun onConfigureFailed(
                    session: CameraCaptureSession
                ) {

                    captureSession = session
                    sessionReady = false

                    log("")
                    log("********************************")
                    log("SESSION CONFIGURATION FAILED")
                    log("********************************")
                    log("")
                    log(
                        "Camera opened, but Vivo rejected"
                    )
                    log(
                        "the 16320 x 12288 session."
                    )
                }

                override fun onClosed(
                    session: CameraCaptureSession
                ) {

                    super.onClosed(session)

                    log("")
                    log("Capture session closed.")
                }
            }

        try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.P
            ) {

                log("")
                log(
                    "Using modern SessionConfiguration..."
                )

                val outputConfigurations =
                    arrayListOf(
                        OutputConfiguration(
                            previewSurface
                        ),
                        OutputConfiguration(
                            fullSurface
                        )
                    )

                val executor =
                    Executor { runnable ->

                        cameraHandler.post(runnable)
                    }

                val sessionConfiguration =
                    SessionConfiguration(
                        SessionConfiguration.SESSION_REGULAR,
                        outputConfigurations,
                        executor,
                        stateCallback
                    )

                try {

                    val sessionBuilder =
                        camera.createCaptureRequest(
                            CameraDevice.TEMPLATE_PREVIEW
                        )

                    sessionBuilder.addTarget(
                        previewSurface
                    )

                    sessionBuilder.set(
                        CaptureRequest.CONTROL_MODE,
                        CameraMetadata.CONTROL_MODE_AUTO
                    )

                    applyVivo200MpKeys(
                        sessionBuilder,
                        "SESSION"
                    )

                    sessionConfiguration
                        .setSessionParameters(
                            sessionBuilder.build()
                        )

                    log("")
                    log(
                        "Vivo session parameters attached."
                    )

                } catch (e: Throwable) {

                    log("")
                    log(
                        "Session parameter setup error:"
                    )
                    log(e.javaClass.name)
                    log(e.message ?: "")
                }

                camera.createCaptureSession(
                    sessionConfiguration
                )

            } else {

                @Suppress("DEPRECATION")
                camera.createCaptureSession(
                    listOf(
                        previewSurface,
                        fullSurface
                    ),
                    stateCallback,
                    cameraHandler
                )
            }

        } catch (e: Throwable) {

            log("")
            log("CREATE SESSION EXCEPTION")
            log("==============================")
            log(e.javaClass.name)
            log(e.message ?: "No error message")
        }
    }

    private fun startPreview(
        session: CameraCaptureSession
    ) {

        val camera =
            cameraDevice ?: return

        val previewSurface =
            previewReader?.surface ?: return

        log("")
        log("STEP 5 - START PREVIEW REQUEST")
        log("------------------------------")

        try {

            val builder =
                camera.createCaptureRequest(
                    CameraDevice.TEMPLATE_PREVIEW
                )

            builder.addTarget(
                previewSurface
            )

            builder.set(
                CaptureRequest.CONTROL_MODE,
                CameraMetadata.CONTROL_MODE_AUTO
            )

            builder.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest
                    .CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )

            builder.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON
            )

            applyVivo200MpKeys(
                builder,
                "PREVIEW"
            )

            session.setRepeatingRequest(
                builder.build(),
                object :
                    CameraCaptureSession.CaptureCallback() {

                    override fun onCaptureFailed(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        failure: CaptureFailure
                    ) {

                        log(
                            "Preview capture failed."
                        )

                        log(
                            "Reason: ${failure.reason}"
                        )
                    }
                },
                cameraHandler
            )

            log("")
            log("Preview request started.")
            log("")
            log("PRESS CAPTURE 200 MP")

        } catch (e: Throwable) {

            log("")
            log("PREVIEW REQUEST FAILED")
            log(e.javaClass.name)
            log(e.message ?: "")
        }
    }

    private fun capture200Mp() {

        if (!sessionReady) {

            log("")
            log("Session is not ready.")
            return
        }

        val camera =
            cameraDevice ?: run {

                log("CameraDevice is null.")
                return
            }

        val session =
            captureSession ?: run {

                log("CaptureSession is null.")
                return
            }

        val fullSurface =
            fullResReader?.surface ?: run {

                log("Full-resolution surface is null.")
                return
            }

        runOnUiThread {
            captureButton.isEnabled = false
        }

        log("")
        log("================================")
        log("SENDING 200 MP CAPTURE")
        log("================================")

        try {

            val builder =
                camera.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE
                )

            builder.addTarget(
                fullSurface
            )

            builder.set(
                CaptureRequest.CONTROL_MODE,
                CameraMetadata.CONTROL_MODE_AUTO
            )

            builder.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest
                    .CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )

            builder.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON
            )

            builder.set(
                CaptureRequest.JPEG_QUALITY,
                100.toByte()
            )

            builder.set(
                CaptureRequest.JPEG_ORIENTATION,
                90
            )

            applyVivo200MpKeys(
                builder,
                "CAPTURE"
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
                            "Capture request COMPLETED."
                        )

                        log(
                            "Waiting for image from ImageReader..."
                        )

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
                        log("********************************")
                        log("200 MP CAPTURE FAILED")
                        log("********************************")

                        log(
                            "Reason: ${failure.reason}"
                        )

                        log(
                            "Sequence ID: ${failure.sequenceId}"
                        )

                        log(
                            "Frame: ${failure.frameNumber}"
                        )

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
            log("==============================")
            log(e.javaClass.name)
            log(e.message ?: "No error message")

            runOnUiThread {
                captureButton.isEnabled = true
            }
        }
    }

    private fun applyVivo200MpKeys(
        builder: CaptureRequest.Builder,
        stage: String
    ) {

        log("")
        log("$stage VIVO CONTROLS")
        log("------------------------------")

        try {

            builder.set(
                aiHighResolutionKey,
                0
            )

            log(
                "OK: ai_highresolution = 0"
            )

        } catch (e: Throwable) {

            log(
                "FAILED: ai_highresolution"
            )

            log(
                "${e.javaClass.simpleName}: " +
                    "${e.message ?: ""}"
            )
        }

        try {

            builder.set(
                ultraHighResolutionKey,
                1
            )

            log(
                "OK: ultra_highresolution = 1"
            )

        } catch (e: Throwable) {

            log(
                "FAILED: ultra_highresolution"
            )

            log(
                "${e.javaClass.simpleName}: " +
                    "${e.message ?: ""}"
            )
        }

        try {

            builder.set(
                real200mpKey,
                1
            )

            log(
                "OK: real200mp_switch_on = 1"
            )

        } catch (e: Throwable) {

            log(
                "FAILED: real200mp_switch_on"
            )

            log(
                "${e.javaClass.simpleName}: " +
                    "${e.message ?: ""}"
            )
        }
    }

    private fun saveImage(
        data: ByteArray
    ) {

        try {

            val directory =
                getExternalFilesDir(
                    android.os.Environment
                        .DIRECTORY_PICTURES
                )

            if (directory == null) {

                log("")
                log(
                    "Could not access app Pictures folder."
                )

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
                    "Vivo_REAL_200MP_$timestamp.jpg"
                )

            FileOutputStream(file).use {
                it.write(data)
            }

            log("")
            log("********************************")
            log("IMAGE SAVED")
            log("********************************")

            log(
                "Path:"
            )

            log(
                file.absolutePath
            )

            val mb =
                file.length().toDouble() /
                    1024.0 /
                    1024.0

            log(
                "File size: ${
                    String.format(
                        Locale.US,
                        "%.2f MB",
                        mb
                    )
                }"
            )

            log("")
            log(
                "Expected dimensions:"
            )

            log(
                "$FULL_WIDTH x $FULL_HEIGHT"
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
            requestCode ==
            CAMERA_PERMISSION_REQUEST
        ) {

            if (
                grantResults.isNotEmpty() &&
                grantResults[0] ==
                PackageManager.PERMISSION_GRANTED
            ) {

                log("")
                log("Camera permission granted.")

                start200MpTest()

            } else {

                log("")
                log("Camera permission DENIED.")
            }
        }
    }

    override fun onDestroy() {

        sessionReady = false

        try {
            captureSession?.close()
        } catch (_: Throwable) {
        }

        captureSession = null

        try {
            cameraDevice?.close()
        } catch (_: Throwable) {
        }

        cameraDevice = null

        try {
            previewReader?.close()
        } catch (_: Throwable) {
        }

        previewReader = null

        try {
            fullResReader?.close()
        } catch (_: Throwable) {
        }

        fullResReader = null

        try {
            cameraThread.quitSafely()
        } catch (_: Throwable) {
        }

        super.onDestroy()
    }
}
