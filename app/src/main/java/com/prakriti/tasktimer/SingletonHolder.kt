package com.prakriti.tasktimer

import android.util.Log

/*
 * Created by Christophe Beyls
 * from https://bladecoder.medium.com/kotlin-singletons-with-argument-194ef06edd9e
 */

private const val TAG = "SingletonHolder"

open class SingletonHolder<out T: Any, in A>(creator: (A) -> T) {
    // usable for any class to make it singleton

    private var creator: ((A) -> T)? = creator
    @Volatile private var instance: T? = null

    fun getInstance(arg: A): T {
        Log.d(TAG, "getInstance called")

        val i = instance
        if (i != null) {
            return i
        }

        return synchronized(this) {
            val i2 = instance
            if (i2 != null) {
                i2
            } else {
                val created = creator!!(arg)
                instance = created
                creator = null
                created
            }
        }
    }
}