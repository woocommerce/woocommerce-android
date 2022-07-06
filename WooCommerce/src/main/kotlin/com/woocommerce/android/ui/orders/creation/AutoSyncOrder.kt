package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.extensions.areSameAs
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.model.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class AutoSyncOrder @Inject constructor(val createUpdateOrderUseCase: CreateUpdateOrder) : SyncStrategy {
    private fun areEquivalent(old: Order, new: Order): Boolean {
        // Make sure to update the prices only when items did change
        val hasSameItems = old.items
            .filter {
                // Check only non-zero quantities, to avoid circular update when removing products
                it.quantity > 0
            }
            .areSameAs(new.items) { newItem ->
                // TODO M3: we need probably to compare the totals too, to account for discounts
                this.productId == newItem.productId &&
                    this.variationId == newItem.variationId &&
                    this.quantity == newItem.quantity
            }

        val hasSameShippingLines = old.shippingLines
            .filter {
                // Check only non-removed shipping lines to avoid circular update when removing
                it.methodId != null
            }
            .areSameAs(new.shippingLines) { newLine ->
                this.methodId == newLine.methodId &&
                    this.methodTitle == newLine.methodTitle &&
                    this.total isEqualTo newLine.total
            }

        val hasSameFeeLines = old.feesLines
            .filter { it.name != null }
            .areSameAs(new.feesLines) { newLine ->
                this.name == newLine.name &&
                    this.total isEqualTo newLine.total
            }

        val hasSameStatus = old.status == new.status

        val hasSameCustomerInfo = old.billingAddress == new.billingAddress &&
            old.shippingAddress == new.shippingAddress &&
            old.customerNote == new.customerNote &&
            old.shippingPhone == new.shippingPhone

        return hasSameItems &&
            hasSameShippingLines &&
            hasSameFeeLines &&
            hasSameStatus &&
            hasSameCustomerInfo
    }

    override fun syncOrderChanges(
        changes: Flow<Order>,
        retryTrigger: Flow<Unit>
    ): Flow<CreateUpdateOrder.OrderUpdateStatus> {
        return createUpdateOrderUseCase(
            changes.distinctUntilChanged(::areEquivalent),
            retryTrigger
        )
    }
}
