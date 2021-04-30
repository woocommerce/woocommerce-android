package com.woocommerce.android.ui.products.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition

@Parcelize
data class SiteParameters(
    val currencyCode: String?,
    val currencySymbol: String?,
    val currencyPosition: CurrencyPosition?,
    val weightUnit: String?,
    val dimensionUnit: String?,
    val gmtOffset: Float
) : Parcelable
