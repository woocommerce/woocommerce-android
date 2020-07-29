package com.woocommerce.android.viewmodel

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavArgs
import java.io.Serializable

/**
 *  A wrapper for the [SavedStateHandle], which takes the arguments [Bundle] and provides a delegate for type-safe
 *  navigation arguments object. The arguments are supplied by the DI, usually coming from Fragment.arguments.
 *
 *  The advantage of mixing the the arguments with the saved state is that they are automatically preserved
 *  in the [SavedStateHandle].
 *
 *  [defaultArgs] are used to supply arguments for testing
 */
open class SavedStateWithArgs(
    private val savedState: SavedStateHandle,
    val arguments: Bundle?,
    val defaultArgs: NavArgs? = null
) {
    init {
        // there's a specific case, when the app is destroyed and the original arguments are lost;
        // the SaveStateHandle would contain the the preserved values, which must be restored
        if (arguments != null) {
            savedState.keys().forEach {
                val value = savedState.get<Any>(it)
                if (!arguments.containsKey(it)) {
                    if (value is Serializable) {
                        arguments.putSerializable(it, value)
                    } else if (value is Parcelable) {
                        arguments.putParcelable(it, value)
                    }
                }
            }
        }
    }

    fun <T> getLiveData(key: String, initialValue: T? = null): MutableLiveData<T> =
            savedState.getLiveData(key, initialValue)

    operator fun <T> get(key: String): T? = savedState.get(key)

    operator fun <T> set(key: String, value: T) = savedState.set(key, value)

    fun contains(key: String): Boolean = savedState.contains(key)

    @MainThread
    inline fun <reified Args : NavArgs> navArgs() = NavArgsWithDefaultLazy(Args::class, defaultArgs) {
        arguments ?: throw IllegalStateException("$this has null arguments")
    }
}
