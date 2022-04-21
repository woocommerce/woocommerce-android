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
    val isStripeExtensionSupported: Boolean,
    val minimumSupportedVersionWCPay: String,
    val minimumSupportedVersionStripeExtension: String
) : CardReaderConfig()

object CardReaderConfigForUnsupportedCountry : CardReaderConfig()
