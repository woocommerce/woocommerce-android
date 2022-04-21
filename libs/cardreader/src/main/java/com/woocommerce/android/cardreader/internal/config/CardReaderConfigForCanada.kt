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
    isStripeExtensionSupported = false,
    minimumSupportedVersionWCPay = "4.0.0",
    // Empty string is temporary here. As soon as support stripe extension in Canada,
    // we replace this with the actual version.
    minimumSupportedVersionStripeExtension = ""
)
