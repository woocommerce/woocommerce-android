package com.woocommerce.android.ui.orders.creation.product.details

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.MapItemToProductUiModel
import com.woocommerce.android.ui.orders.creation.ProductUIModel
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.getStockText
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize
import org.wordpress.android.util.HtmlUtils
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class OrderCreateEditProductDetailsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val mapItemToProductUiModel: MapItemToProductUiModel,
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter

) : ScopedViewModel(savedState) {
    private val args = OrderCreateEditProductDetailsFragmentArgs.fromSavedStateHandle(savedState)

    private val currency = args.currency

    private val item: MutableStateFlow<Order.Item> = savedState.getStateFlow(viewModelScope, args.item, "key_item")

    val viewState = item.map { item ->
        val uiModel: ProductUIModel = mapItemToProductUiModel(item)
        uiModel.item.discount
        ViewState(
            productDetailsState = ProductDetailsState(
                title = getProductTitle(uiModel.item.name),
                stockPriceSubtitle = getStockPriceSubtitle(uiModel),
                skuSubtitle = getSkuSubtitle(uiModel.item.sku),
                imageUrl = uiModel.imageUrl,
            ),
            discountSectionState = DiscountSectionState(
                isVisible = item.isDiscounted(),
                discountAmountText = getDiscountAmountText(uiModel.item),
            )
        )
    }.asLiveData()

    private fun getDiscountAmountText(item: Order.Item): String =
        currencyFormatter.formatCurrency(item.discount, currency)

    private fun Order.Item.isDiscounted(): Boolean = discount > BigDecimal.ZERO

    private fun getStockPriceSubtitle(item: ProductUIModel): String {
        val decimalFormatter = getDecimalFormatter(currencyFormatter, args.currency)
        return buildString {
            if (item.item.isVariation && item.item.attributesDescription.isNotEmpty()) {
                append(item.item.attributesDescription)
            } else {
                append(item.getStockText(resourceProvider))
            }
            append(" \u2022 ")
            append(decimalFormatter(item.item.total).replace(" ", "\u00A0"))
        }
    }

    private fun getDecimalFormatter(
        currencyFormatter: CurrencyFormatter,
        currencyCode: String? = null
    ): (BigDecimal) -> String {
        return currencyCode?.let {
            currencyFormatter.buildBigDecimalFormatter(it)
        } ?: currencyFormatter.buildBigDecimalFormatter()
    }

    private fun getSkuSubtitle(sku: String): String =
        resourceProvider.getString(
            R.string.orderdetail_product_lineitem_sku_value,
            sku
        )

    private fun getProductTitle(productName: String): String =
        if (productName.isEmpty()) {
            productName
        } else {
            HtmlUtils.fastStripHtml(productName)
        }

    fun onAddDiscountClicked() {
        triggerEvent(NavigationTarget.DiscountCreate)
    }

    fun onEditDiscountClicked() {
        triggerEvent(NavigationTarget.DiscountEdit(item.value.discount))
    }

    fun onCloseClicked() {
        if (args.item != this.item.value) {
            triggerEvent(NavigationTarget.Back(ProductDetailsEditResult.ProductDetailsEdited(this.item.value)))
        } else {
            triggerEvent(MultiLiveEvent.Event.Exit)
        }
    }

    fun onProductRemoved() {
        triggerEvent(NavigationTarget.Back(ProductDetailsEditResult.ProductRemoved(this.item.value)))
    }

    data class ViewState(
        val productDetailsState: ProductDetailsState,
        val discountSectionState: DiscountSectionState,
        val addDiscountButtonVisible: Boolean = !discountSectionState.isVisible
    )

    data class ProductDetailsState(
        val title: String,
        val stockPriceSubtitle: String,
        val skuSubtitle: String,
        val imageUrl: String,
    )

    data class DiscountSectionState(
        val isVisible: Boolean,
        val discountAmountText: String,
    )

    @Parcelize
    sealed class ProductDetailsEditResult : Parcelable {
        data class ProductDetailsEdited(val changes: Order.Item) : Parcelable, ProductDetailsEditResult()

        data class ProductRemoved(val item: Order.Item) : Parcelable, ProductDetailsEditResult()
    }

    sealed class NavigationTarget : MultiLiveEvent.Event() {
        data class Back(
            val productDetailsEditResult: ProductDetailsEditResult
        ) : MultiLiveEvent.Event()

        data class DiscountEdit(val discountAmount: BigDecimal) : MultiLiveEvent.Event()
        object DiscountCreate : MultiLiveEvent.Event()
    }
}
