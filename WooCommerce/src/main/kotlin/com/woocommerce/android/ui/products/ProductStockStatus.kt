package com.woocommerce.android.ui.products

import androidx.annotation.StringRes
import com.woocommerce.android.R

sealed class ProductStockStatus(@StringRes val stringResource: Int = 0, val value: String = "") {
    object InStock : ProductStockStatus(R.string.product_stock_status_instock)
    object OutOfStock : ProductStockStatus(R.string.product_stock_status_out_of_stock)
    object OnBackorder : ProductStockStatus(R.string.product_stock_status_on_backorder)
    object NotAvailable : ProductStockStatus()
    class Custom(value: String) : ProductStockStatus(value = value)

    companion object {
        fun fromString(value: String?): ProductStockStatus {
            return when (value) {
                "instock" -> InStock
                "outofstock" -> OutOfStock
                "onbackorder" -> OnBackorder
                null -> NotAvailable
                else -> Custom(value)
            }
        }
    }
}
