package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class WooPlugin(
    val isInstalled: Boolean,
    val isActive: Boolean,
    val version: String?
) : Parcelable {
    @IgnoredOnParcel
    val isOperational = isInstalled && isActive
}
