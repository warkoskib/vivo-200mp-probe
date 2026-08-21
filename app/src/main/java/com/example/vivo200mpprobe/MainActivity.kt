package com.example.vivo200mpprobe

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var outputText: TextView

    private var baselineMillis: Long = 0L

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            append("\n================================")
            append("PERMISSION RESULT")
            append("================================")

            permissions.forEach { (permission, granted) ->
                append("$permission = $granted")
            }

            showCurrentPermissionState()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildUi()

        append("VIVO OEM CAMERA RAW/DNG WATCHER")
        append("================================")
        append("")
        append("Purpose:")
        append("1. Request image-library permission")
        append("2. Record MediaStore baseline")
        append("3. Launch stock Vivo Camera")
        append("4. Manually choose 200 MP")
        append("5. Take ONE photo")
        append("6. Return to this app")
        append("7. Scan for every new camera file")
        append("")
        append("Target package:")
        append("com.android.camera")
        append("")

        showCurrentPermissionState()
    }

    // ============================================================
    // UI
    // ============================================================

    private fun buildUi() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val requestButton = Button(this).apply {
            text = "REQUEST MEDIA PERMISSION"

            setOnClickListener {
                requestMediaPermissions()
            }
        }

        val baselineButton = Button(this).apply {
            text = "RECORD BASELINE"

            setOnClickListener {
                recordBaseline()
            }
        }

        val launchButton = Button(this).apply {
            text = "LAUNCH VIVO CAMERA"

            setOnClickListener {
                launchVivoCamera()
            }
        }

        val scanButton = Button(this).apply {
            text = "SCAN NEW CAMERA FILES"

            setOnClickListener {
                scanNewFiles()
            }
        }

        val copyButton = Button(this).apply {
            text = "COPY OUTPUT"

            setOnClickListener {
                copyOutput()
            }
        }

        outputText = TextView(this).apply {
            textSize = 14f
            setTextIsSelectable(true)
            movementMethod = ScrollingMovementMethod()
        }

        val scroll = ScrollView(this).apply {
            addView(outputText)
        }

        root.addView(requestButton)
        root.addView(baselineButton)
        root.addView(launchButton)
        root.addView(scanButton)
        root.addView(copyButton)

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

    // ============================================================
    // PERMISSIONS
    // ============================================================

    private fun requestMediaPermissions() {

        val permissions = mutableListOf<String>()

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.CAMERA)
        }

        if (Build.VERSION.SDK_INT >= 33) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(
                    Manifest.permission.READ_MEDIA_IMAGES
                )
            }

            if (Build.VERSION.SDK_INT >= 34) {

                if (
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissions.add(
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    )
                }
            }

        } else {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
        }

        if (permissions.isEmpty()) {

            append("")
            append("All applicable permissions already granted.")

            showCurrentPermissionState()

        } else {

            append("")
            append("Requesting permissions:")

            permissions.forEach {
                append(it)
            }

            permissionLauncher.launch(
                permissions.toTypedArray()
            )
        }
    }

    private fun showCurrentPermissionState() {

        append("")
        append("================================")
        append("CURRENT PERMISSION STATE")
        append("================================")

        val cameraGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        append("CAMERA = $cameraGranted")

        if (Build.VERSION.SDK_INT >= 33) {

            val mediaGranted =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED

            append("READ_MEDIA_IMAGES = $mediaGranted")

            if (Build.VERSION.SDK_INT >= 34) {

                val selectedGranted =
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    ) == PackageManager.PERMISSION_GRANTED

                append(
                    "READ_MEDIA_VISUAL_USER_SELECTED = $selectedGranted"
                )
            }

        } else {

            val storageGranted =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED

            append(
                "READ_EXTERNAL_STORAGE = $storageGranted"
            )
        }
    }

    // ============================================================
    // BASELINE
    // ============================================================

    private fun recordBaseline() {

        baselineMillis = System.currentTimeMillis()

        append("")
        append("================================")
        append("BASELINE RECORDED")
        append("================================")

        append("Timestamp = $baselineMillis")

        append(
            "Time = ${
                formatTime(
                    baselineMillis
                )
            }"
        )

        append("")
        append("NEXT:")
        append("1. Press LAUNCH VIVO CAMERA")
        append("2. Switch to 200 MP")
        append("3. Take ONE photo")
        append("4. Wait for saving to finish")
        append("5. Return here")
        append("6. Press SCAN NEW CAMERA FILES")
    }

    // ============================================================
    // LAUNCH OEM CAMERA
    // ============================================================

    private fun launchVivoCamera() {

        if (baselineMillis == 0L) {
            recordBaseline()
        }

        append("")
        append("Launching stock Vivo Camera...")

        try {

            val intent =
                packageManager.getLaunchIntentForPackage(
                    "com.android.camera"
                )

            if (intent != null) {

                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )

                startActivity(intent)

                append("")
                append("Vivo Camera launch intent sent.")

            } else {

                append("")
                append("Could not obtain launch intent")
                append("for com.android.camera.")

                val fallback =
                    Intent(
                        MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA
                    )

                startActivity(fallback)

                append("Generic camera intent launched.")
            }

        } catch (e: Exception) {

            append("")
            append("CAMERA LAUNCH ERROR")
            append("${e.javaClass.simpleName}: ${e.message}")
        }
    }

    // ============================================================
    // MEDIASTORE SCAN
    // ============================================================

    private fun scanNewFiles() {

        if (baselineMillis == 0L) {

            append("")
            append("ERROR:")
            append("No baseline exists.")
            append("Press RECORD BASELINE first.")

            return
        }

        append("")
        append("================================")
        append("SCANNING MEDIASTORE")
        append("================================")

        append("Looking for files created after:")
        append("$baselineMillis")
        append(formatTime(baselineMillis))

        var total = 0

        total += scanImagesCollection()
        total += scanFilesCollection()

        append("")
        append("================================")
        append("SCAN COMPLETE")
        append("================================")

        append("Matching entries found = $total")

        if (total == 0) {

            append("")
            append("No new entries were visible.")
            append("")
            append("Check:")
            append("READ_MEDIA_IMAGES should be TRUE.")
            append("")
            append("Also wait several seconds after")
            append("the Vivo Camera finishes saving")
            append("and press SCAN again.")
        }
    }

    // ============================================================
    // IMAGES COLLECTION
    // ============================================================

    private fun scanImagesCollection(): Int {

        append("")
        append("------------------------------")
        append("Images COLLECTION")
        append("------------------------------")

        val uri =
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection =
            arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.RELATIVE_PATH,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT
            )

        val baselineSeconds =
            (baselineMillis / 1000L) - 5L

        val selection =
            "${MediaStore.Images.Media.DATE_ADDED} >= ?"

        val selectionArgs =
            arrayOf(
                baselineSeconds.toString()
            )

        val sortOrder =
            "${MediaStore.Images.Media.DATE_ADDED} DESC"

        var count = 0

        try {

            contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->

                while (cursor.moveToNext()) {

                    count++

                    dumpImageRow(
                        cursor,
                        uri,
                        count
                    )
                }
            }

        } catch (e: Exception) {

            append("")
            append("Images query ERROR:")
            append(
                "${e.javaClass.simpleName}: ${e.message}"
            )
        }

        append("")
        append("Images entries = $count")

        return count
    }

    private fun dumpImageRow(
        cursor: Cursor,
        baseUri: Uri,
        number: Int
    ) {

        val id =
            cursor.getLongColumn(
                MediaStore.Images.Media._ID
            )

        val name =
            cursor.getStringColumn(
                MediaStore.Images.Media.DISPLAY_NAME
            )

        val mime =
            cursor.getStringColumn(
                MediaStore.Images.Media.MIME_TYPE
            )

        val size =
            cursor.getLongColumn(
                MediaStore.Images.Media.SIZE
            )

        val added =
            cursor.getLongColumn(
                MediaStore.Images.Media.DATE_ADDED
            )

        val modified =
            cursor.getLongColumn(
                MediaStore.Images.Media.DATE_MODIFIED
            )

        val path =
            cursor.getStringColumn(
                MediaStore.Images.Media.RELATIVE_PATH
            )

        val width =
            cursor.getIntColumn(
                MediaStore.Images.Media.WIDTH
            )

        val height =
            cursor.getIntColumn(
                MediaStore.Images.Media.HEIGHT
            )

        val itemUri =
            ContentUris.withAppendedId(
                baseUri,
                id
            )

        append("")
        append("IMAGE ENTRY #$number")
        append("------------------------------")

        append("ID = $id")
        append("Name = $name")
        append("MIME = $mime")
        append("Size bytes = $size")
        append("Size MB = ${mb(size)}")

        append(
            "Dimensions = $width x $height"
        )

        append("Relative path = $path")

        append(
            "Date added = ${
                formatSeconds(
                    added
                )
            }"
        )

        append(
            "Date modified = ${
                formatSeconds(
                    modified
                )
            }"
        )

        append("Content URI = $itemUri")
    }

    // ============================================================
    // FILES COLLECTION
    // ============================================================

    private fun scanFilesCollection(): Int {

        append("")
        append("------------------------------")
        append("Files COLLECTION")
        append("------------------------------")

        val uri =
            MediaStore.Files.getContentUri(
                "external"
            )

        val projection =
            arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.RELATIVE_PATH,
                MediaStore.Files.FileColumns.MEDIA_TYPE
            )

        val baselineSeconds =
            (baselineMillis / 1000L) - 5L

        val selection =
            "${MediaStore.Files.FileColumns.DATE_ADDED} >= ?"

        val selectionArgs =
            arrayOf(
                baselineSeconds.toString()
            )

        val sortOrder =
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        var count = 0

        try {

            contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->

                while (cursor.moveToNext()) {

                    count++

                    dumpFileRow(
                        cursor,
                        uri,
                        count
                    )
                }
            }

        } catch (e: Exception) {

            append("")
            append("Files query ERROR:")
            append(
                "${e.javaClass.simpleName}: ${e.message}"
            )
        }

        append("")
        append("Files entries = $count")

        return count
    }

    private fun dumpFileRow(
        cursor: Cursor,
        baseUri: Uri,
        number: Int
    ) {

        val id =
            cursor.getLongColumn(
                MediaStore.Files.FileColumns._ID
            )

        val name =
            cursor.getStringColumn(
                MediaStore.Files.FileColumns.DISPLAY_NAME
            )

        val mime =
            cursor.getStringColumn(
                MediaStore.Files.FileColumns.MIME_TYPE
            )

        val size =
            cursor.getLongColumn(
                MediaStore.Files.FileColumns.SIZE
            )

        val added =
            cursor.getLongColumn(
                MediaStore.Files.FileColumns.DATE_ADDED
            )

        val modified =
            cursor.getLongColumn(
                MediaStore.Files.FileColumns.DATE_MODIFIED
            )

        val path =
            cursor.getStringColumn(
                MediaStore.Files.FileColumns.RELATIVE_PATH
            )

        val mediaType =
            cursor.getIntColumn(
                MediaStore.Files.FileColumns.MEDIA_TYPE
            )

        val itemUri =
            ContentUris.withAppendedId(
                baseUri,
                id
            )

        append("")
        append("FILE ENTRY #$number")
        append("------------------------------")

        append("ID = $id")
        append("Name = $name")
        append("MIME = $mime")
        append("Media type = $mediaType")
        append("Size bytes = $size")
        append("Size MB = ${mb(size)}")
        append("Relative path = $path")

        append(
            "Date added = ${
                formatSeconds(
                    added
                )
            }"
        )

        append(
            "Date modified = ${
                formatSeconds(
                    modified
                )
            }"
        )

        append("Content URI = $itemUri")

        val lower =
            name.lowercase(Locale.US)

        if (
            lower.endsWith(".dng") ||
            lower.endsWith(".raw") ||
            lower.endsWith(".bin") ||
            lower.endsWith(".heic") ||
            lower.endsWith(".heif")
        ) {

            append("")
            append("*** INTERESTING FILE TYPE ***")
        }
    }

    // ============================================================
    // CURSOR HELPERS
    // ============================================================

    private fun Cursor.getStringColumn(
        name: String
    ): String {

        val index =
            getColumnIndex(name)

        if (index < 0 || isNull(index)) {
            return "null"
        }

        return getString(index)
    }

    private fun Cursor.getLongColumn(
        name: String
    ): Long {

        val index =
            getColumnIndex(name)

        if (index < 0 || isNull(index)) {
            return 0L
        }

        return getLong(index)
    }

    private fun Cursor.getIntColumn(
        name: String
    ): Int {

        val index =
            getColumnIndex(name)

        if (index < 0 || isNull(index)) {
            return 0
        }

        return getInt(index)
    }

    // ============================================================
    // UTILITIES
    // ============================================================

    private fun append(text: String) {

        runOnUiThread {

            outputText.append(text)
            outputText.append("\n")
        }
    }

    private fun formatTime(
        millis: Long
    ): String {

        return try {

            SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss.SSS",
                Locale.US
            ).format(
                Date(millis)
            )

        } catch (e: Exception) {

            millis.toString()
        }
    }

    private fun formatSeconds(
        seconds: Long
    ): String {

        if (seconds <= 0) {
            return "0"
        }

        return "$seconds / ${
            formatTime(
                seconds * 1000L
            )
        }"
    }

    private fun mb(
        bytes: Long
    ): String {

        return String.format(
            Locale.US,
            "%.2f",
            bytes.toDouble() /
                1024.0 /
                1024.0
        )
    }

    private fun copyOutput() {

        val clipboard =
            getSystemService(
                CLIPBOARD_SERVICE
            ) as android.content.ClipboardManager

        val clip =
            android.content.ClipData.newPlainText(
                "Vivo Camera Probe Output",
                outputText.text.toString()
            )

        clipboard.setPrimaryClip(clip)

        append("")
        append("OUTPUT COPIED TO CLIPBOARD")
    }
}
