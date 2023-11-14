package com.woocommerce.android.ui.searchfilter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.woocommerce.android.ui.searchfilter.SearchFilterEvent.ItemSelected
import com.woocommerce.android.ui.searchfilter.SearchFilterViewState.Empty
import com.woocommerce.android.ui.searchfilter.SearchFilterViewState.Loaded
import com.woocommerce.android.ui.searchfilter.SearchFilterViewState.Search
import com.woocommerce.android.viewmodel.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SearchFilterViewModel @Inject constructor() : ViewModel() {
    private val _viewStateLiveData = MutableLiveData<SearchFilterViewState>()
    val viewStateLiveData: LiveData<SearchFilterViewState>
        get() = _viewStateLiveData

    private val _eventLiveData = SingleLiveEvent<SearchFilterEvent>()
    val eventLiveData: LiveData<SearchFilterEvent> = _eventLiveData

    private lateinit var allSearchFilterItems: List<SearchFilterItem>

    private lateinit var requestKey: String

    fun start(searchFilterItems: Array<SearchFilterItem>, searchHint: String, requestKey: String) {
        allSearchFilterItems = searchFilterItems.toList()
        this.requestKey = requestKey
        _viewStateLiveData.value = Loaded(
            searchFilterItems = allSearchFilterItems,
            searchHint = searchHint
        )
    }

    fun onSearch(searchQuery: String) {
        val trimmedQuery = searchQuery.trim()
        val searchedItems = allSearchFilterItems.filter { it.name.contains(trimmedQuery, true) }
        val state = if (searchedItems.isNotEmpty()) {
            Search(searchedItems)
        } else {
            Empty(trimmedQuery)
        }
        _viewStateLiveData.value = state
    }

    fun onItemSelected(selectedItem: SearchFilterItem) {
        _eventLiveData.value = ItemSelected(
            selectedItemValue = selectedItem.value,
            requestKey = requestKey
        )
    }
}
