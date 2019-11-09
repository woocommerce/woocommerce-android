package com.woocommerce.android.viewmodel

import android.os.Bundle
import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavArgs
import androidx.navigation.NavArgsLazy

class SavedState(private val savedState: SavedStateHandle, val defaultArgs: Bundle?) {
    fun <T> getLiveData(key: String, initialValue: T? = null): MutableLiveData<T>
            = savedState.getLiveData(key, initialValue)

    operator fun <T> get(key: String): T? = savedState.get(key)

    operator fun <T> set(key: String, value: T) = savedState.set(key, value)

    fun contains(key: String): Boolean = savedState.contains(key)

    @MainThread
    inline fun <reified Args : NavArgs> navArgs() = NavArgsLazy(Args::class) {
        defaultArgs ?: throw IllegalStateException("$this has null arguments")
    }
}
