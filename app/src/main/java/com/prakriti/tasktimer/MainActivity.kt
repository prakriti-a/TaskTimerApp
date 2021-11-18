package com.prakriti.tasktimer

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.prakriti.tasktimer.databinding.ActivityMainBinding
import com.prakriti.tasktimer.databinding.ContentMainBinding

private const val TAG = "MainActivity"

private const val DIALOG_ID_CANCEL_ADD_EDIT = 2
// confirmation dialog can be shown when exiting AddEditFrag -> i.e back or up button

class MainActivity : AppCompatActivity(), AddEditFragment.OnSaveClicked, MainFragment.OnTaskEdit, AppDialog.DialogEvents {

//    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var activityBinding: ActivityMainBinding
    private lateinit var contentBinding: ContentMainBinding

    // 2-pane mode -> running in landscape or a tablet
    private var twoPane = false
//    private lateinit var mainFragment: Fragment

    // module/global scope bcoz we need to dismiss it in onStop (ex. orientation changes) to avoid memory leaks
    private var aboutDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityBinding.root)
        // widgets can be accessed after setContentView
        setSupportActionBar(activityBinding.toolbar)
        contentBinding = ContentMainBinding.inflate(layoutInflater)

        // in case of device rotation
        twoPane = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE // true if landscape
        // using extension fn here, moved supportFragMgr.findFrag call to ext fn
        val fragment = findFragmentById(R.id.task_details_container)
        if(fragment != null) { // fragment exists
            // fragment to edit tasks existed, so set up panes correctly
            showAddEditPane()
        } else {
            // no fragment -> hide right hand pane
            contentBinding.taskDetailsContainer.visibility = if(twoPane) View.INVISIBLE else View.GONE
            contentBinding.mainFragment.visibility = View.VISIBLE // ------err
        }

    }

    private fun showAddEditPane() {
        contentBinding.taskDetailsContainer.visibility = View.VISIBLE
        // hide the left pane if in single pane view
        contentBinding.mainFragment.visibility = if(twoPane) View.VISIBLE else View.GONE
    }

    private fun removeAddEditPane(fragment: Fragment? = null) {
        // pass frag as param, findFragmentById is an expensive operation
//        var fragment = supportFragmentManager.findFragmentById(R.id.task_details_container)
        Log.i(TAG, "removeAddEditPane called")
        if(fragment != null) {
//            supportFragmentManager.beginTransaction().remove(fragment).commit()
            // replace above code with extension fn
            removeFragment(fragment)
        }
        // set visibility of right hand pane
        contentBinding.taskDetailsContainer.visibility = if(twoPane) View.INVISIBLE else View.GONE
        // show left hand pane
        contentBinding.mainFragment.visibility = View.VISIBLE // .get(0)

        // remove up button for backwards nav from frag
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

//        val appDatabase = AppDatabase.getInstance(this)
//        val db = appDatabase.readableDatabase

//        val cursor = contentResolver.query(TasksContract.CONTENT_URI,
//            null,
//            null,
//            null,
//            null)
//        Log.d(TAG, "************************************************************************")
//        cursor?.use {
//            while (it.moveToNext()) {
//                // loop thru records
//                with(it) {
//                    val id = getLong(0)
//                    val name = getString(1)
//                    val description = getString(2)
//                    val sortOrder = getString(3)
//                    val result = "ID: $id, NAME: $name, DESC: $description, ORDER: $sortOrder"
//                    Log.d(TAG, "onCreate: query result: $result")
//                }
//            }
//        }
//        Log.d(TAG, "************************************************************************")

//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        appBarConfiguration = AppBarConfiguration(navController.graph)
//        setupActionBarWithNavController(navController, appBarConfiguration)


    // test fns
  /*  private fun testDeleteMultiple() {
        val selection = TasksContract.Columns.TASK_DESCRIPTION + " = ?"
        val selectionArgs = arrayOf("Completed")
        val rowsUpdated = contentResolver.delete(TasksContract.CONTENT_URI, selection, selectionArgs)
        Log.d(TAG, "testDeleteMultiple: rows deleted: $rowsUpdated")
    }

    private fun testDelete() {
        val taskUri = TasksContract.buildUriFromId(3)
        val rowsUpdated = contentResolver.delete(taskUri, null, null)
        Log.d(TAG, "testDelete: rows deleted: $rowsUpdated")
    }

    private fun testUpdateMultiple() {
        val values = ContentValues().apply {
            put(TasksContract.Columns.TASK_SORT_ORDER, 9)
            put(TasksContract.Columns.TASK_DESCRIPTION, "Completed")
        }
        val selection = TasksContract.Columns.TASK_SORT_ORDER + " = ?"
        val selectionArgs = arrayOf("2")
        val rowsUpdated = contentResolver.update(TasksContract.CONTENT_URI, values, selection, selectionArgs)
        Log.d(TAG, "testUpdateMultiple: rows updated: $rowsUpdated")
    }

    private fun testUpdate() {
        val values = ContentValues().apply {
            put(TasksContract.Columns.TASK_NAME, "Content Providers")
            put(TasksContract.Columns.TASK_DESCRIPTION, "Create content provider")
        }
        val taskUri = TasksContract.buildUriFromId(4)
        val rowsUpdated = contentResolver.update(taskUri, values, null, null)
        Log.d(TAG, "testUpdate: uri from id: $taskUri")
        Log.d(TAG, "testUpdate: rows updated: $rowsUpdated")
    }

    private fun testInsert() {
        val values = ContentValues().apply {
            put(TasksContract.Columns.TASK_NAME, "Test Task 1")
            put(TasksContract.Columns.TASK_DESCRIPTION, "Description 1")
            put(TasksContract.Columns.TASK_SORT_ORDER, 2)
        }
        val uri = contentResolver.insert(TasksContract.CONTENT_URI, values)
        Log.d(TAG, "testInsert: new row uri: $uri")
        Log.d(TAG, "testInsert: new row id: ${uri?.let { TasksContract.getId(it) }}")
    }
*/

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.menu_main_addTask -> {
                taskEditRequest(null)
            }
            R.id.menu_main_showDurations -> {

            }
            R.id.menu_main_settings -> {

            }
            R.id.main_menu_aboutApp -> {
                showAboutDialog()
            }
            R.id.main_menu_generateData -> {

            }
            android.R.id.home -> {
                // up button on toolbar/action bar for backwards nav -> remove frag on up click
                Log.d(TAG, "onOptionsItemSelected: home button pressed")
                // using extension fn
                val fragment = findFragmentById(R.id.task_details_container)
                // show confirmation dialog to exit in case edits have been made to task in AddEditFrag
                if(fragment is AddEditFragment) { // check before casting to type
                    if((fragment as AddEditFragment).isDataPresent()) {
                        showConfirmationDialog(DIALOG_ID_CANCEL_ADD_EDIT,
                            getString(R.string.dialog_confirm_exit_message),
                            R.string.dialog_abandon_changes_btn, // pos btn
                            R.string.dialog_cont_editing) // neg btn
                    }
                } else {
                    removeAddEditPane(fragment)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showAboutDialog() {
        // in custom dialog, we have to inflate the layout
        val messageView = layoutInflater.inflate(R.layout.about, null, false)
        // root can be null when -> 1) layout is root, has no root view & 2) creating dialog, its not part of underlying activity
        val builder = AlertDialog.Builder(this)

        // the foll lines should be called before builder.create() is called, or it wont appear/work
        builder.setTitle(R.string.app_name)
        builder.setIcon(R.mipmap.ic_launcher)
        // set a button on the dialog
        builder.setPositiveButton(R.string.ok) { dialog, which -> // replace unused args with _ to optimize code
            Log.d(TAG, "showAboutDialog: clicking pos button in about dialog")
            if(aboutDialog != null && aboutDialog?.isShowing == true) aboutDialog?.dismiss() // defensive programming here
        }

        aboutDialog = builder.setView(messageView).create() // set inflated layout here
        aboutDialog?.setCanceledOnTouchOutside(true) // this is default behaviour, and our dialog has no buttons

        // we can also allow dismissing dialog by tapping on dialog itself, set click listener to dialog's view
//        messageView.setOnClickListener {
//            Log.d(TAG, "showAboutDialog: dismiss dialog on clicking view")
//            if(aboutDialog != null && aboutDialog?.isShowing == true) aboutDialog?.dismiss()
//            // * in case of this app -> icon & title were added by dialog.builder & are not part of the layout
//                // so clicking on them does not dismiss the dialog
//            // also, we have enabled autoLink on the textViews making them clickable
//        }
        val aboutVersion = messageView.findViewById(R.id.about_version) as TextView // to find the view inside inflated layout
        // ** using synthetic imports to refer to views in a dialog will compile the code, but wont run **
        aboutVersion.text = BuildConfig.VERSION_NAME
        aboutDialog?.show() // dismiss dialog to avoid mry leaks

        /*
        Info: Links in textViews are made clickable/browsable from API 21 onwards, API 17+ devices till API 21  will req changes:
        (connection, compatibility, security, attribute (like autoLink etc), emulator issues)
        1) separate the textViews: (the url text must be in a single separate textView)
        -> either duplicate the entire main layout xml and modify one for earlier API versions
        -> or create 2 layout variants holding only the textViews for  pre- and post-Api21 versions
                & <include> the file(of same name) in the main layout xml
        2) for the textView containing the url - use a nullable textView (it wont exist on API 21+) and set a click listener.
            launch url on browser via intent: put code just before aboutDialog?.show()
        3) url requires a scheme to be launched on earlier vers (http:// or https://) - raises ActivityNotFound exception:
            modify the string res & add try/catch block (always enclose url string resources within "speech marks")
            (on 21+ vers -> autoLink recognizes web addresses & inserts schema)
           ex: val aboutUrl: TextView? = messageView.findViewById(R.id.about_url)
           aboutUrl?.setOnClickListener { v -> // wont be set for API 21+ devices
                val intent = Intent(Intent.ACTION_VIEW)
                val s = (v as TextView).text.toString
                intent.data = Uri.parse(s)
                try {
                    startActivity(intent)
                } catch(...) {
                    Toast...("No browser application found, cannot visit world-wide-web"...)    }
                }
         */
    }

    override fun onPositiveDialogResult(dialogId: Int, args: Bundle) {
        // POS btn -> abandon_changes selected
        Log.i(TAG, "onPositiveDialogResult called with dialog ID $dialogId")
        if(dialogId == DIALOG_ID_CANCEL_ADD_EDIT) {
            // find frag using extension fn
            val fragment = findFragmentById(R.id.task_details_container)
            removeAddEditPane(fragment)
        }
        // nothing happens on clicking NEG btn -> dialog is dismissed & AddEditFrag remains visible
    }

    private fun taskEditRequest(task: Task?) { // null is passed when adding a new task
        Log.d(TAG, "taskEditRequest called")
        // create fragment to edit task
//        val newFragment = AddEditFragment.newInstance(task)
//        supportFragmentManager.beginTransaction()
//            .replace(R.id.task_details_container, newFragment)
//            .commit()
        // replace above code with extension fn to replace frag
        replaceFragment(AddEditFragment.newInstance(task), R.id.task_details_container)
        showAddEditPane() // swaps frag & shows panes
    }

    override fun onTaskEdit(task: Task) {
        Log.i(TAG, "onTaskEdit called")
        taskEditRequest(task)
    }

    override fun onSaveClicked() {
        Log.i(TAG, "onSaveClicked called")
        // using extension fn
        removeAddEditPane(findFragmentById(R.id.task_details_container))
    }

    override fun onStop() {
        Log.i(TAG, "onStop called")
        super.onStop()
        if(aboutDialog?.isShowing == true) {
            aboutDialog?.dismiss()
        }
    }

    override fun onBackPressed() {
        Log.i(TAG, "onBackPressed called")
        // using extension fn
        val fragment = findFragmentById(R.id.task_details_container)
        // here, if you want confirmation dialog to appear for landscape as well, remove the check for if(twoPane?)
        if(fragment == null || twoPane) { // if frag not visible or if landscape -> exit app on back
            super.onBackPressed()
        } else { // if portrait -> swap out fragment
//            removeAddEditPane(fragment)
        // add exit confirmation dialog code here
            if(fragment is AddEditFragment) { // check before casting to type
                if((fragment as AddEditFragment).isDataPresent()) {
                    showConfirmationDialog(DIALOG_ID_CANCEL_ADD_EDIT,
                        getString(R.string.dialog_confirm_exit_message),
                        R.string.dialog_abandon_changes_btn,
                        R.string.dialog_cont_editing)
                }
            } else {
                removeAddEditPane(fragment)
            }
        } // TODO: add quit confirmation dialog as well
        // dont use dialogs in lifecycle fns as it puts the app on hold till user dismisses the dialog, +bad UX
    }

//    override fun onSupportNavigateUp(): Boolean {
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
//    }

}