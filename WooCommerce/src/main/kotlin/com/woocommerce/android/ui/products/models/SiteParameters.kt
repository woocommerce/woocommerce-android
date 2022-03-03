package com.woocommerce.android.ui.products.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition

@Parcelize
data class SiteParameters(
    val currencyCode: String?,
    val currencySymbol: String?,
    val currencyFormattingParameters: CurrencyFormattingParameters?,
    val weightUnit: String?,
    val dimensionUnit: String?,
    val gmtOffset: Float
) : Parcelable

@Parcelize
data class CurrencyFormattingParameters(
    val currencyThousandSeparator: String,
    val currencyDecimalSeparator: String,
    val currencyDecimalNumber: Int,
    val currencyPosition: CurrencyPosition
) : Parcelable
