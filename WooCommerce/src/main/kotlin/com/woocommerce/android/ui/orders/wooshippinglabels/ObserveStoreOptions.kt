package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.datasource.WooShippingAccountSettingsDataStore
import com.woocommerce.android.ui.orders.wooshippinglabels.models.StoreOptionsModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class ObserveStoreOptions @Inject constructor(
    private val configurationDataStore: WooShippingAccountSettingsDataStore,
    private val fetchAccountSettings: FetchAccountSettings,
    private val wooStore: WooCommerceStore,
    private val site: SelectedSite,
) {
    private var isFirstValue = true

    @OptIn(ExperimentalCoroutinesApi::class)
    // We will use data store as the source of truth and after the first emission we will refresh the values async.
    operator fun invoke(): Flow<StoreOptionsModel?> = configurationDataStore.observeAccountSettings().transformLatest { options ->
        val cachedStoreOptions = options?.storeOptions ?: getStoreOptionsFromSiteSettings(wooStore, site)

        when {
            isFirstValue && cachedStoreOptions == null -> {
                isFirstValue = false
                if (fetchAccountSettings().isFailure) {
                    // We will use null as not available
                    emit(null)
                }
            }

            isFirstValue -> {
                // If there is cached data, emit cached values and refresh the store options async
                isFirstValue = false
                emit(cachedStoreOptions)
                fetchAccountSettings()
            }

            else -> emit(cachedStoreOptions)
        }
    }

    private suspend fun getStoreOptionsFromSiteSettings(wooStore: WooCommerceStore, site: SelectedSite) =
        wooStore.getProductSettings(site.get())?.let { productSettings ->
            StoreOptionsModel.EMPTY.copy(
                weightUnit = productSettings.weightUnit,
                dimensionUnit = productSettings.dimensionUnit
            )
        }
}
