package com.woocommerce.android.cardreader.internal.config

import com.stripe.stripeterminal.external.models.PaymentMethodType
import com.woocommerce.android.cardreader.connection.SpecificReader
import kotlinx.parcelize.Parcelize

@Parcelize
object CardReaderConfigForUSA : CardReaderConfigForSupportedCountry(
    currency = "USD",
    countryCode = "US",
    supportedReaders = listOf(SpecificReader.Chipper2X, SpecificReader.StripeM2),
    paymentMethodType = listOf(PaymentMethodType.CARD_PRESENT),
    supportedExtensions = listOf(
        SupportedExtension(
            type = SupportedExtensionType.STRIPE,
            supportedSince = "6.2.0"
        ),
        SupportedExtension(
            type = SupportedExtensionType.WC_PAY,
            supportedSince = "3.2.1"
        ),
    ),
)
