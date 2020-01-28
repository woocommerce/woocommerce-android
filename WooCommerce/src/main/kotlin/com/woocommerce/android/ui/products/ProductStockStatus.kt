package com.woocommerce.android.ui.products

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import androidx.annotation.StringRes
import com.woocommerce.android.R
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStockStatus

sealed class ProductStockStatus(@StringRes val stringResource: Int = 0, val value: String = "") : Parcelable {
    object InStock : ProductStockStatus(R.string.product_stock_status_instock)
    object OutOfStock : ProductStockStatus(R.string.product_stock_status_out_of_stock)
    object OnBackorder : ProductStockStatus(R.string.product_stock_status_on_backorder)
    object NotAvailable : ProductStockStatus()
    class Custom(value: String) : ProductStockStatus(value = value)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(value)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        fun fromString(value: String?): ProductStockStatus {
            return when (value) {
                "instock" -> InStock
                "outofstock" -> OutOfStock
                "onbackorder" -> OnBackorder
                null, "" -> NotAvailable
                else -> Custom(value)
            }
        }

        fun toStringResource(value: String) = fromString(value).stringResource

        fun toMap(context: Context) = CoreProductStockStatus.values()
                .map { it.value to context.getString(fromString(it.value).stringResource) }
                .toMap()

        @JvmField
        val CREATOR = object : Creator<ProductStockStatus > {
            override fun createFromParcel(parcel: Parcel): ProductStockStatus {
                return fromString(parcel.readString())
            }

            override fun newArray(size: Int): Array<ProductStockStatus?> {
                return arrayOfNulls(size)
            }
        }
    }
}
