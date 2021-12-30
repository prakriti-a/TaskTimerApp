package com.prakriti.tasktimer

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.util.Log

import androidx.core.content.ContentProviderCompat.requireContext

/**
 *  Provider for TaskTimer app. Its the only class to be aware of our db [AppDatabase]
 */
private const val TAG = "AppProvider"

const val CONTENT_AUTHORITY = "com.prakriti.tasktimer.provider" // authority -> unique name of provider

private const val TASKS = 100
private const val TASKS_ID = 101
private const val TIMINGS = 200
private const val TIMINGS_ID = 201
private const val TASK_DURATIONS = 400
private const val TASK_DURATIONS_ID = 401

val CONTENT_AUTHORITY_URI: Uri = Uri.parse("content://$CONTENT_AUTHORITY") // default public, can be used outside app

class AppProvider: ContentProvider() {
// provider must notify any observers about changes to the data -> i.e. when insert(), update() or delete() is called

    private val uriMatcher by lazy { buildUriMatcher() } // fn called the first time we use UriMatcher, instance created only once

    private fun buildUriMatcher(): UriMatcher {
        Log.d(TAG, "buildUriMatcher called")
        // UriMatcher helps matching URIs in content providers, returns code if matched
        val matcher = UriMatcher(UriMatcher.NO_MATCH) // code to match for root URI

        // match 1. content://com.prakriti.tasktimer.provider/Tasks
        matcher.addURI(CONTENT_AUTHORITY, TasksContract.TABLE_NAME, TASKS) // returns code 100 if URI passed matches to this path, etc...
        // match 1. content://com.prakriti.tasktimer.provider/Tasks/_id
        matcher.addURI(CONTENT_AUTHORITY, "${TasksContract.TABLE_NAME}/#", TASKS_ID)
        // # is a wild card replaced by numeric id, * is for matching numbers & text

        // similarly for Timings Table
        matcher.addURI(CONTENT_AUTHORITY, TimingsContract.TABLE_NAME, TIMINGS)
        matcher.addURI(CONTENT_AUTHORITY, "${TimingsContract.TABLE_NAME}/#", TIMINGS_ID)

//        // for Durations View
//        matcher.addURI(CONTENT_AUTHORITY, DurationsContract.TABLE_NAME, TASK_DURATIONS)
//        matcher.addURI(CONTENT_AUTHORITY, "${DurationsContract.TABLE_NAME}/#", TASK_DURATIONS_ID)

        return matcher
    }

    override fun onCreate(): Boolean {
        Log.i(TAG, "onCreate called")
        return true
    }

