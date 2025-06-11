package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.ui.orders.wooshippinglabels.datasource.WooShippingAccountSettingsDataStore
import com.woocommerce.android.ui.orders.wooshippinglabels.models.AccountSettingsModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest
import javax.inject.Inject

class ObserveAccountSettings @Inject constructor(
    private val configurationDataStore: WooShippingAccountSettingsDataStore,
    private val fetchAccountSettings: FetchAccountSettings
) {
    private var isFirstValue = true

    @OptIn(ExperimentalCoroutinesApi::class)
    // We will use data store as the source of truth and after the first emission we will refresh the values async.
    operator fun invoke(): Flow<AccountSettingsModel?> = configurationDataStore.observeAccountSettings()
        .transformLatest { cachedAccountSettings ->
            when {
                isFirstValue && cachedAccountSettings == null -> {
                    isFirstValue = false
                    if (fetchAccountSettings().isFailure) {
                        // We will use null as not available
                        emit(null)
                    }
                }

                isFirstValue -> {
                    // If there is cached data, emit cached values and refresh the store options async
                    isFirstValue = false
                    emit(cachedAccountSettings)
                    fetchAccountSettings()
                }

                else -> emit(cachedAccountSettings)
            }
        }
}
