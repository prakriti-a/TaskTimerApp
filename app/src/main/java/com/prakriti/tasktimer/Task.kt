package com.prakriti.tasktimer

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data class used to hold data, with fns to handle data -> compare, copy, de-structure etc
 * Data classes may have high overheads
 */
// *** check stackOverFlow for more on Parcelize implementation ***
@Parcelize
data class Task(val name: String, val description: String, val sortOrder: Int, var id: Long = 0): Parcelable {
    // instances of this class are passed in bundles, so it must be Serializable or Parcelable
    // properties are val so as to not be edited, id is unknown until new row is created so it is var

//    @IgnoredOnParcel
//    var id: Long = 0
    // not included in constructor, as id for data entered by user cannot be validated against id present in db
    // we need id to be serialized, so move into primary constructor
    // can remove empty {}

}