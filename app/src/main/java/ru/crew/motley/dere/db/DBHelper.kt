package ru.crew.motley.dere.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import ru.crew.motley.dere.db.comment.CommentTable

class DBHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "dereBase.db"
        private const val DB_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("create table ${CommentTable.NAME} (" +
                " _id integer primary key autoincrement, " +
                " ${CommentTable.FILE_NAME}," +
                " ${CommentTable.FILE_COMMENT})")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}
}