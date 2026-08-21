package com.example.vivo200mpprobe

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
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

        private const val FULL_WIDTH = 16320
        private const val FULL_HEIGHT = 12288
    }

    private lateinit var statusText: TextView
    private lateinit var captureButton: Button

    private lateinit var cameraThread: HandlerThread
    private lateinit var cameraHandler: Handler

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null

    private var previewReader: ImageReader? = null
    private var jpegReader: ImageReader? = null

    private var sessionReady = false

    /*
     * VERIFIED VIVO CAMERA 3 TAGS
     */

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

    private val vivoCameraIdKey =
        CaptureRequest.Key(
            "vivo.control.camera_id",
            Int::class.javaObjectType
        )

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        createUi()
        startCameraThread()

        log("VIVO CAMERA 3")
        log("STOCK 200 MP CONFIG TEST")
        log("==============================")
        log("")
        log("Physical Camera: 3")
        log("Preview: 1440 x 1080 YUV")
        log("JPEG target: 16320 x 12288")
        log("Target: 200.54 MP")
        log("")

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            startCamera()

        } else {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        }
    }

    private fun createUi() {

        val root = LinearLayout(this)

        root.orientation =
            LinearLayout.VERTICAL

        root.setPadding(
            24,
            40,
            24,
            40
        )

        captureButton =
            Button(this)

        captureButton.text =
            "CAPTURE 200 MP"

        captureButton.isEnabled =
            false

        statusText =
            TextView(this)

        statusText.textSize =
            13f

        captureButton.setOnClickListener {

            capture200Mp()
        }

        root.addView(
            captureButton
        )

        val scroll =
            ScrollView(this)

        scroll.addView(
            statusText
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

    private fun log(
        message: String
    ) {

        runOnUiThread {

            statusText.append(
                message
            )

            statusText.append(
                "\n"
            )
        }
    }

    private fun startCameraThread() {

        cameraThread =
            HandlerThread(
                "Vivo200MpCamera"
            )

        cameraThread.start()

        cameraHandler =
            Handler(
                cameraThread.looper
            )
    }

    private fun startCamera() {

        log("STEP 1")
        log("Creating stock-style streams...")
        log("")

        createReaders()

        openCamera3()
    }

    private fun createReaders() {

        /*
         * Stock Vivo Camera 3:
         * Stream 0 = 1440 x 1080 YUV
         * Stream 1 = 16320 x 12288 JPEG/BLOB
         */

        try {

            previewReader =
                ImageReader.newInstance(
                    PREVIEW_WIDTH,
                    PREVIEW_HEIGHT,
                    ImageFormat.YUV_420_888,
                    4
                )

            previewReader
                ?.setOnImageAvailableListener(
                    { reader ->

                        try {

                            reader
                                .acquireLatestImage()
                                ?.close()

                        } catch (
                            _: Throwable
                        ) {
                        }

                    },
                    cameraHandler
                )

            log(
                "OK: Preview reader " +
                    "$PREVIEW_WIDTH x $PREVIEW_HEIGHT"
            )

        } catch (
            e: Throwable
        ) {

            log(
                "PREVIEW READER ERROR"
            )

            log(
                e.javaClass.name
            )

            log(
                e.message ?: ""
            )
        }

        try {

            jpegReader =
                ImageReader.newInstance(
                    FULL_WIDTH,
                    FULL_HEIGHT,
                    ImageFormat.JPEG,
                    2
                )

            jpegReader
                ?.setOnImageAvailableListener(
                    { reader ->

                        var image:
                            android.media.Image? =
                            null

                        try {

                            image =
                                reader.acquireNextImage()

                            if (
                                image == null
                            ) {

                                log(
                                    "JPEG ImageReader returned null."
                                )

                                return@setOnImageAvailableListener
                            }

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

                            log("")
                            log("==============================")
                            log("JPEG RECEIVED")
                            log("==============================")

                            log(
                                "ImageReader: " +
                                    "${image.width} x ${image.height}"
                            )

                            log(
                                "Bytes: ${bytes.size}"
                            )

                            val dimensions =
                                parseJpegDimensions(
                                    bytes
                                )

                            if (
                                dimensions != null
                            ) {

                                val width =
                                    dimensions.first

                                val height =
                                    dimensions.second

                                val mp =
                                    width.toDouble() *
                                        height.toDouble() /
                                        1_000_000.0

                                log("")
                                log(
                                    "JPEG SOF:"
                                )

                                log(
                                    "$width x $height"
                                )

                                log(
                                    String.format(
                                        Locale.US,
                                        "%.2f MP",
                                        mp
                                    )
                                )

                                log("")
                                log("==============================")

                                if (
                                    width ==
                                    FULL_WIDTH &&
                                    height ==
                                    FULL_HEIGHT
                                ) {

                                    log(
                                        "SUCCESS:"
                                    )

                                    log(
                                        "TRUE 200 MP JPEG"
                                    )

                                } else {

                                    log(
                                        "NOT 200 MP"
                                    )

                                    log(
                                        "HAL returned " +
                                            "$width x $height"
                                    )
                                }

                                log("==============================")

                            } else {

                                log(
                                    "Could not parse JPEG SOF."
                                )
                            }

                            saveJpeg(
                                bytes
                            )

                        } catch (
                            e: Throwable
                        ) {

                            log("")
                            log(
                                "JPEG PROCESSING ERROR"
                            )

                            log(
                                e.javaClass.name
                            )

                            log(
                                e.message ?: ""
                            )

                        } finally {

                            try {

                                image?.close()

                            } catch (
                                _: Throwable
                            ) {
                            }
                        }

                    },
                    cameraHandler
                )

            log(
                "OK: JPEG reader " +
                    "$FULL_WIDTH x $FULL_HEIGHT"
            )

        } catch (
            e: Throwable
        ) {

            log(
                "JPEG READER ERROR"
            )

            log(
                e.javaClass.name
            )

            log(
                e.message ?: ""
            )
        }
    }

    private fun openCamera3() {

        val manager =
            getSystemService(
                Context.CAMERA_SERVICE
            ) as CameraManager

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {

            return
        }

        log("")
        log("STEP 2")
        log("Opening Camera 3...")

        try {

            manager.openCamera(

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

                        createSession(
                            camera
                        )
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

                        when (
                            error
                        ) {

                            ERROR_CAMERA_IN_USE ->
                                log(
                                    "ERROR_CAMERA_IN_USE"
                                )

                            ERROR_MAX_CAMERAS_IN_USE ->
                                log(
                                    "ERROR_MAX_CAMERAS_IN_USE"
                                )

                            ERROR_CAMERA_DISABLED ->
                                log(
                                    "ERROR_CAMERA_DISABLED"
                                )

                            ERROR_CAMERA_DEVICE ->
                                log(
                                    "ERROR_CAMERA_DEVICE"
                                )

                            ERROR_CAMERA_SERVICE ->
                                log(
                                    "ERROR_CAMERA_SERVICE"
                                )
                        }

                        camera.close()

                        cameraDevice =
                            null
                    }
                },

                cameraHandler
            )

        } catch (
            e: Throwable
        ) {

            log(
                "OPEN CAMERA EXCEPTION"
            )

            log(
                e.javaClass.name
            )

            log(
                e.message ?: ""
            )
        }
    }

    private fun createSession(
        camera: CameraDevice
    ) {

        val previewSurface =
            previewReader
                ?.surface
                ?: return

        val jpegSurface =
            jpegReader
                ?.surface
                ?: return

        log("")
        log("STEP 3")
        log("Creating stock-style session...")
        log("")

        val callback =
            object :
                CameraCaptureSession.StateCallback() {

                override fun onConfigured(
                    session:
                        CameraCaptureSession
                ) {

                    captureSession =
                        session

                    sessionReady =
                        true

                    log(
                        "SESSION CONFIGURED"
                    )

                    log(
                        "Starting Camera 3 preview..."
                    )

                    startPreview(
                        session
                    )
                }

                override fun onConfigureFailed(
                    session:
                        CameraCaptureSession
                ) {

                    sessionReady =
                        false

                    log("")
                    log(
                        "SESSION CONFIGURATION FAILED"
                    )
                }
            }

        try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.P
            ) {

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
                    Executor {
                        runnable ->

                        cameraHandler.post(
                            runnable
                        )
                    }

                val config =
                    SessionConfiguration(
                        SessionConfiguration.SESSION_REGULAR,
                        outputs,
                        executor,
                        callback
                    )

                /*
                 * Session parameters copied from
                 * verified Camera 3 200 MP state.
                 */

                val builder =
                    camera.createCaptureRequest(
                        CameraDevice.TEMPLATE_PREVIEW
                    )

                builder.addTarget(
                    previewSurface
                )

                applyStock200MpControls(
                    builder,
                    "SESSION"
                )

                config.setSessionParameters(
                    builder.build()
                )

                camera.createCaptureSession(
                    config
                )

            } else {

                @Suppress(
                    "DEPRECATION"
                )

                camera.createCaptureSession(
                    listOf(
                        previewSurface,
                        jpegSurface
                    ),
                    callback,
                    cameraHandler
                )
            }

        } catch (
            e: Throwable
        ) {

            log("")
            log(
                "CREATE SESSION ERROR"
            )

            log(
                e.javaClass.name
            )

            log(
                e.message ?: ""
            )
        }
    }

    private fun startPreview(
        session:
            CameraCaptureSession
    ) {

        val camera =
            cameraDevice
                ?: return

        val surface =
            previewReader
                ?.surface
                ?: return

        try {

            val builder =
                camera.createCaptureRequest(
                    CameraDevice.TEMPLATE_PREVIEW
                )

            builder.addTarget(
                surface
            )

            builder.set(
                CaptureRequest.CONTROL_MODE,
                CameraMetadata.CONTROL_MODE_AUTO
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

            applyStock200MpControls(
                builder,
                "PREVIEW"
            )

            session.setRepeatingRequest(

                builder.build(),

                object :
                    CameraCaptureSession.CaptureCallback() {

                    override fun onCaptureFailed(
                        session:
                            CameraCaptureSession,
                        request:
                            CaptureRequest,
                        failure:
                            CaptureFailure
                    ) {

                        log(
                            "Preview failure: " +
                                failure.reason
                        )
                    }
                },

                cameraHandler
            )

            log("")
            log("==============================")
            log("CAMERA 3 PREVIEW RUNNING")
            log("==============================")
            log("")
            log("Press CAPTURE 200 MP.")

            runOnUiThread {

                captureButton
                    .isEnabled =
                    true
            }

        } catch (
            e: Throwable
        ) {

            log("")
            log(
                "PREVIEW ERROR"
            )

            log(
                e.javaClass.name
            )

            log(
                e.message ?: ""
            )
        }
    }

    private fun capture200Mp() {

        if (
            !sessionReady
        ) {

            log(
                "Session not ready."
            )

            return
        }

        val camera =
            cameraDevice
                ?: return

        val session =
            captureSession
                ?: return

        val jpegSurface =
            jpegReader
                ?.surface
                ?: return

        runOnUiThread {

            captureButton
                .isEnabled =
                false
        }

        log("")
        log("==============================")
        log("200 MP CAPTURE REQUEST")
        log("==============================")

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
                CameraMetadata.CONTROL_MODE_AUTO
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
                CaptureRequest.JPEG_QUALITY,
                100.toByte()
            )

            builder.set(
                CaptureRequest.JPEG_ORIENTATION,
                90
            )

            /*
             * We currently keep Camera 3's verified
             * streamsUsage value here rather than
             * copying the Camera 0 value from dumps 5-7.
             */

            applyStock200MpControls(
                builder,
                "CAPTURE"
            )

            session.capture(

                builder.build(),

                object :
                    CameraCaptureSession.CaptureCallback() {

                    override fun onCaptureStarted(
                        session:
                            CameraCaptureSession,
                        request:
                            CaptureRequest,
                        timestamp:
                            Long,
                        frameNumber:
                            Long
                    ) {

                        log(
                            "Capture started."
                        )

                        log(
                            "Frame: $frameNumber"
                        )
                    }

                    override fun onCaptureCompleted(
                        session:
                            CameraCaptureSession,
                        request:
                            CaptureRequest,
                        result:
                            TotalCaptureResult
                    ) {

                        log(
                            "Capture request completed."
                        )

                        log(
                            "Waiting for JPEG..."
                        )

                        runOnUiThread {

                            captureButton
                                .isEnabled =
                                true
                        }
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
                            "Reason: " +
                                failure.reason
                        )

                        runOnUiThread {

                            captureButton
                                .isEnabled =
                                true
                        }
                    }
                },

                cameraHandler
            )

        } catch (
            e: Throwable
        ) {

            log("")
            log(
                "CAPTURE EXCEPTION"
            )

            log(
                e.javaClass.name
            )

            log(
                e.message ?: ""
            )

            runOnUiThread {

                captureButton
                    .isEnabled =
                    true
            }
        }
    }

    private fun applyStock200MpControls(
        builder:
            CaptureRequest.Builder,
        stage:
            String
    ) {

        log("")
        log("$stage VIVO CONTROLS")

        try {

            builder.set(
                aiHighResolutionKey,
                0
            )

            log(
                "OK ai_highresolution = 0"
            )

        } catch (
            e: Throwable
        ) {

            log(
                "FAIL ai_highresolution: " +
                    (e.message ?: "")
            )
        }

        try {

            builder.set(
                portraitHighResolutionKey,
                1.toByte()
            )

            log(
                "OK portrait_high_resolution = 1"
            )

        } catch (
            e: Throwable
        ) {

            log(
                "FAIL portrait_high_resolution: " +
                    (e.message ?: "")
            )
        }

        try {

            builder.set(
                ultraHighResolutionKey,
                1
            )

            log(
                "OK ultra_highresolution = 1"
            )

        } catch (
            e: Throwable
        ) {

            log(
                "FAIL ultra_highresolution: " +
                    (e.message ?: "")
            )
        }

        try {

            builder.set(
                real200mpKey,
                1
            )

            log(
                "OK real200mp_switch_on = 1"
            )

        } catch (
            e: Throwable
        ) {

            log(
                "FAIL real200mp_switch_on: " +
                    (e.message ?: "")
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
                "OK streamsUsage = [2, 1, 0]"
            )

        } catch (
            e: Throwable
        ) {

            log(
                "FAIL streamsUsage: " +
                    (e.message ?: "")
            )
        }

        try {

            builder.set(
                vivoCameraIdKey,
                3
            )

            log(
                "OK camera_id = 3"
            )

        } catch (
            e: Throwable
        ) {

            log(
                "FAIL camera_id: " +
                    (e.message ?: "")
            )
        }
    }

    private fun saveJpeg(
        bytes: ByteArray
    ) {

        try {

            val dir =
                getExternalFilesDir(
                    android.os.Environment
                        .DIRECTORY_PICTURES
                )
                    ?: return

            if (
                !dir.exists()
            ) {

                dir.mkdirs()
            }

            val time =
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.US
                ).format(
                    Date()
                )

            val file =
                File(
                    dir,
                    "Vivo_Camera3_200MP_$time.jpg"
                )

            FileOutputStream(
                file
            ).use {

                it.write(
                    bytes
                )
            }

            val mb =
                file.length()
                    .toDouble() /
                    1024.0 /
                    1024.0

            log("")
            log(
                "Saved:"
            )

            log(
                file.absolutePath
            )

            log(
                String.format(
                    Locale.US,
                    "File size: %.3f MB",
                    mb
                )
            )

        } catch (
            e: Throwable
        ) {

            log(
                "SAVE ERROR"
            )

            log(
                e.message ?: ""
            )
        }
    }

    private fun parseJpegDimensions(
        data: ByteArray
    ): Pair<Int, Int>? {

        if (
            data.size < 4
        ) {

            return null
        }

        if (
            (data[0].toInt() and 0xFF) !=
            0xFF ||
            (data[1].toInt() and 0xFF) !=
            0xD8
        ) {

            return null
        }

        var offset =
            2

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
                    (data[offset]
                        .toInt() and
                        0xFF) shl 8
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

            val isSof =
                marker == 0xC0 ||
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

            if (
                isSof
            ) {

                if (
                    offset + 6 >=
                    data.size
                ) {

                    return null
                }

                val height =
                    (
                        (data[offset + 3]
                            .toInt() and
                            0xFF) shl 8
                        ) or
                        (
                            data[offset + 4]
                                .toInt() and
                                0xFF
                            )

                val width =
                    (
                        (data[offset + 5]
                            .toInt() and
                            0xFF) shl 8
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

            startCamera()

        } else {

            log(
                "Camera permission denied."
            )
        }
    }

    override fun onDestroy() {

        sessionReady =
            false

        try {

            captureSession
                ?.close()

        } catch (
            _: Throwable
        ) {
        }

        try {

            cameraDevice
                ?.close()

        } catch (
            _: Throwable
        ) {
        }

        try {

            previewReader
                ?.close()

        } catch (
            _: Throwable
        ) {
        }

        try {

            jpegReader
                ?.close()

        } catch (
            _: Throwable
        ) {
        }

        try {

            cameraThread
                .quitSafely()

        } catch (
            _: Throwable
        ) {
        }

        super.onDestroy()
    }
}
