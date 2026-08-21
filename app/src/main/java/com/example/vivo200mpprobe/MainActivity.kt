package com.example.vivo200mpprobe

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.*
import android.view.Surface
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

        private const val PREVIEW_WIDTH = 1440
        private const val PREVIEW_HEIGHT = 1080

        private const val JPEG_WIDTH = 16320
        private const val JPEG_HEIGHT = 12288
    }

    private lateinit var logText: TextView
    private lateinit var captureButton: Button

    private lateinit var cameraThread: HandlerThread
    private lateinit var cameraHandler: Handler

    private var cameraDevice: CameraDevice? = null
    private var session: CameraCaptureSession? = null

    private var previewReader: ImageReader? = null
    private var jpegReader: ImageReader? = null

    private var ready = false

    /*
     * CONFIRMED Vivo vendor request keys
     */

    private val aiHighResolutionKey =
        CaptureRequest.Key(
            "vivo.control.ai_highresolution",
            Int::class.javaObjectType
        )

    private val ultraHighResolutionKey =
        CaptureRequest.Key(
            "vivo.control.ultra_highresolution",
            Int::class.javaObjectType
        )

    private val real200MpKey =
        CaptureRequest.Key(
            "vivo.control.real200mp_switch_on",
            Int::class.javaObjectType
        )

    private val portraitHighResolutionKey =
        CaptureRequest.Key(
            "vivo.control.portrait_high_resolution",
            Byte::class.javaObjectType
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildUi()
        startThread()

        log("VIVO CAMERA 3")
        log("200 MP SESSION CLONE")
        log("============================")
        log("")
        log("Camera: 3")
        log("Preview: 1440 x 1080")
        log("JPEG: 16320 x 12288")
        log("")

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            begin()
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
        root.setPadding(20, 40, 20, 40)

        captureButton = Button(this)
        captureButton.text = "CAPTURE 200 MP"
        captureButton.isEnabled = false

        logText = TextView(this)
        logText.textSize = 13f

        captureButton.setOnClickListener {
            capture()
        }

        root.addView(captureButton)

        val scroll = ScrollView(this)
        scroll.addView(logText)

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

    private fun log(s: String) {

        runOnUiThread {
            logText.append(s)
            logText.append("\n")
        }
    }

    private fun startThread() {

        cameraThread =
            HandlerThread("VivoCamera3")

        cameraThread.start()

        cameraHandler =
            Handler(cameraThread.looper)
    }

    private fun begin() {

        log("STEP 1")
        log("Creating ImageReaders...")
        log("")

        try {

            previewReader =
                ImageReader.newInstance(
                    PREVIEW_WIDTH,
                    PREVIEW_HEIGHT,
                    ImageFormat.YUV_420_888,
                    4
                )

            previewReader!!.setOnImageAvailableListener(
                { reader ->

                    try {
                        reader.acquireLatestImage()?.close()
                    } catch (_: Throwable) {
                    }

                },
                cameraHandler
            )

            log("Preview reader OK.")

        } catch (e: Throwable) {

            log("PREVIEW READER FAILED")
            log(e.toString())
            return
        }

        try {

            jpegReader =
                ImageReader.newInstance(
                    JPEG_WIDTH,
                    JPEG_HEIGHT,
                    ImageFormat.JPEG,
                    2
                )

            jpegReader!!.setOnImageAvailableListener(
                { reader ->

                    var image: android.media.Image? = null

                    try {

                        image =
                            reader.acquireNextImage()

                        if (image == null) {

                            log("Null JPEG image.")
                            return@setOnImageAvailableListener
                        }

                        log("")
                        log("============================")
                        log("JPEG RECEIVED")
                        log("============================")

                        log(
                            "Reader dimensions: " +
                                "${image.width} x ${image.height}"
                        )

                        val buffer =
                            image.planes[0].buffer

                        val data =
                            ByteArray(buffer.remaining())

                        buffer.get(data)

                        log("Bytes: ${data.size}")

                        saveJpeg(data)

                    } catch (e: Throwable) {

                        log("")
                        log("JPEG READ ERROR")
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
                "JPEG reader OK: " +
                    "$JPEG_WIDTH x $JPEG_HEIGHT"
            )

        } catch (e: Throwable) {

            log("JPEG READER FAILED")
            log(e.toString())
            return
        }

        openCamera()
    }

    private fun openCamera() {

        log("")
        log("STEP 2")
        log("Opening Camera 3...")

        val manager =
            getSystemService(Context.CAMERA_SERVICE)
                    as CameraManager

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        try {

            manager.openCamera(
                CAMERA_ID,
                object : CameraDevice.StateCallback() {

                    override fun onOpened(
                        camera: CameraDevice
                    ) {

                        cameraDevice = camera

                        log("Camera 3 OPENED.")

                        createSession(camera)
                    }

                    override fun onDisconnected(
                        camera: CameraDevice
                    ) {

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

            log("OPEN CAMERA EXCEPTION")
            log(e.javaClass.name)
            log(e.message ?: "")
        }
    }

    private fun createSession(
        camera: CameraDevice
    ) {

        val previewSurface =
            previewReader?.surface ?: return

        val jpegSurface =
            jpegReader?.surface ?: return

        log("")
        log("STEP 3")
        log("Creating Vivo 200 MP session...")
        log("")

        val callback =
            object :
                CameraCaptureSession.StateCallback() {

                override fun onConfigured(
                    captureSession: CameraCaptureSession
                ) {

                    session = captureSession
                    ready = true

                    log("")
                    log("============================")
                    log("SESSION CONFIGURED")
                    log("============================")

                    startPreview()

                    runOnUiThread {
                        captureButton.isEnabled = true
                    }
                }

                override fun onConfigureFailed(
                    captureSession: CameraCaptureSession
                ) {

                    log("")
                    log("SESSION CONFIGURATION FAILED")
                }
            }

        try {

            if (Build.VERSION.SDK_INT >= 28) {

                val outputs =
                    listOf(
                        OutputConfiguration(
                            previewSurface
                        ),
                        OutputConfiguration(
                            jpegSurface
                        )
                    )

                val executor =
                    Executor { runnable ->
                        cameraHandler.post(runnable)
                    }

                val config =
                    SessionConfiguration(
                        SessionConfiguration.SESSION_REGULAR,
                        outputs,
                        executor,
                        callback
                    )

                /*
                 * Mimic Vivo's active 200 MP session parameters.
                 */

                val sessionBuilder =
                    camera.createCaptureRequest(
                        CameraDevice.TEMPLATE_PREVIEW
                    )

                sessionBuilder.addTarget(
                    previewSurface
                )

                apply200MpKeys(
                    sessionBuilder,
                    "SESSION"
                )

                try {

                    config.setSessionParameters(
                        sessionBuilder.build()
                    )

                    log("Session parameters attached.")

                } catch (e: Throwable) {

                    log("Session parameter error:")
                    log(e.toString())
                }

                camera.createCaptureSession(
                    config
                )

            } else {

                @Suppress("DEPRECATION")
                camera.createCaptureSession(
                    listOf(
                        previewSurface,
                        jpegSurface
                    ),
                    callback,
                    cameraHandler
                )
            }

        } catch (e: Throwable) {

            log("")
            log("SESSION EXCEPTION")
            log(e.javaClass.name)
            log(e.message ?: "")
        }
    }

    private fun startPreview() {

        val camera =
            cameraDevice ?: return

        val captureSession =
            session ?: return

        val previewSurface =
            previewReader?.surface ?: return

        log("")
        log("STEP 4")
        log("Starting Vivo-style preview...")

        try {

            val builder =
                camera.createCaptureRequest(
                    CameraDevice.TEMPLATE_PREVIEW
                )

            builder.addTarget(
                previewSurface
            )

            /*
             * Match values visible in Vivo dump.
             */

            builder.set(
                CaptureRequest.CONTROL_MODE,
                CaptureRequest.CONTROL_MODE_USE_SCENE_MODE
            )

            builder.set(
                CaptureRequest.CONTROL_SCENE_MODE,
                CaptureRequest.CONTROL_SCENE_MODE_FACE_PRIORITY
            )

            builder.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON
            )

            builder.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest
                    .CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )

            builder.set(
                CaptureRequest.CONTROL_ENABLE_ZSL,
                false
            )

            builder.set(
                CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                CaptureRequest
                    .CONTROL_VIDEO_STABILIZATION_MODE_OFF
            )

            builder.set(
                CaptureRequest.JPEG_QUALITY,
                100.toByte()
            )

            builder.set(
                CaptureRequest.JPEG_ORIENTATION,
                90
            )

            apply200MpKeys(
                builder,
                "PREVIEW"
            )

            captureSession.setRepeatingRequest(
                builder.build(),
                object :
                    CameraCaptureSession.CaptureCallback() {

                    override fun onCaptureFailed(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        failure: CaptureFailure
                    ) {

                        log(
                            "Preview failed: ${failure.reason}"
                        )
                    }
                },
                cameraHandler
            )

            log("Preview running.")
            log("")
            log("Press CAPTURE 200 MP.")

        } catch (e: Throwable) {

            log("")
            log("PREVIEW ERROR")
            log(e.javaClass.name)
            log(e.message ?: "")
        }
    }

    private fun capture() {

        val camera =
            cameraDevice ?: run {

                log("CameraDevice missing.")
                return
            }

        val captureSession =
            session ?: run {

                log("Session missing.")
                return
            }

        val jpegSurface =
            jpegReader?.surface ?: return

        runOnUiThread {
            captureButton.isEnabled = false
        }

        log("")
        log("============================")
        log("SENDING 200 MP REQUEST")
        log("============================")

        try {

            val builder =
                camera.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE
                )

            builder.addTarget(
                jpegSurface
            )

            builder.set(
                CaptureRequest.CONTROL_MODE,
                CaptureRequest.CONTROL_MODE_USE_SCENE_MODE
            )

            builder.set(
                CaptureRequest.CONTROL_SCENE_MODE,
                CaptureRequest.CONTROL_SCENE_MODE_FACE_PRIORITY
            )

            builder.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON
            )

            builder.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest
                    .CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )

            builder.set(
                CaptureRequest.CONTROL_ENABLE_ZSL,
                false
            )

            builder.set(
                CaptureRequest.JPEG_QUALITY,
                100.toByte()
            )

            builder.set(
                CaptureRequest.JPEG_ORIENTATION,
                90
            )

            apply200MpKeys(
                builder,
                "CAPTURE"
            )

            captureSession.capture(
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
                            "Capture started, frame $frameNumber"
                        )
                    }

                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {

                        log("Capture request completed.")
                        log(
                            "Waiting for JPEG ImageReader..."
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
                        log("CAPTURE FAILED")
                        log(
                            "Reason: ${failure.reason}"
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
            log(e.javaClass.name)
            log(e.message ?: "")

            runOnUiThread {
                captureButton.isEnabled = true
            }
        }
    }

    private fun apply200MpKeys(
        builder: CaptureRequest.Builder,
        stage: String
    ) {

        log("")
        log("$stage vendor keys:")

        try {

            builder.set(
                aiHighResolutionKey,
                0
            )

            log("ai_highresolution = 0")

        } catch (e: Throwable) {

            log(
                "ai_highresolution FAILED: " +
                    e.javaClass.simpleName
            )
        }

        try {

            builder.set(
                ultraHighResolutionKey,
                1
            )

            log("ultra_highresolution = 1")

        } catch (e: Throwable) {

            log(
                "ultra_highresolution FAILED: " +
                    e.javaClass.simpleName
            )
        }

        try {

            builder.set(
                real200MpKey,
                1
            )

            log("real200mp_switch_on = 1")

        } catch (e: Throwable) {

            log(
                "real200mp_switch_on FAILED: " +
                    e.javaClass.simpleName
            )
        }

        /*
         * Vivo's active Camera 3 request reports this as 1.
         */
        try {

            builder.set(
                portraitHighResolutionKey,
                1.toByte()
            )

            log("portrait_high_resolution = 1")

        } catch (e: Throwable) {

            log(
                "portrait_high_resolution FAILED: " +
                    e.javaClass.simpleName
            )
        }
    }

    private fun saveJpeg(
        bytes: ByteArray
    ) {

        try {

            val folder =
                getExternalFilesDir(
                    Environment.DIRECTORY_PICTURES
                ) ?: return

            if (!folder.exists()) {
                folder.mkdirs()
            }

            val timestamp =
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.US
                ).format(Date())

            val file =
                File(
                    folder,
                    "Vivo_200MP_Test_$timestamp.jpg"
                )

            FileOutputStream(file).use {
                it.write(bytes)
            }

            log("")
            log("============================")
            log("JPEG SAVED")
            log("============================")

            log(file.absolutePath)

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
                "Expected target: 16320 x 12288"
            )

        } catch (e: Throwable) {

            log("")
            log("SAVE FAILED")
            log(e.toString())
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

            begin()

        } else {

            log("Camera permission denied.")
        }
    }

    override fun onDestroy() {

        ready = false

        try {
            session?.close()
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
            jpegReader?.close()
        } catch (_: Throwable) {
        }

        try {
            cameraThread.quitSafely()
        } catch (_: Throwable) {
        }

        super.onDestroy()
    }
}
