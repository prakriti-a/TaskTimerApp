package com.prakriti.tasktimer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.prakriti.tasktimer.databinding.SettingsDialogBinding
import java.util.*

private const val TAG = "SettingsDialog"
// re-query preferences upon settings change -> using PreferenceListener

const val SETTINGS_FIRST_DAY_OF_WEEK = "firstDay"
const val SETTINGS_IGNORE_LESS_THAN = "ignoreLessThan"
const val SETTINGS_DEFAULT_IGNORE_LESS_THAN =
    0 // default to initialize fields/ when user hasn't saved settings

// to show increments of seconds to minutes in the settings dialog for ignoreSeconds seekBar
//                              0  1  2   3   4   5   6   7   8   9   10  11  12  13   14   15   16    17   18   19   20   21   22   23    24
private val deltas = intArrayOf(
    0,
    5,
    10,
    15,
    20,
    25,
    30,
    35,
    40,
    45,
    50,
    55,
    60,
    120,
    180,
    240,
    300,
    360,
    420,
    480,
    540,
    600,
    900,
    1800,
    2700
)

class SettingsDialog : AppCompatDialogFragment() {
// here we inflate the dialog's view onCreateView(), not onCreateDialog/dialog.builder, etc
    // as we need to access individual widgets inside the dialog
    // be careful when both methods are included in one class

    private var _binding: SettingsDialogBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private val defaultFirstDayOfWeek = GregorianCalendar(Locale.getDefault()).firstDayOfWeek
    private var firstDay = defaultFirstDayOfWeek
    private var ignoreLessThan = SETTINGS_DEFAULT_IGNORE_LESS_THAN


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate called")
        super.onCreate(savedInstanceState)
        setStyle(
            AppCompatDialogFragment.STYLE_NORMAL,
            R.style.SettingsDialogStyle
        ) // as defined in themes

        //retainInstance = true // this is used to avoid onDestroy & onCreate of a fragment when its activity is destroyed
        // other frag methods like onCreateView, etc will still be called (uses ex: orientation change, expensive data op)
        // also gives minor improvement in efficiency as frag processing is reduced
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // inflate layout
        Log.i(TAG, "onCreateView called")
        return inflater.inflate(R.layout.settings_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.i(TAG, "onViewCreated called")
        super.onViewCreated(view, savedInstanceState)
        // this line wont work in newer versions (api 23+): title wont appear if added directly in layout
        // add in code, override windowNoTitle=false style attribute in themes
        dialog?.setTitle(R.string.action_settings) // or @string/settings

        binding.sbIgnoreSeconds.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) { // removed ? from seekbar
                if (progress < 12) { // less than a minute
                    binding.txtIgnoreSecondsTitle.text = getString(
                        R.string.settingsIgnoreSecondsTitle,
                        deltas[progress],
                        resources.getQuantityString(R.plurals.settingsLittleUnits, deltas[progress])
                    )
                } else {
                    val minutes = deltas[progress] / 60
                    binding.txtIgnoreSecondsTitle.text = getString(
                        R.string.settingsIgnoreSecondsTitle,
                        minutes, resources.getQuantityString(R.plurals.settingsBigUnits, minutes)
                    )
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        // set onClickListeners here
        binding.btnOk.setOnClickListener {
            saveValues()
            dismiss()
        }
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        // bundle is null when dialog is first created
        Log.d(TAG, "onViewStateRestored called")
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState == null) { // *** test this if statement call on orientation changes, etc
            readValues() // read from pref
            binding.spinFirstDaySpinner.setSelection(firstDay - GregorianCalendar.SUNDAY) // spinner values are zero-based
            // convert back seconds value into index in time values array
            val seekBarValue =
                deltas.binarySearch(ignoreLessThan) // bin search works as our array is sorted
            if (seekBarValue < 0) // shouldn't happen -> programming error
                throw IndexOutOfBoundsException("Value $seekBarValue not found in deltas[] array")
            binding.sbIgnoreSeconds.max =
                deltas.size - 1 // (25 - 1) set explicitly in code to accommodate changes in deltas[] array
            Log.d(TAG, "onViewStateRestored: setting seekbar to $seekBarValue")
            binding.sbIgnoreSeconds.progress = seekBarValue

            // display seekbar text acc to seconds or minutes
            // use seekBar change listener to change title immediately
            if (ignoreLessThan < 60) { // get string res, replace %1 and %2 with [sec/min] [unit/s] ex: 15 seconds, 1 hour
                binding.txtIgnoreSecondsTitle.text = getString(
                    R.string.settingsIgnoreSecondsTitle,
                    ignoreLessThan,
                    resources.getQuantityString(
                        R.plurals.settingsLittleUnits,
                        ignoreLessThan
                    ) // pass qty to get back apt unit acc to lang rules
                )
            } else { // minutes
                val minutes = ignoreLessThan / 60
                binding.txtIgnoreSecondsTitle.text = getString(
                    R.string.settingsIgnoreSecondsTitle,
                    minutes,
                    resources.getQuantityString(R.plurals.settingsBigUnits, minutes)
                )
            }
        }
    }

    private fun readValues() {
        // to ensure values reflect post change, call this in onViewStateRestored()
        with(getDefaultSharedPreferences(context)) {
            firstDay = getInt(
                SETTINGS_FIRST_DAY_OF_WEEK,
                defaultFirstDayOfWeek
            ) // default if not saved yet
            ignoreLessThan = getInt(SETTINGS_IGNORE_LESS_THAN, SETTINGS_DEFAULT_IGNORE_LESS_THAN)
        }
        Log.d(TAG, "readValues: first day: $firstDay, ignore less than: $ignoreLessThan")
    }

    private fun saveValues() {
        val newFirstDay =
            binding.spinFirstDaySpinner.selectedItemPosition + GregorianCalendar.SUNDAY // (adding +1 to pos)
        // num corr to days -> sunday = 0, monday = 1..  *Calendar class uses sunday=1, monday=1, etc
        val newIgnoreLessThan =
            deltas[binding.sbIgnoreSeconds.progress] // deltas[0,24] -> stores corr value (in seconds)
        Log.d(TAG, "saveValues: first day: $newFirstDay, ignore seconds: $newIgnoreLessThan")

        with(getDefaultSharedPreferences(context).edit()) {
            if (newFirstDay != firstDay) {
                putInt(SETTINGS_FIRST_DAY_OF_WEEK, newFirstDay)
            }
            if (newIgnoreLessThan != ignoreLessThan) {
                putInt(SETTINGS_IGNORE_LESS_THAN, newIgnoreLessThan)
            }
            apply()
        }
    }
}