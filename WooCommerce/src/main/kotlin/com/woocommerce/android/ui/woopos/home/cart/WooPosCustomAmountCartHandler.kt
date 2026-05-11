package com.woocommerce.android.ui.woopos.home.cart

import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import javax.inject.Inject

class WooPosCustomAmountCartHandler @Inject constructor(
    private val formatPrice: WooPosFormatPrice,
) {

    suspend fun applySubmittedToCart(
        currentBody: WooPosCartState.Body,
        event: ParentToChildrenEvent.CustomAmountSubmitted,
        nextItemNumber: () -> Int,
    ): SubmittedResult {
        val formattedAmount = formatPrice(event.amount)
        return if (event.editingItemNumber != null) {
            updateExisting(currentBody, event, formattedAmount)
        } else {
            createNewItem(event, formattedAmount, nextItemNumber)
        }
    }

    private fun updateExisting(
        currentBody: WooPosCartState.Body,
        event: ParentToChildrenEvent.CustomAmountSubmitted,
        formattedAmount: String,
    ): SubmittedResult.Edited {
        val items = (currentBody as? WooPosCartState.Body.WithItems)?.itemsInCart ?: emptyList()
        val updated = items.map { item ->
            if (item is WooPosCartItemViewState.CustomAmount &&
                item.itemNumber == event.editingItemNumber
            ) {
                item.copy(
                    name = event.name,
                    amount = event.amount,
                    formattedAmount = formattedAmount,
                    isTaxable = event.isTaxable,
                )
            } else {
                item
            }
        }
        return SubmittedResult.Edited(updated)
    }

    private fun createNewItem(
        event: ParentToChildrenEvent.CustomAmountSubmitted,
        formattedAmount: String,
        nextItemNumber: () -> Int,
    ): SubmittedResult.Added = SubmittedResult.Added(
        WooPosCartItemViewState.CustomAmount(
            itemNumber = nextItemNumber(),
            name = event.name,
            customAmountId = event.customAmountId,
            amount = event.amount,
            formattedAmount = formattedAmount,
            isTaxable = event.isTaxable,
        )
    )

    sealed class SubmittedResult {
        data class Edited(val updatedItems: List<WooPosCartItemViewState>) : SubmittedResult()
        data class Added(val newItem: WooPosCartItemViewState.CustomAmount) : SubmittedResult()
    }
}