    override fun getType(uri: Uri): String {
        Log.i(TAG, "getType called")
        val match = uriMatcher.match(uri)

        return when (match) {
            TASKS -> TasksContract.CONTENT_TYPE
            TASKS_ID -> TasksContract.CONTENT_ITEM_TYPE
            TIMINGS -> TimingsContract.CONTENT_TYPE
            TIMINGS_ID -> TimingsContract.CONTENT_ITEM_TYPE
//            TASK_DURATIONS -> DurationsContract.CONTENT_TYPE
//            TASK_DURATIONS_ID -> DurationsContract.CONTENT_ITEM_TYPE
            else -> throw IllegalArgumentException("unknown Uri: $uri")
        }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?,
                       sortOrder: String?): Cursor? {
        Log.i(TAG, "query called, uri: $uri")
        val match = uriMatcher.match(uri)
        Log.d(TAG, "query, match is $match")
        // use queryBuilder to build the query to executed
        val queryBuilder = SQLiteQueryBuilder()

        when (match) {
            TASKS -> queryBuilder.tables = TasksContract.TABLE_NAME // setTables()
            // if URI has table name only, use tables to tell queryBuilder which table to query

            TASKS_ID -> {
                queryBuilder.tables = TasksContract.TABLE_NAME
                val taskId = TasksContract.getId(uri) // parse id from Contract class
                queryBuilder.appendWhere("${TasksContract.Columns.ID} = ")
                queryBuilder.appendWhereEscapeString("$taskId") // puts input in single quotes(' ')
                    // ^means where _id="?" -> id clause
                // appendWhere() is an opening for SQL injection attacks, use appendWhereEscapeString() to append values instead
            }

            TIMINGS -> queryBuilder.tables = TimingsContract.TABLE_NAME

            TIMINGS_ID -> {
                queryBuilder.tables = TimingsContract.TABLE_NAME
                val timingId = TimingsContract.getId(uri)
                queryBuilder.appendWhere("${TimingsContract.Columns.ID} = ")
                queryBuilder.appendWhereEscapeString("$timingId")
            }

//            TASK_DURATIONS -> queryBuilder.tables = DurationsContract.TABLE_NAME
//
//            TASK_DURATIONS_ID -> {
//                queryBuilder.tables = DurationsContract.TABLE_NAME
//                val durationId = DurationsContract.getId(uri)
//                queryBuilder.appendWhere("${DurationsContract.Columns.ID} = ")
//                queryBuilder.appendWhereEscapeString("$durationId")
//            }

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        val context = requireContext(this) // use this, getContext returns Context? not Context -> type mismatch error
        val db = AppDatabase.getInstance(context).readableDatabase
        val cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder)
        Log.d(TAG, "query: rows in returned cursor = ${cursor.count}")

        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri { // returns URI with id of inserted row
        Log.i(TAG, "insert called, uri is $uri")
        val match = uriMatcher.match(uri)
        Log.d(TAG, "insert, match is $match")

        val recordId: Long
        val returnUri: Uri
        val context = requireContext(this) // use this, getContext returns Context? not Context -> type mismatch error

        when(match) {
            // DB Views are only for querying, so we don't write to the Durations View
            TASKS -> {
                val db = AppDatabase.getInstance(context).writableDatabase
                recordId = db.insert(TasksContract.TABLE_NAME, null, values) // returns -1 if error
                if(recordId != -1L) { // specify long with L
                    returnUri = TasksContract.buildUriFromId(recordId)
                } else {
                    throw SQLException("Failed to insert, Uri: $uri")
                }
            }
            TIMINGS -> {
                val db = AppDatabase.getInstance(context).writableDatabase
                recordId = db.insert(TimingsContract.TABLE_NAME, null, values)
                if(recordId != -1L) {
                    returnUri = TimingsContract.buildUriFromId(recordId)
                } else {
                    throw SQLException("Failed to insert, Uri: $uri")
                }
            }
            else -> throw java.lang.IllegalArgumentException("Unknown Uri: $uri")
        }
        // if insert op was successful, recordId will not be 0
        if(recordId > 0) {
            Log.i(TAG, "insert: notify observers of change with $uri")
            context.contentResolver?.notifyChange(uri, null) // pass original uri passed to insert()
        }
        Log.d(TAG, "insert: returning uri :$returnUri")
        return  returnUri
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        // combo of query and insert params & methods, returns number of rows updated
        Log.i(TAG, "update called, uri is $uri")
        val match = uriMatcher.match(uri)
        Log.d(TAG, "update, match is $match")

        val count: Int
        var selectionCriteria: String
        val context = requireContext(this) // use this, getContext returns Context? not Context -> type mismatch error

        when(match) {
            // Views are not to be updated
            TASKS -> {
                val db = AppDatabase.getInstance(context).writableDatabase
                count = db.update(TasksContract.TABLE_NAME, values, selection, selectionArgs)
            }

            TASKS_ID -> {
                val db = AppDatabase.getInstance(context).writableDatabase
                val id = TasksContract.getId(uri)
                selectionCriteria = "${TasksContract.Columns.ID} = $id"
                if(selection != null && selection.isNotEmpty()) {
                    selectionCriteria += " AND ($selection)"
                }
                count = db.update(TasksContract.TABLE_NAME, values, selectionCriteria, selectionArgs)
            }

            TIMINGS -> {
                val db = AppDatabase.getInstance(context).writableDatabase
                count = db.update(TimingsContract.TABLE_NAME, values, selection, selectionArgs)
            }

            TIMINGS_ID -> {
                val db = AppDatabase.getInstance(context).writableDatabase
                val id = TimingsContract.getId(uri)
                selectionCriteria = "${TimingsContract.Columns.ID} = $id"
                if(selection != null && selection.isNotEmpty()) {
                    selectionCriteria += " AND ($selection)"
                }
                count = db.update(TimingsContract.TABLE_NAME, values, selectionCriteria, selectionArgs)
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        // notify observers
        if(count > 0) {
            Log.i(TAG, "update: notifying observers of change with $uri")
            context.contentResolver?.notifyChange(uri, null)
        }
        Log.d(TAG, "update: returning row count: $count")
        return count
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        // similar to update method, returns number of rows deleted
        Log.i(TAG, "delete called, uri is $uri")
        val match = uriMatcher.match(uri)
        Log.d(TAG, "delete, match is $match")

        val count: Int
        var selectionCriteria: String
        val context = requireContext(this) // use this, getContext returns Context? not Context -> type mismatch error

        when(match) {
            // Views are not accessed/written to
            TASKS -> {
                val db = AppDatabase.getInstance(context).writableDatabase
                count = db.delete(TasksContract.TABLE_NAME, selection, selectionArgs)
            }

            TASKS_ID -> {
                val db = AppDatabase.getInstance(context).writableDatabase
                val id = TasksContract.getId(uri)
                selectionCriteria = "${TasksContract.Columns.ID} = $id"
                if(selection != null && selection.isNotEmpty()) {
                    selectionCriteria += " AND ($selection)"
                }
                count = db.delete(TasksContract.TABLE_NAME, selectionCriteria, selectionArgs)
            }

            TIMINGS -> {
                val db = AppDatabase.getInstance(context).writableDatabase
                count = db.delete(TimingsContract.TABLE_NAME, selection, selectionArgs)
            }

            TIMINGS_ID -> {
                val db = AppDatabase.getInstance(context).writableDatabase
                val id = TimingsContract.getId(uri)
                selectionCriteria = "${TimingsContract.Columns.ID} = $id"
                if(selection != null && selection.isNotEmpty()) {
                    selectionCriteria += " AND ($selection)"
                }
                count = db.delete(TimingsContract.TABLE_NAME, selectionCriteria, selectionArgs)
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        // notify observers
        if(count > 0) {
            Log.i(TAG, "delete: notifying observers of change with $uri")
            context.contentResolver?.notifyChange(uri, null)
        }
        Log.d(TAG, "delete: returning count: $count")
        return count
    }

}