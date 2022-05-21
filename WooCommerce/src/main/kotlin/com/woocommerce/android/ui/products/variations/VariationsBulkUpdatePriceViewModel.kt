package com.woocommerce.android.ui.products.variations

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.LEFT
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.LEFT_SPACE
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class VariationsBulkUpdatePriceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    parameterRepository: ParameterRepository,
    private val variationRepository: VariationRepository,
    private val dispatchers: CoroutineDispatchers,
) : ScopedViewModel(savedStateHandle) {
    private val args: VariationsBulkUpdatePriceFragmentArgs by savedStateHandle.navArgs()
    private val data: PriceUpdateData = args.priceUpdateData
    private val variationsToUpdate: List<ProductVariation> = args.priceUpdateData.variationsToUpdate

    private val parameters: SiteParameters by lazy {
        parameterRepository.getParameters("key_product_parameters", savedState)
    }

    val viewStateData = LiveDataDelegate(savedState, ViewState(priceType = data.priceType))
    private var viewState by viewStateData

    init {
        viewState = viewState.copy(
            currencyPosition = parameters.currencyFormattingParameters?.currencyPosition,
            currency = parameters.currencySymbol,
            pricesGroupType = data.getPriceCollection().groupType(),
            priceType = data.priceType,
            variationsToUpdateCount = data.variationsToUpdate.size,
        )
    }

    fun onDoneClicked() {
        launch(dispatchers.io) {
            val productId = variationsToUpdate.first().remoteProductId
            val variationsIds = variationsToUpdate.map { it.remoteVariationId }
            // TODO: show progress bar
            val result = when (viewState.priceType) {
                PriceType.Regular -> variationRepository.bulkUpdateVariations(productId, variationsIds, viewState.price)
                PriceType.Sale -> variationRepository.bulkUpdateVariations(productId, variationsIds, viewState.price)
            }
            val snackText = if (result) {
                // TODO: use different text for sale price
                R.string.variations_bulk_update_regular_prices_success
            } else {
                R.string.variations_bulk_update_error
            }

            withContext(dispatchers.main) {
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(snackText))
                if (result) triggerEvent(MultiLiveEvent.Event.Exit)
            }
        }
    }

    fun onPriceEntered(price: BigDecimal?) {
        viewState = viewState.copy(price = price)
    }

    @Parcelize
    data class ViewState(
        val currency: String? = null,
        val price: BigDecimal? = null,
        val priceType: PriceType,
        val pricesGroupType: ValuesGroupType? = null,
        val variationsToUpdateCount: Int? = null,
        private val currencyPosition: WCSettingsModel.CurrencyPosition? = null,
    ) : Parcelable {
        val isCurrencyPrefix: Boolean
            get() = currencyPosition == LEFT || currencyPosition == LEFT_SPACE
    }

    @Parcelize
    data class PriceUpdateData(
        val variationsToUpdate: List<ProductVariation>,
        val priceType: PriceType,
    ) : Parcelable

    private fun PriceUpdateData.getPriceCollection(): Collection<BigDecimal?> {
        return when (priceType) {
            PriceType.Sale -> variationsToUpdate.map { it.salePrice }
            PriceType.Regular -> variationsToUpdate.map { it.regularPrice }
        }
    }

    enum class PriceType {
        Regular, Sale
    }
}
