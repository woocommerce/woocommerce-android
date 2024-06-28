package com.woocommerce.android.ui.moremenu.customer

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.customer.CustomerListViewModel
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListGetSupportedSearchModes
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListIsAdvancedSearchSupported
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListRepository
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListViewModelMapper
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import javax.inject.Inject
@HiltViewModel
class CustomerListDetailsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    repository: CustomerListRepository,
    mapper: CustomerListViewModelMapper,
    isAdvancedSearchSupported: CustomerListIsAdvancedSearchSupported,
    getSupportedSearchModes: CustomerListGetSupportedSearchModes,
    analyticsTracker: AnalyticsTrackerWrapper,
    stringUtils: StringUtils,
) : CustomerListViewModel(
    savedState,
    repository,
    mapper,
    isAdvancedSearchSupported,
    getSupportedSearchModes,
    analyticsTracker,
    stringUtils
) {
    override fun onCustomerSelected(customerModel: WCCustomerModel) {
        triggerEvent(CustomerSelected(customerModel.remoteCustomerId))
    }
}

data class CustomerSelected(val remoteCustomerId: Long) : MultiLiveEvent.Event()
