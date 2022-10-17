package com.woocommerce.android.viewmodel

import android.annotation.SuppressLint
import androidx.annotation.MainThread
import androidx.collection.ArrayMap
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavArgs
import java.lang.reflect.Method
import kotlin.reflect.KClass

@MainThread
inline fun <reified Args : NavArgs> SavedStateHandle.navArgs() = NavArgsLazy(Args::class, this)

/** cache the methods for [NavArgsLazy] to avoid depending on reflection for all invocations **/
// TODO investigate the usage of [ConcurrentHashMap] to avoid getting ConcurrentModificationException when accessing
// navigation arguments from multiple threads
private val methodMap = ArrayMap<KClass<out NavArgs>, Method>()

/**
 * An implementation of [Lazy] to retrieve [NavArgs] from the provided [savedStateHandle]
 *
 * The implementation is copied from [androidx.navigation.NavArgsLazy]
 */
class NavArgsLazy<Args : NavArgs>(
    private val navArgsClass: KClass<Args>,
    private val savedStateHandle: SavedStateHandle
) : Lazy<Args> {
    private var cached: Args? = null

    override val value: Args
        get() {
            var args = cached
            if (args == null) {
                val method: Method = methodMap[navArgsClass]
                    ?: navArgsClass.java.getMethod("fromSavedStateHandle", SavedStateHandle::class.java)
                        .also { method ->
                            // Save a reference to the method
                            methodMap[navArgsClass] = method
                        }

                @SuppressLint("BanUncheckedReflection") // needed for method.invoke
                @Suppress("UNCHECKED_CAST")
                args = method.invoke(null, savedStateHandle) as Args
                cached = args
            }
            return args
        }

    override fun isInitialized(): Boolean = cached != null
}
