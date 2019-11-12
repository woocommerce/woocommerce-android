package com.woocommerce.android.viewmodel

import android.os.Bundle
import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavArgs
import androidx.navigation.NavArgsLazy
import java.io.Serializable

/**
 *  A wrapper for the [SavedStateHandle], which takes the arguments [Bundle] and provides a delegate for type-safe
 *  navigation arguments object. The arguments are supplied by the DI, usually coming from Fragment.arguments.
 */
class SavedState(private val savedState: SavedStateHandle, val arguments: Bundle?) {
    init {
        // there's a specific case, when the app is destroyed and the original arguments are lost;
        // the SaveStateHandle would contain the the preserved values, which must be restored
        if (arguments != null) {
            savedState.keys().forEach {
                val value = savedState.get<Any>(it)
                if (!arguments.containsKey(it) && value is Serializable) {
                    arguments.putSerializable(it, savedState.get(it))
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
    inline fun <reified Args : NavArgs> navArgs() = NavArgsLazy(Args::class) {
        arguments ?: throw IllegalStateException("$this has null arguments")
    }
}
