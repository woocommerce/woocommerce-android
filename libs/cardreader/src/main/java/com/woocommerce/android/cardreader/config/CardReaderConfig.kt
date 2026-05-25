package com.woocommerce.android.cardreader.config

import android.os.Parcelable
import com.woocommerce.android.cardreader.connection.ReaderType
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentMethodType
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

sealed class CardReaderConfig : Parcelable {
    abstract val isPosCardPaymentEnabled: Boolean
}

@Suppress("LongParameterList")
sealed class CardReaderConfigForSupportedCountry(
    val currency: String,
    val countryCode: String,
    val supportedReaders: List<ReaderType>,
    val paymentMethodTypes: List<PaymentMethodType>,
    val supportedExtensions: List<SupportedExtension>,
    val minimumAllowedChargeAmount: BigDecimal,
    val maximumTTPAllowedChargeAmountWithoutPin: BigDecimal?,
) : CardReaderConfig() {
    // Remove the CA exclusion once Interac is supported in POS. CA is a fully-supported
    // IPP country (external reader + TTP) for the rest of the app, but POS card payments
    // are blocked here until Interac lands.
    override val isPosCardPaymentEnabled: Boolean
        get() = countryCode != POS_DISABLED_COUNTRY_CA

    private companion object {
        const val POS_DISABLED_COUNTRY_CA = "CA"
    }
}

fun CardReaderConfigForSupportedCountry.isExtensionSupported(type: SupportedExtensionType) =
    supportedExtensions.any { it.type == type }

fun CardReaderConfigForSupportedCountry.minSupportedVersionForExtension(type: SupportedExtensionType) =
    supportedExtensions.first { it.type == type }.supportedSince

@Parcelize
object CardReaderConfigForUnsupportedCountry : CardReaderConfig() {
    override val isPosCardPaymentEnabled: Boolean get() = false
}

data class SupportedExtension(
    val type: SupportedExtensionType,
    val supportedSince: String,
)

enum class SupportedExtensionType {
    WC_PAY, STRIPE
}
