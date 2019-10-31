package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.annotation.StringRes
import com.woocommerce.android.R
import kotlinx.android.parcel.Parcelize

@Parcelize
open class ProductBackorderStatus(@StringRes val stringResource: Int = 0, val value: String = "") : Parcelable {
    object No : ProductBackorderStatus(R.string.product_backorders_no)
    object Yes : ProductBackorderStatus(R.string.product_backorders_yes)
    object Notify : ProductBackorderStatus(R.string.product_backorders_notify)
    object NotAvailable : ProductBackorderStatus()
    class Custom(value: String) : ProductBackorderStatus(value = value)

    companion object {
        fun fromString(value: String?): ProductBackorderStatus {
            return when (value) {
                "no" -> No
                "yes" -> Yes
                "notify" -> Notify
                null -> NotAvailable
                else -> Custom(value)
            }
        }
    }
}
