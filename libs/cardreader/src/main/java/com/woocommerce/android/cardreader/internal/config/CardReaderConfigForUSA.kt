package com.woocommerce.android.cardreader.internal.config

import com.stripe.stripeterminal.external.models.PaymentMethodType
import com.woocommerce.android.cardreader.connection.SpecificReader

object CardReaderConfigForUSA : CardReaderConfig {
    override val currency: String
        get() = "USD"
    override val countryCode: String
        get() = "US"
    override val supportedReaders: List<SpecificReader>
        get() = listOf(
            SpecificReader.Chipper2X,
            SpecificReader.StripeM2,
        )
    override val paymentMethodType: List<PaymentMethodType>
        get() = listOf(
            PaymentMethodType.CARD_PRESENT
        )
    override val isStripeExtensionSupported: Boolean
        get() = true
}
