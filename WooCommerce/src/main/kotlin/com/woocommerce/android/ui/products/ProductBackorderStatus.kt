package com.woocommerce.android.ui.products

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductBackOrders

sealed class ProductBackorderStatus(@StringRes val stringResource: Int = 0, val value: String = "") : Parcelable {
    @Parcelize object No : ProductBackorderStatus(R.string.product_backorders_no)
    @Parcelize object Yes : ProductBackorderStatus(R.string.product_backorders_yes)
    @Parcelize object Notify : ProductBackorderStatus(R.string.product_backorders_notify)
    @Parcelize object NotAvailable : ProductBackorderStatus()
    class Custom(value: String) : ProductBackorderStatus(value = value)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(value)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        fun fromString(value: String?): ProductBackorderStatus {
            return when (value) {
                "no" -> No
                "yes" -> Yes
                "notify" -> Notify
                null, "" -> NotAvailable
                else -> Custom(value)
            }
        }

        fun fromBackorderStatus(backorderStatus: ProductBackorderStatus): String {
            return when (backorderStatus) {
                Yes -> CoreProductBackOrders.YES.value
                Notify -> CoreProductBackOrders.NOTIFY.value
                else -> CoreProductBackOrders.NO.value
            }
        }

        fun toStringResource(value: String) = fromString(value).stringResource

        fun toMap(context: Context) = CoreProductBackOrders.values()
                .map { it.value to context.getString(fromString(it.value).stringResource)
                }.toMap()

        /**
         * returns the product's backorder status formatted for display
         */
        fun backordersToDisplayString(context: Context, backorderStatus: ProductBackorderStatus): String {
            return if (backorderStatus.stringResource != 0) {
                context.getString(backorderStatus.stringResource)
            } else {
                backorderStatus.value
            }
        }

        fun backordersToDisplayString(resources: ResourceProvider, backorderStatus: ProductBackorderStatus): String {
            return if (backorderStatus.stringResource != 0) {
                resources.getString(backorderStatus.stringResource)
            } else {
                backorderStatus.value
            }
        }

        @JvmField
        val CREATOR = object : Creator<ProductBackorderStatus> {
            override fun createFromParcel(parcel: Parcel): ProductBackorderStatus {
                return fromString(parcel.readString())
            }

            override fun newArray(size: Int): Array<ProductBackorderStatus?> {
                return arrayOfNulls(size)
            }
        }
    }
}
