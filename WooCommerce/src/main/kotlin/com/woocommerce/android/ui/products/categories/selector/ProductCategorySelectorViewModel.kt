package com.woocommerce.android.ui.products.categories.selector

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
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

    private val selectedCategories = MutableStateFlow(emptyList<Long>())

    private val loadingState = MutableStateFlow(LoadingState.Idle)

    private val categories = combine(
        listHandler.categories,
        selectedCategories
    ) { categories, selectedCategories ->
        categories.map { it.toUiModel(selectedCategories = selectedCategories) }
    }.toStateFlow(emptyList())

    val viewState = combine(
        flow = categories,
        flow2 = loadingState.withIndex()
            .debounce { (index, loadingState) ->
                if (index != 0 && loadingState == LoadingState.Idle) {
                    // When resetting to Idle, wait a bit to make sure the data has been fetched from DB
                    LOADING_STATE_DELAY
                } else 0L
            }
            .map { it.value }
    ) { categories, loadingState ->
        ViewState(
            categories = categories,
            loadingState = loadingState
        )
    }.asLiveData()

    init {
        launch {
            loadingState.value = LoadingState.Loading
            listHandler.fetchCategories(forceRefresh = true)
            loadingState.value = LoadingState.Idle
        }
    }

    fun onLoadMore() = launch {
        loadingState.value = LoadingState.Appending
        listHandler.loadMore()
        loadingState.value = LoadingState.Idle
    }

    private fun ProductCategoryTreeItem.toUiModel(selectedCategories: List<Long>): CategoryUiModel = CategoryUiModel(
        id = productCategory.remoteCategoryId,
        title = productCategory.name,
        children = children.map { it.toUiModel(selectedCategories) },
        isSelected = selectedCategories.contains(productCategory.remoteCategoryId)
    )

    data class CategoryUiModel(
        val id: Long,
        val title: String,
        val children: List<CategoryUiModel>,
        val isSelected: Boolean
    )

    data class ViewState(
        val categories: List<CategoryUiModel>,
        val loadingState: LoadingState
    )

    enum class LoadingState {
        Idle, Loading, Appending
    }
}
