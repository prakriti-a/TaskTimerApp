package com.prakriti.tasktimer

import android.database.Cursor
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

private const val TAG = "DurationsReport"

enum class SortColumns {
    NAME, DESCRIPTION, START_DATE, DURATION
}

class DurationsReportActivity : AppCompatActivity() {

    private val durationsAdapter by lazy { DurationsRVAdapter(this, null) }
    var databaseCursor: Cursor? = null
    var sortOrder = SortColumns.NAME

    // also allowing display of records between two dates by passing selection to content resolver obj
    private val selection = "${DurationsContract.Columns.START_TIME} Between ? AND ?"
    private var selectionArgs =
        arrayOf("1556668800", "1559347199") // temp hardcoded range for 1st - 31st May


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.task_durations)
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }
}