package com.woocommerce.android.cardreader.internal.config

import com.stripe.stripeterminal.external.models.PaymentMethodType
import com.woocommerce.android.cardreader.connection.SpecificReader

interface CardReaderConfig {
    val currency: String
    val countryCode: String
    val supportedReaders: List<SpecificReader>
    val paymentMethodType: List<PaymentMethodType>
    val isStripeExtensionSupported: Boolean
    val allReaders: List<SpecificReader>
        get() = listOf(
            SpecificReader.Chipper2X,
            SpecificReader.StripeM2,
            SpecificReader.WisePade3
        )
}
