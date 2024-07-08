package com.woocommerce.android.ui.orders.creation.totals

import com.woocommerce.android.R
import com.woocommerce.android.extensions.WindowSizeClass
import com.woocommerce.android.extensions.isNotEqualTo
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.extensions.sumByBigDecimal
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel.ViewState
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ResourceProvider
import java.math.BigDecimal
import javax.inject.Inject

class OrderCreateEditTotalsHelper @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter
) {
    @Suppress("LongParameterList")
    fun mapToPaymentTotalsState(
        order: Order,
        mode: OrderCreateEditViewModel.Mode,
        viewState: ViewState,
        onCouponsClicked: () -> Unit,
        onGiftClicked: () -> Unit,
        onTaxesLearnMore: () -> Unit,
        onMainButtonClicked: () -> Unit,
        onRecalculateButtonClicked: () -> Unit,
        onExpandCollapseClicked: () -> Unit,
        onHeightChanged: (height: Int) -> Unit
    ): TotalsSectionsState {
        val bigDecimalFormatter = currencyFormatter.buildBigDecimalFormatter(
            currencyCode = order.currency
        )

        return if (order.items.isEmpty() && order.feesLines.isEmpty()) {
            TotalsSectionsState.Minimised(
                orderTotal = order.toOrderTotals(bigDecimalFormatter),
                onHeightChanged = onHeightChanged,
                onExpandCollapseClicked = onExpandCollapseClicked,
                recalculateButton = viewState.getRecalculateButton(onRecalculateButtonClicked)
            )
        } else {
            TotalsSectionsState.Full(
                lines = listOfNotNull(
                    order.toProductsSection(bigDecimalFormatter),
                    order.toCustomAmountSection(bigDecimalFormatter),
                    order.toShippingSection(bigDecimalFormatter),
                    order.toCouponsSection(
                        enabled = viewState.isCouponButtonEnabled && viewState.isIdle,
                        bigDecimalFormatter,
                        onClick = onCouponsClicked
                    ),
                    order.toGiftSection(
                        enabled = viewState.isAddGiftCardButtonEnabled && viewState.isIdle,
                        bigDecimalFormatter,
                        onClick = onGiftClicked
                    ),
                    order.toTaxesSection(
                        bigDecimalFormatter,
                        viewState.taxBasedOnSettingLabel,
                        onClick = onTaxesLearnMore
                    ),
                    order.toDiscountSection(bigDecimalFormatter),
                ),
                orderTotal = order.toOrderTotals(bigDecimalFormatter),
                mainButton = viewState.getMainButton(mode, onMainButtonClicked, onRecalculateButtonClicked),
                isExpanded = viewState.isTotalsExpanded,
                onExpandCollapseClicked = onExpandCollapseClicked,
                onHeightChanged = onHeightChanged,
            )
        }
    }

    private fun ViewState.getRecalculateButton(
        onRecalculateButtonClicked: () -> Unit
    ): TotalsSectionsState.Button? {
        return if (windowSizeClass != WindowSizeClass.Compact && isRecalculateNeeded) {
            TotalsSectionsState.Button(
                text = resourceProvider.getString(R.string.order_creation_recalculate_button),
                enabled = canCreateOrder,
                onClick = onRecalculateButtonClicked,
            )
        } else {
            null
        }
    }

    private fun ViewState.getMainButton(
        mode: OrderCreateEditViewModel.Mode,
        onMainButtonClicked: () -> Unit,
        onRecalculateButtonClicked: () -> Unit
    ): TotalsSectionsState.Button {
        return if (windowSizeClass == WindowSizeClass.Compact) {
            TotalsSectionsState.Button(
                text = mode.toButtonText(),
                enabled = canCreateOrder,
                onClick = onMainButtonClicked,
            )
        } else {
            if (!isRecalculateNeeded) {
                TotalsSectionsState.Button(
                    text = mode.toButtonText(),
                    enabled = canCreateOrder,
                    onClick = onMainButtonClicked,
                )
            } else {
                TotalsSectionsState.Button(
                    text = resourceProvider.getString(R.string.order_creation_recalculate_button),
                    enabled = canCreateOrder,
                    onClick = onRecalculateButtonClicked,
                )
            }
        }
    }

    private fun Order.toOrderTotals(bigDecimalFormatter: (BigDecimal) -> String) =
        TotalsSectionsState.OrderTotal(
            label = resourceProvider.getString(R.string.order_creation_payment_order_total),
            value = bigDecimalFormatter(total)
        )

    private fun Order.toProductsSection(
        bigDecimalFormatter: (BigDecimal) -> String
    ): TotalsSectionsState.Line? =
        if (items.isNotEmpty()) {
            TotalsSectionsState.Line.Simple(
                label = resourceProvider.getString(R.string.order_creation_payment_products),
                value = bigDecimalFormatter(productsTotal)
            )
        } else {
            null
        }

    private fun Order.toCustomAmountSection(
        bigDecimalFormatter: (BigDecimal) -> String
    ): TotalsSectionsState.Line? {
        val hasCustomAmount = feesTotal.isNotEqualTo(BigDecimal.ZERO)

        return if (hasCustomAmount) {
            TotalsSectionsState.Line.Simple(
                label = resourceProvider.getString(R.string.custom_amounts),
                value = bigDecimalFormatter(feesTotal)
            )
        } else {
            null
        }
    }

    private fun Order.toShippingSection(bigDecimalFormatter: (BigDecimal) -> String): TotalsSectionsState.Line? =
        if (shippingLines.filter { it.methodId != null }.isNotEmpty()) {
            TotalsSectionsState.Line.Simple(
                label = resourceProvider.getString(R.string.shipping),
                value = shippingLines.sumByBigDecimal { it.total }.let(bigDecimalFormatter)
            )
        } else {
            null
        }

    private fun Order.toCouponsSection(
        enabled: Boolean,
        bigDecimalFormatter: (BigDecimal) -> String,
        onClick: () -> Unit
    ): TotalsSectionsState.Line? =
        if (discountCodes.isNotNullOrEmpty()) {
            TotalsSectionsState.Line.Button(
                text = resourceProvider.getString(R.string.order_creation_coupon_button),
                value = resourceProvider.getString(
                    R.string.order_creation_coupon_discount_value,
                    bigDecimalFormatter(discountTotal)
                ),
                extraValue = discountCodes,
                enabled = enabled,
                onClick = onClick,
            )
        } else {
            null
        }

    private fun Order.toGiftSection(
        enabled: Boolean,
        bigDecimalFormatter: (BigDecimal) -> String,
        onClick: () -> Unit
    ): TotalsSectionsState.Line? =
        if (!selectedGiftCard.isNullOrEmpty()) {
            TotalsSectionsState.Line.Button(
                text = resourceProvider.getString(R.string.order_gift_card),
                value = giftCardDiscountedAmount?.let { "-" + bigDecimalFormatter(it) } ?: "",
                extraValue = selectedGiftCard,
                enabled = enabled,
                onClick = onClick,
            )
        } else {
            null
        }

    private fun Order.toTaxesSection(
        bigDecimalFormatter: (BigDecimal) -> String,
        taxBasedOnSettingLabel: String,
        onClick: () -> Unit
    ): TotalsSectionsState.Line =
        TotalsSectionsState.Line.Block(
            lines = listOf(
                TotalsSectionsState.Line.Simple(
                    label = resourceProvider.getString(R.string.order_creation_payment_tax_label),
                    value = bigDecimalFormatter(totalTax)
                )
            ) + taxLines.map {
                TotalsSectionsState.Line.SimpleSmall(
                    label = "${it.label} · ${it.ratePercent}%",
                    value = bigDecimalFormatter(BigDecimal(it.taxTotal))
                )
            } + if (shippingTax > BigDecimal.ZERO) {
                listOf(
                    TotalsSectionsState.Line.SimpleSmall(
                        label = resourceProvider.getString(R.string.order_creation_payment_shipping_tax_label),
                        value = bigDecimalFormatter(shippingTax)
                    )
                )
            } else {
                emptyList()
            } + TotalsSectionsState.Line.LearnMore(
                text = taxBasedOnSettingLabel,
                buttonText = resourceProvider.getString(R.string.learn_more),
                onClick = onClick
            )
        )

    private fun Order.toDiscountSection(
        bigDecimalFormatter: (BigDecimal) -> String,
    ): TotalsSectionsState.Line? =
        if (discountTotal.isNotEqualTo(BigDecimal.ZERO)) {
            TotalsSectionsState.Line.Simple(
                label = resourceProvider.getString(R.string.order_creation_discounts_total),
                value = resourceProvider.getString(
                    R.string.order_creation_discounts_total_value,
                    bigDecimalFormatter(discountTotal)
                )
            )
        } else {
            null
        }

    private fun OrderCreateEditViewModel.Mode.toButtonText() =
        when (this) {
            is OrderCreateEditViewModel.Mode.Creation -> resourceProvider.getString(
                R.string.order_creation_collect_payment_button
            )

            is OrderCreateEditViewModel.Mode.Edit -> resourceProvider.getString(R.string.done)
        }
}

sealed class TotalsSectionsState(open val onHeightChanged: (height: Int) -> Unit) {
    data class Full(
        val lines: List<Line>,
        val orderTotal: OrderTotal,
        val mainButton: Button,
        val isExpanded: Boolean,
        val onExpandCollapseClicked: () -> Unit,
        override val onHeightChanged: (height: Int) -> Unit,
    ) : TotalsSectionsState(onHeightChanged)

    data class Minimised(
        val orderTotal: OrderTotal,
        val onExpandCollapseClicked: () -> Unit,
        val recalculateButton: Button? = null,
        override val onHeightChanged: (height: Int) -> Unit
    ) : TotalsSectionsState(onHeightChanged)

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
            val enabled: Boolean,
            val onClick: () -> Unit
        ) : Line()

        data class Block(val lines: List<Line>) : Line()

        data class LearnMore(val text: String, val buttonText: String, val onClick: () -> Unit) : Line()
    }
}
