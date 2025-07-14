package com.woocommerce.android.extensions

import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

inline fun <reified T> Parcel.parcelable(loader: ClassLoader?): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> readParcelable(loader, T::class.java)
    else ->
        @Suppress("DEPRECATION")
        readParcelable(loader)
}

inline fun <reified T> Bundle.parcelable(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelable(key, T::class.java)
    else ->
        @Suppress("DEPRECATION")
        getParcelable(key)
}

inline fun <reified T : Parcelable> Bundle.parcelableArrayList(key: String): ArrayList<T>? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelableArrayList(key, T::class.java)
    else ->
        @Suppress("DEPRECATION")
        getParcelableArrayList(key)
}

inline fun <reified T : Serializable> Bundle.serializable(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getSerializable(key, T::class.java)
    else ->
        @Suppress("DEPRECATION")
        getSerializable(key)
            as? T
}
