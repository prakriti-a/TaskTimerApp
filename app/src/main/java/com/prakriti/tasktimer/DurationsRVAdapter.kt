package com.prakriti.tasktimer

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class DurationsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val txtName: TextView = itemView.findViewById(R.id.txtViewTaskName)
    val txtDescription: TextView = itemView.findViewById(R.id.txtViewTaskDesc)
    val txtStartDate: TextView = itemView.findViewById(R.id.txtViewStartTime)
    val txtDuration: TextView = itemView.findViewById(R.id.txtViewDuration)
}

private const val TAG = "DurationsRVAdapter"

class DurationsRVAdapter(context: Context, private var cursor: Cursor?) :
    RecyclerView.Adapter<DurationsViewHolder>() {

    private val dateFormat =
        DateFormat.getDateFormat(context) // Date classes can't handle time > 24 hours
    // uses context to figure out locale to use when formatting dates

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DurationsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.task_durations_items, parent, false)
        return DurationsViewHolder(view)
    }

    @SuppressLint("Range")
    override fun onBindViewHolder(holder: DurationsViewHolder, position: Int) {
        val cursor = cursor
        if (cursor != null && cursor.count != 0) {
            if (!cursor.moveToPosition(position)) {
                throw IllegalStateException("Couldn't move cursor to position $position")
            }
            val name = cursor.getString(cursor.getColumnIndex(DurationsContract.Columns.NAME))
            val description =
                cursor.getString(cursor.getColumnIndex(DurationsContract.Columns.DESCRIPTION))
            val startTime =
                cursor.getLong(cursor.getColumnIndex(DurationsContract.Columns.START_TIME))
            val totalDuration =
                cursor.getLong(cursor.getColumnIndex(DurationsContract.Columns.DURATION))

            val userDate =
                dateFormat.format(startTime * 1000) // DB stores seconds, we need milliseconds
            val totalTime = formatDuration(totalDuration)

            holder.txtName.text = name
            holder.txtDescription?.text = description // description is not present in portrait
            holder.txtStartDate.text = userDate
            holder.txtDuration.text = totalTime
        }
    }

    override fun getItemCount(): Int {
        return cursor?.count ?: 0
    }

    private fun formatDuration(duration: Long): String {
        // duration is in seconds, convert to hours:minutes:seconds
        // allowing for >24 hours -> so we can't use time data type
        val hours = duration / 3600
        val remainder = duration - hours * 3600
        val minutes = remainder / 60
        // val seconds = remainder - minutes * 60
        val seconds = remainder % 60

        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        // default locale as specified in the device settings
    }

    // modifying RV-Adapter -> add functionality for Cursor adapter in RecyclerView Adapter
    /**
     * swap in a new cursor, returning the old cursor which is *not* closed
     *
     * @param newCursor: the new cursor to be used
     * @return Returns previously set cursor, or null if there wasn't one
     * If newCursor is the same instance as the previous one, null is returned
     */
    fun swapCursor(newCursor: Cursor?): Cursor? {
        // called whenever cursor used by adapter is changed
        Log.d(TAG, "swapCursor called")
        if (newCursor === cursor) {
            return null
        }
        val numItems = itemCount
        val oldCursor = cursor
        cursor = newCursor // swap here
        if (newCursor != null) {
            // notify observers about new cursor
            notifyDataSetChanged()
        } else {
            // notify observers about lack of data set
            notifyItemRangeRemoved(0, numItems)
        }
        return oldCursor
    }


}