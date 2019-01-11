package ru.crew.motley.dere.photo

import android.os.Environment


class PhotoSaver {

    companion object {
        const val APP_FOLER = "Dere"
        const val PHOTO_FILE_NAME = "photo_"
        const val PHOTO_FILE_EXT = ".jpg"
        val APP_FOLDER_PATH = Environment.getExternalStorageDirectory().absolutePath +
                "/" + PhotoSaver.APP_FOLER
    }

}