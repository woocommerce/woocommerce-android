package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProductFile(val id: String?, val name: String, val url: String) : Parcelable
