package com.brett.vivoprobe
import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Size
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val cameraPermissionRequest = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                cameraPermissionRequest
            )
        } else {
            showCameraInfo()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (
            requestCode == cameraPermissionRequest &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            showCameraInfo()
        }
    }

    private fun showCameraInfo() {

        val textView = TextView(this)
        textView.textSize = 14f
        textView.setPadding(30, 30, 30, 30)

        val scrollView = ScrollView(this)
        scrollView.addView(textView)
        setContentView(scrollView)

        val cameraManager =
            getSystemService(CAMERA_SERVICE) as CameraManager

        val output = StringBuilder()

        for (cameraId in cameraManager.cameraIdList) {

            output.append("CAMERA ID: $cameraId\n")
            output.append("========================\n")

            try {

                val characteristics =
                    cameraManager.getCameraCharacteristics(cameraId)

                val facing =
                    characteristics.get(CameraCharacteristics.LENS_FACING)

                output.append(
                    "Facing: ${
                        when (facing) {
                            CameraCharacteristics.LENS_FACING_BACK -> "BACK"
                            CameraCharacteristics.LENS_FACING_FRONT -> "FRONT"
                            CameraCharacteristics.LENS_FACING_EXTERNAL -> "EXTERNAL"
                            else -> "UNKNOWN"
                        }
                    }\n"
                )

                val pixelArray =
                    characteristics.get(
                        CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE
                    )

                output.append(
                    "Sensor Pixel Array: ${
                        pixelArray?.width ?: 0
                    } x ${
                        pixelArray?.height ?: 0
                    }\n"
                )

                if (pixelArray != null) {

                    val mp =
                        (pixelArray.width.toLong() *
                                pixelArray.height.toLong()) / 1_000_000.0

                    output.append(
                        "Approx Sensor MP: %.2f MP\n".format(mp)
                    )
                }

                val activeArray =
                    characteristics.get(
                        CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE
                    )

                if (activeArray != null) {

                    val activeMP =
                        (activeArray.width().toLong() *
                                activeArray.height().toLong()) / 1_000_000.0

                    output.append(
                        "Active Array: ${activeArray.width()} x ${activeArray.height()}\n"
                    )

                    output.append(
                        "Approx Active MP: %.2f MP\n".format(activeMP)
                    )
                }

                val map =
                    characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                    )

                if (map != null) {

                    val jpegSizes: Array<Size>? =
                        map.getOutputSizes(android.graphics.ImageFormat.JPEG)

                    output.append("\nJPEG OUTPUT SIZES:\n")

                    jpegSizes
                        ?.sortedByDescending {
                            it.width.toLong() * it.height.toLong()
                        }
                        ?.forEach { size ->

                            val mp =
                                (size.width.toLong() *
                                        size.height.toLong()) / 1_000_000.0

                            output.append(
                                "${size.width} x ${size.height} = %.2f MP\n"
                                    .format(mp)
                            )
                        }
                }

            } catch (e: Exception) {

                output.append(
                    "ERROR: ${e.message}\n"
                )
            }

            output.append("\n\n")
        }

        textView.text = output.toString()
    }
}
