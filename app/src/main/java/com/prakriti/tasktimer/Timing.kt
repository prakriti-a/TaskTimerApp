package com.prakriti.tasktimer

import android.util.Log
import java.util.*

private const val TAG = "Timing"

class Timing(val taskId: Long, val startTime: Long = Date().time / 1000, var id: Long = 0) {
    // taskId & startTime wont be changed - so use val
    // startTime & id have default value of current time & 0 resp

    var duration: Long = 0
        private set // private setter for duration field

    fun setDuration() {
        // calculate duration from startTime to currentTime
        duration = Date().time / 1000 - startTime // using seconds, not milliseconds
        Log.d(TAG, "setDuration: $taskId - Start time: $startTime - Duration: $duration")
    }
}