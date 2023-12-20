package com.woocommerce.android.ui.orders.creation.totals

import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.TabletOrdersFeatureFlagWrapper
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.viewmodel.ResourceProvider
import javax.inject.Inject

class OrderCreateEditTotalsHelper @Inject constructor(
    private val isTabletOrdersM1Enabled: TabletOrdersFeatureFlagWrapper,
    private val resourceProvider: ResourceProvider,
) {
    fun mapToPaymentTotalsState(
        mode: OrderCreateEditViewModel.Mode,
        order: Order,
        onButtonClicked: () -> Unit
    ): TotalsSectionsState {
        return if (isTabletOrdersM1Enabled()) {
            if (order.items.isEmpty() && order.feesLines.isEmpty()) {
                TotalsSectionsState.Hidden
            } else {
                TotalsSectionsState.Shown(
                    button = TotalsSectionsState.Button(
                        text = mode.toButtonText(),
                        onClick = onButtonClicked
                    )
                )
            }
        } else {
            TotalsSectionsState.Disabled
        }
    }

    private fun OrderCreateEditViewModel.Mode.toButtonText() =
        when (this) {
            OrderCreateEditViewModel.Mode.Creation -> resourceProvider.getString(
                R.string.order_creation_collect_payment_button
            )
            is OrderCreateEditViewModel.Mode.Edit -> resourceProvider.getString(R.string.save)
        }
}

sealed class TotalsSectionsState {
    data class Shown(
        val button: Button
    ) : TotalsSectionsState()

    object Hidden : TotalsSectionsState()

    object Disabled : TotalsSectionsState()

    data class Button(
        val text: String,
        val onClick: () -> Unit
    )
}
