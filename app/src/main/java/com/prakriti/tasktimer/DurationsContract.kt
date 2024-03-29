package com.prakriti.tasktimer

import android.net.Uri

object DurationsContract {
// Views in sqlite3 are read-only

    internal const val TABLE_NAME = "viewTaskDurations" // access modifier for db constants

    /**
     * URI to access TaskDurations view
     */
    val CONTENT_URI: Uri = Uri.withAppendedPath(CONTENT_AUTHORITY_URI, TABLE_NAME)

    const val CONTENT_TYPE =
        "vnd.android.cursor.dir/vnd.$CONTENT_AUTHORITY.$TABLE_NAME" // for multiple items
    const val CONTENT_ITEM_TYPE =
        "vnd.android.cursor.item/vnd.$CONTENT_AUTHORITY.$TABLE_NAME" // for single item

    // this view joins columns from Tasks and Timings table, doesn't have its own ID
    object Columns {
        const val NAME = TasksContract.Columns.TASK_NAME
        const val DESCRIPTION = TasksContract.Columns.TASK_DESCRIPTION
        const val START_TIME = TimingsContract.Columns.TIMING_START_TIME
        const val START_DATE = "StartDate" // calculated by sqlite3 functions
        const val DURATION = TimingsContract.Columns.TIMING_DURATION
    }
}