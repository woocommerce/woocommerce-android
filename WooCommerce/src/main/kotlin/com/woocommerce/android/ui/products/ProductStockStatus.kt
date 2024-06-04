package com.woocommerce.android.ui.products

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStockStatus

sealed class ProductStockStatus(@StringRes val stringResource: Int = 0, val value: String = "") : Parcelable {
    @Parcelize
    data object InStock : ProductStockStatus(R.string.product_stock_status_instock)

    @Parcelize
    data object LowStock : ProductStockStatus(R.string.product_stock_status_low_stock)

    @Parcelize
    data object OutOfStock : ProductStockStatus(R.string.product_stock_status_out_of_stock)

    @Parcelize
    data object OnBackorder : ProductStockStatus(R.string.product_stock_status_on_backorder)

    @Parcelize
    data object InsufficientStock : ProductStockStatus(R.string.product_stock_status_insufficient_stock)

    @Parcelize
    data object NotAvailable : ProductStockStatus()
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
                "lowstock" -> LowStock
                "outofstock" -> OutOfStock
                "onbackorder" -> OnBackorder
                "insufficientstock" -> InsufficientStock
                null, "" -> NotAvailable
                else -> Custom(value)
            }
        }

        fun fromStockStatus(stockStatus: ProductStockStatus): String {
            return when (stockStatus) {
                LowStock -> CoreProductStockStatus.LOW_STOCK.value
                OnBackorder -> CoreProductStockStatus.ON_BACK_ORDER.value
                OutOfStock -> CoreProductStockStatus.OUT_OF_STOCK.value
                else -> CoreProductStockStatus.IN_STOCK.value
            }
        }

        fun ProductStockStatus.toCoreProductStockStatus(): CoreProductStockStatus {
            return when (this) {
                InStock -> CoreProductStockStatus.IN_STOCK
                LowStock -> CoreProductStockStatus.LOW_STOCK
                OutOfStock -> CoreProductStockStatus.OUT_OF_STOCK
                OnBackorder -> CoreProductStockStatus.ON_BACK_ORDER
                else -> CoreProductStockStatus.IN_STOCK
            }
        }

        fun toStringResource(value: String) = fromString(value).stringResource

        fun getMapForInventoryStockStatuses(context: Context) = CoreProductStockStatus.values()
            .associate { it.value to context.getString(fromString(it.value).stringResource) }
            .filterKeys { it != CoreProductStockStatus.LOW_STOCK.value }

        /**
         * returns the product's stock status formatted for display
         */
        fun stockStatusToDisplayString(context: Context, status: ProductStockStatus): String {
            return if (status.stringResource != 0) {
                context.getString(status.stringResource)
            } else {
                status.value
            }
        }

        fun stockStatusToDisplayString(resources: ResourceProvider, status: ProductStockStatus): String {
            return if (status.stringResource != 0) {
                resources.getString(status.stringResource)
            } else {
                status.value
            }
        }

        @JvmField
        val CREATOR = object : Creator<ProductStockStatus> {
            override fun createFromParcel(parcel: Parcel): ProductStockStatus {
                return fromString(parcel.readString())
            }

            override fun newArray(size: Int): Array<ProductStockStatus?> {
                return arrayOfNulls(size)
            }
        }
    }
}
