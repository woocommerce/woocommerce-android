package com.woocommerce.android.ui.orders.creation.addcustomer

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.list.OrderListViewModel.OrderListEvent.ShowErrorSnack
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.ThrottleLiveData
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import org.wordpress.android.fluxc.model.customer.WCCustomerListDescriptor
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.store.ListStore

typealias PagedCustomersList = PagedList<CustomerListItemType>

class AddCustomerViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    coroutineDispatchers: CoroutineDispatchers,
    private val listStore: ListStore,
    private val selectedSite: SelectedSite,
    private val listItemDataSource: AddCustomerListItemDataSource
) : ScopedViewModel(savedState, coroutineDispatchers), LifecycleOwner {
    private val _pagedListData = MediatorLiveData<PagedCustomersList>()
    val pagedListData: LiveData<PagedCustomersList> = _pagedListData

    private val _isLoadingMore = MediatorLiveData<Boolean>()
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _isFetchingFirstPage = MediatorLiveData<Boolean>()
    val isFetchingFirstPage: LiveData<Boolean> = _isFetchingFirstPage

    private val _emptyViewType: ThrottleLiveData<EmptyViewType?> by lazy {
        ThrottleLiveData<EmptyViewType?>(
            offset = 100,
            coroutineScope = this,
            mainDispatcher = coroutineDispatchers.main,
            backgroundDispatcher = coroutineDispatchers.computation
        )
    }
    val emptyViewType: LiveData<EmptyViewType?> = _emptyViewType

    private val lifecycleRegistry: LifecycleRegistry by lazy { LifecycleRegistry(this) }

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    private val pagedListWrapper by lazy {
        listStore.getList(
            listDescriptor = WCCustomerListDescriptor(site = selectedSite.get()),
            dataSource = listItemDataSource,
            lifecycle = lifecycle
        ).apply { fetchFirstPage() }
    }

    init {
        with(pagedListWrapper) {
            _pagedListData.addSource(data) { pagedList ->
                pagedList?.let {
                    _pagedListData.value = it
                }
            }
            _isFetchingFirstPage.addSource(isFetchingFirstPage) { isFetchingInProgress ->
                _isFetchingFirstPage.value = isFetchingInProgress
            }
            _isLoadingMore.addSource(isLoadingMore) {
                _isLoadingMore.value = it
            }
            pagedListWrapper.listError.observe(this@AddCustomerViewModel, Observer {
                it?.let {
                    triggerEvent(ShowErrorSnack(R.string.order_creation_add_customer_error_fetching_generic))
                }
            })

            _emptyViewType.addSource(isEmpty) { createAndPostEmptyViewType(this) }
            _emptyViewType.addSource(isFetchingFirstPage) { createAndPostEmptyViewType(this) }
            _emptyViewType.addSource(listError) { createAndPostEmptyViewType(this) }
        }
    }

    fun onRefresh() {
        pagedListWrapper.fetchFirstPage()
    }

    private fun createAndPostEmptyViewType(wrapper: PagedListWrapper<CustomerListItemType>) {
        val isListEmpty = wrapper.isEmpty.value ?: true
        val isError = wrapper.listError.value != null
        val isLoadingData = wrapper.isFetchingFirstPage.value ?: false || wrapper.data.value == null

        val newEmptyViewType: EmptyViewType? = if (isListEmpty) {
            when {
                isError -> EmptyViewType.NETWORK_ERROR
                isLoadingData -> EmptyViewType.CUSTOMER_LIST_LOADING
                else -> EmptyViewType.CUSTOMER_LIST
            }
        } else {
            null
        }

        _emptyViewType.postValue(newEmptyViewType)
    }

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<AddCustomerViewModel>
}
