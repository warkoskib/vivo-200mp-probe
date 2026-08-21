package com.example.vivo200mpprobe

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.*
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
import java.util.*
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 1001

        // Vivo stock 200 MP mode uses this camera ID
        private const val CAMERA_ID = "3"

        // Confirmed from Vivo camera-service dump
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

    // ------------------------------------------------------------
    // Vivo vendor request keys
    // ------------------------------------------------------------

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
        log("Waiting for camera permission...")

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            start200MpTest()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        }
    }

    // ------------------------------------------------------------
    // UI
    // ------------------------------------------------------------

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

            val parent = statusText.parent

            if (parent is ScrollView) {
                parent.post {
                    parent.fullScroll(View.FOCUS_DOWN)
                }
            }
        }
    }

    // ------------------------------------------------------------
    // CAMERA THREAD
    // ------------------------------------------------------------

    private fun startCameraThread() {

        cameraThread =
            HandlerThread("Vivo200MpCamera")

        cameraThread.start()

        cameraHandler =
            Handler(cameraThread.looper)
    }

    // ------------------------------------------------------------
    // START TEST
    // ------------------------------------------------------------

    private fun start200MpTest() {

        log("")
        log("STEP 1 - CHECK CAMERA")
        log("------------------------------")

        val manager =
            getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {

            log(
                "Public camera IDs: ${
                    manager.cameraIdList.joinToString(", ")
                }"
            )

            log("Attempting direct access to Camera $CAMERA_ID...")

            val characteristics =
                manager.getCameraCharacteristics(CAMERA_ID)

            val focalLengths =
                characteristics.get(
                    CameraCharacteristics
                        .LENS_INFO_AVAILABLE_FOCAL_LENGTHS
                )

            if (focalLengths != null) {
                log(
                    "Focal length: ${
                        focalLengths.joinToString()
                    } mm"
                )
            }

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
                        "Public JPEG maximum: " +
                                "${largest.width} x ${largest.height}"
                    )
                }
            }

            log("")
            log(
                "IMPORTANT: We are intentionally requesting"
            )
            log(
                "$FULL_WIDTH x $FULL_HEIGHT even if it is"
            )
            log(
                "NOT listed in the public Camera2 map."
            )

            createImageReaders()

            openCamera(manager)

        } catch (e: Throwable) {

            log("")
            log("CAMERA 3 ACCESS FAILED")
            log("==============================")
            log(e.javaClass.name)
            log(e.message ?: "No error message")
            log("")
            log(
                "If this happens, Vivo is blocking direct"
            )
            log(
                "third-party access to Camera ID 3."
            )
        }
    }

    // ------------------------------------------------------------
    // IMAGE READERS
    // ------------------------------------------------------------

    private fun createImageReaders() {

        log("")
        log("STEP 2 - CREATE STREAMS")
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
                "Preview ImageReader created: " +
                        "$PREVIEW_WIDTH x $PREVIEW_HEIGHT"
            )

        } catch (e: Throwable) {

            log("Preview ImageReader FAILED")
            log(e.javaClass.simpleName)
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

                    try {

                        val image =
                            reader.acquireNextImage()
                                ?: return@setOnImageAvailableListener

                        log("")
                        log("******** IMAGE RECEIVED ********")
                        log(
                            "ImageReader dimensions: " +
                                    "${image.width} x ${image.height}"
                        )

                        val buffer =
                            image.planes[0].buffer

                        val bytes =
                            ByteArray(buffer.remaining())

                        buffer.get(bytes)

                        image.close()

                        log(
                            "JPEG bytes received: " +
                                    "${bytes.size}"
                        )

                        saveImage(bytes)

                    } catch (e: Throwable) {

                        log("")
                        log("IMAGE READ ERROR")
                        log(e.javaClass.name)
                        log(e.message ?: "")
                    }
                },
                cameraHandler
            )

            log(
                "200 MP ImageReader CREATED:"
            )

            log(
                "$FULL_WIDTH x $FULL_HEIGHT"
            )

            log(
                "This is the first important test."
            )

        } catch (e: Throwable) {

            log("")
            log("200 MP ImageReader CREATION FAILED")
            log(e.javaClass.name)
            log(e.message ?: "")
        }
    }

    // ------------------------------------------------------------
    // OPEN CAMERA 3
    // ------------------------------------------------------------

    private fun openCamera(
        manager: CameraManager
    ) {

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
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

                        log("SUCCESS: Camera 3 opened.")

                        create200MpSession(camera)
                    }

                    override fun onDisconnected(
                        camera: CameraDevice
                    ) {

                        log("Camera 3 disconnected.")

                        camera.close()
                    }

                    override fun onError(
                        camera: CameraDevice,
                        error: Int
                    ) {

                        log("")
                        log("CAMERA ERROR: $error")

                        when (error) {

                            ERROR_CAMERA_IN_USE ->
                                log("CAMERA_IN_USE")

                            ERROR_MAX_CAMERAS_IN_USE ->
                                log("MAX_CAMERAS_IN_USE")

                            ERROR_CAMERA_DISABLED ->
                                log("CAMERA_DISABLED")

                            ERROR_CAMERA_DEVICE ->
                                log("CAMERA_DEVICE")

                            ERROR_CAMERA_SERVICE ->
                                log("CAMERA_SERVICE")
                        }

                        camera.close()
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

    // ------------------------------------------------------------
    // CREATE 200 MP SESSION
    // ------------------------------------------------------------

    private fun create200MpSession(
        camera: CameraDevice
    ) {

        val preview =
            previewReader?.surface

        val full =
            fullResReader?.surface

        if (preview == null || full == null) {

            log("ImageReader surface missing.")
            return
        }

        log("")
        log("STEP 4 - REQUEST 200 MP SESSION")
        log("--------------------------------")

        log(
            "Output 0: $PREVIEW_WIDTH x $PREVIEW_HEIGHT YUV"
        )

        log(
            "Output 1: $FULL_WIDTH x $FULL_HEIGHT JPEG"
        )

        val callback =
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

                    startPreview(session)

                    runOnUiThread {
                        captureButton.isEnabled = true
                    }
                }

                override fun onConfigureFailed(
                    session: CameraCaptureSession
                ) {

                    sessionReady = false

                    log("")
                    log("********************************")
                    log("SESSION CONFIGURATION FAILED")
                    log("********************************")
                    log("")
                    log(
                        "This means Android/Vivo rejected the"
                    )
                    log(
                        "16320 x 12288 stream for this app."
                    )
                }

                override fun onClosed(
                    session: CameraCaptureSession
                ) {

                    log("Capture session closed.")
                }
            }

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

                log(
                    "Using SessionConfiguration with Vivo " +
                            "session parameters..."
                )

                val outputs =
                    listOf(
                        OutputConfiguration(preview),
                        OutputConfiguration(full)
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

                // Send Vivo's 200 MP controls during
                // session configuration.
                try {

                    val sessionBuilder =
                        camera.createCaptureRequest(
                            CameraDevice.TEMPLATE_PREVIEW
                        )

                    sessionBuilder.addTarget(preview)

                    applyVivo200MpKeys(
                        sessionBuilder,
                        "SESSION"
                    )

                    configuration.setSessionParameters(
                        sessionBuilder.build()
                    )

                    log(
                        "Vivo 200 MP session parameters attached."
                    )

                } catch (e: Throwable) {

                    log(
                        "Could not attach session parameters:"
                    )

                    log(
                        "${e.javaClass.simpleName}: " +
                                "${e.message}"
                    )
                }

                camera.createCaptureSession(configuration)

            } else {

                @Suppress("DEPRECATION")
                camera.createCaptureSession(
                    listOf(
                        preview,
                        full
                    ),
                    callback,
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

    // ------------------------------------------------------------
    // PREVIEW
    // ------------------------------------------------------------

    private fun startPreview(
        session: CameraCaptureSession
    ) {

        val camera =
            cameraDevice ?: return

        val preview =
            previewReader?.surface ?: return

        try {

            val builder =
                camera.createCaptureRequest(
                    CameraDevice.TEMPLATE_PREVIEW
                )

            builder.addTarget(preview)

            builder.set(
                CaptureRequest.CONTROL_MODE,
                CameraMetadata.CONTROL_MODE_AUTO
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
                            "Preview capture failure: " +
                                    failure.reason
                        )
                    }
                },
                cameraHandler
            )

            log("")
            log("200 MP preview request running.")
            log("")
            log("PRESS 'CAPTURE 200 MP'.")

        } catch (e: Throwable) {

            log("")
            log("PREVIEW REQUEST FAILED")
            log(e.javaClass.name)
            log(e.message ?: "")
        }
    }

    // ------------------------------------------------------------
    // CAPTURE
    // ------------------------------------------------------------

    private fun capture200Mp() {

        if (!sessionReady) {

            log("Session is not ready.")
            return
        }

        val camera =
            cameraDevice ?: return

        val session =
            captureSession ?: return

        val fullSurface =
            fullResReader?.surface ?: return

        captureButton.isEnabled = false

        log("")
        log("================================")
        log("SENDING 200 MP CAPTURE")
        log("================================")

        try {

            val builder =
                camera.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE
                )

            builder.addTarget(fullSurface)

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

                        log(
                            "Capture started. Frame: $frameNumber"
                        )
                    }

                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {

                        log("Capture request COMPLETED.")
                        log(
                            "Waiting for 16320 x 12288 image..."
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
                        log("200 MP CAPTURE FAILED")
                        log(
                            "Reason: ${failure.reason}"
                        )

                        log(
                            "Sequence ID: ${failure.sequenceId}"
                        )

                        log(
                            "Frame number: ${failure.frameNumber}"
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
            log(e.message ?: "No message")

            runOnUiThread {
                captureButton.isEnabled = true
            }
        }
    }

    // ------------------------------------------------------------
    // VIVO VENDOR KEYS
    // ------------------------------------------------------------

    private fun applyVivo200MpKeys(
        builder: CaptureRequest.Builder,
        stage: String
    ) {

        log("")
        log("$stage vendor controls:")

        try {

            builder.set(
                aiHighResolutionKey,
                0
            )

            log(
                "✓ ai_highresolution = 0"
            )

        } catch (e: Throwable) {

            log(
                "✗ ai_highresolution rejected: " +
                        e.javaClass.simpleName
            )
        }

        try {

            builder.set(
                ultraHighResolutionKey,
                1
            )

            log(
                "✓ ultra_highresolution = 1"
            )

        } catch (e: Throwable) {

            log(
                "✗ ultra_highresolution rejected: " +
                        e.javaClass.simpleName
            )
        }

        try {

            builder.set(
                real200mpKey,
                1
            )

            log(
                "✓ real200mp_switch_on = 1"
            )

        } catch (e: Throwable) {

            log(
                "✗ real200mp_switch_on rejected: " +
                        e.javaClass.simpleName
            )
        }
    }

    // ------------------------------------------------------------
    // SAVE JPEG
    // ------------------------------------------------------------

    private fun saveImage(
        data: ByteArray
    ) {

        try {

            val directory =
                getExternalFilesDir(
                    android.os.Environment.DIRECTORY_PICTURES
                )

            if (directory == null) {

                log("Could not access Pictures directory.")
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
            log("IMAGE SAVED!")
            log("********************************")
            log("File:")
            log(file.absolutePath)

            log(
                "File size: " +
                        String.format(
                            Locale.US,
                            "%.2f MB",
                            file.length() /
                                    1024.0 /
                                    1024.0
                        )
            )

            log("")
            log(
                "If the JPEG is actually " +
                        "$FULL_WIDTH x $FULL_HEIGHT,"
            )

            log(
                "WE HAVE DIRECT 200 MP CAPTURE."
            )

        } catch (e: Throwable) {

            log("")
            log("SAVE FAILED")
            log(e.javaClass.name)
            log(e.message ?: "")
        }
    }

    // ------------------------------------------------------------
    // PERMISSION
    // ------------------------------------------------------------

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

            log("Camera permission granted.")
            start200MpTest()

        } else {

            log("Camera permission denied.")
        }
    }

    // ------------------------------------------------------------
    // CLEANUP
    // ------------------------------------------------------------

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
            previewReader?.close()
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
