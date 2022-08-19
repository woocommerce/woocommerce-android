package com.woocommerce.android.extensions

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Parcel

inline fun <reified T> Parcel.parcelable(loader: ClassLoader?): T? = when {
    SDK_INT >= TIRAMISU -> readParcelable(loader, T::class.java)
    else -> @Suppress("DEPRECATION") readParcelable(loader) as? T
}
