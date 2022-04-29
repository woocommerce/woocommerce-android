package com.woocommerce.android.util

import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.locale.LocaleProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Singleton
class CurrencyFormatter @Inject constructor(
    private val wcStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    private val localeProvider: LocaleProvider,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
    private val dispatchers: CoroutineDispatchers
) {
    companion object {
        private const val ONE_THOUSAND = 1000
        private const val ONE_MILLION = 1000000

        private const val BACKOFF_DELAY = 1_000L
        private const val BACKOFF_INTENTS = 3

        // Formats the value to two decimal places
        private val currencyFormatter: DecimalFormat by lazy {
            DecimalFormat("0.00")
        }

        // Formats the value to one decimal place
        private val currencyFormatterRounded: DecimalFormat by lazy {
            DecimalFormat("0.0")
        }

        private fun currencyStringRounded(rawValue: Double): String {
            val roundedValue = rawValue.roundToInt().toDouble()
            return if (roundedValue.absoluteValue >= ONE_MILLION) {
                currencyFormatterRounded.format(roundedValue / ONE_MILLION) + "m"
            } else if (roundedValue.absoluteValue >= ONE_THOUSAND) {
                currencyFormatterRounded.format(roundedValue / ONE_THOUSAND) + "k"
            } else {
                currencyFormatter.format(roundedValue).toString().removeSuffix(".00")
            }
        }
    }

    private var defaultCurrencyCode = ""

    init {
        appCoroutineScope.launch {
            selectedSite.observe()
                .onEach { defaultCurrencyCode = "" }
                .filterNotNull()
                .map { site -> getCurrencyCode(site) }
                .flowOn(dispatchers.io)
                .collect { currencyCode ->
                    defaultCurrencyCode = currencyCode
                }
        }
    }

    private suspend fun getCurrencyCode(site: SiteModel): String {
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

    /**
     * Formats a raw amount for display based on the WooCommerce site settings, rounding the values to the nearest int.
     *
     * Additionally, if the value is a thousand or more, we return it rounded to the nearest tenth
     * and suffixed with "k" (2500 -> 2.5k).
     *
     * Similarly, we add "m" for values a million or higher.
     *
     * @param rawValue the value to be formatted
     * @param currencyCode the ISO 4217 currency code to use for formatting
     * @return the formatted value for display
     */
    fun formatCurrencyRounded(rawValue: Double, currencyCode: String = defaultCurrencyCode): String {
        val displayFormatted = currencyStringRounded(rawValue)
        return displayFormatted.takeIf { it.isNotEmpty() }?.let {
            return wcStore.formatCurrencyForDisplay(it, selectedSite.get(), currencyCode, false)
        }.orEmpty()
    }

    /**
     * Utility function that returns a reduced function for formatting currencies for orders.
     *
     * For order objects, we generally want to show exact values, and the currency used can be set once at a global
     * level - then the same function can be used for all the various currency fields of an order.
     *
     * @param currencyCode the ISO 4217 currency code to use for formatting
     * @return a function which, given a raw amount as a String, returns the String formatted for display as a currency
     */
    fun buildFormatter(currencyCode: String = defaultCurrencyCode) = { rawValue: String? ->
        formatCurrency(rawValue ?: "0.0", currencyCode, true)
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
    fun formatAmountWithCurrency(amount: Double, currencyCode: String = defaultCurrencyCode): String {
        val locale = localeProvider.provideLocale() ?: Locale.getDefault()
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.currency = if (currencyCode.isEmpty()) {
            Currency.getInstance(locale)
        } else {
            Currency.getInstance(currencyCode)
        }
        return formatter.format(amount)
    }
}
