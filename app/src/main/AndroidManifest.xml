package com.example.vivo200mpprobe

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class MainActivity : AppCompatActivity() {

    companion object {
        private const val VIVO_CAMERA_PACKAGE = "com.android.camera"

        // Poll fast enough to observe files changing during processing.
        private const val POLL_INTERVAL_MS = 350L

        // Look slightly before monitoring began so timestamp rounding
        // cannot hide an entry.
        private const val DATE_MARGIN_SECONDS = 5L
    }

    private lateinit var output: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var scanButton: Button

    private val mainHandler = Handler(Looper.getMainLooper())

    private var monitoring = false
    private var monitorStartMs = 0L
    private var scanNumber = 0

    /*
     * Track every MediaStore object we've seen.
     *
     * Key:
     * collection + ID
     *
     * Value:
     * most recent state
     */
    private val knownEntries =
        ConcurrentHashMap<String, MediaEntry>()

    private var imagesObserver: ContentObserver? = null
    private var filesObserver: ContentObserver? = null

    // ============================================================
    // DATA
    // ============================================================

    data class MediaEntry(
        val collection: String,
        val id: Long,
        val name: String?,
        val mime: String?,
        val size: Long,
        val width: Int,
        val height: Int,
        val relativePath: String?,
        val dateAdded: Long,
        val dateModified: Long,
        val pending: Int,
        val uri: Uri
    )

    // ============================================================
    // PERMISSIONS
    // ============================================================

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->

            log("")
            log("==============================")
            log("PERMISSION RESULT")
            log("==============================")

            result.forEach { (permission, granted) ->
                log("$permission = $granted")
            }

            printPermissionState()
        }

    // ============================================================
    // ACTIVITY
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createUi()

        log("VIVO LIVE RAW / PRE-PROCESS WATCHER")
        log("===================================")
        log("")
        log("This app monitors the OEM camera")
        log("WHILE the 200 MP image is being")
        log("captured and processed.")
        log("")
        log("It watches:")
        log("• MediaStore Images")
        log("• MediaStore Files")
        log("• new entries")
        log("• file-size changes")
        log("• dimension changes")
        log("• pending/finalized transitions")
        log("• RAW/DNG/YUV/BIN/temp-like files")
        log("")
        log("Target OEM camera:")
        log(VIVO_CAMERA_PACKAGE)

        printPermissionState()
    }

    // ============================================================
    // UI
    // ============================================================

    private fun createUi() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

        val permissionButton =
            Button(this).apply {

                text = "1 - REQUEST PERMISSIONS"

                setOnClickListener {
                    requestPermissionsNow()
                }
            }

        startButton =
            Button(this).apply {

                text = "2 - START WATCH + OPEN VIVO CAMERA"

                setOnClickListener {
                    startMonitorAndLaunchCamera()
                }
            }

        stopButton =
            Button(this).apply {

                text = "3 - STOP WATCHING"

                isEnabled = false

                setOnClickListener {
                    stopMonitoring()
                }
            }

        scanButton =
            Button(this).apply {

                text = "MANUAL DEEP SCAN"

                setOnClickListener {
                    deepScan(true)
                }
            }

        val copyButton =
            Button(this).apply {

                text = "COPY OUTPUT"

                setOnClickListener {
                    copyOutput()
                }
            }

        val clearButton =
            Button(this).apply {

                text = "CLEAR OUTPUT"

                setOnClickListener {
                    output.text = ""
                }
            }

        output =
            TextView(this).apply {

                textSize = 13f
                setTextIsSelectable(true)
                setPadding(0, 15, 0, 150)
            }

        val scroll =
            ScrollView(this).apply {
                addView(output)
            }

        root.addView(permissionButton)
        root.addView(startButton)
        root.addView(stopButton)
        root.addView(scanButton)
        root.addView(copyButton)
        root.addView(clearButton)

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
    // PERMISSION HANDLING
    // ============================================================

    private fun requestPermissionsNow() {

        val needed = mutableListOf<String>()

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            needed += Manifest.permission.CAMERA
        }

        if (Build.VERSION.SDK_INT >= 33) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                needed += Manifest.permission.READ_MEDIA_IMAGES
            }

            if (Build.VERSION.SDK_INT >= 34) {

                if (
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    needed +=
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                }
            }

        } else {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                needed += Manifest.permission.READ_EXTERNAL_STORAGE
            }
        }

        if (needed.isEmpty()) {

            log("")
            log("No additional permissions required.")
            printPermissionState()

        } else {

            permissionLauncher.launch(
                needed.toTypedArray()
            )
        }
    }

    private fun printPermissionState() {

        log("")
        log("==============================")
        log("PERMISSION STATE")
        log("==============================")

        log(
            "CAMERA = ${
                hasPermission(
                    Manifest.permission.CAMERA
                )
            }"
        )

        if (Build.VERSION.SDK_INT >= 33) {

            log(
                "READ_MEDIA_IMAGES = ${
                    hasPermission(
                        Manifest.permission.READ_MEDIA_IMAGES
                    )
                }"
            )

            if (Build.VERSION.SDK_INT >= 34) {

                log(
                    "VISUAL_USER_SELECTED = ${
                        hasPermission(
                            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                        )
                    }"
                )
            }

        } else {

            log(
                "READ_EXTERNAL_STORAGE = ${
                    hasPermission(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                }"
            )
        }
    }

    private fun hasPermission(
        permission: String
    ): Boolean {

        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    // ============================================================
    // START
    // ============================================================

    private fun startMonitorAndLaunchCamera() {

        if (monitoring) {
            log("")
            log("Watcher is already running.")
            return
        }

        monitorStartMs =
            System.currentTimeMillis()

        scanNumber = 0
        knownEntries.clear()

        log("")
        log("")
        log("################################")
        log("LIVE MONITOR STARTED")
        log("################################")

        log("Start ms = $monitorStartMs")
        log("Start time = ${formatMs(monitorStartMs)}")

        /*
         * Establish the starting MediaStore state first.
         */
        deepScan(
            verbose = false,
            establishBaseline = true
        )

        registerMediaObservers()

        monitoring = true

        startButton.isEnabled = false
        stopButton.isEnabled = true

        /*
         * Start active polling as well as ContentObservers.
         * Some OEM updates don't generate notifications at every
         * intermediate state.
         */
        mainHandler.post(pollRunnable)

        launchVivoCamera()
    }

    // ============================================================
    // CONTENT OBSERVERS
    // ============================================================

    private fun registerMediaObservers() {

        unregisterMediaObservers()

        imagesObserver =
            object :
                ContentObserver(mainHandler) {

                override fun onChange(
                    selfChange: Boolean,
                    uri: Uri?
                ) {

                    super.onChange(
                        selfChange,
                        uri
                    )

                    if (!monitoring) {
                        return
                    }

                    log("")
                    log(">>> IMAGE MEDIASTORE CHANGE")

                    if (uri != null) {
                        log("Changed URI = $uri")
                    }

                    deepScan(false)
                }
            }

        filesObserver =
            object :
                ContentObserver(mainHandler) {

                override fun onChange(
                    selfChange: Boolean,
                    uri: Uri?
                ) {

                    super.onChange(
                        selfChange,
                        uri
                    )

                    if (!monitoring) {
                        return
                    }

                    log("")
                    log(">>> FILE MEDIASTORE CHANGE")

                    if (uri != null) {
                        log("Changed URI = $uri")
                    }

                    deepScan(false)
                }
            }

        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            imagesObserver!!
        )

        contentResolver.registerContentObserver(
            MediaStore.Files.getContentUri("external"),
            true,
            filesObserver!!
        )

        log("")
        log("MediaStore ContentObservers active.")
    }

    private fun unregisterMediaObservers() {

        try {

            imagesObserver?.let {
                contentResolver.unregisterContentObserver(it)
            }

        } catch (_: Throwable) {
        }

        try {

            filesObserver?.let {
                contentResolver.unregisterContentObserver(it)
            }

        } catch (_: Throwable) {
        }

        imagesObserver = null
        filesObserver = null
    }

    // ============================================================
    // POLLING
    // ============================================================

    private val pollRunnable =
        object :
            Runnable {

            override fun run() {

                if (!monitoring) {
                    return
                }

                deepScan(false)

                mainHandler.postDelayed(
                    this,
                    POLL_INTERVAL_MS
                )
            }
        }

    // ============================================================
    // OEM CAMERA
    // ============================================================

    private fun launchVivoCamera() {

        log("")
        log("==============================")
        log("OPENING STOCK VIVO CAMERA")
        log("==============================")

        log("")
        log("IN VIVO CAMERA:")
        log("1. Select 200 MP")
        log("2. Take ONE picture")
        log("3. Let it process")
        log("4. Return to this app")
        log("")
        log("DO NOT stop the watcher first.")
        log("")

        try {

            val intent =
                packageManager.getLaunchIntentForPackage(
                    VIVO_CAMERA_PACKAGE
                )

            if (intent != null) {

                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )

                startActivity(intent)

                log("OEM camera launched.")

                return
            }

        } catch (e: Throwable) {

            log(
                "Package launch failed: " +
                    e.javaClass.simpleName
            )
        }

        /*
         * Fallback to the exported OEM CameraActivity
         * discovered earlier.
         */
        try {

            val explicit =
                Intent().apply {

                    setClassName(
                        VIVO_CAMERA_PACKAGE,
                        "com.android.camera.CameraActivity"
                    )

                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }

            startActivity(explicit)

            log("Explicit CameraActivity launched.")

        } catch (e: Throwable) {

            log("")
            log("FAILED TO LAUNCH OEM CAMERA")
            log(
                "${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }

    // ============================================================
    // DEEP SCAN
    // ============================================================

    private fun deepScan(
        verbose: Boolean,
        establishBaseline: Boolean = false
    ) {

        scanNumber++

        if (verbose) {

            log("")
            log("==============================")
            log("DEEP SCAN #$scanNumber")
            log("==============================")
        }

        scanImages(
            verbose,
            establishBaseline
        )

        scanFiles(
            verbose,
            establishBaseline
        )
    }

    // ============================================================
    // IMAGES
    // ============================================================

    private fun scanImages(
        verbose: Boolean,
        establishBaseline: Boolean
    ) {

        val uri =
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection =
            mutableListOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT
            )

        if (Build.VERSION.SDK_INT >= 29) {
            projection += MediaStore.Images.Media.RELATIVE_PATH
            projection += MediaStore.Images.Media.IS_PENDING
        }

        val selection: String?
        val args: Array<String>?

        if (monitorStartMs > 0) {

            val seconds =
                monitorStartMs / 1000L -
                    DATE_MARGIN_SECONDS

            selection =
                "${MediaStore.Images.Media.DATE_ADDED} >= ?"

            args =
                arrayOf(
                    seconds.toString()
                )

        } else {

            selection = null
            args = null
        }

        try {

            contentResolver.query(
                uri,
                projection.toTypedArray(),
                selection,
                args,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->

                while (cursor.moveToNext()) {

                    val entry =
                        imageEntryFromCursor(
                            cursor,
                            uri
                        )

                    processEntry(
                        entry,
                        establishBaseline,
                        verbose
                    )
                }
            }

        } catch (e: Throwable) {

            if (verbose) {

                log(
                    "Images query failed: " +
                        "${e.javaClass.simpleName}: ${e.message}"
                )
            }
        }
    }

    private fun imageEntryFromCursor(
        cursor: Cursor,
        baseUri: Uri
    ): MediaEntry {

        val id =
            cursor.longValue(
                MediaStore.Images.Media._ID
            )

        return MediaEntry(
            collection = "IMAGE",
            id = id,

            name =
                cursor.stringValue(
                    MediaStore.Images.Media.DISPLAY_NAME
                ),

            mime =
                cursor.stringValue(
                    MediaStore.Images.Media.MIME_TYPE
                ),

            size =
                cursor.longValue(
                    MediaStore.Images.Media.SIZE
                ),

            width =
                cursor.intValue(
                    MediaStore.Images.Media.WIDTH
                ),

            height =
                cursor.intValue(
                    MediaStore.Images.Media.HEIGHT
                ),

            relativePath =
                if (Build.VERSION.SDK_INT >= 29)
                    cursor.stringValue(
                        MediaStore.Images.Media.RELATIVE_PATH
                    )
                else null,

            dateAdded =
                cursor.longValue(
                    MediaStore.Images.Media.DATE_ADDED
                ),

            dateModified =
                cursor.longValue(
                    MediaStore.Images.Media.DATE_MODIFIED
                ),

            pending =
                if (Build.VERSION.SDK_INT >= 29)
                    cursor.intValue(
                        MediaStore.Images.Media.IS_PENDING
                    )
                else 0,

            uri =
                ContentUris.withAppendedId(
                    baseUri,
                    id
                )
        )
    }

    // ============================================================
    // FILES
    // ============================================================

    private fun scanFiles(
        verbose: Boolean,
        establishBaseline: Boolean
    ) {

        val uri =
            MediaStore.Files.getContentUri(
                "external"
            )

        val projection =
            mutableListOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.DATE_MODIFIED
            )

        if (Build.VERSION.SDK_INT >= 29) {
            projection +=
                MediaStore.Files.FileColumns.RELATIVE_PATH

            projection +=
                MediaStore.Files.FileColumns.IS_PENDING
        }

        val seconds =
            if (monitorStartMs > 0)
                monitorStartMs / 1000L -
                    DATE_MARGIN_SECONDS
            else 0L

        val selection =
            if (monitorStartMs > 0)
                "${MediaStore.Files.FileColumns.DATE_ADDED} >= ?"
            else null

        val args =
            if (monitorStartMs > 0)
                arrayOf(seconds.toString())
            else null

        try {

            contentResolver.query(
                uri,
                projection.toTypedArray(),
                selection,
                args,
                "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
            )?.use { cursor ->

                while (cursor.moveToNext()) {

                    val entry =
                        fileEntryFromCursor(
                            cursor,
                            uri
                        )

                    processEntry(
                        entry,
                        establishBaseline,
                        verbose
                    )
                }
            }

        } catch (e: Throwable) {

            if (verbose) {

                log(
                    "Files query failed: " +
                        "${e.javaClass.simpleName}: ${e.message}"
                )
            }
        }
    }

    private fun fileEntryFromCursor(
        cursor: Cursor,
        baseUri: Uri
    ): MediaEntry {

        val id =
            cursor.longValue(
                MediaStore.Files.FileColumns._ID
            )

        return MediaEntry(
            collection = "FILE",
            id = id,

            name =
                cursor.stringValue(
                    MediaStore.Files.FileColumns.DISPLAY_NAME
                ),

            mime =
                cursor.stringValue(
                    MediaStore.Files.FileColumns.MIME_TYPE
                ),

            size =
                cursor.longValue(
                    MediaStore.Files.FileColumns.SIZE
                ),

            width = 0,
            height = 0,

            relativePath =
                if (Build.VERSION.SDK_INT >= 29)
                    cursor.stringValue(
                        MediaStore.Files.FileColumns.RELATIVE_PATH
                    )
                else null,

            dateAdded =
                cursor.longValue(
                    MediaStore.Files.FileColumns.DATE_ADDED
                ),

            dateModified =
                cursor.longValue(
                    MediaStore.Files.FileColumns.DATE_MODIFIED
                ),

            pending =
                if (Build.VERSION.SDK_INT >= 29)
                    cursor.intValue(
                        MediaStore.Files.FileColumns.IS_PENDING
                    )
                else 0,

            uri =
                ContentUris.withAppendedId(
                    baseUri,
                    id
                )
        )
    }

    // ============================================================
    // CHANGE DETECTION
    // ============================================================

    private fun processEntry(
        entry: MediaEntry,
        establishBaseline: Boolean,
        verbose: Boolean
    ) {

        val key =
            "${entry.collection}:${entry.id}"

        val old =
            knownEntries[key]

        if (old == null) {

            knownEntries[key] = entry

            if (!establishBaseline) {

                log("")
                log("********************************")
                log("NEW MEDIA OBJECT")
                log("********************************")

                dumpEntry(entry)

                if (isInteresting(entry)) {

                    log("")
                    log("*** HIGH-INTEREST CAPTURE FILE ***")
                }

                tryOpen(entry)
            }

            return
        }

        /*
         * Detect processing-stage changes.
         *
         * OEMs may insert an entry at 0 bytes / pending and then
         * repeatedly replace or enlarge it as processing finishes.
         */
        val changed =
            old.size != entry.size ||
                old.width != entry.width ||
                old.height != entry.height ||
                old.pending != entry.pending ||
                old.dateModified != entry.dateModified ||
                old.mime != entry.mime ||
                old.name != entry.name

        if (changed) {

            knownEntries[key] = entry

            log("")
            log(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
            log("MEDIA OBJECT CHANGED")
            log(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")

            log("Name = ${entry.name}")
            log("URI = ${entry.uri}")

            if (old.size != entry.size) {

                log(
                    "SIZE: ${old.size} -> ${entry.size} bytes"
                )

                log(
                    "      ${mb(old.size)} -> ${mb(entry.size)} MB"
                )
            }

            if (
                old.width != entry.width ||
                old.height != entry.height
            ) {

                log(
                    "DIMENSIONS: " +
                        "${old.width}x${old.height} -> " +
                        "${entry.width}x${entry.height}"
                )
            }

            if (old.pending != entry.pending) {

                log(
                    "IS_PENDING: " +
                        "${old.pending} -> ${entry.pending}"
                )
            }

            if (old.mime != entry.mime) {

                log(
                    "MIME: ${old.mime} -> ${entry.mime}"
                )
            }

            if (isInteresting(entry)) {

                log(
                    "*** INTERESTING PROCESSING OBJECT ***"
                )
            }
        } else if (verbose) {

            /*
             * Manual scan can still display interesting stable files.
             */
            if (isInteresting(entry)) {
                dumpEntry(entry)
            }
        }
    }

    // ============================================================
    // ENTRY REPORT
    // ============================================================

    private fun dumpEntry(
        entry: MediaEntry
    ) {

        log("Collection = ${entry.collection}")
        log("ID = ${entry.id}")
        log("Name = ${entry.name}")
        log("MIME = ${entry.mime}")
        log("Size = ${entry.size} bytes")
        log("Size = ${mb(entry.size)} MB")

        if (
            entry.width > 0 ||
            entry.height > 0
        ) {

            log(
                "Dimensions = " +
                    "${entry.width} x ${entry.height}"
            )

            if (
                entry.width > 0 &&
                entry.height > 0
            ) {

                val mp =
                    entry.width.toDouble() *
                        entry.height.toDouble() /
                        1_000_000.0

                log(
                    String.format(
                        Locale.US,
                        "Megapixels = %.2f MP",
                        mp
                    )
                )
            }
        }

        log(
            "Relative path = ${entry.relativePath}"
        )

        log(
            "IS_PENDING = ${entry.pending}"
        )

        log(
            "Date added = ${formatSeconds(entry.dateAdded)}"
        )

        log(
            "Date modified = ${formatSeconds(entry.dateModified)}"
        )

        log("URI = ${entry.uri}")
    }

    // ============================================================
    // HIGH-INTEREST FILTER
    // ============================================================

    private fun isInteresting(
        entry: MediaEntry
    ): Boolean {

        val name =
            entry.name
                ?.lowercase(Locale.US)
                ?: ""

        val mime =
            entry.mime
                ?.lowercase(Locale.US)
                ?: ""

        /*
         * Potential intermediate/raw representations.
         */
        if (
            name.endsWith(".dng") ||
            name.endsWith(".raw") ||
            name.endsWith(".yuv") ||
            name.endsWith(".bin") ||
            name.endsWith(".dat") ||
            name.endsWith(".tmp")
        ) {
            return true
        }

        if (
            mime.contains("dng") ||
            mime.contains("raw") ||
            mime.contains("octet-stream")
        ) {
            return true
        }

        /*
         * Anything near the expected full-resolution raster.
         */
        if (
            entry.width >= 8000 ||
            entry.height >= 6000
        ) {
            return true
        }

        /*
         * Large intermediate files.
         */
        if (
            entry.size >=
            40L * 1024L * 1024L
        ) {
            return true
        }

        /*
         * Pending objects are especially useful during processing.
         */
        if (entry.pending != 0) {
            return true
        }

        return false
    }

    // ============================================================
    // CAN WE READ THE OBJECT?
    // ============================================================

    private fun tryOpen(
        entry: MediaEntry
    ) {

        try {

            contentResolver
                .openFileDescriptor(
                    entry.uri,
                    "r"
                )
                ?.use { pfd ->

                    log(
                        "Openable = YES"
                    )

                    log(
                        "Descriptor size = ${pfd.statSize}"
                    )
                }

        } catch (e: Throwable) {

            log(
                "Openable = NO"
            )

            log(
                "Open error = " +
                    "${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }

    // ============================================================
    // STOP
    // ============================================================

    private fun stopMonitoring() {

        if (!monitoring) {
            return
        }

        monitoring = false

        mainHandler.removeCallbacks(
            pollRunnable
        )

        unregisterMediaObservers()

        log("")
        log("")
        log("################################")
        log("LIVE MONITOR STOPPED")
        log("################################")

        log(
            "Tracked objects = ${knownEntries.size}"
        )

        log("")
        log("Running final deep scan...")

        deepScan(true)

        startButton.isEnabled = true
        stopButton.isEnabled = false

        log("")
        log("Press COPY OUTPUT.")
    }

    // ============================================================
    // CURSOR HELPERS
    // ============================================================

    private fun Cursor.stringValue(
        column: String
    ): String? {

        val index =
            getColumnIndex(column)

        if (
            index < 0 ||
            isNull(index)
        ) {
            return null
        }

        return getString(index)
    }

    private fun Cursor.longValue(
        column: String
    ): Long {

        val index =
            getColumnIndex(column)

        if (
            index < 0 ||
            isNull(index)
        ) {
            return 0L
        }

        return getLong(index)
    }

    private fun Cursor.intValue(
        column: String
    ): Int {

        val index =
            getColumnIndex(column)

        if (
            index < 0 ||
            isNull(index)
        ) {
            return 0
        }

        return getInt(index)
    }

    // ============================================================
    // OUTPUT
    // ============================================================

    private fun log(
        value: String
    ) {

        runOnUiThread {

            output.append(value)
            output.append("\n")
        }
    }

    private fun copyOutput() {

        val clipboard =
            getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "Vivo Live RAW Watcher",
                output.text.toString()
            )
        )

        Toast.makeText(
            this,
            "Output copied",
            Toast.LENGTH_SHORT
        ).show()
    }

    // ============================================================
    // FORMATTING
    // ============================================================

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

    private fun formatMs(
        milliseconds: Long
    ): String {

        return SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS",
            Locale.US
        ).format(
            Date(milliseconds)
        )
    }

    private fun formatSeconds(
        seconds: Long
    ): String {

        if (seconds <= 0) {
            return "0"
        }

        return "$seconds / ${
            formatMs(
                seconds * 1000L
            )
        }"
    }

    // ============================================================
    // CLEANUP
    // ============================================================

    override fun onDestroy() {

        monitoring = false

        mainHandler.removeCallbacks(
            pollRunnable
        )

        unregisterMediaObservers()

        super.onDestroy()
    }
}
