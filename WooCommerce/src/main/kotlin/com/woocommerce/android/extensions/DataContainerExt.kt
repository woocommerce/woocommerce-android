package com.woocommerce.android.extensions

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.BundleCompat
import androidx.core.os.ParcelCompat
import java.io.Serializable

// These delegate to AndroidX compat helpers on purpose: the Android 13 (API 33) typed
// getParcelable/readParcelable overloads are bugged (b/232589966) and crash with a NullPointerException
// in Class.isAssignableFrom during lazy unmarshalling. BundleCompat/ParcelCompat only use the typed API
// on API 34+ and fall back to the safe deprecated path on 33.

inline fun <reified T : Parcelable> Parcel.parcelable(loader: ClassLoader?): T? =
    ParcelCompat.readParcelable(this, loader, T::class.java)

inline fun <reified T> Bundle.parcelable(key: String): T? =
    BundleCompat.getParcelable(this, key, T::class.java)

inline fun <reified T : Parcelable> Bundle.parcelableArrayList(key: String): ArrayList<T>? =
    BundleCompat.getParcelableArrayList(this, key, T::class.java)

inline fun <reified T : Serializable> Bundle.serializable(key: String): T? =
    BundleCompat.getSerializable(this, key, T::class.java)
