package com.woocommerce.android.ui.products.variations

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_VARIANTS_BULK_UPDATE_REGULAR_PRICE_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_VARIANTS_BULK_UPDATE_SALE_PRICE_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.track
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.ui.products.variations.VariationsBulkUpdatePriceViewModel.PriceType
import com.woocommerce.android.ui.products.variations.VariationsBulkUpdatePriceViewModel.PriceUpdateData
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

/**
 * Responsible for calculating view state - [ProductVariation] attribute subtitles
 * which are represented by [ValuesGroupType] class.
 */
@HiltViewModel
class VariationsBulkUpdateAttrPickerViewModel @Inject constructor(
    savedState: SavedStateHandle,
    parameterRepository: ParameterRepository,
) : ScopedViewModel(savedState) {
    private val args: VariationsBulkUpdateAttrPickerDialogArgs by savedState.navArgs()

    private val parameters: SiteParameters by lazy {
        parameterRepository.getParameters("key_product_parameters", savedState)
    }

    private val _viewState = MutableStateFlow(
        ViewState(
            currency = parameters.currencySymbol
        )
    )
    val viewState: LiveData<ViewState> = _viewState.asLiveData()

    init {
        val regularPriceValues = args.variationsToUpdate.asList().map { it.regularPrice }
        val salePriceValues = args.variationsToUpdate.asList().map { it.salePrice }
        _viewState.value = _viewState.value.copy(
            regularPriceGroupType = regularPriceValues.groupType(),
            salePriceGroupType = salePriceValues.groupType(),
        )
    }

    fun onRegularPriceUpdateClicked() {
        track(PRODUCT_VARIANTS_BULK_UPDATE_REGULAR_PRICE_TAPPED)
        triggerEvent(
            OpenVariationsBulkUpdatePrice(PriceUpdateData(args.variationsToUpdate.toList(), PriceType.Regular))
        )
    }

    fun onSalePriceUpdateClicked() {
        track(PRODUCT_VARIANTS_BULK_UPDATE_SALE_PRICE_TAPPED)
        triggerEvent(
            OpenVariationsBulkUpdatePrice(PriceUpdateData(args.variationsToUpdate.toList(), PriceType.Sale))
        )
    }

    data class ViewState(
        val currency: String? = null,
        val regularPriceGroupType: ValuesGroupType = ValuesGroupType.None,
        val salePriceGroupType: ValuesGroupType = ValuesGroupType.None,
    )

    data class OpenVariationsBulkUpdatePrice(
        val data: PriceUpdateData
    ) : Event()
}
