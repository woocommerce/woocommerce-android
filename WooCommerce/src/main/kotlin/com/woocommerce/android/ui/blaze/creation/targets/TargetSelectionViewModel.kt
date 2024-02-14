package com.woocommerce.android.ui.blaze.creation.targets

import androidx.lifecycle.LiveData
import com.woocommerce.android.ui.blaze.creation.targets.TargetSelectionViewState.SearchState.Results.SearchItem
import com.woocommerce.android.ui.blaze.creation.targets.TargetSelectionViewState.SelectionItem

interface TargetSelectionViewModel {
    val viewState: LiveData<TargetSelectionViewState>

    fun onSaveTapped()
    fun onBackPressed()
    fun onAllButtonTapped()
    fun onItemToggled(item: SelectionItem)
    fun onSearchItemTapped(item: SearchItem) = Unit
    fun onSearchActiveStateChanged(isActive: Boolean) = Unit
    fun onSearchQueryChanged(query: String) = Unit
}
