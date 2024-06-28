package com.woocommerce.android.ui.moremenu.customer

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.CustomerWithAnalytics
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class CustomerDetailsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val refreshCustomerData: RefreshCustomerData,
    private val getCustomerWithStats: GetCustomerWithStats,
) : ScopedViewModel(savedState) {

    private val navArgs: CustomerDetailsFragmentArgs by savedState.navArgs()

    private val _viewState = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = CustomerViewState(navArgs.customer, true, false)
    )
    val viewState: LiveData<CustomerViewState> = _viewState.asLiveData()

    init {
        refreshAndUpdateData()
    }

    private fun refreshAndUpdateData() {
        launch {
            val customer = navArgs.customer
            refreshCustomerData(customer.remoteCustomerId, customer.analyticsCustomerId)
            getCustomerWithStats(customer.remoteCustomerId, customer.analyticsCustomerId).fold(
                onSuccess = { refreshedCustomer ->
                    _viewState.value = CustomerViewState(
                        customerWithAnalytics = refreshedCustomer,
                        isLoadingAnalytics = false,
                        isRefreshingData = false
                    )
                },
                onFailure = { }
            )
        }
    }

    fun refresh() {
        _viewState.value = _viewState.value.copy(isRefreshingData = true)
        refreshAndUpdateData()
    }

    fun onNavigateBack() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }
}

@Parcelize
data class CustomerViewState(
    val customerWithAnalytics: CustomerWithAnalytics,
    val isLoadingAnalytics: Boolean,
    val isRefreshingData: Boolean
) : Parcelable
