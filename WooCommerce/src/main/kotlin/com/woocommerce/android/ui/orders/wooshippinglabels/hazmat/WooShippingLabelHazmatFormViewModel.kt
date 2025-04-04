package com.woocommerce.android.ui.orders.wooshippinglabels.hazmat

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelHazmatCategory
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class WooShippingLabelHazmatFormViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val navArgs: WooShippingLabelHazmatFormFragmentArgs by savedState.navArgs()

    private val _viewState = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = ViewState()
    )
    val viewState = _viewState.asLiveData()

    init {
        _viewState.update { viewState ->
            val selectedCategory = navArgs.selectedCategoryName?.let {
                ShippingLabelHazmatCategory.valueOf(it)
            }

            viewState.copy(
                containsHazmatChecked = selectedCategory != null,
                currentHazmatSelection = selectedCategory
            )
        }
    }

    fun onContainsHazmatChanged(containsHazmatChecked: Boolean) {
        _viewState.update { viewState ->
            viewState.copy(
                containsHazmatChecked = containsHazmatChecked,
                currentHazmatSelection = viewState.currentHazmatSelection.takeIf { containsHazmatChecked }
            )
        }
    }

    fun onSelectCategoryClick() {
        triggerEvent(OnSelectCategoryClicked)
    }

    fun onHazmatCategorySelected(selectedCategory: ShippingLabelHazmatCategory?) {
        if (_viewState.value.containsHazmatChecked.not()) return

        _viewState.update { viewState ->
            viewState.copy(currentHazmatSelection = selectedCategory)
        }
        triggerEvent(OnHazmatCategorySelected(selectedCategory))
    }

    fun onUrlSelected(url: String) {
        triggerEvent(OnUrlSelected(url))
    }

    fun onBackPressed() {
        _viewState.value.currentHazmatSelection
            .let { OnHazmatCategorySelected(it) }
            .let { triggerEvent(it) }
    }

    @Parcelize
    data class ViewState(
        val containsHazmatChecked: Boolean = false,
        val currentHazmatSelection: ShippingLabelHazmatCategory? = null
    ) : Parcelable

    data object OnSelectCategoryClicked : Event()

    data class OnUrlSelected(val url: String) : Event()
    data class OnHazmatCategorySelected(val selectedCategory: ShippingLabelHazmatCategory?) : Event()

    companion object {
        const val KEY_HAZMAT_CATEGORY_SELECTOR_RESULT = "hazmat_category_selector_result"
        const val HAZMAT_CATEGORY_RESULT = "hazmat_category_result"
    }
}
