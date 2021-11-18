package com.prakriti.tasktimer

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.prakriti.tasktimer.databinding.FragmentAddEditBinding

private const val TAG = "AddEditFragment"

private const val ARG_TASK = "task"

class AddEditFragment : Fragment() {
// for this frag, if we want to exit without saving task details, MainActivity will have to handle the dialog confirmation
// MainActivity is responsible for its fragments

    private var _binding: FragmentAddEditBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: TaskTimerViewModel by viewModels() // read Doc on this code - check scope of VM, where it comes from
    // use activity scope to avoid multiple instances of VM being created for each fragment
    // lazy delegate inits a var the first time its referred to, activity may be null before onActivityCreated()
    // check LogCat to see if appDB is re-queried on every frag creation

    private var task: Task? = null
    private var listener: OnSaveClicked? = null


    override fun onAttach(context: Context) {
        Log.i(TAG, "onAttach called")
        super.onAttach(context)
        if(context is OnSaveClicked) { // TODO: later, add cancel button too
            listener = context
        } else {
            throw RuntimeException("$context must implement OnSaveClicked")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // dont try to get any references to UI here
        Log.i(TAG, "onCreate called")
        super.onCreate(savedInstanceState)
        task = arguments?.getParcelable<Task>(ARG_TASK) // Task implements Parcelable
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // frag instantiates its UI here, if it has one -> released in onDestroyView()
        Log.i(TAG, "onCreateView called")
        _binding = FragmentAddEditBinding.inflate(inflater, container, false)
        // widgets in fragment layout can be accessed after inflating
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // called after onCreateView before saved state restoration, view hierarchy completely created here
        Log.i(TAG, "onViewCreated called")
//        super.onViewCreated(view, savedInstanceState)
        // modify UI -> if editing existing task, populate saved details from db
        // if new task, widgets will be empty
        if(savedInstanceState == null) { // if user has not already made any edits to the widgets
            // or when frag is first created, savedInstanceState = null
            val task = task // create local var to avoid smart cast error and using !! on every line since task is nullable
            if (task != null) {
                // edit existing task
                Log.d(TAG, "onViewCreated: task details exist: ${task.id}")
                binding.edtInputTaskName.setText(task.name)
                binding.edtInputTaskDesc.setText(task.description)
                binding.edtInputSortOrder.setText(Integer.toString(task.sortOrder)) // this warning is imp in case of decimal number usage
            } else {
                // add new task
                Log.d(TAG, "onViewCreated: adding new task, no record existing")
            }
        }
    }

    private fun createTaskFromUi(): Task {
        val sortOrder = if(binding.edtInputSortOrder.text.isNotEmpty()) {
            Integer.parseInt(binding.edtInputSortOrder.text.toString())
        } else {
            0
        }
        // create Task (data class)
        val newTask = Task(binding.edtInputTaskName.text.toString(), binding.edtInputTaskDesc.text.toString(), sortOrder)
        newTask.id = task?.id ?: 0 // take id of Task that is passed as Bundle to AddEditFrag
        // assign 0 if frag's task is null i.e. adding new Task (?: elvis op)
        return newTask
    }

    //
    fun isDataPresent(): Boolean {
        val newTask = createTaskFromUi()
        return ((newTask != task) && // if task being edited is not equal to the task passed in Bundle, aka, edits have been made
                // task-null in case of adding new task, so also check input pulled from editText fields if anyone field has data
                (newTask.name.isNotBlank() || newTask.description.isNotBlank() || newTask.sortOrder != 0))
    }

    private fun saveTask() {
        // update db if & only if, atleast 1 filed is edited
        // saving business logic occurs in VM -> pass task object created from UI
        // create new Task with details passed in UI, call VM's saveTask()
        // Task is a data class, we can compare new & old task, save only if they are different
        Log.d(TAG, "saveTask called")

        val newTask = createTaskFromUi()
        if(!newTask.equals(task)) { // can also replace this with != operator, works with data classes
            Log.d(TAG, "saveTask: saving task #${newTask.id}")
            task = viewModel.saveTask(newTask)
            Log.d(TAG, "saveTask: task id is ${task?.id}")
        }
        //==del all this

//        if (task != null) { // cross check values w existing task
//            Log.d(TAG, "saveTask: updating existing task")
//            if(binding.edtInputTaskName.text.toString() != task.name) {
//                values.put(TasksContract.Columns.TASK_NAME, binding.edtInputTaskName.text.toString())
//            }
//            if(binding.edtInputTaskDesc.text.toString() != task.description) {
//                values.put(TasksContract.Columns.TASK_DESCRIPTION, binding.edtInputTaskDesc.text.toString())
//            }
//            if(sortOrder != task.sortOrder) {
//                values.put(TasksContract.Columns.TASK_SORT_ORDER, sortOrder)
//            }
//            if(values.size() != 0) {
//                Log.d(TAG, "saveTask: updating task")
//                activity?.contentResolver?.update(TasksContract.buildUriFromId(task.id), values, null, null)
//            }
//        } else { // adding new record
//            Log.d(TAG, "saveTask: adding new task record")
//            if(binding.edtInputTaskName.text.isNotEmpty()) {
//                values.put(TasksContract.Columns.TASK_NAME, binding.edtInputTaskName.text.toString())
//                if(binding.edtInputTaskDesc.text.isNotEmpty()) {
//                    values.put(TasksContract.Columns.TASK_DESCRIPTION, binding.edtInputTaskDesc.text.toString())
//                }
//                values.put(TasksContract.Columns.TASK_SORT_ORDER, sortOrder) // default 0 if empty, as spec in above code
//                activity?.contentResolver?.insert(TasksContract.CONTENT_URI, values)
//            }
//        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.i(TAG, "onActivityCreated called")
        super.onActivityCreated(savedInstanceState)
        if(listener is AppCompatActivity) {
            val actionBar = (listener as AppCompatActivity?)?.supportActionBar // avoid this, listener is nullable -> use ?. in cast
            actionBar?.setDisplayHomeAsUpEnabled(true)
        }
        binding.btnSave.setOnClickListener {
            saveTask()
            listener?.onSaveClicked() } // safe-call in case activity is destroyed
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        // called when save state is restored into frag's view hierarchy
        super.onViewStateRestored(savedInstanceState)
    }

    override fun onStart() {
        // called when frag is visible to user (interactive in onResume)
        // onStop -> frag is not visible but not destroyed, may be resumed
        super.onStart()
    }

    override fun onPause() {
        // can add code to save data here, when frag is paused/left
        // onSaveInstanceState can be called anytime before onDestroy to save & reconstruct current state
        super.onPause()
    }

    override fun onDestroyView() {
        // view created by onCreateView is detached from fragment
        // onDestroy -> called when activity is destroyed, frag no longer used
        Log.i(TAG, "onDestroyView called")
        super.onDestroyView()
        _binding = null
    }

    override fun onDetach() {
        // frag detached from activity
        Log.i(TAG, "onDetach called")
        super.onDetach()
        listener = null
    }

    interface OnSaveClicked { // must be implemented by any class using this fragment
        fun onSaveClicked()
    }

    companion object { // factory method that simplifies fragment creation
        // task object can be applied here as bundle param instead of with calling code
        // fragments must have a no-param constructor
        @JvmStatic
        fun newInstance(task: Task?) =
            AddEditFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_TASK, task)
                }
            }
    }

}