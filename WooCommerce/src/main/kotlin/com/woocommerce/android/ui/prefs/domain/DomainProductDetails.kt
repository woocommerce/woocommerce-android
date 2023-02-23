package com.woocommerce.android.ui.prefs.domain

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@SuppressLint("ParcelCreator")
data class DomainProductDetails(
    val productId: Int,
    val domainName: String
) : Parcelable
