package com.woocommerce.android.extensions

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.os.Parcel

inline fun <reified T> Parcel.parcelable(loader: ClassLoader?): T? = when {
    SDK_INT >= TIRAMISU -> readParcelable(loader, T::class.java)
    else -> @Suppress("DEPRECATION") readParcelable(loader) as? T
}

inline fun <reified T> Bundle.parcelable(key: String): T? = when {
    SDK_INT >= TIRAMISU -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}
