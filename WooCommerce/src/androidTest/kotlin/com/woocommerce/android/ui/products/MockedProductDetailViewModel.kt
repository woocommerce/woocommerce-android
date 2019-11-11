package com.woocommerce.android.ui.products

import com.woocommerce.android.viewmodel.SavedState
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.LiveDataDelegate
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import kotlin.math.roundToInt

final class MockedProductDetailViewModel @AssistedInject constructor(
    dispatchers: CoroutineDispatchers,
    wooCommerceStore: WooCommerceStore,
    selectedSite: SelectedSite,
    productRepository: ProductDetailRepository,
    networkStatus: NetworkStatus,
    private val currencyFormatter: CurrencyFormatter,
    @Assisted val arg0: SavedState
) : ProductDetailViewModel(
        arg0,
        dispatchers,
        selectedSite,
        productRepository,
        networkStatus,
        currencyFormatter,
        wooCommerceStore
) {
    override val viewStateData: LiveDataDelegate<ViewState> =
            LiveDataDelegate(arg0, ViewState(), "", onChange = {
                combineData(it.product!!, Parameters("$", "oz", "in"))
            })

    private fun combineData(product: Product, parameters: Parameters): ViewState {
        val weight = if (product.weight > 0) "${product.weight.roundToInt()}${parameters.weightUnit ?: ""}" else ""

        val hasLength = product.length > 0
        val hasWidth = product.width > 0
        val hasHeight = product.height > 0
        val unit = parameters.dimensionUnit ?: ""
        val size = if (hasLength && hasWidth && hasHeight) {
            "${product.length.roundToInt()} x ${product.width.roundToInt()} x ${product.height.roundToInt()} $unit"
        } else if (hasWidth && hasHeight) {
            "${product.width.roundToInt()} x ${product.height.roundToInt()} $unit"
        } else {
            ""
        }.trim()

        return ViewState(
                product,
                weight,
                size,
                formatCurrency(product.price, parameters.currencyCode),
                formatCurrency(product.salePrice, parameters.currencyCode),
                formatCurrency(product.regularPrice, parameters.currencyCode)
        )
    }

    private fun formatCurrency(amount: BigDecimal?, currencyCode: String?): String {
        return currencyCode?.let {
            currencyFormatter.formatCurrency(amount ?: BigDecimal.ZERO, it)
        } ?: amount.toString()
    }

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<MockedProductDetailViewModel>
}
