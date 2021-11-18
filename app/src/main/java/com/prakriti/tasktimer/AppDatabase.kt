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
private const val DATABASE_VERSION = 1

internal class AppDatabase private constructor(context: Context): SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
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
        Log.d(TAG, "onCreate: SQL cmd: $createTasksSQL")
        db.execSQL(createTasksSQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d(TAG, "onUpgrade called")
        when(oldVersion) {
            1 -> {
                //upgrade logic from version 1
            }
            else -> throw  IllegalStateException("onUpgrade() with unknown newVersion: $newVersion")
        }
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