package com.woocommerce.android.ui.products.variations

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_VARIANTS_BULK_UPDATE_REGULAR_PRICE_DONE_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_VARIANTS_BULK_UPDATE_SALE_PRICE_DONE_TAPPED
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class VariationsBulkUpdatePriceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    parameterRepository: ParameterRepository,
    private val variationRepository: VariationRepository,
    dispatchers: CoroutineDispatchers,
) : VariationsBulkUpdateBaseViewModel(savedStateHandle, dispatchers) {

    private val args: VariationsBulkUpdatePriceFragmentArgs by savedStateHandle.navArgs()
    private val data: PriceUpdateData = args.priceUpdateData
    private val variationsToUpdate: List<ProductVariation> = args.priceUpdateData.variationsToUpdate

    private val parameters: SiteParameters by lazy {
        parameterRepository.getParameters("key_product_parameters", savedState)
    }

    /**
     * Saving more data than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can replace @Suppress("OPT_IN_USAGE")
     * with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateData = LiveDataDelegate(savedState, ViewState(priceType = data.priceType))
    private var viewState: ViewState by viewStateData

    init {
        viewState = viewState.copy(
            currency = parameters.currencySymbol,
            pricesGroupType = data.getPriceCollection().groupType(),
            priceType = data.priceType,
            variationsToUpdateCount = data.variationsToUpdate.size,
        )
    }

    fun onPriceEntered(price: String) {
        viewState = viewState.copy(price = price)
    }

    override fun getDoneClickedAnalyticsEvent() = when (data.priceType) {
        PriceType.Regular -> PRODUCT_VARIANTS_BULK_UPDATE_REGULAR_PRICE_DONE_TAPPED
        PriceType.Sale -> PRODUCT_VARIANTS_BULK_UPDATE_SALE_PRICE_DONE_TAPPED
    }

    override fun getSnackbarSuccessMessageTextRes(): Int = when (viewState.priceType) {
        PriceType.Regular -> R.string.variations_bulk_update_regular_prices_success
        PriceType.Sale -> R.string.variations_bulk_update_sale_prices_success
    }

    override suspend fun performBulkUpdate(): Boolean {
        val productId = variationsToUpdate.first().remoteProductId
        val variationsIds = variationsToUpdate.map { it.remoteVariationId }
        val result = when (viewState.priceType) {
            PriceType.Regular -> variationRepository.bulkUpdateVariations(
                productId,
                variationsIds,
                newRegularPrice = viewState.price ?: ""
            )
            PriceType.Sale -> variationRepository.bulkUpdateVariations(
                productId,
                variationsIds,
                newSalePrice = viewState.price ?: ""
            )
        }
        return result
    }

    @Parcelize
    data class ViewState(
        val currency: String? = null,
        val price: String? = null,
        val priceType: PriceType,
        val pricesGroupType: ValuesGroupType? = null,
        val variationsToUpdateCount: Int? = null,
    ) : Parcelable

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
