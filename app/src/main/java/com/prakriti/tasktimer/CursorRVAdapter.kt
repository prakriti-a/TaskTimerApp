package com.prakriti.tasktimer

import android.annotation.SuppressLint
import android.database.Cursor
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) { //, LayoutContainer -> uses synthetic imports
// look up CursorAdapter

    // kotlin extensions only work in activity, frag & view classes -> look up LayoutContainer to avoid this
    private var name = itemView.findViewById<TextView>(R.id.txtTaskName)
    private var description = itemView.findViewById<TextView>(R.id.txtTaskDescription)
    private var buttonEdit = itemView.findViewById<ImageButton>(R.id.ibEditTask)
    private var buttonDelete = itemView.findViewById<ImageButton>(R.id.ibDeleteTask)

    fun bind(task: Task, listener: CursorRVAdapter.OnTaskClickListener) {
        name.text = task.name
        description.text = task.description
        buttonEdit.visibility = View.VISIBLE
        buttonDelete.visibility = View.VISIBLE

        buttonEdit.setOnClickListener {
            Log.d(TAG, "bind: edit button tapped, task name: ${task.name}")
            listener.onEditClicked(task)
        }
        buttonDelete.setOnClickListener {
            Log.d(TAG, "bind: delete button tapped, task name: ${task.name}")
            listener.onDeleteClicked(task)
        }
        itemView.setOnLongClickListener {
            Log.d(TAG, "bind: itemView is long clicked, task name: ${task.name}")
            listener.onTaskLongClicked(task)
            true // longClickListener returns true if tap is handled
        }

    }

    fun getName(): TextView {
        return name
    }

    fun getDescription(): TextView {
        return description
    }

    fun getEditButton(): ImageButton {
        return buttonEdit
    }

    fun getDeleteButton(): ImageButton {
        return buttonDelete
    }
}

private const val TAG = "CursorRVAdapter"

class CursorRVAdapter(private var cursor: Cursor?, val listener: OnTaskClickListener): RecyclerView.Adapter<TaskViewHolder>() {

//    private var context: Context? = null

    // define callbacks interface, pass in reference to listener in primary constructor
    interface OnTaskClickListener {
        fun onEditClicked(task: Task)
        fun onDeleteClicked(task: Task)
        fun onTaskLongClicked(task: Task)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
//        context = parent.context
        val view = LayoutInflater.from(parent.context).inflate(R.layout.task_list_item, parent, false)
        return TaskViewHolder(view)
    }

    @SuppressLint("Range")
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val cursor = cursor // use local var to avoid smart cast error, as cursor param is nullable
        if(cursor == null || cursor.count == 0) {
            Log.d(TAG, "onBindViewHolder: cursor is null")
            holder.getName().setText(R.string.instructions_title)
            holder.getDescription().setText(R.string.instructions_text)
            holder.getEditButton().visibility = View.GONE
            holder.getDeleteButton().visibility = View.GONE
        } else { // record present
            if(!cursor.moveToPosition(position)) {
                throw IllegalStateException("Couldn't move cursor to position $position")
            }
            // create a Task object from cursor data
            val task = Task(cursor.getString(cursor.getColumnIndex(TasksContract.Columns.TASK_NAME)),
                cursor.getString(cursor.getColumnIndex(TasksContract.Columns.TASK_DESCRIPTION)),
                cursor.getInt(cursor.getColumnIndex(TasksContract.Columns.TASK_SORT_ORDER)))
            // ID isn't set in constructor
            task.id = cursor.getLong(cursor.getColumnIndex(TasksContract.Columns.ID))
            // update fields
            holder.bind(task, listener)
//            holder.getName().text = task.name
//            holder.getDescription().text = task.description
//            holder.getEditButton().visibility = View.VISIBLE
//            holder.getDeleteButton().visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int {
        // return viewholder with instructions in case of no records, so always return atleast 1
        val cursor = cursor
        val count = if(cursor == null || cursor.count == 0) {
            1
        } else {
            cursor.count
        }
        return count
    }

    // modifying RV-Adapter -> add functionality for Cursor adapter in RecyclerView Adapter
    /**
     * swap in a new cursor, returning the old cursor
     * the returned old cursor is *not* closed
     *
     * @param newCursor: the new cursor to be used
     * @return Returns previously set cursor, or null if there wasn't one
     * If newCursor is the same instance as the previous one, null is returned
     */
    fun swapCursor(newCursor: Cursor?): Cursor? {
        // called whenever cursor used by adapter is changed
        Log.d(TAG, "swapCursor called")
        if(newCursor === cursor ) {
            return null
        }
        val numItems = itemCount
        val oldCursor = cursor
        cursor = newCursor // swap here
        if(newCursor != null) {
            // notify observers about new cursor
            notifyDataSetChanged()
        } else {
            // notify observers about lack of data set
            notifyItemRangeRemoved(0, numItems)
        }
        return oldCursor
    }

}


