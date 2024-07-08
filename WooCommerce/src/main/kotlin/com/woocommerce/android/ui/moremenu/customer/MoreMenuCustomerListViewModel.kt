package com.woocommerce.android.ui.moremenu.customer

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.CustomerWithAnalytics
import com.woocommerce.android.model.toCustomerWithAnalytics
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
class MoreMenuCustomerListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: CustomerListRepository,
    private val mapper: CustomerListViewModelMapper,
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
        triggerEvent(CustomerSelected(customerModel.toCustomerWithAnalytics(repository, mapper)))
    }
}

data class CustomerSelected(val customer: CustomerWithAnalytics) : MultiLiveEvent.Event()
