package com.woocommerce.android.ui.orders.wooshippinglabels.address.origin

import com.woocommerce.android.ui.orders.wooshippinglabels.datasource.WooShippingAddressDataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.transformLatest
import javax.inject.Inject

class ObserveOriginAddresses @Inject constructor(
    private val addressDataStore: WooShippingAddressDataStore,
    private val fetchOriginAddresses: FetchOriginAddresses
) {
    private var isFirstValue = true

    @OptIn(ExperimentalCoroutinesApi::class)
    // We will use data store as the source of truth and after the first emission we will refresh the values async.
    operator fun invoke() = addressDataStore.observeOriginAddresses().transformLatest { addresses ->
        when {
            isFirstValue && addresses == null -> {
                // If there is no cached data, refresh the origin addresses before emitting any value
                isFirstValue = false
                if (fetchOriginAddresses().isFailure) {
                    // We will use null as not available
                    emit(null)
                }
            }

            isFirstValue && addresses != null -> {
                // If there is cached data, emit cached values and refresh the origin addresses async
                isFirstValue = false
                emit(addresses)
                if (fetchOriginAddresses().isFailure) {
                    emit(null)
                }
            }

            else -> emit(addresses)
        }
    }
}
