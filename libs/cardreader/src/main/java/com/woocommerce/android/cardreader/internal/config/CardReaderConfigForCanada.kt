package com.woocommerce.android.cardreader.internal.config

import com.stripe.stripeterminal.external.models.PaymentMethodType
import com.woocommerce.android.cardreader.connection.SpecificReader

object CardReaderConfigForCanada : CardReaderConfig {
    override val currency: String
        get() = "CAD"
    override val countryCode: String
        get() = "CA"
    override val supportedReaders: List<SpecificReader>
        get() = listOf(
            SpecificReader.WisePade3
        )
    override val paymentMethodType: List<PaymentMethodType>
        get() = listOf(
            PaymentMethodType.CARD_PRESENT,
            PaymentMethodType.INTERAC_PRESENT
        )
    override val isStripeExtensionSupported: Boolean
        get() = false
}
