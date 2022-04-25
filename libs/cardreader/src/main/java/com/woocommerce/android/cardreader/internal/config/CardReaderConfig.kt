package com.woocommerce.android.cardreader.internal.config

import com.stripe.stripeterminal.external.models.PaymentMethodType
import com.woocommerce.android.cardreader.connection.SpecificReader

sealed class CardReaderConfig

@Suppress("LongParameterList")
sealed class CardReaderConfigForSupportedCountry(
    val currency: String,
    val countryCode: String,
    val supportedReaders: List<SpecificReader>,
    val paymentMethodType: List<PaymentMethodType>,
    val supportedExtensions: List<SupportedExtension>,
) : CardReaderConfig()

fun CardReaderConfigForSupportedCountry.isExtensionSupported(type: SupportedExtensionType) =
    supportedExtensions.any { it.type == type }

fun CardReaderConfigForSupportedCountry.minSupportedVersionForExtension(type: SupportedExtensionType) =
    supportedExtensions.first { it.type == type }.supportedSince

object CardReaderConfigForUnsupportedCountry : CardReaderConfig()

data class SupportedExtension(
    val type: SupportedExtensionType,
    val supportedSince: String,
)

enum class SupportedExtensionType {
    WC_PAY, STRIPE
}
