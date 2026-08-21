package com.example.vivo200mpprobe

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.lang.reflect.Field
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_ID = "3"
        private const val REQUEST_CAMERA = 1001
    }

    private lateinit var cameraManager: CameraManager
    private lateinit var output: TextView
    private lateinit var scanButton: Button

    private var cameraDevice: CameraDevice? = null

    /*
     * These are the keys that matter most to the
     * 200 MP / remosaic path we have discovered.
     */
    private val targetKeyNames = listOf(

        // Vivo 200 MP / resolution controls
        "vivo.control.real200mp_switch_on",
        "vivo.control.ultra_highresolution",
        "vivo.control.portrait_high_resolution",
        "vivo.control.ai_highresolution",

        // Sensor mode controls
        "vivo.control.forceSensorMode",
        "vivo.control.sensorMode",
        "vivo.preview.sensorMode",
        "vivo.parameter.niceCaptureSensorMode",

        // Vivo remosaic
        "vivo.control.EngineerRemosaicMode",
        "vivo.control.advance_fullsize",
        "vivo.control.remosaic.capability",
        "vivo.control.seamless.remosaic.enable",
        "vivo.control.seamless.roiRemosaic",
        "vivo.parameter.remosaicType",
        "vivo.parameter.remosaicOTPData",

        // MediaTek remosaic
        "com.mediatek.control.capture.remosaicenable",
        "com.mediatek.control.capture.seamless.remosaicenable",

        // MediaTek sensor mode
        "com.mediatek.seamlessfeature.cameraScenario",
        "com.mediatek.seamlessfeature.sensorScenario",
        "com.mediatek.seamlessfeature.sensorScenarioCustomHint",
        "com.mediatek.seamlessfeature.sensorScenarioSwitchPolicy",

        // Stream / image size controls
        "vivo.control.picturesize.value",
        "vivo.control.snapJpegSize",
        "vivo.control.snapshotYuvStreamMap",
        "vivo.control.streamsUsage",
        "vivo.control.vcfStreamType",
        "vcf.parameter.SnapshotJpegStreamMap",
        "vcf.parameter.sensorSizeList",

        // Other potentially useful capture controls
        "vivo.control.isCapture",
        "vivo.control.is_snapshot",
        "vivo.control.isUpscale",
        "vivo.control.isNativeMode",
        "vivo.control.raw_capture_type",
        "vivo.control.is_ProRaw_on",
        "vivo.parameter.highResolutionDngType",
        "vivo.parameter.niceCaptureInfoMask"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraManager =
            getSystemService(CAMERA_SERVICE) as CameraManager

        buildUi()

        log("VIVO 200 MP KEY TYPE PROBE")
        log("==============================")
        log("")
        log("Camera ID: $CAMERA_ID")
        log("Device: ${Build.MODEL}")
        log("Android: ${Build.VERSION.RELEASE}")
        log("")
        log("This probe does NOT capture an image.")
        log("")
        log("Press SCAN KEY TYPES.")
    }

    private fun buildUi() {

        val root =
            LinearLayout(this)

        root.orientation =
            LinearLayout.VERTICAL

        root.setPadding(
            20,
            30,
            20,
            30
        )

        scanButton =
            Button(this)

        scanButton.text =
            "SCAN KEY TYPES"

        scanButton.setOnClickListener {

            output.text = ""

            if (
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.CAMERA
                    ),
                    REQUEST_CAMERA
                )

            } else {

                startProbe()
            }
        }

        root.addView(scanButton)

        val copyButton =
            Button(this)

        copyButton.text =
            "COPY OUTPUT"

        copyButton.setOnClickListener {

            val text =
                output.text.toString()

            if (text.isBlank()) {

                Toast.makeText(
                    this,
                    "No output yet.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val clipboard =
                getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as ClipboardManager

            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    "Vivo 200MP Key Types",
                    text
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
            12f

        output.setTextIsSelectable(
            true
        )

        output.setPadding(
            0,
            20,
            0,
            150
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

    private fun startProbe() {

        scanButton.isEnabled =
            false

        log(
            "VIVO 200 MP KEY TYPE PROBE"
        )

        log(
            "=============================="
        )

        log("")

        dumpCharacteristics()

        openCameraForRequestDefaults()
    }

    /*
     * First inspect CameraCharacteristics.
     *
     * This lets us see if any of the target names
     * are characteristic keys with actual values.
     */
    private fun dumpCharacteristics() {

        try {

            val chars =
                cameraManager.getCameraCharacteristics(
                    CAMERA_ID
                )

            log("")
            log(
                "=============================="
            )

            log(
                "TARGET CHARACTERISTIC VALUES"
            )

            log(
                "=============================="
            )

            var matches = 0

            for (key in chars.keys) {

                if (
                    targetKeyNames.contains(
                        key.name
                    )
                ) {

                    matches++

                    log("")
                    log(
                        "KEY:"
                    )

                    log(
                        key.name
                    )

                    try {

                        val value =
                            chars.get(key)

                        log(
                            "VALUE:"
                        )

                        log(
                            formatValue(value)
                        )

                        log(
                            "RUNTIME TYPE:"
                        )

                        log(
                            value?.javaClass?.name
                                ?: "null"
                        )

                    } catch (
                        e: Throwable
                    ) {

                        log(
                            "VALUE READ ERROR:"
                        )

                        log(
                            e.javaClass.name +
                                ": " +
                                (e.message ?: "")
                        )
                    }

                    dumpReflectionInfo(
                        key
                    )
                }
            }

            log("")
            log(
                "Characteristic matches: $matches"
            )

            /*
             * Summary of whether target keys appear
             * in the request/session/result lists.
             */

            log("")
            log(
                "=============================="
            )

            log(
                "TARGET KEY AVAILABILITY"
            )

            log(
                "=============================="
            )

            val requestNames =
                chars.availableCaptureRequestKeys
                    .map {
                        it.name
                    }
                    .toSet()

            val resultNames =
                chars.availableCaptureResultKeys
                    .map {
                        it.name
                    }
                    .toSet()

            val sessionNames =
                if (
                    Build.VERSION.SDK_INT >= 28
                ) {

                    chars.availableSessionKeys
                        ?.map {
                            it.name
                        }
                        ?.toSet()
                        ?: emptySet()

                } else {

                    emptySet()
                }

            for (
                name in targetKeyNames
            ) {

                val isRequest =
                    requestNames.contains(
                        name
                    )

                val isResult =
                    resultNames.contains(
                        name
                    )

                val isSession =
                    sessionNames.contains(
                        name
                    )

                if (
                    isRequest ||
                    isResult ||
                    isSession
                ) {

                    log("")
                    log(
                        name
                    )

                    log(
                        "  REQUEST = $isRequest"
                    )

                    log(
                        "  RESULT  = $isResult"
                    )

                    log(
                        "  SESSION = $isSession"
                    )
                }
            }

        } catch (
            e: Throwable
        ) {

            log("")
            log(
                "CHARACTERISTICS ERROR"
            )

            log(
                e.javaClass.name
            )

            log(
                e.message ?: ""
            )
        }
    }

    /*
     * Open Camera 3 only so Android will let us create
     * CaptureRequest templates and inspect their default
     * values.
     *
     * No capture session is created.
     * No image is taken.
     */
    private fun openCameraForRequestDefaults() {

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }

        log("")
        log(
            "=============================="
        )

        log(
            "OPENING CAMERA 3"
        )

        log(
            "=============================="
        )

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
                            "Camera opened."
                        )

                        inspectRequestKeys(
                            camera
                        )

                        try {
                            camera.close()
                        } catch (_: Throwable) {
                        }

                        cameraDevice =
                            null

                        log("")
                        log(
                            "=============================="
                        )

                        log(
                            "PROBE COMPLETE"
                        )

                        log(
                            "=============================="
                        )

                        log("")
                        log(
                            "No image was captured."
                        )

                        log("")
                        log(
                            "Press COPY OUTPUT."
                        )

                        runOnUiThread {
                            scanButton.isEnabled =
                                true
                        }
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

                        runOnUiThread {
                            scanButton.isEnabled =
                                true
                        }
                    }

                    override fun onError(
                        camera: CameraDevice,
                        error: Int
                    ) {

                        log(
                            "Camera error: $error"
                        )

                        camera.close()

                        cameraDevice =
                            null

                        runOnUiThread {
                            scanButton.isEnabled =
                                true
                        }
                    }
                },

                null
            )

        } catch (
            e: Throwable
        ) {

            log(
                "OPEN CAMERA ERROR"
            )

            log(
                e.javaClass.name
            )

            log(
                e.message ?: ""
            )

            runOnUiThread {
                scanButton.isEnabled =
                    true
            }
        }
    }

    private fun inspectRequestKeys(
        camera: CameraDevice
    ) {

        val chars =
            cameraManager.getCameraCharacteristics(
                CAMERA_ID
            )

        val requestKeys =
            chars.availableCaptureRequestKeys

        val sessionKeys =
            if (
                Build.VERSION.SDK_INT >= 28
            ) {

                chars.availableSessionKeys
                    ?: emptyList()

            } else {

                emptyList()
            }

        log("")
        log(
            "=============================="
        )

        log(
            "TARGET REQUEST KEY TYPES"
        )

        log(
            "=============================="
        )

        /*
         * Inspect both PREVIEW and STILL templates because
         * Vivo may give different defaults depending on
         * request template.
         */

        inspectTemplate(
            camera,
            CameraDevice.TEMPLATE_PREVIEW,
            "TEMPLATE_PREVIEW",
            requestKeys,
            sessionKeys
        )

        inspectTemplate(
            camera,
            CameraDevice.TEMPLATE_STILL_CAPTURE,
            "TEMPLATE_STILL_CAPTURE",
            requestKeys,
            sessionKeys
        )
    }

    private fun inspectTemplate(
        camera: CameraDevice,
        template: Int,
        templateName: String,
        requestKeys:
            List<CaptureRequest.Key<*>>,
        sessionKeys:
            List<CaptureRequest.Key<*>>
    ) {

        log("")
        log("")
        log(
            "################################"
        )

        log(
            templateName
        )

        log(
            "################################"
        )

        val builder =
            try {

                camera.createCaptureRequest(
                    template
                )

            } catch (
                e: Throwable
            ) {

                log(
                    "Could not create template:"
                )

                log(
                    e.toString()
                )

                return
            }

        val request =
            try {

                builder.build()

            } catch (
                e: Throwable
            ) {

                log(
                    "Could not build request:"
                )

                log(
                    e.toString()
                )

                return
            }

        val keyMap =
            mutableMapOf<
                String,
                CaptureRequest.Key<*>
                >()

        for (
            key in requestKeys
        ) {

            keyMap[
                key.name
            ] = key
        }

        for (
            key in sessionKeys
        ) {

            keyMap[
                key.name
            ] = key
        }

        for (
            name in targetKeyNames
        ) {

            val key =
                keyMap[name]
                    ?: continue

            log("")
            log(
                "--------------------------------"
            )

            log(
                name
            )

            val isSession =
                sessionKeys.any {
                    it.name ==
                        name
                }

            log(
                "SESSION KEY: $isSession"
            )

            /*
             * Try reading the template's default value.
             */

            try {

                @Suppress(
                    "UNCHECKED_CAST"
                )

                val genericKey =
                    key as
                        CaptureRequest.Key<Any>

                val value =
                    request.get(
                        genericKey
                    )

                log(
                    "DEFAULT VALUE:"
                )

                log(
                    formatValue(value)
                )

                log(
                    "DEFAULT VALUE CLASS:"
                )

                log(
                    value?.javaClass?.name
                        ?: "null"
                )

            } catch (
                e: Throwable
            ) {

                log(
                    "DEFAULT READ ERROR:"
                )

                log(
                    e.javaClass.name +
                        ": " +
                        (e.message ?: "")
                )
            }

            /*
             * Now inspect the actual CaptureRequest.Key
             * object with reflection.
             */

            dumpReflectionInfo(
                key
            )
        }
    }

    /*
     * Attempt to identify the internal type Android associates
     * with the metadata key.
     *
     * Some Android versions block private-field reflection.
     * That's fine -- we report exactly what was accessible.
     */
    private fun dumpReflectionInfo(
        keyObject: Any
    ) {

        log(
            "REFLECTION:"
        )

        try {

            log(
                "Key Java class: " +
                    keyObject.javaClass.name
            )

            dumpObjectFields(
                keyObject,
                "  ",
                0
            )

        } catch (
            e: Throwable
        ) {

            log(
                "  Reflection failed:"
            )

            log(
                "  " +
                    e.javaClass.name +
                    ": " +
                    (e.message ?: "")
            )
        }
    }

    private fun dumpObjectFields(
        obj: Any,
        indent: String,
        depth: Int
    ) {

        if (
            depth > 2
        ) {
            return
        }

        val clazz =
            obj.javaClass

        val fields =
            try {

                clazz.declaredFields

            } catch (
                _: Throwable
            ) {

                emptyArray<Field>()
            }

        for (
            field in fields
        ) {

            try {

                field.isAccessible =
                    true

                val value =
                    field.get(
                        obj
                    )

                log(
                    indent +
                        "FIELD " +
                        field.name +
                        " : " +
                        field.type.name
                )

                log(
                    indent +
                        "VALUE = " +
                        formatValue(value)
                )

                /*
                 * CaptureRequest.Key wraps another internal key
                 * object. If we find one, inspect that object too.
                 */

                if (
                    value != null &&
                    depth < 2 &&
                    shouldRecurse(
                        value
                    )
                ) {

                    dumpObjectFields(
                        value,
                        indent + "  ",
                        depth + 1
                    )
                }

            } catch (
                e: Throwable
            ) {

                log(
                    indent +
                        "FIELD " +
                        field.name +
                        " = <BLOCKED: " +
                        e.javaClass.simpleName +
                        ">"
                )
            }
        }
    }

    private fun shouldRecurse(
        value: Any
    ): Boolean {

        val name =
            value.javaClass.name
                .lowercase(
                    Locale.US
                )

        return (
            name.contains(
                "camera"
            ) ||
            name.contains(
                "metadata"
            ) ||
            name.contains(
                "key"
            )
        )
    }

    private fun formatValue(
        value: Any?
    ): String {

        if (
            value == null
        ) {

            return "null"
        }

        return when (
            value
        ) {

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

            is BooleanArray ->
                value.contentToString()

            is ShortArray ->
                value.contentToString()

            is CharArray ->
                value.contentToString()

            is Array<*> ->
                value.contentDeepToString()

            else ->
                value.toString()
        }
    }

    private fun log(
        message: String
    ) {

        runOnUiThread {

            output.append(
                message
            )

            output.append(
                "\n"
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
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
            REQUEST_CAMERA &&
            grantResults.isNotEmpty() &&
            grantResults[0] ==
            PackageManager.PERMISSION_GRANTED
        ) {

            startProbe()
        }
    }

    override fun onDestroy() {

        try {

            cameraDevice?.close()

        } catch (_: Throwable) {
        }

        cameraDevice =
            null

        super.onDestroy()
    }
}
