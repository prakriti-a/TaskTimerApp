package com.prakriti.tasktimer

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

    init { // called when VM instance is created - for every new instance
        Log.i(TAG, "TaskTimerViewModel created")
        // register observer here, can have 1 observer per URI, or one observer for many URIs
        getApplication<Application>().contentResolver
            .registerContentObserver(TasksContract.CONTENT_URI, true, contentObserver)
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
            if (cursor != null) { // null-check bug?
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
                    getApplication<Application>().contentResolver?.update(TasksContract.buildUriFromId(task.id), values, null, null)
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
            getApplication<Application>().contentResolver?.delete(TasksContract.buildUriFromId(taskId), null, null)
        }
    }

    override fun onCleared() {
        Log.i(TAG, "onCleared called")
        // unregister observer, avoid memory leaks
        getApplication<Application>().contentResolver.unregisterContentObserver(contentObserver)
    }
}