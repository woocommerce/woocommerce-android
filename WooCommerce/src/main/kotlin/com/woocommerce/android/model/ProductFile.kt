package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ProductFile(private val id: String?, private val name: String, private val url: String) : Parcelable
