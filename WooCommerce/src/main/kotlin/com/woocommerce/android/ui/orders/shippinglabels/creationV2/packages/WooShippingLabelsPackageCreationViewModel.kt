package com.woocommerce.android.ui.orders.shippinglabels.creationV2.packages

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.parcelize.Parcelize

@HiltViewModel
class WooShippingLabelsPackageCreationViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    private val _viewState = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = ViewState(createPageTabs())
    )
    val viewState = _viewState.asLiveData()

    private fun createPageTabs(): List<PageTab> {
        return listOf(
            PageTab(R.string.woo_shipping_labels_package_creation_tab_custom),
            PageTab(R.string.woo_shipping_labels_package_creation_tab_carrier),
            PageTab(R.string.woo_shipping_labels_package_creation_tab_saved),
        )
    }

    @Parcelize
    data class ViewState(
        val pageTabs: List<PageTab> = emptyList()
    ) : Parcelable

    @Parcelize
    data class PageTab(
        val titleResource: Int
    ) : Parcelable
}
