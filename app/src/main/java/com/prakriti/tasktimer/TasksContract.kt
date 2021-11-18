package com.prakriti.tasktimer

import android.content.ContentUris
import android.net.Uri
import android.provider.BaseColumns

object TasksContract {
    // objects are automatically -> thread safe Singleton
    // objects don't take args through constructor

    internal const val TABLE_NAME = "Tasks" // access modifier for db constants

    /**
     * URI to access Tasks table
     */
    val CONTENT_URI: Uri = Uri.withAppendedPath(CONTENT_AUTHORITY_URI, TABLE_NAME)

    const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.$CONTENT_AUTHORITY.$TABLE_NAME" // for multiple items
    const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.$CONTENT_AUTHORITY.$TABLE_NAME" // for single item

    // Tasks fields/columns
    // cannot make companion object inside object
    object Columns {
        const val ID = BaseColumns._ID // AS uses predefined primary key _id
        const val TASK_NAME = "Name"
        const val TASK_DESCRIPTION = "Description"
        const val TASK_SORT_ORDER =  "SortOrder"
    }

    fun getId(uri: Uri): Long {
        // ContentUris has methods for working with Uri objects using the "content://" scheme
        return ContentUris.parseId(uri)
    }

    fun buildUriFromId(id: Long): Uri {
        return ContentUris.withAppendedId(CONTENT_URI, id) // appends given id to end of path
    }
}