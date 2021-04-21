package com.woocommerce.android.ui.products

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import androidx.annotation.StringRes
import com.woocommerce.android.R
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductTaxStatus

sealed class ProductTaxStatus(@StringRes val stringResource: Int = 0, val value: String = "") : Parcelable {
    @Parcelize object Taxable : ProductTaxStatus(R.string.product_tax_status_taxable)
    @Parcelize object Shipping : ProductTaxStatus(R.string.product_tax_status_shipping)
    @Parcelize object None : ProductTaxStatus(R.string.product_tax_status_none)
    @Parcelize object NotAvailable : ProductTaxStatus()
    class Custom(value: String) : ProductTaxStatus(value = value)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(value)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        fun fromString(value: String?): ProductTaxStatus {
            return when (value) {
                "taxable" -> Taxable
                "shipping" -> Shipping
                "none" -> None
                null, "" -> NotAvailable
                else -> Custom(value)
            }
        }

        fun fromTaxStatus(taxStatus: ProductTaxStatus): String {
            return when (taxStatus) {
                Taxable -> CoreProductTaxStatus.TAXABLE.value
                Shipping -> CoreProductTaxStatus.SHIPPING.value
                else -> CoreProductTaxStatus.NONE.value
            }
        }

        fun toStringResource(value: String) = fromString(value).stringResource

        fun toMap(context: Context) = CoreProductTaxStatus.values()
                .map { it.value to context.getString(fromString(it.value).stringResource) }
                .toMap()

        fun taxStatusToDisplayString(context: Context, status: ProductTaxStatus): String {
            return if (status.stringResource != 0) {
                context.getString(status.stringResource)
            } else {
                status.value
            }
        }

        @JvmField
        val CREATOR = object : Creator<ProductTaxStatus> {
            override fun createFromParcel(parcel: Parcel): ProductTaxStatus {
                return fromString(parcel.readString())
            }

            override fun newArray(size: Int): Array<ProductTaxStatus?> {
                return arrayOfNulls(size)
            }
        }
    }
}
