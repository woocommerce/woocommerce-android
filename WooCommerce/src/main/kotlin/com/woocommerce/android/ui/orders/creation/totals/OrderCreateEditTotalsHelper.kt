package com.woocommerce.android.ui.orders.creation.totals

import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.TabletOrdersFeatureFlagWrapper
import com.woocommerce.android.viewmodel.ResourceProvider
import javax.inject.Inject

class OrderCreateEditTotalsHelper @Inject constructor(
    private val isTabletOrdersM1Enabled: TabletOrdersFeatureFlagWrapper,
    private val resourceProvider: ResourceProvider,
) {
    @Suppress("UNUSED_PARAMETER")
    fun mapToPaymentTotalsState(order: Order): TotalsSectionsState {
        return if (isTabletOrdersM1Enabled()) {
            TotalsSectionsState.Shown(
                button = TotalsSectionsState.Button(
                    text = resourceProvider.getString(R.string.order_creation_collect_payment_button),
                    onClick = {
                        // TODO
                    }
                )
            )
        } else {
            TotalsSectionsState.Hidden
        }
    }
}

sealed class TotalsSectionsState {
    data class Shown(
        val button: Button
    ) : TotalsSectionsState()

    object Hidden : TotalsSectionsState()

    data class Button(
        val text: String,
        val onClick: () -> Unit
    )
}
