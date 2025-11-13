package com.woocommerce.android.ui.bookings.filter.customer

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListGetSupportedSearchModes
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListIsAdvancedSearchSupported
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListRepository
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListSelectionViewModel
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListViewModelMapper
import com.woocommerce.android.util.StringUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@Suppress("LongParameterList")
@HiltViewModel(assistedFactory = BookingCustomerFilterViewModel.Factory::class)
class BookingCustomerFilterViewModel @AssistedInject constructor(
    savedStateHandle: SavedStateHandle,
    repository: CustomerListRepository,
    mapper: CustomerListViewModelMapper,
    isAdvancedSearchSupported: CustomerListIsAdvancedSearchSupported,
    getSupportedSearchModes: CustomerListGetSupportedSearchModes,
    analyticsTracker: AnalyticsTrackerWrapper,
    stringUtils: StringUtils,
    @Assisted private val onCustomerSelected: (Order.Customer) -> Unit
) : CustomerListSelectionViewModel(
    savedStateHandle,
    repository,
    mapper,
    isAdvancedSearchSupported,
    getSupportedSearchModes,
    analyticsTracker,
    stringUtils
) {
    override fun exitWithCustomer(customer: Order.Customer) {
        this.onCustomerSelected.invoke(customer)
    }

    @AssistedFactory
    interface Factory {
        fun create(onCustomerSelected: (Order.Customer) -> Unit): BookingCustomerFilterViewModel
    }
}
