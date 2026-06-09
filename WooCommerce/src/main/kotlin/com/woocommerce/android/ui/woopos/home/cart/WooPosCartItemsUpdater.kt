package com.woocommerce.android.ui.woopos.home.cart

import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.home.WooPosOrderCreatedData
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState.Coupon.CouponValidationState
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import javax.inject.Inject

class WooPosCartItemsUpdater @Inject constructor(
    private val formatPrice: WooPosFormatPrice,
    private val productsCache: WooPosProductsCache,
    private val localCatalogStore: WooPosLocalCatalogStore,
    private val selectedSite: SelectedSite,
    private val wooPosLogWrapper: WooPosLogWrapper,
    private val crashLogger: CrashLogging,
) {
    suspend operator fun invoke(
        itemsInCart: List<WooPosCartItemViewState>,
        updatedProducts: List<WooPosOrderCreatedData.ProductInfo>,
        updatedCoupons: List<WooPosOrderCreatedData.CouponInfo>,
    ): CartItemsUpdaterResult {
        val mutableCurrentBodyList = itemsInCart.toMutableList()
        var productsChanged = false
        var couponsChanged = false

        val availableProductsMap = createAvailableProductsMap(updatedProducts)

        itemsInCart.forEachIndexed { index, item ->
            when (item) {
                is WooPosCartItemViewState.Product -> {
                    val result = processProduct(item, availableProductsMap, updatedProducts)
                    mutableCurrentBodyList[index] = result.updatedItem
                    productsChanged = productsChanged || result.changed
                }

                is WooPosCartItemViewState.Coupon -> {
                    couponsChanged = true
                    mutableCurrentBodyList[index] = updateCouponsWithFormattedDiscount(updatedCoupons, item)
                }

                is WooPosCartItemViewState.CustomAmount -> Unit
                is WooPosCartItemViewState.Error -> error("unsupported item $item")
                is WooPosCartItemViewState.Loading -> error("unsupported item $item")
            }
        }

        return CartItemsUpdaterResult(
            updatedItems = mutableCurrentBodyList,
            productsChanged = productsChanged,
            couponsChanged = couponsChanged,
        )
    }

    private suspend fun processProduct(
        item: WooPosCartItemViewState.Product,
        availableProductsMap: MutableMap<String, Int>,
        updatedProducts: List<WooPosOrderCreatedData.ProductInfo>
    ): ProductProcessResult {
        val productKey = getProductKey(item)
        val availableQuantity = availableProductsMap[productKey] ?: 0
        var changed = false

        val updatedItem = if (availableQuantity > 0) {
            availableProductsMap[productKey] = availableQuantity - 1
            val updatedProduct = findMatchingProduct(item, updatedProducts)

            if (updatedProduct != null) {
                val newItem = updateProductWithNewInfo(item, updatedProduct)
                val itemChanged = newItem.name != item.name || newItem.price != item.price

                if (itemChanged) {
                    updateProductInCache(newItem, updatedProduct)
                }

                changed = itemChanged
                newItem
            } else {
                item
            }
        } else {
            val newItem = markProductAsNotExisting(item)
            val itemChanged = newItem.name != item.name ||
                newItem.price != item.price ||
                newItem.productDoesNotExist != item.productDoesNotExist

            if (itemChanged) {
                deleteProductFromCache(newItem)
            }

            changed = itemChanged
            newItem
        }

        return ProductProcessResult(updatedItem, changed)
    }

    private suspend fun updateCouponsWithFormattedDiscount(
        updatedCoupons: List<WooPosOrderCreatedData.CouponInfo>,
        item: WooPosCartItemViewState.Coupon,
    ) = updatedCoupons.find { it.code == item.name }?.let {
        item.copy(validationState = CouponValidationState.Valid("-${formatPrice(it.discountAmount)}"))
    } ?: item.also {
        val message = "Coupon not found in the cart"
        wooPosLogWrapper.e(message)
        crashLogger.sendReport(IllegalStateException(message))
    }

    private suspend fun updateProductInCache(
        updatedItem: WooPosCartItemViewState.Product,
        updatedProduct: WooPosOrderCreatedData.ProductInfo
    ) {
        // TBD Local Catalog The app should update cache based on the data coming from backend not from the view layer
        productsCache.getProductById(updatedItem.id)?.let { product ->
            productsCache.updateProduct(
                product.copy(
                    name = updatedItem.name,
                    pricing = when (product.pricing) {
                        WooPosProductModel.WooPosPricing.NoPricing ->
                            WooPosProductModel.WooPosPricing.NoPricing

                        is WooPosProductModel.WooPosPricing.RegularPricing ->
                            WooPosProductModel.WooPosPricing.RegularPricing(
                                updatedProduct.subtotalPricePerItem()
                            )

                        is WooPosProductModel.WooPosPricing.SalePricing ->
                            WooPosProductModel.WooPosPricing.SalePricing(
                                product.pricing.regularPrice,
                                updatedProduct.subtotalPricePerItem()
                            )
                    }
                )
            )
        }
    }

    private suspend fun deleteProductFromCache(item: WooPosCartItemViewState.Product) {
        when (item) {
            is WooPosCartItemViewState.Product.Simple -> {
                productsCache.deleteProduct(item.id)
                selectedSite.getOrNull()?.let { site ->
                    val siteId = LocalOrRemoteId.LocalId(site.id)
                    val remoteId = LocalOrRemoteId.RemoteId(item.id)
                    localCatalogStore.deleteProducts(siteId, listOf(remoteId))
                        .onFailure { error ->
                            wooPosLogWrapper.e("Failed to delete product from Local Catalog DB: ${error.message}")
                        }
                }
            }
            is WooPosCartItemViewState.Product.Variation -> {
                productsCache.deleteProduct(item.id)
                selectedSite.getOrNull()?.let { site ->
                    val siteId = LocalOrRemoteId.LocalId(site.id)
                    val productId = LocalOrRemoteId.RemoteId(item.id)
                    val variationId = LocalOrRemoteId.RemoteId(item.variationId)
                    localCatalogStore.deleteVariations(siteId, listOf(productId to variationId))
                        .onFailure { error ->
                            wooPosLogWrapper.e("Failed to delete variation from Local Catalog DB: ${error.message}")
                        }
                }
            }
        }
    }

    private fun getProductKey(item: WooPosCartItemViewState.Product): String {
        return when (item) {
            is WooPosCartItemViewState.Product.Simple -> "simple_${item.id}"
            is WooPosCartItemViewState.Product.Variation -> "variation_${item.id}_${item.variationId}"
        }
    }

    private fun findMatchingProduct(
        item: WooPosCartItemViewState.Product,
        updatedProducts: List<WooPosOrderCreatedData.ProductInfo>
    ): WooPosOrderCreatedData.ProductInfo? {
        return when (item) {
            is WooPosCartItemViewState.Product.Simple ->
                updatedProducts.find {
                    it is WooPosOrderCreatedData.ProductInfo.Simple && it.id == item.id
                }

            is WooPosCartItemViewState.Product.Variation ->
                updatedProducts.find { product ->
                    product is WooPosOrderCreatedData.ProductInfo.Variation &&
                        product.id == item.id &&
                        product.variationId == item.variationId
                }
        }
    }

    private fun createAvailableProductsMap(
        updatedProducts: List<WooPosOrderCreatedData.ProductInfo>
    ): MutableMap<String, Int> {
        val productMap = mutableMapOf<String, Int>()

        updatedProducts.forEach { product ->
            val key = when (product) {
                is WooPosOrderCreatedData.ProductInfo.Simple -> "simple_${product.id}"
                is WooPosOrderCreatedData.ProductInfo.Variation ->
                    "variation_${product.id}_${product.variationId}"
            }
            val currentQuantity = productMap[key] ?: 0
            productMap[key] = currentQuantity + product.quantity.toInt()
        }

        return productMap
    }

    private suspend fun updateProductWithNewInfo(
        item: WooPosCartItemViewState.Product,
        updatedProduct: WooPosOrderCreatedData.ProductInfo
    ): WooPosCartItemViewState.Product {
        return when (item) {
            is WooPosCartItemViewState.Product.Simple -> {
                if (updatedProduct is WooPosOrderCreatedData.ProductInfo.Simple) {
                    item.copy(
                        name = updatedProduct.name,
                        price = formatPrice(updatedProduct.subtotalPricePerItem()),
                    )
                } else {
                    item
                }
            }

            is WooPosCartItemViewState.Product.Variation -> {
                if (updatedProduct is WooPosOrderCreatedData.ProductInfo.Variation) {
                    item.copy(
                        name = updatedProduct.name,
                        price = formatPrice(updatedProduct.subtotalPricePerItem())
                    )
                } else {
                    item
                }
            }
        }
    }

    private fun markProductAsNotExisting(
        item: WooPosCartItemViewState.Product
    ): WooPosCartItemViewState.Product {
        return when (item) {
            is WooPosCartItemViewState.Product.Simple -> item.copy(productDoesNotExist = true)
            is WooPosCartItemViewState.Product.Variation -> item.copy(productDoesNotExist = true)
        }
    }

    private fun WooPosOrderCreatedData.ProductInfo.subtotalPricePerItem() =
        basePrice.div(quantity.toBigDecimal())

    data class CartItemsUpdaterResult(
        val updatedItems: List<WooPosCartItemViewState>,
        val productsChanged: Boolean,
        val couponsChanged: Boolean,
    )

    private data class ProductProcessResult(
        val updatedItem: WooPosCartItemViewState.Product,
        val changed: Boolean
    )
}
