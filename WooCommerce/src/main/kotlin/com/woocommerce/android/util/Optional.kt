package com.woocommerce.android.util

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

class Optional<T>(var value: T? = null) {
    val hasValue: Boolean
        get() = value != null
}

@Parcelize
class OptionalViewState<T : Parcelable>(val value: T? = null) : Parcelable
