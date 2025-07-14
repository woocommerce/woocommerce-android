package com.woocommerce.android.ui.orders.wooshippinglabels.models

import android.os.Parcelable
import com.woocommerce.android.model.Address
import kotlinx.parcelize.Parcelize

@Parcelize
data class DestinationShippingAddress(
    val address: Address,
    val isVerified: Boolean
) : Parcelable {
    companion object {
        val EMPTY = DestinationShippingAddress(Address.EMPTY, false)
    }
}
