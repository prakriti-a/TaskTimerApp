package com.prakriti.tasktimer

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

// extension fn for confirmation dialog so it can be used anywhere in a FragmentActivity (in this case, MainActivity)
// extension fns are used like fns of a class w/o making any changes to the class' code itself
// here, it extends FragmentActivity.class
// receiver of extension fn -> the instance of the class it is called on
    // (so we get its "this" context, in this case, an instance of FragmentActivity)
fun FragmentActivity.showConfirmationDialog(id: Int, message: String,
                                            positiveButton: Int = R.string.ok, negativeButton: Int = R.string.cancel) {
    val args = Bundle().apply {
        putInt(DIALOG_ID, id)
        putString(DIALOG_MESSAGE, message)
        putInt(DIALOG_POSITIVE_RID, positiveButton)
        putInt(DIALOG_NEGATIVE_RID, negativeButton)
    }
    val dialog = AppDialog()
    dialog.arguments = args
    dialog.show(supportFragmentManager, null) // supportFragMgr is called on "this" : instance of class calling this extension fn
}

fun FragmentActivity.findFragmentById(id: Int): Fragment? {
    // only to find frag, not other frag transactions/ops
    return supportFragmentManager.findFragmentById(id) // returns frag if found, or null
}

/**
 * Extensions based on an article by Dinesh Babuhunky
 * at https://medium.com/thoughts-overflow/how-to-add-a-fragment-in-kotlin-way-73203c5a450b
 */

inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
    beginTransaction().func().commit()
}

fun FragmentActivity.addFragment(fragment: Fragment, frameId: Int) {
    supportFragmentManager.inTransaction { add(frameId, fragment)}
}

fun FragmentActivity.replaceFragment(fragment: Fragment, frameId: Int) {
    supportFragmentManager.inTransaction{ replace(frameId, fragment)}
}

fun FragmentActivity.removeFragment(fragment: Fragment) {
    supportFragmentManager.inTransaction { remove(fragment)}
}