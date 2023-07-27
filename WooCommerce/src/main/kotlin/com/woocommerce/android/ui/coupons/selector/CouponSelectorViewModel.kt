package com.woocommerce.android.ui.coupons.selector

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppConstants
import com.woocommerce.android.R
import com.woocommerce.android.ui.coupons.CouponListHandler
import com.woocommerce.android.ui.coupons.CouponListViewModel
import com.woocommerce.android.ui.coupons.CouponListViewModel.LoadingState.Idle
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class CouponSelectorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val couponListHandler: CouponListHandler,
) : ScopedViewModel(savedState) {

    private val searchQuery = savedState.getNullableStateFlow(this, null, String::class.java)
    private val loadingState = MutableStateFlow(Idle)

    init {
        if (searchQuery.value == null) {
            fetchCoupons()
        }
        monitorSearchQuery()
    }

    private fun fetchCoupons() = launch {
        loadingState.value = CouponListViewModel.LoadingState.Loading
        couponListHandler.fetchCoupons(forceRefresh = true)
            .onFailure {
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.coupon_list_loading_failed))
            }
        loadingState.value = CouponListViewModel.LoadingState.Idle
    }

    private fun monitorSearchQuery() {
        viewModelScope.launch {
            searchQuery
                .withIndex()
                .filterNot {
                    it.index == 0 && it.value == null
                }
                .map { it.value }
                .onEach {
                    loadingState.value = CouponListViewModel.LoadingState.Loading
                }
                .debounce { if (it.isNullOrEmpty()) 0L else AppConstants.SEARCH_TYPING_DELAY_MS }
                .collectLatest { query ->
                    loadingState.value = CouponListViewModel.LoadingState.Idle
                    couponListHandler.fetchCoupons(query)
                        .onFailure {
                            triggerEvent(
                                MultiLiveEvent.Event.ShowSnackbar(
                                    if (query == null) R.string.coupon_list_loading_failed
                                    else R.string.coupon_list_search_failed
                                )
                            )
                        }
                    loadingState.value = CouponListViewModel.LoadingState.Idle
                }
        }
    }
}

