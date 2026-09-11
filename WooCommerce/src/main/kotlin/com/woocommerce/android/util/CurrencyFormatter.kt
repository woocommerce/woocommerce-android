package com.woocommerce.android.util

import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.extensions.NumberExtensionsWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.locale.LocaleProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

@Singleton
class CurrencyFormatter @Inject constructor(
    private val wcStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    private val siteIndependentCurrencyFormatter: SiteIndependentCurrencyFormatter,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
    private val dispatchers: CoroutineDispatchers,
    private val numberExtensionsWrapper: NumberExtensionsWrapper,
    private val localeProvider: LocaleProvider
) {
    private var defaultCurrencyCode = ""

    init {
        appCoroutineScope.launch {
            combine(
                selectedSite.observe().filterNotNull(),
                wcStore.observeAllSiteSettings()
            ) { site, settingsMap ->
                site to (settingsMap[LocalId(site.id)]?.currencyCode ?: "")
            }
                .flowOn(dispatchers.io)
                .collect { (site, currencyCode) ->
                    defaultCurrencyCode = currencyCode.ifEmpty {
                        getOrFetchCurrencyCode(site)
                    }
                }
        }
    }

    private suspend fun getOrFetchCurrencyCode(site: SiteModel): String {
        val localSettings = wcStore.getSiteSettings(site)
        if (localSettings != null) return localSettings.currencyCode

        var currentDelay = BACKOFF_DELAY
        var currencyCode = ""
        for (i in 0 until BACKOFF_INTENTS) {
            val settings = wcStore.fetchSiteGeneralSettings(site).model
            if (settings != null) {
                currencyCode = settings.currencyCode
                break
            }
            delay(currentDelay)
            currentDelay = (currentDelay * i)
        }
        return currencyCode
    }

    /**
     * Formats a raw amount for display based on the WooCommerce site settings.
     *
     * @param rawValue the value to be formatted
     * @param currencyCode the ISO 4217 currency code to use for formatting
     * @return the formatted value for display
     */
    fun formatCurrency(
        rawValue: String,
        currencyCode: String = defaultCurrencyCode,
        applyDecimalFormatting: Boolean = true
    ) = wcStore.formatCurrencyForDisplay(rawValue, selectedSite.get(), currencyCode, applyDecimalFormatting)

    /**
     * Formats the amount for display based on the WooCommerce site settings.
     *
     * @param amount the value to be formatted
     * @param currencyCode the ISO 4217 currency code to use for formatting
     * @return the formatted value for display
     */
    fun formatCurrency(
        amount: BigDecimal,
        currencyCode: String = defaultCurrencyCode,
        applyDecimalFormatting: Boolean = true
    ) = formatCurrency(amount.toString(), currencyCode, applyDecimalFormatting)

    fun formatCurrencyGivenInTheSmallestCurrencyUnit(
        amount: Long,
        currencyCode: String,
        applyDecimalFormatting: Boolean = true
    ): String {
        val currencyObj = Currency.getInstance(currencyCode)
        val smallestCurrencyUnit = BigDecimal.TEN.pow(currencyObj.defaultFractionDigits)
        val value = BigDecimal.valueOf(amount).divide(smallestCurrencyUnit)
        return formatCurrency(value, currencyCode, applyDecimalFormatting)
    }

    fun formatCurrencyRounded(rawValue: Double, currencyCode: String = defaultCurrencyCode): String {
        val locale = localeProvider.provideLocale() ?: Locale.getDefault()
        val displayFormatted = numberExtensionsWrapper.compactNumberCompat(rawValue.roundToLong(), locale)
        return wcStore.formatCurrencyForDisplay(displayFormatted, selectedSite.get(), currencyCode, false)
    }

    /**
     * Utility function that returns a reduced function for formatting currencies for orders.
     *
     * For order objects, we generally want to show exact values, and the currency used can be set once at a global
     * level - then the same function can be used for all the various currency fields of an order.
     *
     * @param currencyCode the ISO 4217 currency code to use for formatting
     * @return a function which, given an amount as a BigDecimal, returns the String formatted for display as a currency
     */
    fun buildBigDecimalFormatter(currencyCode: String = defaultCurrencyCode) = { amount: BigDecimal ->
        formatCurrency(amount, currencyCode, true)
    }

    /**
     * Returns formatted amount with currency symbol - eg. $113.5 for EN/USD or 113,5€ for FR/EUR.
     */
    fun formatAmountWithCurrency(amount: Double, currencyCode: String = defaultCurrencyCode): String =
        siteIndependentCurrencyFormatter.formatAmountWithCurrency(amount, currencyCode)

    /**
     * Returns formatted amount with currency symbol with 0.0 rounded to 0
     */
    fun getFormattedAmountZeroRounded(revenue: Double, currencyCode: String) =
        if (revenue == 0.0) {
            formatCurrencyRounded(revenue, currencyCode)
        } else {
            formatCurrency(revenue.toBigDecimal(), currencyCode)
        }

    private companion object {
        const val BACKOFF_DELAY = 1_000L
        const val BACKOFF_INTENTS = 3
    }
}
