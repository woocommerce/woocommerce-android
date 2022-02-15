package com.woocommerce.android.cardreader.internal.config

import com.stripe.stripeterminal.external.models.PaymentMethodType
import com.woocommerce.android.cardreader.connection.SpecificReader

object CardReaderConfigForUnSupportedCountry : CardReaderConfig {
    override val currency: String
        get() = "NA"
    override val countryCode: String
        get() = "NA"
    override val supportedReaders: List<SpecificReader>
        get() = listOf()
    override val paymentMethodType: List<PaymentMethodType>
        get() = listOf()
    override val isStripeExtensionSupported: Boolean
        get() = false
}
