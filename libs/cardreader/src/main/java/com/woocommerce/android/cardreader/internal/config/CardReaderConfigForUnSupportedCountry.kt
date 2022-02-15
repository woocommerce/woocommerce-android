package com.woocommerce.android.cardreader.internal.config

import com.stripe.stripeterminal.external.models.PaymentMethodType
import com.woocommerce.android.cardreader.connection.SpecificReader

object CardReaderConfigForUnSupportedCountry : CardReaderConfig {
    override val currency: String
        get() = "N/A"
    override val countryCode: String
        get() = "N/A"
    override val supportedReaders: List<SpecificReader>
        get() = emptyList()
    override val paymentMethodType: List<PaymentMethodType>
        get() = emptyList()
    override val isStripeExtensionSupported: Boolean
        get() = false
}
