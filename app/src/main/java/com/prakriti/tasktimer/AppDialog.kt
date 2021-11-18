package com.prakriti.tasktimer

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDialogFragment

private const val TAG = "AppDialog"

const val DIALOG_ID = "id"
const val DIALOG_MESSAGE = "message"
const val DIALOG_POSITIVE_RID = "positive_rid"
const val DIALOG_NEGATIVE_RID = "negative_rid"

class AppDialog: AppCompatDialogFragment() {

    private var dialogEvents: DialogEvents? = null // listener activity to be called back

    /**
     * The dialog's callback interface: to notify of user selected results (confirm deletion, etc)
     */
    internal interface DialogEvents {
        fun onPositiveDialogResult(dialogId: Int, args: Bundle)
        // when creating a dialog, activity gives it a unique ID which can be checked in the callback
        // use Bundle to pass values into Fragment, as its constructor should be empty
//        fun onNegativeDialogResult(dialogId: Int, args: Bundle)
//        fun onDialogCancelled(dialogId: Int)
    }

    override fun onAttach(context: Context) {
        Log.i(TAG, "onAttach called: context is $context")
        super.onAttach(context)
        // activities/fragments containing this fragment must implement its callbacks
        dialogEvents = try {
            // if there is a parent fragment, that will be called back. else it'll be null
            parentFragment as DialogEvents
        } catch (e: TypeCastException) {
            // no parent frag, we have context -> activity will be called back using context
            try {
                context as DialogEvents
            } catch (e: ClassCastException) {
                // here, activity doesn't implement the interface
                throw ClassCastException("Activity $context must implement AppDialog.DialogEvents interface")
            }
        } catch (e: ClassCastException) {
            // here, parent frag doesn't implement the interface
            throw ClassCastException("Fragment $parentFragment must implement AppDialog.DialogEvents interface")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.i(TAG, "onCreateDialog called")
        val builder = AlertDialog.Builder(context) // requireContext throws IllegalStateEx (used instead of !!)
        // fix 'smart cast to Bundle is impossible bcoz 'arguments is a mutable property'
        val arguments = arguments
        val dialogId: Int
        val messageString: String?
        var positiveStringId: Int
        var negativeStringId: Int

        if(arguments != null) {
            // callback interface requires ID and dialog requires MESSAGE, raise exception if null
            dialogId = arguments.getInt(DIALOG_ID)
            messageString = arguments.getString(DIALOG_MESSAGE)
            if(dialogId == 0 || messageString == null) {
                throw IllegalArgumentException("DIALOG_ID and/or DIALOG_MESSAGE not present in Bundle")
            }
            // optional (getInt returns 0 if key is empty)
            positiveStringId = arguments.getInt(DIALOG_POSITIVE_RID)
            if(positiveStringId == 0) positiveStringId = R.string.ok
            negativeStringId = arguments.getInt(DIALOG_NEGATIVE_RID)
            if(negativeStringId == 0) negativeStringId = R.string.cancel
        } else {
            throw IllegalArgumentException("Must pass DIALOG_ID and DIALOG_MESSAGE in Bundle")
        }

        return builder.setMessage(messageString)
                .setPositiveButton(positiveStringId) { dialogInterface, which ->
                    // here in click listener, we get ref to the dialog (dialogInterface), & clicked button/ item pos (which)
                    // callback positive result function
                    dialogEvents?.onPositiveDialogResult(dialogId, arguments)
                    // dialog auto-dismisses itself when button is tapped // have to manually dismiss if using custom dialog
                }
                .setNegativeButton(negativeStringId) { dialogInterface, which ->
                    // callback negative result function
//                    dialogEvents?.onNegativeDialogResult(dialogId, arguments)
                }
                .create()
    }

    override fun onCancel(dialog: DialogInterface) {
        // called if user dismisses dialog using back button or tapping screen. neither of the button's callback fns will be called
        Log.i(TAG, "onCancel called")
        val dialogId = arguments?.getInt(DIALOG_ID) // requireArguments() used instead of !! assert not-null op
            // require_() throws IllegalStateException if null
//        dialogEvents?.onDialogCancelled(dialogId)
    }

    // remove this from code
    override fun onDismiss(dialog: DialogInterface) {
        // called when dialog is dismissed (from button taps), as well as when onCancel is called
        // onCancel is not called when dialogName.dismiss is called
        Log.i(TAG, "onDismiss called")
        super.onDismiss(dialog) // Don't remove this call
    }

    override fun onDetach() {
        super.onDetach()
        // don't call back functions if activity has been destroyed, reset activity callbacks interface
        dialogEvents = null
    }
}