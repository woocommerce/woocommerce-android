package com.woocommerce.android.ui.payments.cardreader.onboarding

import com.woocommerce.android.AppPrefs

data class PersistentOnboardingData(
    val status: AppPrefs.CardReaderOnboardingStatus,
    val preferredPlugin: PluginType?,
    val version: String?,
)

enum class PluginType(val pluginName: String) {
    WOOCOMMERCE_PAYMENTS("woocommerce-payments"),
    STRIPE_EXTENSION_GATEWAY("woocommerce-gateway-stripe")
}
