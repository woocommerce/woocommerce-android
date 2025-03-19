package com.woocommerce.android.ui.woopos

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.util.IsRemoteFeatureFlagEnabled
import com.woocommerce.android.util.RemoteFeatureFlag.WOO_POS
import kotlinx.coroutines.coroutineScope
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosIsEnabled @Inject constructor(
    private val selectedSite: SelectedSite,
    private val isScreenSizeAllowed: WooPosIsScreenSizeAllowed,
    private val getWooCoreVersion: GetWooCorePluginCachedVersion,
    private val wooCommerceStore: WooCommerceStore,
    private val isRemoteFeatureFlagEnabled: IsRemoteFeatureFlagEnabled,
) {
    @Suppress("ReturnCount")
    suspend operator fun invoke(): Reason = coroutineScope {
        val selectedSite = selectedSite.getOrNull()
            ?: return@coroutineScope Reason.Disabled.InvalidSelectedSite

        if (!isRemoteFeatureFlagEnabled(WOO_POS)) return@coroutineScope Reason.Disabled.FeatureFlagDisabled
        if (!isScreenSizeAllowed()) return@coroutineScope Reason.Disabled.ScreenSizeNotAllowed
        if (!isWooCoreSupportsOrderAutoDraftsAndExtraPaymentsProps()) {
            return@coroutineScope Reason.Disabled.WooCoreVersionNotSupported
        }

        val siteSettings = wooCommerceStore.getSiteSettings(selectedSite)
            ?: return@coroutineScope Reason.Disabled.InvalidSiteSettings

        return@coroutineScope if (isCountryAndCurrencySupported(
                countryCode = siteSettings.countryCode,
                currency = siteSettings.currencyCode
            )
        ) {
            Reason.Enabled
        } else {
            Reason.Disabled.CountryCurrencyNotSupported(
                country = siteSettings.countryCode,
                currency = siteSettings.currencyCode
            )
        }
    }

    private fun isCountryAndCurrencySupported(countryCode: String, currency: String) =
        SUPPORTED_COUNTRY_CURRENCY_PAIRS.any { it.first.equals(countryCode, true) && it.second.equals(currency, true) }

    private fun isWooCoreSupportsOrderAutoDraftsAndExtraPaymentsProps(): Boolean {
        val wooCoreVersion = getWooCoreVersion() ?: return false
        return wooCoreVersion.semverCompareTo(WC_VERSION_SUPPORTS_POS_PRODUCT_FILTERING) >= 0
    }

    private companion object {
        const val WC_VERSION_SUPPORTS_POS_PRODUCT_FILTERING = "9.6.0"

        val SUPPORTED_COUNTRY_CURRENCY_PAIRS = listOf("us" to "usd", "gb" to "gbp")
    }

    sealed class Reason {
        data object Enabled : Reason()
        sealed class Disabled : Reason() {
            data object InvalidSelectedSite : Disabled()
            data object InvalidSiteSettings : Disabled()
            data object FeatureFlagDisabled : Disabled()
            data object ScreenSizeNotAllowed : Disabled()
            data class CountryCurrencyNotSupported(val country: String, val currency: String) : Disabled()
            data object WooCoreVersionNotSupported : Disabled()
        }
    }
}
