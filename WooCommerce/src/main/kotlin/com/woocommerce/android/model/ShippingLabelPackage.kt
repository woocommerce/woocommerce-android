package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class ShippingLabelPackage(
    val position: Int,
    val selectedPackage: ShippingPackage?,
    val weight: Float,
    val items: List<Item>
) : Parcelable {
    @IgnoredOnParcel
    val packageId: String
        get() = "package$position"

    @IgnoredOnParcel
    val title: UiString
        get() = UiStringRes(
            R.string.shipping_label_package_details_title_template,
            listOf(UiStringText(position.toString()))
        )

    @Parcelize
    data class Item(
        val productId: Long,
        val name: String,
        val attributesList: String,
        val quantity: Int,
        val weight: Float,
        val value: BigDecimal
    ) : Parcelable
}
