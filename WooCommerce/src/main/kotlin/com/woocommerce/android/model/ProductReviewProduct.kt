package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ProductReviewProduct(val remoteProductId: Long, val name: String, val externalUrl: String) : Parcelable
