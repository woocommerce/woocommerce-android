package com.woocommerce.android.ui.products.variations.picker

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.VariantOption
import com.woocommerce.android.ui.products.variations.selector.VariationListHandler
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class VariationPickerViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val variationListHandler: VariationListHandler
) : ScopedViewModel(savedState) {
    companion object {
        private const val STATE_UPDATE_DELAY = 100L
    }

    private val navArgs: VariationPickerFragmentArgs by savedState.navArgs()
    private val allowedVariations = navArgs.allowedVatiations?.toSet() ?: emptySet()

    private val loadingState = MutableStateFlow(LoadingState.IDLE)
    private val loadingWithDebounce
        get() = loadingState.withIndex().debounce {
            if (it.index != 0 && it.value == LoadingState.IDLE) {
                // When resetting to IDLE, wait a bit to make sure the list has been fetched from DB
                STATE_UPDATE_DELAY
            } else {
                0L
            }
        }.map { it.value }

    init {
        viewModelScope.launch {
            loadingState.value = LoadingState.LOADING
            variationListHandler.fetchVariations(productId = navArgs.productId, forceRefresh = true)
            loadingState.value = LoadingState.IDLE
        }
    }

    val viewSate = combine(
        variationListHandler.getVariationsFlow(navArgs.productId),
        loadingWithDebounce
    ) { variations, loadingState ->
        ViewState(
            loadingState = loadingState,
            variations = variations.filter { variation ->
                allowedVariations.isEmpty() || variation.remoteVariationId in allowedVariations
            }.map { it.toVariationListItem() }
        )
    }.asLiveData()

    fun onLoadMore() {
        viewModelScope.launch {
            loadingState.value = LoadingState.APPENDING
            variationListHandler.loadMore(navArgs.productId)
            loadingState.value = LoadingState.IDLE
        }
    }

    fun onCancel() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onSelectVariation(variation: VariationListItem) {
        triggerEvent(
            MultiLiveEvent.Event.ExitWithResult(
                VariationPickerResult(
                    itemId = navArgs.itemId,
                    productId = navArgs.productId,
                    variationId = variation.id,
                    attributes = variation.attributes
                )
            )
        )
    }

    private fun ProductVariation.toVariationListItem() =
        VariationListItem(
            id = remoteVariationId,
            title = getName(),
            imageUrl = image?.source,
            attributes = attributes.toList()
        )

    data class VariationListItem(
        val id: Long,
        val title: String,
        val imageUrl: String? = null,
        val attributes: List<VariantOption>
    )

    data class ViewState(
        val loadingState: LoadingState = LoadingState.IDLE,
        val variations: List<VariationListItem> = emptyList()
    )

    enum class LoadingState {
        IDLE, LOADING, APPENDING
    }

    @Parcelize
    data class VariationPickerResult(
        val itemId: Long,
        val productId: Long,
        val variationId: Long,
        val attributes: List<VariantOption>
    ) : Parcelable
}
