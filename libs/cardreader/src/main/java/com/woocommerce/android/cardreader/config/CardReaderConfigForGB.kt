package com.woocommerce.android.cardreader.config

import com.woocommerce.android.cardreader.connection.ReaderType
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentMethodType
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
object CardReaderConfigForGB : CardReaderConfigForSupportedCountry(
    currency = "GBP",
    countryCode = "GB",
    supportedReaders = listOf(ReaderType.ExternalReader.WisePade3),
    paymentMethodTypes = listOf(PaymentMethodType.CARD_PRESENT),
    supportedExtensions = listOf(
        SupportedExtension(
            type = SupportedExtensionType.WC_PAY,
            supportedSince = "4.4.0"
        ),
    ),
    minimumAllowedChargeAmount = BigDecimal("0.30")
)
