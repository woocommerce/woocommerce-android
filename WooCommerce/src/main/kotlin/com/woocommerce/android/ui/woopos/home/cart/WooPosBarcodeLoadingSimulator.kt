package com.woocommerce.android.ui.woopos.home.cart

import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper
import com.woocommerce.android.ui.woopos.common.data.WooPosSearchProductByIdentifier
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
    private val searchProductByIdentifier: WooPosSearchProductByIdentifier,
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
            val barcodeValue = "00${Random.nextInt(10000000, 99999999)}${Random.nextInt(10000, 99999)}"
            
            searchAndAddProductToCart(
                barcodeValue = barcodeValue,
                barcodeFormat = GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN13,
                itemNumber = itemNumber,
                state = state,
                scope = scope
            )
        }
    }

    fun searchAndAddProductToCart(
        barcodeValue: String,
        barcodeFormat: GoogleBarcodeFormatMapper.BarcodeFormat,
        itemNumber: Int,
        state: MutableStateFlow<WooPosCartState>,
        scope: CoroutineScope
    ) {
        state.update { currentState ->
            val currentBody = currentState.body
            val newItems = when (currentBody) {
                is WooPosCartState.Body.Empty -> {
                    listOf(
                        WooPosCartItemViewState.Loading(
                            itemNumber = itemNumber,
                            name = barcodeValue
                        )
                    )
                }

                is WooPosCartState.Body.WithItems -> {
                    listOf(
                        WooPosCartItemViewState.Loading(
                            itemNumber = itemNumber,
                            name = barcodeValue
                        )
                    ) + currentBody.itemsInCart
                }
            }
            currentState.copy(
                body = WooPosCartState.Body.WithItems(newItems)
            )
        }

        scope.launch {
            val result = searchProductByIdentifier(barcodeValue, barcodeFormat)

            state.update { currentState ->
                val body = currentState.body
                if (body is WooPosCartState.Body.WithItems) {
                    val updatedItems = body.itemsInCart.map { item ->
                        if (item is WooPosCartItemViewState.Loading && item.itemNumber == itemNumber) {
                            if (result.isSuccess) {
                                val product = result.getOrThrow()
                                WooPosCartItemViewState.Product.Simple(
                                    itemNumber = item.itemNumber,
                                    id = product.remoteId,
                                    name = product.name,
                                    description = null,
                                    price = formatPrice(product.price),
                                    imageUrl = product.firstImageUrl
                                )
                            } else {
                                WooPosCartItemViewState.Error(
                                    itemNumber = item.itemNumber,
                                    name = item.name,
                                    message = "Product not found"
                                )
                            }
                        } else {
                            item
                        }
                    }
                    currentState.copy(
                        body = WooPosCartState.Body.WithItems(updatedItems)
                    )
                } else {
                    currentState
                }
            }
        }
    }
}
