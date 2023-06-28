package com.woocommerce.android.ui.orders.creation.customerlistnew

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListRepository
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
@Suppress("UnusedPrivateMember")
class CustomerListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val customerListRepository: CustomerListRepository
) : ScopedViewModel(savedState) {
    private val _viewState = MutableLiveData<ViewState>()
    val viewState: LiveData<ViewState> = _viewState

    private var searchQuery: String
        get() = savedState.get<String>(SEARCH_QUERY_KEY) ?: ""
        set(value) {
            savedState[SEARCH_QUERY_KEY] = value
        }
    private var searchMode: SearchMode
        get() = savedState.get<SearchMode>(SEARCH_MODE_KEY) ?: SearchMode.ALL
        set(value) {
            savedState[SEARCH_MODE_KEY] = value
        }

    sealed class ViewState {
        object Loading : ViewState()
        object Empty : ViewState()
        object Error : ViewState()
        data class Loaded(val customers: List<ListItem>) : ViewState()

        data class ListItem(
            val remoteId: Long,
            val firstName: String,
            val lastName: String,
            val email: String,
        )
    }

    enum class SearchMode(val value: String) {
        ALL("all"),
        EMAIL("email"),
        NAME("name"),
        USERNAME("username"),
    }

    private companion object {
        private const val SEARCH_QUERY_KEY = "search_query"
        private const val SEARCH_MODE_KEY = "search_mode"
    }
}
