package com.woocommerce.android.ui.prefs.domain

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@SuppressLint("ParcelCreator")
data class DomainProductDetails(
    val domainName: String,
    val productId: Int
) : Parcelable
