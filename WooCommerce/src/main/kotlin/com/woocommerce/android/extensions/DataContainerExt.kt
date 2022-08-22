package com.woocommerce.android.extensions

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.os.Parcel
import java.io.Serializable

inline fun <reified T> Parcel.parcelable(loader: ClassLoader?): T? = when {
    SDK_INT >= TIRAMISU -> readParcelable(loader, T::class.java)
    else -> @Suppress("DEPRECATION") readParcelable(loader) as? T
}

inline fun <reified T> Bundle.parcelable(key: String): T? = when {
    SDK_INT >= TIRAMISU -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

inline fun <reified T : Serializable> Bundle.serializable(key: String): T? = when {
    SDK_INT >= TIRAMISU -> getSerializable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getSerializable(key) as? T
}
