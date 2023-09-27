package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.models.CurrencyFormattingParameters
import com.woocommerce.android.ui.products.models.SiteParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class ParameterRepository @Inject constructor(
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite
) {
    fun getParameters(key: String, savedState: SavedStateHandle): SiteParameters {
        val parameters = savedState.get<SiteParameters>(key) ?: loadParameters()
        savedState[key] = parameters
        return parameters
    }

    fun getParameters(): SiteParameters = loadParameters()

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
            return@coroutineScope Result.failure(WooException(siteSettingsTask.getCompleted().error))
        }
        if (productSettingsTask.getCompleted().isError) {
            return@coroutineScope Result.failure(WooException(productSettingsTask.getCompleted().error))
        }

        return@coroutineScope withContext(Dispatchers.IO) {
            Result.success(loadParameters())
        }
    }

    private fun loadParameters(): SiteParameters {
        val siteSettings = wooCommerceStore.getSiteSettings(selectedSite.get())
        val currencyCode = siteSettings?.currencyCode
        val currencySymbol = wooCommerceStore.getSiteCurrency(selectedSite.get(), currencyCode)
        val gmtOffset = selectedSite.get().timezone?.toFloat() ?: 0f
        val (weightUnit, dimensionUnit) = wooCommerceStore.getProductSettings(selectedSite.get()).let {
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

        return SiteParameters(
            currencyCode,
            currencySymbol,
            currencyFormattingParameters,
            weightUnit,
            dimensionUnit,
            gmtOffset
        )
    }
}
