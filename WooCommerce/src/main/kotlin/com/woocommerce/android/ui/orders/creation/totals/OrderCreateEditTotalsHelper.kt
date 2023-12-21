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
                    lines = listOf(
                        TotalsSectionsState.Line.Simple(
                            label = resourceProvider.getString(R.string.order_creation_payment_products),
                            value = "$125.00"
                        ),
                        TotalsSectionsState.Line.Simple(
                            label = resourceProvider.getString(R.string.custom_amounts),
                            value = "$2.00"
                        ),
                        TotalsSectionsState.Line.Button(
                            text = resourceProvider.getString(R.string.shipping),
                            value = "$16.25",
                            onClick = {},
                        ),
                        TotalsSectionsState.Line.Button(
                            text = resourceProvider.getString(R.string.order_creation_coupon_button),
                            value = "-$4.25",
                            extraValue = "20 OFF",
                            onClick = {},
                        ),
                        TotalsSectionsState.Line.Button(
                            text = resourceProvider.getString(R.string.order_gift_card),
                            value = "-$4.25",
                            extraValue = "1234-5678-9987-6543",
                            onClick = {},
                        ),
                        TotalsSectionsState.Line.Simple(
                            label = resourceProvider.getString(R.string.order_creation_payment_tax_label),
                            value = "$15.33"
                        ),
                        TotalsSectionsState.Line.SimpleSmall(
                            label = "Government Sales Tax · 10%",
                            value = "$12.50"
                        ),
                        TotalsSectionsState.Line.SimpleSmall(
                            label = "State Tax · 5%",
                            value = "$6.25"
                        ),
                        TotalsSectionsState.Line.LearnMore(
                            text = resourceProvider.getString(R.string.order_creation_tax_based_on_billing_address),
                            buttonText = resourceProvider.getString(R.string.learn_more),
                            onClick = {}
                        ),
                    ),
                    orderTotal = TotalsSectionsState.OrderTotal(
                        label = resourceProvider.getString(R.string.order_creation_payment_order_total),
                        value = "$143.75"
                    ),
                    mainButton = TotalsSectionsState.Button(
                        text = mode.toButtonText(),
                        enabled = true,
                        onClick = onButtonClicked,
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
        val lines: List<Line>,
        val orderTotal: OrderTotal,
        val mainButton: Button,
    ) : TotalsSectionsState()

    object Hidden : TotalsSectionsState()

    object Disabled : TotalsSectionsState()

    data class Button(
        val text: String,
        val enabled: Boolean,
        val onClick: () -> Unit
    )

    data class OrderTotal(
        val label: String,
        val value: String,
    )

    sealed class Line {
        data class Simple(
            val label: String,
            val value: String,
        ) : Line()

        data class SimpleSmall(val label: String, val value: String) : Line()

        data class Button(
            val text: String,
            val value: String,
            val extraValue: String? = null,
            val enabled: Boolean = true,
            val onClick: () -> Unit
        ) : Line()


        data class LearnMore(val text: String, val buttonText: String, val onClick: () -> Unit) : Line()
    }
}
