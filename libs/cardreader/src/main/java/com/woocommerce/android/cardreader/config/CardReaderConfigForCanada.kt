package com.woocommerce.android.cardreader.config

import com.woocommerce.android.cardreader.connection.SpecificReader
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentMethodType
import kotlinx.parcelize.Parcelize

@Parcelize
object CardReaderConfigForCanada : CardReaderConfigForSupportedCountry(
    currency = "CAD",
    countryCode = "CA",
    supportedReaders = listOf(SpecificReader.WisePade3),
    paymentMethodTypes = listOf(
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
