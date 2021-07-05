package com.woocommerce.android.model

import android.content.Context
import android.os.Parcelable
import com.woocommerce.android.R
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

    @Parcelize
    data class Item(
        val productId: Long,
        val name: String,
        val attributesList: String,
        val quantity: Int,
        val weight: Float,
        val value: BigDecimal
    ) : Parcelable {
        fun isSameProduct(otherItem: Item): Boolean {
            return productId == otherItem.productId &&
                attributesList == otherItem.attributesList
        }
    }
}

fun ShippingLabelPackage.getTitle(context: Context) =
    context.getString(R.string.shipping_label_package_details_title_template, position)
