package com.prakriti.tasktimer

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentValues
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

private const val TAG = "TaskTimerViewModel"

class TaskTimerViewModel(application: Application): AndroidViewModel(application) {
// observe notifications from content provider about changes to data
// even with multiple VM instances created, all will be notified of data updates from the single content provider of the app
// but allowing multiple VM instance is waste of resources as not all observers require all functions in VM
// instead get VM instances using activity scope, so only single instance is created & queries wont be executed multiple times

    // all dab actions -> save, add, delete should go via VM, not happen directly in UI classes

    private val contentObserver = object: ContentObserver(Handler()) {
        // when change occurs & is notified
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            Log.i(TAG, "onChange: content observer's on change called with $uri")
            loadTasks()
        }
    }

    private val databaseCursor = MutableLiveData<Cursor>()
    val cursor: LiveData<Cursor> get() = databaseCursor

    // var to store the timing
    private var currentTiming: Timing? = null // set to new instance when user starts timing a task

    private val taskTiming = MutableLiveData<String>()
    val timing: LiveData<String> get() = taskTiming

    init { // called when VM instance is created - for every new instance
        Log.i(TAG, "TaskTimerViewModel created")
        // register observer here, can have 1 observer per URI, or one observer for many URIs
        getApplication<Application>().contentResolver
            .registerContentObserver(TasksContract.CONTENT_URI, true, contentObserver)
        // retrieve task that is being currently timed before loading the task list
        // so user wont start timing a task when current/retrieved task is possibly still being timed
        currentTiming = retrieveTiming()
        loadTasks()
    }

    private fun loadTasks() {
        val projection = arrayOf(TasksContract.Columns.ID, TasksContract.Columns.TASK_NAME,
                            TasksContract.Columns.TASK_DESCRIPTION, TasksContract.Columns.TASK_SORT_ORDER)
        // order by <sort_order>, <name>
        val orderBy = "${TasksContract.Columns.TASK_SORT_ORDER}, ${TasksContract.Columns.TASK_NAME}"

        thread { // easier way to run threads when results are not required immediately after running
            val cursor = getApplication<Application>().contentResolver.query(
                // use getter & specify exact type of application within <>
                TasksContract.CONTENT_URI, projection, null, null, orderBy
            )
            if (cursor != null) {
                // *BUG* requires !! asserted call even after null-check
                databaseCursor.postValue(cursor!!) // use postValue -> different thread
            }
        }
    }

    fun saveTask(task: Task): Task {
        val values = ContentValues()
        if(task.name.isNotEmpty()) { // name is the mandatory field, model deals with checking this, not the UI
            values.put(TasksContract.Columns.TASK_NAME, task.name)
            values.put(TasksContract.Columns.TASK_DESCRIPTION, task.description)
            values.put(TasksContract.Columns.TASK_SORT_ORDER, task.sortOrder) // default 0
            if(task.id == 0L) { // adding new task
                thread {
                    Log.d(TAG, "saveTask: adding new task record")
                    val uri = getApplication<Application>().contentResolver?.insert(TasksContract.CONTENT_URI, values)
                    if(uri != null) {
                        task.id = TasksContract.getId(uri)
                        Log.d(TAG, "saveTask: new task id is ${task.id}")
                    }
                }
            } else { // update existing record
                thread {
                    Log.d(TAG, "saveTask: updating existing task record")
                    getApplication<Application>().contentResolver?.update(
                        TasksContract.buildUriFromId(
                            task.id
                        ), values, null, null
                    )
                }
            }
        }
        // no else case -> if task.name is empty
        return task // return the passed task itself, as it may have a new id in case of adding new record -> passed back to frag
    }

    // use thread extension fn to run the database related tasks on a bg thread
    fun deleteTask(taskId: Long) {
        Log.i(TAG, "deleteTask called on separate thread")
        // to use coroutines instead of thread, use 'GlobalScope.launch' -> include coroutines dependency in module gradle
        // with coroutines -> threads are reused from a pool of threads, reduces overhead of creating threads
        thread { // thread uses default values
            getApplication<Application>().contentResolver?.delete(
                TasksContract.buildUriFromId(
                    taskId
                ), null, null
            )
        }
    }

    fun timeTask(task: Task) {
        Log.d(TAG, "timeTask() called")
        // use local var to allow smart casts
        val timingRecord = currentTiming
        if (timingRecord == null) {
            // no task is being timed, start timing the new task
            currentTiming = Timing(task.id)
            saveTiming(currentTiming!!)
            // ^ or pass assign Timing object to timingRecord, pass that & assign it back to currentTiming
        } else {
            // task is being timed, so save it
            timingRecord.setDuration() // update duration of task
            saveTiming(timingRecord)

            if (task.id == timingRecord.taskId) {
                // current task was long tapped a second time, stop timing
                currentTiming = null
            } else {
                // a new task is being long tapped, time new task
                currentTiming = Timing(task.id)
                saveTiming(currentTiming!!)
            }
        }
        // update live data object
        taskTiming.value =
            if (currentTiming != null) task.name else null // first check that something is being timed
    }

    private fun saveTiming(currentTiming: Timing) {
        Log.d(TAG, "saveTiming() called")
        // check if we are updating, or inserting a new row
        val inserting = (currentTiming.duration == 0L)

        val values = ContentValues().apply {
            if (inserting) {
                put(TimingsContract.Columns.TIMING_TASK_ID, currentTiming.taskId)
                put(TimingsContract.Columns.TIMING_START_TIME, currentTiming.startTime)
            }
            // in case of updating, only duration will change
            put(TimingsContract.Columns.TIMING_DURATION, currentTiming.duration)
        }
        GlobalScope.launch {
            if (inserting) {
                val uri = getApplication<Application>().contentResolver.insert(
                    TimingsContract.CONTENT_URI,
                    values
                )
                if (uri != null) {
                    currentTiming.id = TimingsContract.getId(uri)
                }
            } else {
                getApplication<Application>().contentResolver.update(
                    TimingsContract.buildUriFromId(
                        currentTiming.id
                    ), values, null, null
                )
            }
        }
    }

    @SuppressLint("Range")
    private fun retrieveTiming(): Timing? {
        // for queries to viewCurrentTiming
        Log.d(TAG, "retrieveTiming called")
        val timing: Timing?
        // accessing db on main UI thread to ensure timing is loaded before user interacts with UI
        val timingCursor: Cursor? = getApplication<Application>().contentResolver.query(
            CurrentTimingContract.CONTENT_URI,
            null, // get all columns
            null,
            null,
            null
        )

        if (timingCursor != null && timingCursor.moveToFirst()) {
            // in descending order, so get the first one
            // we have an un-timed record
            val id =
                timingCursor.getLong(timingCursor.getColumnIndex(CurrentTimingContract.Columns.TIMING_ID))
            val taskId =
                timingCursor.getLong(timingCursor.getColumnIndex(CurrentTimingContract.Columns.TASK_ID))
            val startTime =
                timingCursor.getLong(timingCursor.getColumnIndex(CurrentTimingContract.Columns.START_TIME))
            val name =
                timingCursor.getString(timingCursor.getColumnIndex(CurrentTimingContract.Columns.TASK_NAME))
            timing = Timing(taskId, startTime, id)

            // update LiveData obj
            taskTiming.value = name
        } else {
            // no timing record found with 0 duration
            timing = null
        }
        timingCursor?.close()
        return timing
    }

    override fun onCleared() {
        // this method will not be called on device shut down due to 0% battery, or android killing the app due to low memory
        // so don't use this method to persist data or saving state, use activity/fragment's onPause or onStop in such cases
        Log.i(TAG, "onCleared called")
        // unregister observer, avoid memory leaks
        getApplication<Application>().contentResolver.unregisterContentObserver(contentObserver)
    }
}