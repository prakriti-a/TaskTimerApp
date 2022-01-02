package com.prakriti.tasktimer.debug

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import com.prakriti.tasktimer.TasksContract
import com.prakriti.tasktimer.TimingsContract
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

internal class TestTiming internal constructor(var taskId: Long, date: Long, var duration: Long) {
    var startTime: Long = 0

    init {
        this.startTime = date / 1000
    }
}

object TestData {
// create random Timing data for Tasks in DB to test reports

    private const val SECS_IN_DAY = 86400
    private const val LOWER_BOUND = 100
    private const val UPPER_BOUND = 500
    private const val MAX_DURATION = SECS_IN_DAY / 6

    @SuppressLint("Range")
    fun generateTestData(contentResolver: ContentResolver) {
        // query the DB for the tasks
        val projection = arrayOf(TasksContract.Columns.ID)
        val uri = TasksContract.CONTENT_URI
        val cursor = contentResolver.query(uri, projection, null, null, null)

        if (cursor != null && cursor.moveToFirst()) {
            do {
                val taskId = cursor.getLong(cursor.getColumnIndex(TasksContract.Columns.ID))
                val loopCount = LOWER_BOUND + getRandomInt(UPPER_BOUND - LOWER_BOUND)

                for (i in 0 until loopCount) {
                    // generate random date/time
                    val randomDate = randomDateTime()
                    // generate random duration between 0 and 4 hours
                    val randomDuration = getRandomInt(MAX_DURATION).toLong()
                    // create new TestTiming record with random date and duration
                    val testTiming = TestTiming(taskId, randomDate, randomDuration)
                    // add it to DB
                    saveCurrentTiming(contentResolver, testTiming)
                }
            } while (cursor.moveToNext())
            cursor.close()
        }
    }

    private fun getRandomInt(max: Int): Int {
        // random() returns a +ve double value -> [0.0, 1.0]
        // round() returns the closest Long to the arg passed (rounded off)
        return Math.round(Math.random() * max).toInt()
    }

    private fun randomDateTime(): Long {
        val startYear = 2021
        val endYear = 2022
        // to get random # of seconds, min, hours, etc
        val sec = getRandomInt(59)
        val min = getRandomInt(59)
        val hour = getRandomInt(23)
        val month = getRandomInt(11)
        val year = startYear + getRandomInt(endYear - startYear)

        val gc = GregorianCalendar(year, month, 1)
        val day = 1 + getRandomInt(gc.getActualMaximum(GregorianCalendar.DAY_OF_MONTH) - 1)
        // getActualMaximum() also accounts for leap years
        gc.set(year, month, day, hour, min, sec)
        return gc.timeInMillis
    }

    // to store random timing data in DB
    private fun saveCurrentTiming(contentResolver: ContentResolver, currentTiming: TestTiming) {
        val values = ContentValues()
        values.put(TimingsContract.Columns.TIMING_TASK_ID, currentTiming.taskId)
        values.put(TimingsContract.Columns.TIMING_START_TIME, currentTiming.startTime)
        values.put(TimingsContract.Columns.TIMING_DURATION, currentTiming.duration)

        GlobalScope.launch {
            contentResolver.insert(TimingsContract.CONTENT_URI, values)
        }

    }
}