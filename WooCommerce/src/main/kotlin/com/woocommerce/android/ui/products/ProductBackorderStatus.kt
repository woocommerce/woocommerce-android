package com.woocommerce.android.ui.products

import androidx.annotation.StringRes
import com.woocommerce.android.R

sealed class ProductBackorderStatus(@StringRes val stringResource: Int = 0, val value: String = "") {
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
