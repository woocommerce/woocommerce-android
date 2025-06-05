package com.woocommerce.android.ui.woopos.home.cart

import com.woocommerce.android.ui.woopos.common.data.searchbyidentifier.WooPosSearchByIdentifier
import com.woocommerce.android.ui.woopos.common.data.searchbyidentifier.WooPosSearchByIdentifierResult
import com.woocommerce.android.ui.woopos.featureflags.WooPosIsBarcodesScanningFeatureFlagEnabled
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.random.Random

class WooPosBarcodeLoadingSimulator @Inject constructor(
    private val scanningFeatureFlagEnabled: WooPosIsBarcodesScanningFeatureFlagEnabled,
    private val searchProductByIdentifier: WooPosSearchByIdentifier,
    private val formatPrice: WooPosFormatPrice
) {
    private val itemNumberCounter = AtomicInteger(1000)

    @Suppress("MagicNumber", "LongMethod")
    fun maybeSimulateLoadingItem(
        state: MutableStateFlow<WooPosCartState>,
        scope: CoroutineScope
    ) {
        if (!scanningFeatureFlagEnabled()) return

        if (Random.nextBoolean()) {
            val itemNumber = itemNumberCounter.getAndIncrement()
            val randomValue = "00${Random.nextInt(10000000, 99999999)}${Random.nextInt(10000, 99999)}"
            val realBarcodes = listOf(
                "00012345678905",
                "00012345678906",
                "heavy-duty-steel-car-62383189"
            )
            searchAndAddProductToCartFromHIDScanner(
                barcodeValue = if (Random.nextBoolean()) {
                    realBarcodes.random()
                } else {
                    randomValue
                },
                itemNumber = itemNumber,
                state = state,
                scope = scope
            )
        }
    }

    fun searchAndAddProductToCartFromHIDScanner(
        barcodeValue: String,
        itemNumber: Int,
        state: MutableStateFlow<WooPosCartState>,
        scope: CoroutineScope
    ) {
        scope.launch {
            addLoadingItemToCart(barcodeValue, itemNumber, state)
            val result = searchProductByIdentifier(barcodeValue)
            updateCartWithSearchResult(result, itemNumber, state)
        }
    }

    private fun addLoadingItemToCart(
        barcodeValue: String,
        itemNumber: Int,
        state: MutableStateFlow<WooPosCartState>
    ) {
        state.update { currentState ->
            val loadingItem = WooPosCartItemViewState.Loading(
                itemNumber = itemNumber,
                name = barcodeValue
            )
            val newItems = when (val currentBody = currentState.body) {
                is WooPosCartState.Body.Empty -> listOf(loadingItem)
                is WooPosCartState.Body.WithItems -> listOf(loadingItem) + currentBody.itemsInCart
            }
            currentState.copy(body = WooPosCartState.Body.WithItems(newItems))
        }
    }

    private suspend fun updateCartWithSearchResult(
        result: WooPosSearchByIdentifierResult,
        itemNumber: Int,
        state: MutableStateFlow<WooPosCartState>
    ) {
        val loadingItem = state.value.body.let { body ->
            if (body is WooPosCartState.Body.WithItems) {
                body.itemsInCart.firstOrNull {
                    it is WooPosCartItemViewState.Loading && it.itemNumber == itemNumber
                } as? WooPosCartItemViewState.Loading
            } else {
                null
            }
        }

        if (loadingItem != null) {
            val newCartItem = createCartItemFromSearchResult(result, loadingItem)

            state.update { currentState ->
                val body = currentState.body
                if (body is WooPosCartState.Body.WithItems) {
                    val updatedItems = body.itemsInCart.map { item ->
                        if (item is WooPosCartItemViewState.Loading && item.itemNumber == itemNumber) {
                            newCartItem
                        } else {
                            item
                        }
                    }
                    currentState.copy(body = WooPosCartState.Body.WithItems(updatedItems))
                } else {
                    currentState
                }
            }
        }
    }

    private suspend fun createCartItemFromSearchResult(
        result: WooPosSearchByIdentifierResult,
        loadingItem: WooPosCartItemViewState.Loading
    ): WooPosCartItemViewState {
        return when (result) {
            is WooPosSearchByIdentifierResult.Success -> {
                val product = result.product
                WooPosCartItemViewState.Product.Simple(
                    itemNumber = loadingItem.itemNumber,
                    id = product.remoteId,
                    name = product.name,
                    description = null,
                    price = formatPrice(product.price),
                    imageUrl = product.firstImageUrl
                )
            }
            is WooPosSearchByIdentifierResult.Failure -> {
                val errorMessage = when (result.error) {
                    WooPosSearchByIdentifierResult.Error.ProductNotFound -> "Product not found"
                    WooPosSearchByIdentifierResult.Error.NetworkError -> "Network error"
                    WooPosSearchByIdentifierResult.Error.RequestCancelled -> "Request cancelled"
                    is WooPosSearchByIdentifierResult.Error.UnknownError -> result.error.message
                }
                WooPosCartItemViewState.Error(
                    itemNumber = loadingItem.itemNumber,
                    name = loadingItem.name,
                    message = errorMessage
                )
            }
        }
    }
}
