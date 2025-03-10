package com.woocommerce.android.ui.orders.wooshippinglabels.customs

import android.os.Parcelable
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.ContentType
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.WooShippingCustomsFormViewModel.RestrictionType
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class CustomsData(
    val packageId: String,
    val packageName: String,
    val contentType: ContentType,
    val contentDescription: String,
    val restrictionType: RestrictionType,
    val restrictionDescription: String,
    val noDeliveryOption: Boolean,
    val itn: String,
    val items: List<CustomsItem>
) : Parcelable

@Parcelize
data class CustomsItem(
    val productID: Long,
    val description: String,
    val quantity: Float,
    val value: BigDecimal,
    val weight: Float,
    val hsTariffNumber: String,
    val originCountry: String
) : Parcelable
