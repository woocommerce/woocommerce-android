package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.extensions.areSameAs
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.model.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class AutoSyncOrder @Inject constructor(val createUpdateOrderUseCase: CreateUpdateOrder) : SyncStrategy {
    @Suppress("ComplexMethod")
    private fun areEquivalent(old: Order, new: Order): Boolean {
        // Make sure to update the prices only when items did change
        val hasSameItems = old.items
            .filter {
                // Check only non-zero quantities, to avoid circular update when removing products
                it.quantity > 0
            }
            .areSameAs(new.items) { newItem ->
                this.productId == newItem.productId &&
                    this.variationId == newItem.variationId &&
                    this.quantity == newItem.quantity &&
                    this.total == newItem.total
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
                    this.total isEqualTo newLine.total &&
                    this.taxStatus == newLine.taxStatus
                // Verify the tax status for custom amounts to ensure accuracy.
                // The final total amount may vary as users have the option to modify custom amounts
                // by altering the tax status. This check is crucial to prevent discrepancies
                // in tax calculations and maintain consistency in the user's entries.
            }

        val hasSameCouponLines = old.couponLines.areSameAs(new.couponLines) { newLine ->
            this.code == newLine.code
        }

        val hasSameStatus = old.status == new.status

        val hasSameCustomerInfo = old.billingAddress == new.billingAddress &&
            old.shippingAddress == new.shippingAddress &&
            old.customerNote == new.customerNote &&
            old.shippingPhone == new.shippingPhone

        return hasSameItems &&
            hasSameShippingLines &&
            hasSameFeeLines &&
            hasSameCouponLines &&
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
