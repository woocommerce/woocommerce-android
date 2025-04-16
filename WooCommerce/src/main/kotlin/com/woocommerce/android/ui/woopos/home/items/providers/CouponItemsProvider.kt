package com.woocommerce.android.ui.woopos.home.items.providers

import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CouponItemsProvider @Inject constructor(
    private val couponsDataSource: WooPosCouponsDataSource,
) : WooPosItemDataProvider {
    private val _data = MutableStateFlow(DataProviderState.fetching(emptyList(), isPullToRefresh = false))
    override val data: Flow<DataProviderState> = _data

    private val couponsList = couponsDataSource.couponsFlow

    override suspend fun init() {
        fetchItems(
            forceRefresh = false,
        )

        couponsList
            .map { coupons ->
                coupons.map { coupon ->
                    WooPosItemSelectionViewState.Coupon(
                        id = coupon.id,
                        name = coupon.code ?: "",
                    )
                }
            }
            // Collect each new view state emitted from the flow.
            .collect { newState ->
                // TODO we need to differentiate between cached and remote
                //  data - essentially whether we should keep showing loading.
                _data.value = DataProviderState.dataShown(newState)
            }
    }

    override suspend fun fetchItems(
        forceRefresh: Boolean, // todo should this be called pullToRefresh?
    ) {
        _data.value = DataProviderState.fetching(_data.value.items, isPullToRefresh = forceRefresh)
        val result = couponsDataSource.clearCacheAndFetchFirstPage()

        if (!result.isSuccess) {
            _data.value = DataProviderState.remoteRequestFailed(
                _data.value.items,
                result.exceptionOrNull()?.message ?: ""
            )
        }
    }

    override suspend fun loadMore() {
        _data.value = DataProviderState.loadingMore(_data.value.items)
        val result = couponsDataSource.loadMore()
        if (!result.isSuccess) {
            _data.value = DataProviderState.loadingMoreFailed(
                _data.value.items,
                result.exceptionOrNull()?.message ?: ""
            )
        }
    }
}
