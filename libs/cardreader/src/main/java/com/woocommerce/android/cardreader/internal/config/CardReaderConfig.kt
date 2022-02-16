package com.woocommerce.android.cardreader.internal.config

import com.stripe.stripeterminal.external.models.PaymentMethodType
import com.woocommerce.android.cardreader.connection.SpecificReader

sealed class CardReaderConfig

sealed class CardReaderConfigSupportedCountry(
    val currency: String,
    val countryCode: String,
    val supportedReaders: List<SpecificReader>,
    val paymentMethodType: List<PaymentMethodType>,
    val isStripeExtensionSupported: Boolean,
) : CardReaderConfig()

object CardReaderConfigUnSupportedCountry : CardReaderConfig()
