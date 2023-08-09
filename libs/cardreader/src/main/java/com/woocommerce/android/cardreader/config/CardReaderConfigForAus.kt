package com.woocommerce.android.cardreader.config

import com.woocommerce.android.cardreader.connection.ReaderType
import com.woocommerce.android.cardreader.payments.CardPaymentStatus
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
object CardReaderConfigForAus : CardReaderConfigForSupportedCountry(
    currency = "AUD",
    countryCode = "AU",
    supportedReaders = listOf(ReaderType.ExternalReader.WisePade3),
    paymentMethodTypes = listOf(
        CardPaymentStatus.PaymentMethodType.CARD_PRESENT,
        // TODO EFTPOS?
    ),
    supportedExtensions = listOf(
        SupportedExtension(
            type = SupportedExtensionType.WC_PAY,
            supportedSince = "6.2.2" // TODO correct version
        ),
    ),
    minimumAllowedChargeAmount = BigDecimal("0.50")
)
