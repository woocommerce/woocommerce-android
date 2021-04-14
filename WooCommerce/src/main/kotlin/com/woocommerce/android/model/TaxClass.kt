package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.taxes.WCTaxClassModel

@Parcelize
data class TaxClass(
    val name: String = "",
    val slug: String = ""
) : Parcelable

fun WCTaxClassModel.toAppModel(): TaxClass {
    return TaxClass(
            name = this.name,
            slug = this.slug
    )
}
