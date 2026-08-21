package com.example.vivo200mpprobe

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_ID = "3"
        private const val CAMERA_PERMISSION_REQUEST = 1001
    }

    private lateinit var output: TextView
    private lateinit var cameraManager: CameraManager

    private var cameraDevice: CameraDevice? = null

    // Keys we care about most.
    private val targetNames = listOf(
        "vcf.parameter.SnapshotJpegStreamMap",
        "vcf.parameter.sensorSizeList",

        "vivo.control.snapshotYuvStreamMap",
        "vivo.control.snapJpegSize",
        "vivo.control.picturesize.value",

        "vivo.control.streamsUsage",
        "vivo.control.vcfStreamType",

        "vivo.control.raw_capture_type",
        "vivo.parameter.highResolutionDngType",

        "vivo.parameter.niceCaptureSensorMode",
        "vivo.control.sensorMode",
        "vivo.preview.sensorMode",

        "vivo.control.real200mp_switch_on",
        "vivo.control.ultra_highresolution",
        "vivo.control.advance_fullsize",
        "vivo.control.EngineerRemosaicMode",

        "vivo.control.remosaic.capability",
        "vivo.control.seamless.remosaic.enable",

        "com.mediatek.seamlessfeature.cameraScenario",
        "com.mediatek.seamlessfeature.sensorScenario",
        "com.mediatek.seamlessfeature.sensorScenarioCustomHint",

        "vcf.parameter.ImageEcho"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraManager =
            getSystemService(CAMERA_SERVICE) as CameraManager

        buildUi()

        log("VIVO VCF KEY TYPE PROBE")
        log("==============================")
        log("")
        log("Camera ID: $CAMERA_ID")
        log("")
        log("Purpose:")
        log("Recover the real Java/native types")
        log("of Vivo/VCF Camera2 vendor keys.")
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

    private fun buildUi() {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setPadding(20, 30, 20, 30)

        val probeButton = Button(this)

        probeButton.text = "PROBE VCF KEY TYPES"

        probeButton.setOnClickListener {
            Thread {
                runProbe()
            }.start()
        }

        root.addView(probeButton)

        val copyButton = Button(this)

        copyButton.text = "COPY OUTPUT"

        copyButton.setOnClickListener {

            val clipboard =
                getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as ClipboardManager

            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    "Vivo VCF Key Type Probe",
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

        val clearButton = Button(this)

        clearButton.text = "CLEAR"

        clearButton.setOnClickListener {
            output.text = ""
        }

        root.addView(clearButton)

        val scroll = ScrollView(this)

        output = TextView(this)

        output.textSize = 13f
        output.setTextIsSelectable(true)
        output.setPadding(0, 20, 0, 120)

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

                object : CameraDevice.StateCallback() {

                    override fun onOpened(
                        camera: CameraDevice
                    ) {

                        cameraDevice = camera

                        log("Camera 3 opened.")
                        log("")
                        log("Press PROBE VCF KEY TYPES.")
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

                        log("CAMERA ERROR: $error")

                        camera.close()
                        cameraDevice = null
                    }
                },

                null
            )

        } catch (e: Throwable) {

            log("OPEN ERROR")
            log(e.toString())
        }
    }

    // =========================================================
    // MAIN PROBE
    // =========================================================

    private fun runProbe() {

        val camera =
            cameraDevice

        if (camera == null) {

            log("Camera is not open.")
            return
        }

        log("")
        log("")
        log("################################")
        log("START KEY TYPE PROBE")
        log("################################")

        try {

            val chars =
                cameraManager.getCameraCharacteristics(
                    CAMERA_ID
                )

            val requestKeys =
                chars.availableCaptureRequestKeys

            val resultKeys =
                chars.availableCaptureResultKeys

            val sessionKeys =
                try {
                    chars.availableSessionKeys
                } catch (_: Throwable) {
                    emptyList()
                }

            log("")
            log("==============================")
            log("KEY COUNTS")
            log("==============================")

            log(
                "Request keys: ${requestKeys.size}"
            )

            log(
                "Result keys: ${resultKeys.size}"
            )

            log(
                "Session keys: ${sessionKeys.size}"
            )

            val previewBuilder =
                camera.createCaptureRequest(
                    CameraDevice.TEMPLATE_PREVIEW
                )

            val stillBuilder =
                camera.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE
                )

            for (target in targetNames) {

                log("")
                log("")
                log("################################")
                log(target)
                log("################################")

                val requestKey =
                    requestKeys.firstOrNull {
                        it.name == target
                    }

                val sessionKey =
                    sessionKeys.firstOrNull {
                        it.name == target
                    }

                val resultKey =
                    resultKeys.firstOrNull {
                        it.name == target
                    }

                log("")
                log(
                    "REQUEST PRESENT: ${requestKey != null}"
                )

                log(
                    "SESSION PRESENT: ${sessionKey != null}"
                )

                log(
                    "RESULT PRESENT: ${resultKey != null}"
                )

                if (requestKey != null) {

                    log("")
                    log("------------------------------")
                    log("REQUEST KEY OBJECT")
                    log("------------------------------")

                    inspectObject(
                        requestKey,
                        0,
                        mutableSetOf()
                    )

                    dumpBuilderDefault(
                        previewBuilder,
                        requestKey,
                        "PREVIEW DEFAULT"
                    )

                    dumpBuilderDefault(
                        stillBuilder,
                        requestKey,
                        "STILL DEFAULT"
                    )
                }

                if (
                    sessionKey != null &&
                    sessionKey !== requestKey
                ) {

                    log("")
                    log("------------------------------")
                    log("SESSION KEY OBJECT")
                    log("------------------------------")

                    inspectObject(
                        sessionKey,
                        0,
                        mutableSetOf()
                    )

                    dumpBuilderDefault(
                        previewBuilder,
                        sessionKey,
                        "SESSION/PREVIEW DEFAULT"
                    )

                    dumpBuilderDefault(
                        stillBuilder,
                        sessionKey,
                        "SESSION/STILL DEFAULT"
                    )
                }

                if (resultKey != null) {

                    log("")
                    log("------------------------------")
                    log("RESULT KEY OBJECT")
                    log("------------------------------")

                    inspectObject(
                        resultKey,
                        0,
                        mutableSetOf()
                    )
                }
            }

            log("")
            log("")
            log("==============================")
            log("ALL SESSION KEY NAMES")
            log("==============================")

            for (key in sessionKeys) {

                log(
                    key.name
                )
            }

            log("")
            log("")
            log("==============================")
            log("PROBE COMPLETE")
            log("==============================")

            log("")
            log("Press COPY OUTPUT.")

        } catch (e: Throwable) {

            log("")
            log("PROBE EXCEPTION")
            log(e.javaClass.name)
            log(e.message ?: "")
        }
    }

    // =========================================================
    // BUILDER DEFAULT
    // =========================================================

    @Suppress("UNCHECKED_CAST")
    private fun dumpBuilderDefault(
        builder: CaptureRequest.Builder,
        key: CaptureRequest.Key<*>,
        label: String
    ) {

        log("")
        log(label)

        try {

            val typed =
                key as CaptureRequest.Key<Any>

            val value =
                builder.get(
                    typed
                )

            if (value == null) {

                log("  VALUE: null")

            } else {

                log(
                    "  VALUE CLASS: " +
                        value.javaClass.name
                )

                log(
                    "  VALUE: " +
                        formatValue(value)
                )
            }

        } catch (e: Throwable) {

            log(
                "  READ FAILED: " +
                    e.javaClass.simpleName +
                    ": " +
                    (e.message ?: "")
            )
        }
    }

    // =========================================================
    // REFLECTION
    // =========================================================

    private fun inspectObject(
        obj: Any?,
        depth: Int,
        visited: MutableSet<Int>
    ) {

        if (obj == null) {

            indent(depth)
            logDirect("null")

            return
        }

        if (depth > 4) {
            return
        }

        val identity =
            System.identityHashCode(
                obj
            )

        if (visited.contains(identity)) {
            return
        }

        visited.add(identity)

        indent(depth)

        logDirect(
            "CLASS = ${obj.javaClass.name}"
        )

        // -----------------------------------------------------
        // Try useful methods first.
        // -----------------------------------------------------

        val methodNames =
            listOf(
                "getName",
                "getType",
                "getVendorId",
                "getNativeKey"
            )

        for (name in methodNames) {

            try {

                val method =
                    findMethod(
                        obj.javaClass,
                        name
                    )

                if (method != null) {

                    method.isAccessible =
                        true

                    val value =
                        method.invoke(
                            obj
                        )

                    indent(
                        depth + 1
                    )

                    logDirect(
                        "METHOD $name() -> " +
                            describeValue(value)
                    )
                }

            } catch (e: Throwable) {

                indent(
                    depth + 1
                )

                logDirect(
                    "METHOD $name FAILED: " +
                        e.javaClass.simpleName
                )
            }
        }

        // -----------------------------------------------------
        // Dump fields.
        // -----------------------------------------------------

        var current:
            Class<*>? =
            obj.javaClass

        while (
            current != null &&
            current != Any::class.java
        ) {

            val fields =
                try {
                    current.declaredFields
                } catch (_: Throwable) {
                    emptyArray<Field>()
                }

            for (field in fields) {

                if (
                    Modifier.isStatic(
                        field.modifiers
                    )
                ) {
                    continue
                }

                indent(
                    depth + 1
                )

                try {

                    field.isAccessible =
                        true

                    val value =
                        field.get(
                            obj
                        )

                    logDirect(
                        "FIELD ${field.name}"
                    )

                    indent(
                        depth + 2
                    )

                    logDirect(
                        "DECLARED TYPE = " +
                            field.type.name
                    )

                    indent(
                        depth + 2
                    )

                    logDirect(
                        "VALUE = " +
                            describeValue(value)
                    )

                    /*
                     * Camera2 Key objects usually wrap an
                     * internal CameraMetadataNative.Key.
                     *
                     * Follow that object recursively.
                     */

                    if (
                        value != null &&
                        depth < 3 &&
                        (
                            field.name.contains(
                                "key",
                                true
                            ) ||
                            field.name.contains(
                                "type",
                                true
                            )
                        )
                    ) {

                        inspectObject(
                            value,
                            depth + 2,
                            visited
                        )
                    }

                } catch (e: Throwable) {

                    logDirect(
                        "FIELD ${field.name} " +
                            "ACCESS FAILED: " +
                            e.javaClass.simpleName
                    )
                }
            }

            current =
                current.superclass
        }
    }

    private fun findMethod(
        clazz: Class<*>,
        name: String
    ): Method? {

        var current:
            Class<*>? =
            clazz

        while (current != null) {

            try {

                val method =
                    current.declaredMethods
                        .firstOrNull {
                            it.name == name &&
                                it.parameterTypes.isEmpty()
                        }

                if (method != null) {
                    return method
                }

            } catch (_: Throwable) {
            }

            current =
                current.superclass
        }

        return null
    }

    // =========================================================
    // VALUE FORMAT
    // =========================================================

    private fun describeValue(
        value: Any?
    ): String {

        if (value == null) {
            return "null"
        }

        return (
            "${value.javaClass.name} : " +
                formatValue(value)
        )
    }

    private fun formatValue(
        value: Any
    ): String {

        return when (value) {

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

            is Array<*> ->
                value.contentDeepToString()

            else ->
                value.toString()
        }
    }

    // =========================================================
    // LOGGING
    // =========================================================

    private fun indent(
        depth: Int
    ) {

        repeat(depth) {
            logDirect("  ")
        }
    }

    private fun logDirect(
        text: String
    ) {

        runOnUiThread {

            output.append(text)
            output.append("\n")
        }
    }

    private fun log(
        text: String
    ) {

        logDirect(text)
    }

    // =========================================================
    // PERMISSIONS / CLEANUP
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
            CAMERA_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] ==
            PackageManager.PERMISSION_GRANTED
        ) {

            openCamera()
        }
    }

    override fun onDestroy() {

        try {
            cameraDevice?.close()
        } catch (_: Throwable) {
        }

        super.onDestroy()
    }
}
