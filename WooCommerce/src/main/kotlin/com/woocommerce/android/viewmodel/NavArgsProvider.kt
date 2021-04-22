package com.woocommerce.android.viewmodel

import android.os.Bundle
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavArgs
import androidx.navigation.NavArgsLazy
import com.woocommerce.android.annotations.OpenClassOnDebug
import java.io.Serializable
import javax.inject.Inject
import kotlin.reflect.KClass

@OpenClassOnDebug
class NavArgsProvider @Inject constructor() {
    fun <Args : NavArgs> navArgs(savedStateHandle: SavedStateHandle, argsClass: KClass<Args>): Lazy<Args> {
        return NavArgsLazy(argsClass) {
            val bundle = Bundle()
            savedStateHandle.keys().forEach {
                val value = savedStateHandle.get<Any>(it)
                if (value is Serializable) {
                    bundle.putSerializable(it, value)
                } else if (value is Parcelable) {
                    bundle.putParcelable(it, value)
                }
            }
            bundle
        }
    }
}
