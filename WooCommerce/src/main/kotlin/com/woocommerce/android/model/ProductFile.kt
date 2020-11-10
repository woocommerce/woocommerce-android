package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ProductFile(val id: String?, val name: String, val url: String) : Parcelable
