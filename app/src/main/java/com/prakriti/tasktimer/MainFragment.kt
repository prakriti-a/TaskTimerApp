package com.prakriti.tasktimer

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.prakriti.tasktimer.databinding.FragmentMainBinding

private const val TAG = "MainFragment"

private const val DIALOG_ID_DELETE = 1
private const val DIALOG_TASK_ID = "task_id"

class MainFragment : Fragment(), CursorRVAdapter.OnTaskClickListener, AppDialog.DialogEvents {

    private var _binding: FragmentMainBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: TaskTimerViewModel by viewModels()
    private val cursorRVAdapter = CursorRVAdapter(null, this) // no record yet

    interface OnTaskEdit {
        fun onTaskEdit(task: Task) // call in onEditClicked()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context !is OnTaskEdit) {
            throw RuntimeException("$context must implement OnTaskEdit interface")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // subscribe to viewmodel
        viewModel.cursor.observe(this, Observer {
            cursor -> cursorRVAdapter.swapCursor(cursor)?.close() }) // swapping cursor when it changes
            // swapCursor() returns the old cursor, and it is closed here^
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
//        _binding = FragmentMainBinding.inflate(inflater, container, false)
//        // widgets in fragment layout can be accessed after inflating
//        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // attach adapter to rv here
        binding.rvTaskList.layoutManager = LinearLayoutManager(context)
        binding.rvTaskList.adapter = cursorRVAdapter
    }

    override fun onEditClicked(task: Task) {
        Log.i(TAG, "onEditClicked called")
        // a fragment shouldn't try to add another frag to its activity's layout, so call back to main activity
        // use listener fn to callback to activity
        (activity as OnTaskEdit?)?.onTaskEdit(task)
    }

    override fun onDeleteClicked(task: Task) { // show appDialog to confirm delete operation
        Log.i(TAG, "onDeleteClicked called")
        val args = Bundle().apply {
            putInt(DIALOG_ID, DIALOG_ID_DELETE)
            // use replaceable values for the string
            putString(DIALOG_MESSAGE, getString(R.string.delete_dialog_message, task.id, task.name))
            putInt(DIALOG_POSITIVE_RID, R.string.delete_dialog_btn) // neg button defaults to cancel
            putLong(DIALOG_TASK_ID, task.id) // pass task id so we can retrieve it in the callback fn
        }
        val dialog = AppDialog()
        dialog.arguments = args // this bundle is passed to dialog's callback fns
        dialog.show(childFragmentManager, null) // when showing dialog from frag, pass childFragMgr not supportFragMgr
    }

    override fun onPositiveDialogResult(dialogId: Int, args: Bundle) {
        // android dialogs do not behave as modal dialogs
        // ** Modal dialog -> pauses pgm until dialog is dismissed, after which the pgm resumes
        Log.i(TAG, "onPositiveDialogResult called")
        // ensure we're responding to the correct dialog
        if(dialogId == DIALOG_ID_DELETE) {
            val taskId = args.getLong(DIALOG_TASK_ID) // getLong returns 0 if key/value DNE in Bundle
            // include check for task_id, throw exception if not present.
                // since it'll be due to programming error, can use AssertionError instead -> more like warning to programmer
            if(BuildConfig.DEBUG && taskId == 0L) throw AssertionError("Task ID is null")
            // this code wont compile into app in Release version, only for Debug version
            // tell VM to delete the task
            viewModel.deleteTask(taskId)
        }
    }

    override fun onTaskLongClicked(task: Task) {
        Log.i(TAG, "onTaskLongClicked called")
    }

    companion object {}

}