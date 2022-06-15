package com.woocommerce.android.ui.products.categories.selector

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppConstants
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class ProductCategorySelectorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val listHandler: ProductCategoryListHandler
) : ScopedViewModel(savedState) {
    companion object {
        private const val LOADING_STATE_DELAY = 100L
    }

    private val navArgs: ProductCategorySelectorFragmentArgs by savedState.navArgs()

    private val searchQuery = savedState.getStateFlow(this, "")
    private val selectedCategories = savedState.getStateFlow(this, navArgs.categoryIds.toSet())
    private val loadingState = MutableStateFlow(LoadingState.Idle)
    private val categories = listHandler.categories

    val viewState = combine(
        flow = categories,
        flow2 = selectedCategories,
        flow3 = searchQuery,
        flow4 = loadingState.withIndex()
            .debounce { (index, loadingState) ->
                if (index != 0 && loadingState == LoadingState.Idle) {
                    // When resetting to Idle, wait a bit to make sure the data has been fetched from DB
                    LOADING_STATE_DELAY
                } else 0L
            }
            .map { it.value }
    ) { categories, selectedCategories, searchQuery, loadingState ->
        ViewState(
            categories = categories.map { it.toUiModel(selectedCategories = selectedCategories) },
            selectedCategoriesCount = selectedCategories.size,
            searchQuery = searchQuery,
            loadingState = loadingState
        )
    }.asLiveData()

    init {
        monitorSearchQuery()
        launch {
            loadingState.value = LoadingState.Loading
            listHandler.fetchCategories(forceRefresh = true).onFailure {
                triggerEvent(ShowSnackbar(R.string.product_category_selector_loading_failed))
            }
            loadingState.value = LoadingState.Idle
        }
    }

    fun onLoadMore() = launch {
        loadingState.value = LoadingState.Appending
        listHandler.loadMore().onFailure {
            triggerEvent(ShowSnackbar(R.string.product_category_selector_loading_failed))
        }
        loadingState.value = LoadingState.Idle
    }

    fun onClearSelectionClick() {
        selectedCategories.value = emptySet()
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onDoneClick() {
        triggerEvent(ExitWithResult(selectedCategories.value))
    }

    private fun monitorSearchQuery() {
        viewModelScope.launch {
            searchQuery
                .withIndex()
                .filterNot {
                    // Skip initial value to avoid double fetching product categories
                    it.index == 0 && it.value.isEmpty()
                }
                .map { it.value }
                .onEach {
                    loadingState.value = LoadingState.Loading
                }
                .debounce {
                    if (it.isNullOrEmpty()) 0L else AppConstants.SEARCH_TYPING_DELAY_MS
                }
                .collectLatest { query ->
                    try {
                        listHandler.fetchCategories(searchQuery = query)
                            .onFailure {
                                val message = if (query.isEmpty()) R.string.product_category_selector_loading_failed
                                else R.string.product_category_selector_search_failed
                                triggerEvent(ShowSnackbar(message))
                            }
                    } finally {
                        loadingState.value = LoadingState.Idle
                    }
                }
        }
    }

    private fun ProductCategoryTreeItem.toUiModel(selectedCategories: Set<Long>): CategoryUiModel = CategoryUiModel(
        id = productCategory.remoteCategoryId,
        title = productCategory.name,
        children = children.map { it.toUiModel(selectedCategories) },
        isSelected = selectedCategories.contains(productCategory.remoteCategoryId),
        onItemClick = {
            this@ProductCategorySelectorViewModel.selectedCategories.update {
                if (it.contains(productCategory.remoteCategoryId)) {
                    it - productCategory.remoteCategoryId
                } else {
                    it + productCategory.remoteCategoryId
                }
            }
        }
    )

    data class CategoryUiModel(
        val id: Long,
        val title: String,
        val children: List<CategoryUiModel>,
        val isSelected: Boolean,
        val onItemClick: () -> Unit
    )

    data class ViewState(
        val categories: List<CategoryUiModel>,
        val selectedCategoriesCount: Int,
        val searchQuery: String,
        val loadingState: LoadingState
    )

    enum class LoadingState {
        Idle, Loading, Appending
    }
}
