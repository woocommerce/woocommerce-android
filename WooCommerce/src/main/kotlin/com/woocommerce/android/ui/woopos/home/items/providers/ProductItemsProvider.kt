package com.woocommerce.android.ui.woopos.home.items.providers

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

class ProductItemsProvider @Inject constructor(
    private val productsDataSource: WooPosProductsDataSource,
    private val priceFormat: WooPosFormatPrice,
) : WooPosItemDataProvider {
    private val _data = MutableStateFlow(DataProviderState.fetching(emptyList(), isPullToRefresh = false))
    override val data: Flow<DataProviderState> = _data

    override suspend fun init() {
        fetchItems(
            forceRefresh = false,
        )
    }

    override suspend fun fetchItems(
        forceRefresh: Boolean,
    ) {
        _data.value = DataProviderState.fetching(_data.value.items, isPullToRefresh = forceRefresh)

        productsDataSource.loadSimpleProducts(forceRefreshProducts = forceRefresh).collect { result ->
            when (result) {
                is WooPosProductsDataSource.ProductsResult.Cached -> {
                    _data.value = DataProviderState.fetching(
                        result.products.map { it.toItemSelectionViewState() },
                        isPullToRefresh = forceRefresh
                    )
                }

                is WooPosProductsDataSource.ProductsResult.Remote -> {
                    if (result.productsResult.isSuccess) {
                        val products = result.productsResult.getOrThrow()
                        _data.value = DataProviderState.dataShown(
                            products.map { it.toItemSelectionViewState() }
                        )
                    } else {
                        _data.value = DataProviderState.remoteRequestFailed(_data.value.items, "")
                    }
                }
            }
        }
    }

    override suspend fun loadMore() {
        _data.value = DataProviderState.loadingMore(_data.value.items)

        val result = productsDataSource.loadMore()
        _data.value = if (result.isSuccess) {
            DataProviderState.dataShown(result.getOrThrow().map { it.toItemSelectionViewState() })
        } else {
            DataProviderState.loadingMoreFailed(_data.value.items, "")
        }
    }

    private suspend fun Product.toItemSelectionViewState(): WooPosItemSelectionViewState {
        return if (this.isVariable()) {
            WooPosItemSelectionViewState.Product.Variable(
                id = this.remoteId,
                name = this.name,
                price = priceFormat(this.price),
                imageUrl = this.firstImageUrl,
                numOfVariations = this.numVariations,
                variationIds = this.variationIds
            )
        } else {
            WooPosItemSelectionViewState.Product.Simple(
                id = this.remoteId,
                name = this.name,
                price = priceFormat(this.price),
                imageUrl = this.firstImageUrl,
            )
        }
    }

    private fun Product.isVariable() =
        productType == ProductType.VARIABLE ||
            productType == ProductType.VARIATION
}
