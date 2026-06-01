package com.woocommerce.android.cardreader.config

import com.woocommerce.android.cardreader.connection.ReaderType
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentMethodType
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
object CardReaderConfigForAustralia : CardReaderConfigForSupportedCountry(
    currency = "AUD",
    countryCode = "AU",
    supportedReaders = listOf(
        ReaderType.ExternalReader.WisePade3,
    ),
    paymentMethodTypes = listOf(PaymentMethodType.CARD_PRESENT),
    supportedExtensions = listOf(
        SupportedExtension(
            type = SupportedExtensionType.WC_PAY,
            supportedSince = "10.8.0-test-1"
        ),
    ),
    minimumAllowedChargeAmount = BigDecimal("0.50"),
    maximumTTPAllowedChargeAmountWithoutPin = null,
)
