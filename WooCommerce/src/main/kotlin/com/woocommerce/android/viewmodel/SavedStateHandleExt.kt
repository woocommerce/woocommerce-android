package com.woocommerce.android.viewmodel

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.MainThread
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavArgs
import androidx.navigation.NavArgsLazy
import java.io.Serializable

/**
 * The current implementation of `fromSavedStateHandle` can't restore Parcelable[] properly
 * So we'll keep using the old implementation temporarily.
 * TODO go back to using `fromSavedStateHandle` when the issue is fixed https://issuetracker.google.com/issues/207315994
 */
@MainThread
inline fun <reified Args : NavArgs> SavedStateHandle.navArgs() = NavArgsLazy(Args::class) {
    val bundle = Bundle()
    keys().forEach {
        val value = get<Any>(it)
        if (value is Serializable?) {
            bundle.putSerializable(it, value)
        } else if (value is Parcelable?) {
            bundle.putParcelable(it, value)
        }
    }
    bundle
}
