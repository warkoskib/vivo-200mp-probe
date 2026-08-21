package com.example.vivo200mpprobe

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 1001
        private const val STORAGE_PERMISSION_REQUEST = 1002

        private const val VIVO_CAMERA_PACKAGE = "com.android.camera"

        /*
         * We'll treat everything created after this timestamp
         * as potentially belonging to the OEM 200 MP capture.
         */
        private var baselineTimeMs: Long = 0L
    }

    private lateinit var output: TextView

    private lateinit var launchCameraButton: Button
    private lateinit var scanButton: Button
    private lateinit var copyButton: Button
    private lateinit var clearButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildUi()

        log("VIVO OEM CAMERA RAW/DNG WATCHER")
        log("================================")
        log("")
        log("Purpose:")
        log("1. Launch stock Vivo Camera")
        log("2. Manually choose 200 MP")
        log("3. Take a photo")
        log("4. Return here")
        log("5. Scan MediaStore for new files")
        log("")
        log("Target package:")
        log(VIVO_CAMERA_PACKAGE)
        log("")

        requestNeededPermissions()
    }

    // =========================================================
    // UI
    // =========================================================

    private fun buildUi() {

        val root =
            LinearLayout(this)

        root.orientation =
            LinearLayout.VERTICAL

        root.setPadding(
            20,
            25,
            20,
            25
        )

        launchCameraButton =
            Button(this)

        launchCameraButton.text =
            "1 - OPEN VIVO CAMERA"

        launchCameraButton.setOnClickListener {
            launchVivoCamera()
        }

        root.addView(
            launchCameraButton
        )

        scanButton =
            Button(this)

        scanButton.text =
            "2 - SCAN NEW CAMERA FILES"

        scanButton.setOnClickListener {
            scanForNewFiles()
        }

        root.addView(
            scanButton
        )

        copyButton =
            Button(this)

        copyButton.text =
            "COPY OUTPUT"

        copyButton.setOnClickListener {

            val clipboard =
                getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as ClipboardManager

            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    "Vivo OEM RAW DNG Watcher",
                    output.text.toString()
                )
            )

            Toast.makeText(
                this,
                "Output copied",
                Toast.LENGTH_SHORT
            ).show()
        }

        root.addView(
            copyButton
        )

        clearButton =
            Button(this)

        clearButton.text =
            "CLEAR"

        clearButton.setOnClickListener {
            output.text = ""
        }

        root.addView(
            clearButton
        )

        val scroll =
            ScrollView(this)

        output =
            TextView(this)

        output.textSize =
            13f

        output.setTextIsSelectable(
            true
        )

        output.setPadding(
            0,
            20,
            0,
            150
        )

        scroll.addView(
            output
        )

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(
            root
        )
    }

    // =========================================================
    // PERMISSIONS
    // =========================================================

    private fun requestNeededPermissions() {

        val permissions =
            mutableListOf<String>()

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            permissions.add(
                Manifest.permission.CAMERA
            )
        }

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            if (
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                permissions.add(
                    Manifest.permission.READ_MEDIA_IMAGES
                )
            }

        } else {

            if (
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                permissions.add(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
        }

        if (permissions.isNotEmpty()) {

            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                STORAGE_PERMISSION_REQUEST
            )
        }
    }

    // =========================================================
    // LAUNCH STOCK VIVO CAMERA
    // =========================================================

    private fun launchVivoCamera() {

        baselineTimeMs =
            System.currentTimeMillis()

        log("")
        log("================================")
        log("BASELINE RECORDED")
        log("================================")

        log(
            "Timestamp = $baselineTimeMs"
        )

        log(
            "Time = " +
                SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss.SSS",
                    Locale.US
                ).format(
                    Date(
                        baselineTimeMs
                    )
                )
        )

        log("")
        log("Launching stock Vivo Camera...")
        log("")
        log("IN THE VIVO CAMERA:")
        log("1. Switch to 200 MP mode")
        log("2. Take ONE photo")
        log("3. Wait until it finishes saving")
        log("4. Return to this app")
        log("5. Press SCAN NEW CAMERA FILES")
        log("")

        try {

            val packageManager =
                packageManager

            val launchIntent =
                packageManager
                    .getLaunchIntentForPackage(
                        VIVO_CAMERA_PACKAGE
                    )

            if (launchIntent != null) {

                launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )

                startActivity(
                    launchIntent
                )

                log(
                    "Vivo Camera launch intent sent."
                )

                return
            }

        } catch (e: Throwable) {

            log(
                "Normal package launch failed:"
            )

            log(
                e.javaClass.simpleName +
                    ": " +
                    (e.message ?: "")
            )
        }

        /*
         * Fallback to the exported CameraActivity
         * discovered earlier.
         */
        try {

            val explicitIntent =
                Intent()

            explicitIntent.setClassName(
                VIVO_CAMERA_PACKAGE,
                "com.android.camera.CameraActivity"
            )

            explicitIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )

            startActivity(
                explicitIntent
            )

            log(
                "Explicit CameraActivity launch sent."
            )

        } catch (e: Throwable) {

            log("")
            log(
                "FAILED TO OPEN VIVO CAMERA"
            )

            log(
                e.javaClass.name
            )

            log(
                e.message ?: ""
            )
        }
    }

    // =========================================================
    // MEDIASTORE SCAN
    // =========================================================

    private fun scanForNewFiles() {

        if (baselineTimeMs == 0L) {

            log("")
            log(
                "No baseline timestamp exists."
            )

            log(
                "Press OPEN VIVO CAMERA first."
            )

            return
        }

        log("")
        log("")
        log("================================")
        log("SCANNING MEDIASTORE")
        log("================================")

        log(
            "Looking for files created after:"
        )

        log(
            "$baselineTimeMs"
        )

        log("")

        var foundCount =
            0

        foundCount +=
            scanCollection(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                "Images"
            )

        /*
         * Some OEMs can publish RAW/DNG files through Files rather
         * than Images. Scan Files as well.
         */
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {

            foundCount +=
                scanCollection(
                    MediaStore.Files.getContentUri(
                        MediaStore.VOLUME_EXTERNAL
                    ),
                    "Files"
                )

        } else {

            foundCount +=
                scanCollection(
                    MediaStore.Files.getContentUri(
                        "external"
                    ),
                    "Files"
                )
        }

        log("")
        log("================================")
        log("SCAN COMPLETE")
        log("================================")

        log(
            "Matching entries found = $foundCount"
        )

        if (foundCount == 0) {

            log("")
            log(
                "No new JPEG/HEIC/DNG/RAW entries were found."
            )

            log(
                "Wait a few seconds and scan again."
            )
        }

        log("")
        log(
            "Press COPY OUTPUT and paste the result here."
        )
    }

    private fun scanCollection(
        collection:
            Uri,
        label:
            String
    ): Int {

        var found =
            0

        log("")
        log("------------------------------")
        log("$label COLLECTION")
        log("------------------------------")

        val projection =
            mutableListOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.DATE_MODIFIED
            )

        /*
         * WIDTH/HEIGHT aren't guaranteed on every Files provider,
         * so we'll attempt them but handle failures gracefully.
         */
        projection.add(
            MediaStore.MediaColumns.WIDTH
        )

        projection.add(
            MediaStore.MediaColumns.HEIGHT
        )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {

            projection.add(
                MediaStore.MediaColumns.RELATIVE_PATH
            )
        }

        /*
         * DATE_ADDED is stored in seconds.
         */
        val baselineSeconds =
            baselineTimeMs / 1000L

        val selection =
            "${MediaStore.MediaColumns.DATE_ADDED} >= ?"

        val selectionArgs =
            arrayOf(
                baselineSeconds.toString()
            )

        val sortOrder =
            "${MediaStore.MediaColumns.DATE_ADDED} ASC"

        var cursor:
            Cursor? =
            null

        try {

            cursor =
                contentResolver.query(
                    collection,
                    projection.toTypedArray(),
                    selection,
                    selectionArgs,
                    sortOrder
                )

            if (cursor == null) {

                log(
                    "Query returned null cursor."
                )

                return 0
            }

            val idIndex =
                cursor.getColumnIndex(
                    MediaStore.MediaColumns._ID
                )

            val nameIndex =
                cursor.getColumnIndex(
                    MediaStore.MediaColumns.DISPLAY_NAME
                )

            val mimeIndex =
                cursor.getColumnIndex(
                    MediaStore.MediaColumns.MIME_TYPE
                )

            val sizeIndex =
                cursor.getColumnIndex(
                    MediaStore.MediaColumns.SIZE
                )

            val dateAddedIndex =
                cursor.getColumnIndex(
                    MediaStore.MediaColumns.DATE_ADDED
                )

            val modifiedIndex =
                cursor.getColumnIndex(
                    MediaStore.MediaColumns.DATE_MODIFIED
                )

            val widthIndex =
                cursor.getColumnIndex(
                    MediaStore.MediaColumns.WIDTH
                )

            val heightIndex =
                cursor.getColumnIndex(
                    MediaStore.MediaColumns.HEIGHT
                )

            val relativePathIndex =
                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.Q
                ) {

                    cursor.getColumnIndex(
                        MediaStore.MediaColumns.RELATIVE_PATH
                    )

                } else {

                    -1
                }

            while (
                cursor.moveToNext()
            ) {

                val id =
                    if (idIndex >= 0) {
                        cursor.getLong(
                            idIndex
                        )
                    } else {
                        -1L
                    }

                val name =
                    if (nameIndex >= 0) {
                        cursor.getString(
                            nameIndex
                        )
                    } else {
                        null
                    }

                val mime =
                    if (mimeIndex >= 0) {
                        cursor.getString(
                            mimeIndex
                        )
                    } else {
                        null
                    }

                /*
                 * Filter down to image/raw-looking entries.
                 */
                if (
                    !looksInteresting(
                        name,
                        mime
                    )
                ) {
                    continue
                }

                found++

                val size =
                    if (sizeIndex >= 0) {
                        cursor.getLong(
                            sizeIndex
                        )
                    } else {
                        -1L
                    }

                val dateAdded =
                    if (dateAddedIndex >= 0) {
                        cursor.getLong(
                            dateAddedIndex
                        )
                    } else {
                        0L
                    }

                val modified =
                    if (modifiedIndex >= 0) {
                        cursor.getLong(
                            modifiedIndex
                        )
                    } else {
                        0L
                    }

                val width =
                    if (widthIndex >= 0) {
                        cursor.getInt(
                            widthIndex
                        )
                    } else {
                        0
                    }

                val height =
                    if (heightIndex >= 0) {
                        cursor.getInt(
                            heightIndex
                        )
                    } else {
                        0
                    }

                val relativePath =
                    if (
                        relativePathIndex >= 0
                    ) {

                        cursor.getString(
                            relativePathIndex
                        )

                    } else {

                        null
                    }

                val itemUri =
                    ContentUris.withAppendedId(
                        collection,
                        id
                    )

                log("")
                log("********************************")
                log("NEW MEDIA ENTRY #$found")
                log("********************************")

                log(
                    "Collection = $label"
                )

                log(
                    "Name = ${name ?: "null"}"
                )

                log(
                    "MIME = ${mime ?: "null"}"
                )

                log(
                    "Width = $width"
                )

                log(
                    "Height = $height"
                )

                log(
                    "Pixels = " +
                        formatMegapixels(
                            width,
                            height
                        )
                )

                log(
                    "Size bytes = $size"
                )

                log(
                    "Size MB = " +
                        formatMb(
                            size
                        )
                )

                log(
                    "Date added = $dateAdded"
                )

                log(
                    "Date modified = $modified"
                )

                if (relativePath != null) {

                    log(
                        "Relative path = $relativePath"
                    )
                }

                log(
                    "URI = $itemUri"
                )

                /*
                 * Try opening the entry.
                 */
                try {

                    contentResolver
                        .openFileDescriptor(
                            itemUri,
                            "r"
                        )
                        ?.use {
                                descriptor ->

                            log(
                                "Openable = YES"
                            )

                            log(
                                "FD statSize = ${descriptor.statSize}"
                            )
                        }

                } catch (e: Throwable) {

                    log(
                        "Openable = NO"
                    )

                    log(
                        "Open error = " +
                            e.javaClass.simpleName +
                            ": " +
                            (e.message ?: "")
                    )
                }
            }

        } catch (e: Throwable) {

            log(
                "Collection query error:"
            )

            log(
                e.javaClass.name
            )

            log(
                e.message ?: ""
            )

        } finally {

            try {
                cursor?.close()
            } catch (_: Throwable) {
            }
        }

        return found
    }

    // =========================================================
    // FILTER
    // =========================================================

    private fun looksInteresting(
        name:
            String?,
        mime:
            String?
    ): Boolean {

        val lowerName =
            name?.lowercase(
                Locale.US
            ) ?: ""

        val lowerMime =
            mime?.lowercase(
                Locale.US
            ) ?: ""

        if (
            lowerMime.startsWith(
                "image/"
            )
        ) {
            return true
        }

        if (
            lowerMime.contains(
                "dng"
            ) ||
            lowerMime.contains(
                "raw"
            )
        ) {
            return true
        }

        val extensions =
            listOf(
                ".jpg",
                ".jpeg",
                ".heic",
                ".heif",
                ".dng",
                ".raw",
                ".bin"
            )

        return extensions.any {
            lowerName.endsWith(
                it
            )
        }
    }

    // =========================================================
    // FORMAT HELPERS
    // =========================================================

    private fun formatMegapixels(
        width:
            Int,
        height:
            Int
    ): String {

        if (
            width <= 0 ||
            height <= 0
        ) {
            return "unknown"
        }

        val mp =
            width.toDouble() *
                height.toDouble() /
                1_000_000.0

        return String.format(
            Locale.US,
            "%.2f MP",
            mp
        )
    }

    private fun formatMb(
        bytes:
            Long
    ): String {

        if (bytes < 0) {
            return "unknown"
        }

        return String.format(
            Locale.US,
            "%.2f MB",
            bytes /
                1024.0 /
                1024.0
        )
    }

    // =========================================================
    // LOG
    // =========================================================

    private fun log(
        text:
            String
    ) {

        runOnUiThread {

            output.append(
                text
            )

            output.append(
                "\n"
            )
        }
    }
}
