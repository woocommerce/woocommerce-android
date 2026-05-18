package com.woocommerce.android.ui.woopos.home.cart

import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import javax.inject.Inject

class WooPosCustomAmountCartHandler @Inject constructor() {

    fun applySubmittedToCart(
        event: ParentToChildrenEvent.CustomAmountSubmitted,
        formattedAmount: String,
        nextItemNumber: () -> Int,
    ): SubmittedResult {
        return if (event.editingItemNumber != null) {
            SubmittedResult.Edited(
                editingItemNumber = event.editingItemNumber,
                updatedItem = WooPosCartItemViewState.CustomAmount(
                    itemNumber = event.editingItemNumber,
                    name = event.name,
                    amount = event.amount,
                    formattedAmount = formattedAmount,
                    isTaxable = event.isTaxable,
                ),
            )
        } else {
            SubmittedResult.Added(
                newItem = WooPosCartItemViewState.CustomAmount(
                    itemNumber = nextItemNumber(),
                    name = event.name,
                    amount = event.amount,
                    formattedAmount = formattedAmount,
                    isTaxable = event.isTaxable,
                ),
            )
        }
    }

    sealed class SubmittedResult {
        data class Edited(
            val editingItemNumber: Int,
            val updatedItem: WooPosCartItemViewState.CustomAmount,
        ) : SubmittedResult()

        data class Added(val newItem: WooPosCartItemViewState.CustomAmount) : SubmittedResult()
    }
}
