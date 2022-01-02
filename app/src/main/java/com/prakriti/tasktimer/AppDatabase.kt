package com.prakriti.tasktimer

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

/**
 *  Only our content provider [AppProvider] should use this DB class
 */
private const val TAG = "AppDatabase"

private const val DATABASE_NAME = "TaskTimer.db"
private const val DATABASE_VERSION = 4

internal class AppDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
// Singleton class -> private constructor, getter in companion object

    init { // cannot instantiate AppDatabase as its constructor is private
        Log.d(TAG, "Initialising")
    }

    override fun onCreate(db: SQLiteDatabase) { // db change to non null type
        // called if DB doesn't already exist
        // CREATE TABLE Tasks (_id INTEGER PRIMARY KEY NOT NULL, Name TEXT NOT NULL, Description TEXT, SortOrder INTEGER);
        Log.d(TAG, "onCreate called")
        val createTasksSQL = """CREATE TABLE ${TasksContract.TABLE_NAME} (
            ${TasksContract.Columns.ID} INTEGER PRIMARY KEY NOT NULL, 
            ${TasksContract.Columns.TASK_NAME} TEXT NOT NULL, 
            ${TasksContract.Columns.TASK_DESCRIPTION} TEXT, 
            ${TasksContract.Columns.TASK_SORT_ORDER} INTEGER);
        """.replaceIndent(" ")
        Log.d(TAG, "onCreate: create Tasks table: $createTasksSQL")
        db.execSQL(createTasksSQL)

        // any changes in db called in onCreate must also be added in onUpgrade upon changing db version
        addTimingsTable(db)
        addCurrentTimingView(db)
        addDurationsView(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d(TAG, "onUpgrade called")
        when (oldVersion) {
            1 -> { // upgrade logic from version 1
                addTimingsTable(db)
                addCurrentTimingView(db)
                addDurationsView(db)
                // upgrade will be performed for existing devices
            }
            2 -> {
                addCurrentTimingView(db)
                addDurationsView(db)
            }
            3 -> {
                addDurationsView(db)
            }
            else -> throw  IllegalStateException("onUpgrade() with unknown newVersion: $newVersion")
        }
    }

    private fun addTimingsTable(db: SQLiteDatabase) {
        val createTimingsSQL = """CREATE TABLE ${TimingsContract.TABLE_NAME} (
            ${TimingsContract.Columns.ID} INTEGER PRIMARY KEY NOT NULL,
            ${TimingsContract.Columns.TIMING_TASK_ID} INTEGER NOT NULL,
            ${TimingsContract.Columns.TIMING_START_TIME} INTEGER,
            ${TimingsContract.Columns.TIMING_DURATION} INTEGER);
        """.replaceIndent(" ")
        Log.d(TAG, "onCreate: create Timings table: $createTimingsSQL")
        db.execSQL(createTimingsSQL)

        // run trigger to auto-delete Timings record upon deletion of a Task record
        val removeTaskTrigger = """CREATE TRIGGER Remove_Task
            AFTER DELETE ON ${TasksContract.TABLE_NAME}
            FOR EACH ROW
            BEGIN
            DELETE FROM ${TimingsContract.TABLE_NAME}
            WHERE ${TimingsContract.Columns.TIMING_TASK_ID} = OLD.${TasksContract.Columns.ID};
            END;
        """.replaceIndent(" ")
        Log.d(TAG, "onCreate: remove tasks trigger: $removeTaskTrigger")
        db.execSQL(removeTaskTrigger)
    }

    // table to create view CurrentTiming record
    private fun addCurrentTimingView(db: SQLiteDatabase) {
        /* // SQL command
        CREATE VIEW viewCurrentTiming
             AS SELECT Timings._id, Timings.TaskId, Timings.StartTime, Tasks.Name
             FROM Timings JOIN Tasks
             ON Timings.TaskId = Tasks._id
             WHERE Timings.Duration = 0
             ORDER BY Timings.StartTime DESC;
         */
        val createTimingViewSQL = """CREATE VIEW ${CurrentTimingContract.TABLE_NAME}
            AS SELECT ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.ID},
            ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_TASK_ID},
            ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_START_TIME},
            ${TasksContract.TABLE_NAME}.${TasksContract.Columns.TASK_NAME} 
            FROM ${TimingsContract.TABLE_NAME} 
            JOIN ${TasksContract.TABLE_NAME} 
            ON ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_TASK_ID} = 
            ${TasksContract.TABLE_NAME}.${TasksContract.Columns.ID} 
            WHERE ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_DURATION} = 0 
            ORDER BY ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_START_TIME} DESC;
            """.replaceIndent(" ")
        Log.d(TAG, createTimingViewSQL)
        db.execSQL(createTimingViewSQL)
    }

    // create view for the durations -> we only read data, no scope for updating the db from this view
    private fun addDurationsView(db: SQLiteDatabase) {
        /* // sql cmd
        CREATE VIEW viewTaskDurations
             AS SELECT Tasks.Name, Tasks.Description, Timings.StartTime,
             DATE(Timings.StartTime, 'unixepoch', 'localtime') AS StartDate,
             SUM(Timings.Duration) AS Duration
             FROM Tasks INNER JOIN Timings
             ON Tasks._id = Timings.TaskId
             GROUP BY Tasks._id, StartDate;
         */
        val createDurationsViewSQL = """CREATE VIEW ${DurationsContract.TABLE_NAME}
            AS SELECT ${TasksContract.TABLE_NAME}.${TasksContract.Columns.TASK_NAME}, 
            ${TasksContract.TABLE_NAME}.${TasksContract.Columns.TASK_DESCRIPTION}, 
            ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_START_TIME}, 
            DATE(${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_START_TIME}, 'unixepoch', 'localtime') 
            AS ${DurationsContract.Columns.START_DATE}, 
            SUM(${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_DURATION}) 
            AS ${DurationsContract.Columns.DURATION} 
            FROM ${TasksContract.TABLE_NAME} INNER JOIN ${TimingsContract.TABLE_NAME} 
            ON ${TasksContract.TABLE_NAME}.${TasksContract.Columns.ID} = 
            ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_TASK_ID} 
            GROUP BY ${TasksContract.TABLE_NAME}.${TasksContract.Columns.ID}, ${DurationsContract.Columns.START_DATE};
            """.replaceIndent(" ")
        Log.d(TAG, createDurationsViewSQL)
        db.execSQL(createDurationsViewSQL)
    }

    companion object : SingletonHolder<AppDatabase, Context>(::AppDatabase)
    // this makes AppDatabase a singleton class, accessed by getInstance()

/*
    // replaced by SingletonHolder class
    companion object {

        @Volatile // means that writes to this field are immediately made visible to other threads.
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            // perform 2 null-checks using Elvis operator (?:) to accommodate multithreading in singleton pattern

            // 1. return instance if not null, else run synchronized {} which locks the var preventing access from another thread
            return instance ?: synchronized(this) {
                // 2. return instance if not null, else create & return a new AppDatabase instance
                instance ?: AppDatabase(context).also { instance = it }
                // in also {} -> { assigns new instance to var }
                // also calls the {} block and returns it
            }
        }
    }
*/
}