package com.woocommerce.android.ui.products.categories.selector

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class ProductCategorySelectorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    repository: ProductCategorySelectorRepository
) : ScopedViewModel(savedState) {
    private val selectedCategories = MutableStateFlow(emptyList<Long>())

    private val categories = combine(
        repository.observeCategories()
            .map { it.convertToTree() },
        selectedCategories
    ) { categories, selectedCategories ->
        categories.map { it.toUiModel(selectedCategories = selectedCategories) }
    }.toStateFlow(emptyList())

    val viewState = categories.map {
        ViewState(categories = it)
    }.asLiveData()

    private fun ProductCategoryTreeItem.toUiModel(selectedCategories: List<Long>): CategoryUiModel = CategoryUiModel(
        title = productCategory.name,
        children = children.map { it.toUiModel(selectedCategories) },
        isSelected = selectedCategories.contains(productCategory.remoteCategoryId)
    )

    data class CategoryUiModel(
        val title: String,
        val children: List<CategoryUiModel>,
        val isSelected: Boolean
    )

    data class ViewState(
        val categories: List<CategoryUiModel>
    )
}
