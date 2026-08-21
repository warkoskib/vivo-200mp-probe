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

    // Vivo vendor controls
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

        log("VIVO 200 MP JPEG HEADER TEST")
        log("==============================")
        log("")
        log("Camera ID: $CAMERA_ID")
        log("Requested: $FULL_WIDTH x $FULL_HEIGHT")

        val requestedMp =
            FULL_WIDTH.toDouble() *
                FULL_HEIGHT.toDouble() /
                1_000_000.0

        log(
            "Requested MP: ${
                String.format(
                    Locale.US,
                    "%.2f",
                    requestedMp
                )
            }"
        )

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

        val scrollView =
            ScrollView(this)

        scrollView.addView(
            statusText
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
                "Vivo200MpThread"
            )

        cameraThread.start()

        cameraHandler =
            Handler(
                cameraThread.looper
            )
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

            fullResReader
                ?.setOnImageAvailableListener(
                    { reader ->

                        var image:
                            android.media.Image? =
                            null

                        try {

                            image =
                                reader.acquireNextImage()

                            if (image == null) {

                                log(
                                    "ImageReader returned null."
                                )

                                return@setOnImageAvailableListener
                            }

                            val readerWidth =
                                image.width

                            val readerHeight =
                                image.height

                            val readerMp =
                                readerWidth.toDouble() *
                                    readerHeight.toDouble() /
                                    1_000_000.0

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
                                "ImageReader dimensions: " +
                                    "$readerWidth x $readerHeight"
                            )

                            log(
                                "ImageReader MP: ${
                                    String.format(
                                        Locale.US,
                                        "%.2f",
                                        readerMp
                                    )
                                }"
                            )

                            log(
                                "JPEG bytes: ${bytes.size}"
                            )

                            val file =
                                saveImage(
                                    bytes
                                )

                            verifyJpeg(
                                bytes,
                                file,
                                readerWidth,
                                readerHeight
                            )

                        } catch (
                            e: Throwable
                        ) {

                            log("")
                            log(
                                "IMAGE PROCESSING ERROR"
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
                "SUCCESS: ImageReader created."
            )

            log(
                "$FULL_WIDTH x $FULL_HEIGHT"
            )

        } catch (
            e: Throwable
        ) {

            log("")
            log(
                "IMAGE READER FAILED"
            )

            log(
                e.javaClass.name
            )

            log(
                e.message ?: ""
            )

            return
        }

        openCamera()
    }

    private fun openCamera() {

        val manager =
            getSystemService(
                Context.CAMERA_SERVICE
            ) as CameraManager

        if (
            ActivityCompat
                .checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        log("")
        log("STEP 2 - OPEN CAMERA 3")
        log("------------------------------")

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

                        createStillOnlySession(
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

                        when (error) {

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

            log("")
            log(
                "OPEN CAMERA FAILED"
            )

            log(
                e.javaClass.name
            )

            log(
                e.message ?: ""
            )
        }
    }

    private fun createStillOnlySession(
        camera: CameraDevice
    ) {

        val surface =
            fullResReader
                ?.surface
                ?: return

        log("")
        log(
            "STEP 3 - CREATE STILL-ONLY SESSION"
        )

        log(
            "------------------------------"
        )

        val callback =
            object :
                CameraCaptureSession
                    .StateCallback() {

                override fun onConfigured(
                    session:
                        CameraCaptureSession
                ) {

                    captureSession =
                        session

                    sessionReady =
                        true

                    log("")
                    log(
                        "******************************"
                    )

                    log(
                        "STILL-ONLY SESSION CONFIGURED"
                    )

                    log(
                        "******************************"
                    )

                    log("")
                    log(
                        "No preview request was started."
                    )

                    log(
                        "Press CAPTURE 200 MP."
                    )

                    runOnUiThread {

                        captureButton
                            .isEnabled =
                            true
                    }
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
                            surface
                        )
                    )

                val executor =
                    Executor {
                        runnable ->

                        cameraHandler.post(
                            runnable
                        )
                    }

                val configuration =
                    SessionConfiguration(
                        SessionConfiguration
                            .SESSION_REGULAR,
                        outputs,
                        executor,
                        callback
                    )

                try {

                    val builder =
                        camera
                            .createCaptureRequest(
                                CameraDevice
                                    .TEMPLATE_STILL_CAPTURE
                            )

                    builder.addTarget(
                        surface
                    )

                    applyVivoKeys(
                        builder,
                        "SESSION"
                    )

                    configuration
                        .setSessionParameters(
                            builder.build()
                        )

                    log(
                        "Vivo session parameters attached."
                    )

                } catch (
                    e: Throwable
                ) {

                    log(
                        "Session parameter error:"
                    )

                    log(
                        e.javaClass.name
                    )

                    log(
                        e.message ?: ""
                    )
                }

                camera.createCaptureSession(
                    configuration
                )

            } else {

                @Suppress(
                    "DEPRECATION"
                )

                camera.createCaptureSession(
                    listOf(
                        surface
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
                "CREATE SESSION EXCEPTION"
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

        if (!sessionReady) {

            log(
                "Session is not ready."
            )

            return
        }

        val camera =
            cameraDevice

        val session =
            captureSession

        val surface =
            fullResReader
                ?.surface

        if (
            camera == null ||
            session == null ||
            surface == null
        ) {

            log("")
            log(
                "Capture cannot start."
            )

            log(
                "Camera/session/surface missing."
            )

            return
        }

        runOnUiThread {

            captureButton
                .isEnabled =
                false
        }

        log("")
        log("==============================")
        log("SENDING STILL CAPTURE")
        log("==============================")

        try {

            val builder =
                camera
                    .createCaptureRequest(
                        CameraDevice
                            .TEMPLATE_STILL_CAPTURE
                    )

            builder.addTarget(
                surface
            )

            builder.set(
                CaptureRequest
                    .CONTROL_AE_MODE,
                CaptureRequest
                    .CONTROL_AE_MODE_ON
            )

            builder.set(
                CaptureRequest
                    .CONTROL_AF_MODE,
                CaptureRequest
                    .CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )

            builder.set(
                CaptureRequest
                    .JPEG_QUALITY,
                100.toByte()
            )

            builder.set(
                CaptureRequest
                    .JPEG_ORIENTATION,
                90
            )

            applyVivoKeys(
                builder,
                "CAPTURE"
            )

            session.capture(

                builder.build(),

                object :
                    CameraCaptureSession
                        .CaptureCallback() {

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

                        log("")
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

                        log("")
                        log(
                            "Capture request completed."
                        )

                        log(
                            "Waiting for JPEG verification..."
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
                            "Reason: ${failure.reason}"
                        )

                        log(
                            "Frame: ${failure.frameNumber}"
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

    private fun applyVivoKeys(
        builder:
            CaptureRequest.Builder,
        stage:
            String
    ) {

        log("")
        log(
            "$stage VIVO KEYS"
        )

        log(
            "------------------------------"
        )

        try {

            builder.set(
                aiHighResolutionKey,
                0
            )

            log(
                "OK: ai_highresolution = 0"
            )

        } catch (
            e: Throwable
        ) {

            log(
                "FAILED: ai_highresolution"
            )

            log(
                e.message ?: ""
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

        } catch (
            e: Throwable
        ) {

            log(
                "FAILED: ultra_highresolution"
            )

            log(
                e.message ?: ""
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

        } catch (
            e: Throwable
        ) {

            log(
                "FAILED: real200mp_switch_on"
            )

            log(
                e.message ?: ""
            )
        }
    }

    private fun saveImage(
        data: ByteArray
    ): File? {

        return try {

            val dir =
                getExternalFilesDir(
                    android.os
                        .Environment
                        .DIRECTORY_PICTURES
                ) ?: return null

            if (!dir.exists()) {

                dir.mkdirs()
            }

            val timestamp =
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.US
                ).format(
                    Date()
                )

            val file =
                File(
                    dir,
                    "Vivo_200MP_Verify_$timestamp.jpg"
                )

            FileOutputStream(
                file
            ).use {

                it.write(
                    data
                )
            }

            log("")
            log("==============================")
            log("JPEG SAVED")
            log("==============================")

            log(
                file.absolutePath
            )

            val mb =
                file.length()
                    .toDouble() /
                    1024.0 /
                    1024.0

            log(
                "File size: ${
                    String.format(
                        Locale.US,
                        "%.3f MB",
                        mb
                    )
                }"
            )

            file

        } catch (
            e: Throwable
        ) {

            log("")
            log(
                "SAVE FAILED"
            )

            log(
                e.javaClass.name
            )

            log(
                e.message ?: ""
            )

            null
        }
    }

    private fun verifyJpeg(
        bytes: ByteArray,
        file: File?,
        readerWidth: Int,
        readerHeight: Int
    ) {

        log("")
        log("==============================")
        log("JPEG SOF HEADER")
        log("==============================")

        val jpegDimensions =
            parseJpegDimensions(
                bytes
            )

        if (
            jpegDimensions == null
        ) {

            log(
                "ERROR: JPEG SOF marker not found."
            )

            log("")
            log(
                "Could not verify encoded JPEG dimensions."
            )

            return
        }

        val jpegWidth =
            jpegDimensions.first

        val jpegHeight =
            jpegDimensions.second

        val jpegMp =
            jpegWidth.toDouble() *
                jpegHeight.toDouble() /
                1_000_000.0

        log(
            "JPEG encoded dimensions:"
        )

        log(
            "$jpegWidth x $jpegHeight"
        )

        log(
            "JPEG encoded MP: ${
                String.format(
                    Locale.US,
                    "%.2f MP",
                    jpegMp
                )
            }"
        )

        log("")
        log("==============================")
        log("COMPARISON")
        log("==============================")

        log(
            "Requested:"
        )

        log(
            "$FULL_WIDTH x $FULL_HEIGHT"
        )

        log("")

        log(
            "ImageReader:"
        )

        log(
            "$readerWidth x $readerHeight"
        )

        log("")

        log(
            "JPEG SOF:"
        )

        log(
            "$jpegWidth x $jpegHeight"
        )

        if (
            file != null
        ) {

            val fileMb =
                file.length()
                    .toDouble() /
                    1024.0 /
                    1024.0

            log("")

            log(
                "JPEG file size:"
            )

            log(
                String.format(
                    Locale.US,
                    "%.3f MB",
                    fileMb
                )
            )
        }

        log("")
        log("==============================")
        log("VERIFICATION RESULT")
        log("==============================")

        if (
            jpegWidth ==
            FULL_WIDTH &&
            jpegHeight ==
            FULL_HEIGHT
        ) {

            log(
                "JPEG HEADER CONFIRMS 200 MP"
            )

            log(
                "$jpegWidth x $jpegHeight"
            )

            log(
                String.format(
                    Locale.US,
                    "%.2f MP",
                    jpegMp
                )
            )

            log("")
            log(
                "The encoded JPEG itself reports"
            )

            log(
                "the requested 200 MP dimensions."
            )

        } else {

            log(
                "JPEG IS NOT ENCODED AT 200 MP"
            )

            log("")

            log(
                "ImageReader:"
            )

            log(
                "$readerWidth x $readerHeight"
            )

            log(
                "JPEG:"
            )

            log(
                "$jpegWidth x $jpegHeight"
            )

            log("")

            log(
                "The ImageReader dimensions do not"
            )

            log(
                "match the encoded JPEG dimensions."
            )
        }
    }

    /*
     * Reads the JPEG byte stream directly.
     *
     * JPEG SOF markers contain the actual
     * encoded image width and height.
     *
     * We do NOT need to decode a 200 MP bitmap.
     */
    private fun parseJpegDimensions(
        data: ByteArray
    ): Pair<Int, Int>? {

        if (
            data.size < 4
        ) {
            return null
        }

        // JPEG must begin FF D8
        if (
            (data[0].toInt() and 0xFF) !=
            0xFF ||
            (data[1].toInt() and 0xFF) !=
            0xD8
        ) {

            log(
                "WARNING: JPEG SOI marker missing."
            )

            return null
        }

        var offset =
            2

        while (
            offset <
            data.size - 1
        ) {

            if (
                (data[offset]
                    .toInt() and 0xFF) !=
                0xFF
            ) {

                offset++

                continue
            }

            // Skip repeated FF padding
            while (
                offset <
                data.size &&
                (data[offset]
                    .toInt() and 0xFF) ==
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

            // Standalone markers
            if (
                marker == 0xD8 ||
                marker == 0xD9 ||
                marker in
                    0xD0..0xD7 ||
                marker == 0x01
            ) {

                continue
            }

            // Start of Scan.
            // SOF should already have appeared.
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

            val segmentLength =
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
                segmentLength < 2
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

                /*
                 * Segment:
                 *
                 * offset + 0 = length high
                 * offset + 1 = length low
                 * offset + 2 = precision
                 * offset + 3 = height high
                 * offset + 4 = height low
                 * offset + 5 = width high
                 * offset + 6 = width low
                 */

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
                segmentLength
        }

        return null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
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
            CAMERA_PERMISSION_REQUEST &&
            grantResults
                .isNotEmpty() &&
            grantResults[0] ==
            PackageManager
                .PERMISSION_GRANTED
        ) {

            startTest()

        } else {

            log(
                "Camera permission denied."
            )
        }
    }

    override fun onDestroy() {

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

            fullResReader
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
