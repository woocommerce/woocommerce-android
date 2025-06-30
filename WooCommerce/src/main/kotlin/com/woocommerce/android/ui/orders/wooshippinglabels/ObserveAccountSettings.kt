package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.ui.orders.wooshippinglabels.datasource.WooShippingAccountSettingsDataStore
import com.woocommerce.android.ui.orders.wooshippinglabels.models.AccountSettingsModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.withIndex
import javax.inject.Inject

class ObserveAccountSettings @Inject constructor(
    private val configurationDataStore: WooShippingAccountSettingsDataStore,
    private val fetchAccountSettings: FetchAccountSettings
) {
    // We will use data store as the source of truth and after the first emission we will refresh the values async.
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<AccountSettingsModel?> = configurationDataStore.observeAccountSettings()
        .withIndex()
        .transformLatest { (index, value) ->
            val isFirstValue = index == 0
            if (isFirstValue && value == null) {
                if (fetchAccountSettings().isFailure) {
                    // We will use null as not available
                    emit(null)
                }
            } else {
                // Start by emitting cached values then refresh the store options async if needed
                emit(value)
                if (isFirstValue) {
                    fetchAccountSettings()
                }
            }
        }
}
