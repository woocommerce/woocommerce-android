package com.woocommerce.android.ui.woopos.home.cart

import com.woocommerce.android.ui.woopos.featureflags.WooPosIsBarcodesScanningFeatureFlagEnabled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.random.Random

class WooPosBarcodeLoadingSimulator @Inject constructor(
    private val scanningFeatureFlagEnabled: WooPosIsBarcodesScanningFeatureFlagEnabled
) {
    private val itemNumberCounter = AtomicInteger(1000)

    fun maybeSimulateLoadingItem(
        state: MutableStateFlow<WooPosCartState>,
        scope: CoroutineScope
    ) {
        if (!scanningFeatureFlagEnabled()) return

        if (Random.nextBoolean()) {
            val itemNumber = itemNumberCounter.getAndIncrement()
            val loadingItemName = "00${Random.nextInt(10000000, 99999999)}${Random.nextInt(10000, 99999)}"

            state.update { currentState ->
                val currentBody = currentState.body
                val newItems = when (currentBody) {
                    is WooPosCartState.Body.Empty -> {
                        listOf(WooPosCartItemViewState.Loading(
                            itemNumber = itemNumber,
                            name = loadingItemName
                        ))
                    }
                    is WooPosCartState.Body.WithItems -> {
                        currentBody.itemsInCart + WooPosCartItemViewState.Loading(
                            itemNumber = itemNumber,
                            name = loadingItemName
                        )
                    }
                }
                currentState.copy(
                    body = WooPosCartState.Body.WithItems(newItems)
                )
            }

            scope.launch {
                delay(3000)

                state.update { currentState ->
                    val body = currentState.body
                    if (body is WooPosCartState.Body.WithItems) {
                        val updatedItems = body.itemsInCart.map { item ->
                            if (item is WooPosCartItemViewState.Loading && item.itemNumber == itemNumber) {
                                WooPosCartItemViewState.Error(
                                    itemNumber = item.itemNumber,
                                    name = item.name,
                                    message = "Product not found"
                                )
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

                delay(3000)

                state.update { currentState ->
                    val body = currentState.body
                    if (body is WooPosCartState.Body.WithItems) {
                        val remainingItems = body.itemsInCart.filter { item ->
                            !(item is WooPosCartItemViewState.Error && item.itemNumber == itemNumber)
                        }
                        if (remainingItems.isEmpty()) {
                            currentState.copy(body = WooPosCartState.Body.Empty)
                        } else {
                            currentState.copy(
                                body = WooPosCartState.Body.WithItems(remainingItems)
                            )
                        }
                    } else {
                        currentState
                    }
                }
            }
        }
    }
}
