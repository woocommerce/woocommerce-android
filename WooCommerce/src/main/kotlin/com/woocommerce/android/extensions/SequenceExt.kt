package com.woocommerce.android.extensions

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
