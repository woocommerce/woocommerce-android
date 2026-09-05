package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.models.CurrencyFormattingParameters
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class ParameterRepository @Inject constructor(
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite
) {
    suspend fun getParameters(key: String, savedState: SavedStateHandle): SiteParameters {
        val parameters = savedState.get<SiteParameters>(key) ?: loadParameters()
        savedState[key] = parameters
        return parameters
    }

    suspend fun getParameters(): SiteParameters = loadParameters()

    /**
     * Blocks the calling thread. Kept for the WooCommerce Services shipping label screens, which are
     * scheduled for removal, and must not be used anywhere else.
     */
    fun getParametersBlocking(key: String, savedState: SavedStateHandle): SiteParameters = runBlocking {
        getParameters(key, savedState)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun fetchParameters(): Result<SiteParameters> = coroutineScope {
        val siteSettingsTask = async {
            wooCommerceStore.fetchSiteGeneralSettings(selectedSite.get())
        }

        val productSettingsTask = async {
            wooCommerceStore.fetchSiteProductSettings(selectedSite.get())
        }

        awaitAll(siteSettingsTask, productSettingsTask)

        if (siteSettingsTask.getCompleted().isError) {
            siteSettingsTask.getCompleted().error.let {
                WooLog.e(WooLog.T.UTILS, "Error fetching site settings,  ${it.type} ${it.message}")
            }
            return@coroutineScope Result.failure(WooException(siteSettingsTask.getCompleted().error))
        }
        if (productSettingsTask.getCompleted().isError) {
            productSettingsTask.getCompleted().error.let {
                WooLog.e(WooLog.T.UTILS, "Error fetching product settings,  ${it.type} ${it.message}")
            }
            return@coroutineScope Result.failure(WooException(productSettingsTask.getCompleted().error))
        }

        return@coroutineScope Result.success(loadParameters())
    }

    private suspend fun loadParameters(): SiteParameters = withContext(Dispatchers.IO) {
        val site = selectedSite.get()
        val siteSettings = wooCommerceStore.getSiteSettings(site)
        val currencyCode = siteSettings?.currencyCode
        val currencySymbol = wooCommerceStore.getSiteCurrency(site, currencyCode)
        val gmtOffset = site.timezone?.toFloat() ?: 0f
        val (weightUnit, dimensionUnit) = wooCommerceStore.getProductSettings(site).let {
            Pair(it?.weightUnit, it?.dimensionUnit)
        }
        val currencyFormattingParameters = siteSettings?.let {
            CurrencyFormattingParameters(
                currencyDecimalNumber = it.currencyDecimalNumber,
                currencyPosition = it.currencyPosition,
                currencyDecimalSeparator = it.currencyDecimalSeparator,
                currencyThousandSeparator = it.currencyThousandSeparator
            )
        }

        SiteParameters(
            currencyCode,
            currencySymbol,
            currencyFormattingParameters,
            weightUnit,
            dimensionUnit,
            gmtOffset
        )
    }
}
