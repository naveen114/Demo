package ru.crew.motley.dere.photo

import android.annotation.TargetApi
import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.hardware.camera2.CameraCharacteristics
import android.media.ExifInterface
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException
import android.graphics.Bitmap
import android.graphics.Matrix


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class CameraUtils {
    companion object {
        private val TAG = CameraUtils::class.java.simpleName

        fun printCameraFocusModes(context: Context) {
            val cameraManager = context.getSystemService(CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            cameraManager.getCameraCharacteristics("0")
                    .get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
                    ?.let {

                        if (it.size <= 1) {
                            Log.d(TAG, "Camera doesn't have autofocus")
                        } else {
                            Log.d(TAG, "Camera has autofocus")
                        }

                        Log.d(TAG, "CONTROL_AF_AVAILABLE_MODES:")
                        for (position in it) {
                            when (it[position]) {
                                0 -> Log.d(TAG, "CONTROL_AF_MODE_OFF (0)")
                                1 -> Log.d(TAG, "CONTROL_AF_MODE_AUTO (1)")
                                2 -> Log.d(TAG, "CONTROL_AF_MODE_MACRO (2)")
                                3 -> Log.d(TAG, "CONTROL_AF_MODE_CONTINUOUS_VIDEO (3)")
                                4 -> Log.d(TAG, "CONTROL_AF_MODE_CONTINUOUS_PICTURE (4)")
                                5 -> Log.d(TAG, "CONTROL_AF_MODE_EDOF (5)")
                                else -> Log.d(TAG, it[position].toString())
                            }
                        }
                    }
        }

        fun isFocusable(context: Context): Boolean {
            val cameraManager = context.getSystemService(CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            cameraManager.getCameraCharacteristics("0")
                    .get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
                    ?.let {
                        return if (it.size <= 1) {
                            Log.d(TAG, "Camera doesn't have autofocus")
                            false
                        } else {
                            true
                        }
                    }
            return false
        }

        @Throws(IOException::class)
        fun orientation(path: String): Int {
            val exifInterface = ExifInterface(path)
            val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)
            var rotationDegrees = 0
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotationDegrees = 90
                ExifInterface.ORIENTATION_ROTATE_180 -> rotationDegrees = 180
                ExifInterface.ORIENTATION_ROTATE_270 -> rotationDegrees = 270
            }
            return rotationDegrees
        }

        fun orientation(degrees: Int): String {
            return when (degrees) {
                90 -> ExifInterface.ORIENTATION_ROTATE_90.toString()
                180 -> ExifInterface.ORIENTATION_ROTATE_180.toString()
                270 -> ExifInterface.ORIENTATION_ROTATE_270.toString()
                else -> ExifInterface.ORIENTATION_UNDEFINED.toString()
            }
        }

        @Throws(IOException::class)
        fun orientation(file: File): Int {
            return orientation(file.absolutePath)
        }

        fun rotateImage(source: Bitmap, angle: Float): Bitmap {
            val matrix = Matrix()
            matrix.postRotate(angle)
            return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix,
                    true)
        }
    }
}