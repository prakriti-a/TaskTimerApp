package com.prakriti.tasktimer

import android.net.Uri

object CurrentTimingContract {
// Views in sqlite3 are read-only

    internal const val TABLE_NAME = "viewCurrentTiming" // access modifier for db constants

    /**
     * URI to access CurrentTiming view
     */
    val CONTENT_URI: Uri = Uri.withAppendedPath(CONTENT_AUTHORITY_URI, TABLE_NAME)

    const val CONTENT_TYPE =
        "vnd.android.cursor.dir/vnd.$CONTENT_AUTHORITY.$TABLE_NAME" // for multiple items
    const val CONTENT_ITEM_TYPE =
        "vnd.android.cursor.item/vnd.$CONTENT_AUTHORITY.$TABLE_NAME" // for single item

    // this view joins columns from Tasks and Timings table, doesn't have its own ID
    object Columns {
        const val TIMING_ID = TimingsContract.Columns.ID
        const val TASK_ID = TimingsContract.Columns.TIMING_TASK_ID
        const val START_TIME = TimingsContract.Columns.TIMING_START_TIME
        const val TASK_NAME = TasksContract.Columns.TASK_NAME
    }
}