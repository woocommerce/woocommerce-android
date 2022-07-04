package com.woocommerce.android.cardreader.internal.config

import com.stripe.stripeterminal.external.models.PaymentMethodType
import com.woocommerce.android.cardreader.connection.SpecificReader

object CardReaderConfigForCanada : CardReaderConfigForSupportedCountry(
    currency = "CAD",
    countryCode = "CA",
    supportedReaders = listOf(SpecificReader.WisePade3),
    paymentMethodType = listOf(
        PaymentMethodType.CARD_PRESENT,
        PaymentMethodType.INTERAC_PRESENT
    ),
    supportedExtensions = listOf(
        SupportedExtension(
            type = SupportedExtensionType.WC_PAY,
            supportedSince = "4.0.0"
        ),
    ),
)
