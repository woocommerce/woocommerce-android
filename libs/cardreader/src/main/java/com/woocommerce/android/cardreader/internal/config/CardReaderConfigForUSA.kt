package com.woocommerce.android.cardreader.internal.config

import com.stripe.stripeterminal.external.models.PaymentMethodType
import com.woocommerce.android.cardreader.connection.SpecificReader

object CardReaderConfigForUSA : CardReaderConfigForSupportedCountry(
    currency = "USD",
    countryCode = "US",
    supportedReaders = listOf(SpecificReader.Chipper2X, SpecificReader.StripeM2),
    paymentMethodType = listOf(PaymentMethodType.CARD_PRESENT),
    isStripeExtensionSupported = true,
    minimumSupportedVersionWCPay = "3.2.1",
    minimumSupportedVersionStripeExtension = "6.2.0"
)
