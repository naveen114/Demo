package ru.crew.motley.dere.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import ru.crew.motley.dere.db.comment.CommentTable

private fun DBLab.getContentValues() {

}

data class Comment(var id: Int? = null, val fileName: String, var fileComment: String)

class DBLab private constructor(context: Context) {

    companion object {

        @Volatile
        private var INSTANCE: DBLab? = null

        fun getInstance(context: Context): DBLab =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: buildDatabase(context)
                            .also { INSTANCE = it }
                }

        private fun buildDatabase(context: Context) =
                DBLab(context.applicationContext)
    }

    private val database by lazy { DBHelper(context).writableDatabase }

    @Throws(SQLiteConstraintException::class)
    fun updateComment(newComment: Comment): Boolean {
//        val lastCommentValue = readCommentValue(newComment.fileName)
        val cv = ContentValues().apply {
            if (newComment.id != null)
                put("_id", newComment.id)
            put(CommentTable.FILE_NAME, newComment.fileName)
            put(CommentTable.FILE_COMMENT, newComment.fileComment)
        }
        val id = database.insertWithOnConflict(
                CommentTable.NAME,
                null,
                cv,
                SQLiteDatabase.CONFLICT_IGNORE)
        if (id == -1L) {
            database.update(CommentTable.NAME,
                    cv,
                    "_id= ? ",
                    arrayOf(newComment.id.toString()))
        } else {
            newComment.id = id.toInt()
        }
//        val newRowId = database.insert(CommentTable.NAME, null, cv)
        return true
    }

    fun readCommentValue(fileName: String): Comment? {
        database.query(
                CommentTable.NAME,
                null,
                "${CommentTable.FILE_NAME} = ?",
                arrayOf(fileName),
                null,
                null,
                null)
                ?.use {
                    if (it.count == 1) {
                        it.moveToFirst()
                        return Comment(
                                it.getInt(it.getColumnIndex("_id")),
                                it.getString(it.getColumnIndex((CommentTable.FILE_NAME))),
                                it.getString(it.getColumnIndex(CommentTable.FILE_COMMENT)))
                    }
                }
        return null
    }

    fun readAllComments(): MutableList<Comment> {
        val result = mutableListOf<Comment>()
        database.query(
                CommentTable.NAME,
                null,
                null,
                null,
                null,
                null,
                null)
                ?.use {
                    it.moveToFirst()
                    while (!it.isAfterLast) {
                        result.add(
                                Comment(
                                        it.getInt(it.getColumnIndex("_id")),
                                        it.getString(it.getColumnIndex((CommentTable.FILE_NAME))),
                                        it.getString(it.getColumnIndex(CommentTable.FILE_COMMENT)))
                        )
                        it.moveToNext()
                    }
                }
        return result
    }


}

