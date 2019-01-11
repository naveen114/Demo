package ru.crew.motley.dere.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.AsyncTask
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import ru.crew.motley.dere.photo.fragment.AwaitingDialog
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

//fun getSampledImage(mFragment: Fragment) = GetSampledImage(mFragment.activity!!)
class GetSampledImage(private val mSampledImageAsyncResp: SampledImageAsyncResp, private val mContext: Context) : AsyncTask<String, Void, File>() {

    private val JPEG_FILE_PREFIX = "IMG_"
    private val JPEG_FILE_SUFFIX = ".jpg"
    //    private var mSampledImageAsyncResp: SampledImageAsyncResp? = null
    private lateinit var awaitingDialog: AwaitingDialog

    companion object {
        private val UPLOADING_DIALOG = "uploadingDialog"
    }

    init {
//        mSampledImageAsyncResp = mActivity as SampledImageAsyncResp
    }

    override fun onPreExecute() {
        super.onPreExecute()
//        awaitingDialog = AwaitingDialog.newInstance("Uploading...")
//        awaitingDialog.show(mActivity.childFragmentManager, PLOADING_DIALOG)
//        mMyCustomLoader.showProgressDialog(mActivity.getString(R.string.processing_image))
    }

    override fun doInBackground(vararg params: String): File? {
        try {
            val picturePath = params[0]
            val imageDirectory = params[1]
            val reqImageWidth = Integer.parseInt(params[2])
            val exif = ExifInterface(picturePath)
            val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, 1)
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(picturePath, options)
            options.inSampleSize = calculateInSampleSize(options, reqImageWidth, reqImageWidth)
            options.inJustDecodeBounds = false
            var imageBitmap: Bitmap? = BitmapFactory.decodeFile(picturePath, options)
            when (orientation) {
                6 -> {
                    val matrix = Matrix()
                    matrix.postRotate(90f)
                    imageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0,
                            imageBitmap!!.width, imageBitmap.height,
                            matrix, true)
                }
                8 -> {
                    val matrix = Matrix()
                    matrix.postRotate(270f)
                    imageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0,
                            imageBitmap!!.width, imageBitmap.height,
                            matrix, true)
                }
                3 -> {
                    val matrix = Matrix()
                    matrix.postRotate(180f)
                    imageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0,
                            imageBitmap!!.width, imageBitmap.height,
                            matrix, true)
                }
            }
            if (null != imageBitmap) {
                return getImageFile(imageBitmap, picturePath, imageDirectory)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    override fun onPostExecute(file: File?) {
        super.onPostExecute(file)
//        mMyCustomLoader.dismissProgressDialog()
        if (null != file && null != mSampledImageAsyncResp) {
            mSampledImageAsyncResp!!.onSampledImageAsyncPostExecute(file)
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options,
                                      reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {

            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize > reqHeight && halfWidth / inSampleSize > reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun getImageFile(bmp: Bitmap, picturePath: String, imageDirectory: String): File? {
        try {
            val fOut: OutputStream?
            val file: File = File(picturePath)

            fOut = FileOutputStream(file)
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, fOut)
            fOut.flush()
            fOut.close()
            MediaStore.Images.Media.insertImage(mContext.contentResolver,
                    file.absolutePath, file.name,
                    file.name)
            return file
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    @Throws(IOException::class)
    private fun setUpImageFile(directory: String): File? {
        var imageFile: File? = null
        if (Environment.MEDIA_MOUNTED == Environment
                        .getExternalStorageState()) {
            val storageDir = File(directory)
            if (!storageDir.mkdirs()) {
                if (!storageDir.exists()) {
                    Log.d("CameraSample", "failed to create directory")
                    return null
                }
            }

            imageFile = File.createTempFile(JPEG_FILE_PREFIX
                    + System.currentTimeMillis() + "_",
                    JPEG_FILE_SUFFIX, storageDir)
        }
        return imageFile
    }

    interface SampledImageAsyncResp {
        fun onSampledImageAsyncPostExecute(file: File)
    }

}
