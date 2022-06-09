package com.woocommerce.android.ui.orders.creation.customerlist

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppConstants
import com.woocommerce.android.R
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import javax.inject.Inject

@HiltViewModel
class CustomerListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val networkStatus: NetworkStatus,
    private val customerListRepository: CustomerListRepository
) : ScopedViewModel(savedState) {
    private val _customerList = MutableLiveData<List<WCCustomerModel>>()
    val customerList: LiveData<List<WCCustomerModel>> = _customerList

    val viewStateLiveData = LiveDataDelegate(savedState, CustomerListViewState())
    private var viewState by viewStateLiveData

    private var searchJob: Job? = null

    fun onCustomerClick(customer: WCCustomerModel) {
        // TODO nbradbury
    }

    fun onSearchQueryChanged(query: String?) {
        if (query != null && query.length > 2) {
            // cancel any existing search, then start a new one after a brief delay so we don't
            // actually perform the fetch until the user stops typing
            searchJob?.cancel()
            searchJob = launch {
                delay(AppConstants.SEARCH_TYPING_DELAY_MS)
                searchCustomerList(query)
            }
        } else {
            launch {
                searchJob?.cancelAndJoin()
                _customerList.value = emptyList()
                viewState = viewState.copy(isEmptyViewVisible = false)
            }
        }
    }

    private suspend fun searchCustomerList(query: String) {
        if (networkStatus.isConnected()) {
            viewState = viewState.copy(
                isSkeletonShown = true,
                isEmptyViewVisible = false
            )
            _customerList.value = customerListRepository.searchCustomerList(query)
            viewState = viewState.copy(
                isSkeletonShown = false,
                isEmptyViewVisible = _customerList.value?.isEmpty() == true
            )
        } else {
            triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
        }
    }

    @Parcelize
    data class CustomerListViewState(
        val isSkeletonShown: Boolean? = null,
        val isEmptyViewVisible: Boolean? = null
    ) : Parcelable
}
