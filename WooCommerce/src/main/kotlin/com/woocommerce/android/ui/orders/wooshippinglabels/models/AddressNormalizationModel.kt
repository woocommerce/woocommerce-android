package com.woocommerce.android.ui.orders.wooshippinglabels.models

import com.woocommerce.android.model.Address

data class AddressNormalizationModel(
    val address: Address,
    val normalizedAddress: Address,
    val isTrivial: Boolean
)
