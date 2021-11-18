package com.prakriti.tasktimer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import com.prakriti.tasktimer.databinding.SettingsDialogBinding

private const val TAG = "SettingsDialog"

class SettingsDialog: AppCompatDialogFragment() {

    private var _binding: SettingsDialogBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // inflate layout
        Log.i(TAG, "onCreateView called")
        return inflater.inflate(R.layout.settings_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.i(TAG, "onViewCreated called")
        super.onViewCreated(view, savedInstanceState)
        // set onClickListeners here
        binding.btnOk.setOnClickListener {
            // saveValues()
            dismiss()
        }
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }
}