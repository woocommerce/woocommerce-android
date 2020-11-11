package com.woocommerce.android.extensions

import android.view.View

/**
 * Extension function to verify if there's at least one instance of a given type inside a Sequence.
 *
 * Instead of trying to verify if there's an exact copy of a given element inside the sequence,
 * this function create an alternative for the Sequence<T>.contains function checking only the instance type.
 */
inline fun <reified T> Sequence<View>.containsInstanceOf(element: T) =
    filterIsInstance(T::class.java)
        .toList()
        .isNotEmpty()

inline fun <E : Any, T : Collection<E>> T?.whenNotNullNorEmpty(func: (T) -> Unit): Otherwise {
    return if (this != null && this.isNotEmpty()) {
        func(this)
        OtherwiseIgnore
    } else {
        OtherwiseInvoke
    }
}

interface Otherwise {
    fun otherwise(func: () -> Unit)
}

object OtherwiseInvoke : Otherwise {
    override fun otherwise(func: () -> Unit) { func() }
}

object OtherwiseIgnore : Otherwise {
    override fun otherwise(func: () -> Unit) { }
}
