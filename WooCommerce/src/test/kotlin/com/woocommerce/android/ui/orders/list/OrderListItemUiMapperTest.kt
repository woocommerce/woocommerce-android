package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.TimeGroup
import com.woocommerce.android.ui.compose.designsystem.component.WooBadgeTone
import com.woocommerce.android.ui.orders.list.OrderListItemUIType.LoadingItem
import com.woocommerce.android.ui.orders.list.OrderListItemUIType.OrderListItemUI
import com.woocommerce.android.ui.orders.list.OrderListItemUIType.SectionHeader
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.WCOrderStatusModel

class OrderListItemUiMapperTest {
    @Test
    fun `given an order, when mapped, then display fields and badges are preserved`() {
        // GIVEN
        val item = order(
            status = Order.Status.Processing.value,
            salesChannelLabel = OrderListItemUI.SalesChannelLabel.Visible("POS"),
        )
        val statuses = mapOf(
            item.status to WCOrderStatusModel(
                statusKey = item.status,
                label = "Processing order",
            )
        )

        // WHEN
        val result = item.toUiModel(
            orderStatusOptions = statuses,
            formatCurrency = { rawValue, currencyCode -> "$currencyCode $rawValue" },
            resolveString = { "unused" },
        )

        // THEN
        assertThat(result).isEqualTo(
            OrderListItemUiModel.Order(
                orderId = ORDER_ID,
                number = "#42",
                customerName = "Ada Lovelace",
                dateCreated = "Today",
                total = "USD 19.50",
                badges = listOf(
                    OrderListBadgeUiModel("Processing order", WooBadgeTone.Success),
                    OrderListBadgeUiModel("POS", WooBadgeTone.NeutralOutlined),
                ),
                isCompleted = false,
                showDivider = true,
            )
        )
    }

    @Test
    fun `given a last order without a channel, when mapped, then divider and channel badge are omitted`() {
        // GIVEN
        val item = order(
            status = Order.Status.Completed.value,
            isLastItemInSection = true,
        )

        // WHEN
        val result = item.toUiModel(
            orderStatusOptions = emptyMap(),
            formatCurrency = { rawValue, _ -> rawValue },
            resolveString = { "unused" },
        ) as OrderListItemUiModel.Order

        // THEN
        assertThat(result.showDivider).isFalse()
        assertThat(result.isCompleted).isTrue()
        assertThat(result.badges).containsExactly(
            OrderListBadgeUiModel(Order.Status.Completed.value, WooBadgeTone.Info)
        )
    }

    @Test
    fun `given core and custom statuses, when mapped, then semantic badge tones are stable`() {
        val statuses = listOf(
            Order.Status.Processing.value to WooBadgeTone.Success,
            Order.Status.Completed.value to WooBadgeTone.Info,
            Order.Status.OnHold.value to WooBadgeTone.Caution,
            Order.Status.Failed.value to WooBadgeTone.Error,
            Order.Status.Pending.value to WooBadgeTone.Neutral,
            Order.Status.Cancelled.value to WooBadgeTone.Neutral,
            Order.Status.Refunded.value to WooBadgeTone.Neutral,
            "custom-status" to WooBadgeTone.Neutral,
        )

        statuses.forEach { (status, expectedTone) ->
            // WHEN
            val result = order(status = status).toUiModel(
                orderStatusOptions = emptyMap(),
                formatCurrency = { rawValue, _ -> rawValue },
                resolveString = { "unused" },
            ) as OrderListItemUiModel.Order

            // THEN
            assertThat(result.badges.single().tone).isEqualTo(expectedTone)
        }
    }

    @Test
    fun `given a section header, when mapped, then its localized title is used`() {
        // GIVEN
        val item = SectionHeader(TimeGroup.GROUP_TODAY)

        // WHEN
        val result = item.toUiModel(
            orderStatusOptions = emptyMap(),
            formatCurrency = { rawValue, _ -> rawValue },
            resolveString = { resourceId -> "resource:$resourceId" },
        )

        // THEN
        assertThat(result).isEqualTo(
            OrderListItemUiModel.DateSection("resource:${TimeGroup.GROUP_TODAY.labelRes}")
        )
    }

    @Test
    fun `given a loading item, when mapped, then its order identity is retained`() {
        // GIVEN
        val item = LoadingItem(ORDER_ID)

        // WHEN
        val result = item.toUiModel(
            orderStatusOptions = emptyMap(),
            formatCurrency = { rawValue, _ -> rawValue },
            resolveString = { "unused" },
        )

        // THEN
        assertThat(result).isEqualTo(OrderListItemUiModel.Loading(ORDER_ID))
    }

    private fun order(
        status: String,
        isLastItemInSection: Boolean = false,
        salesChannelLabel: OrderListItemUI.SalesChannelLabel = OrderListItemUI.SalesChannelLabel.Hidden,
    ) = OrderListItemUI(
        orderId = ORDER_ID,
        orderNumber = "42",
        orderName = "Ada Lovelace",
        orderTotal = "19.50",
        status = status,
        dateCreated = "Today",
        currencyCode = "USD",
        isLastItemInSection = isLastItemInSection,
        salesChannelLabel = salesChannelLabel,
    )

    private companion object {
        const val ORDER_ID = 42L
    }
}
