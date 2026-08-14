package com.woocommerce.android.ui.orders.filters

import com.google.gson.Gson
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory
import com.woocommerce.android.ui.orders.filters.model.OrderFilterCategoryUiModel
import com.woocommerce.android.ui.orders.filters.model.OrderFilterOptionUiModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class OrderFilterHistoryMapperTest {
    private val sut = OrderFilterHistoryMapper(Gson())

    @Test
    fun `given selected options, when building readable string, then only selected non-all names are joined`() {
        val categories = listOf(
            category(
                OrderListFilterCategory.ORDER_STATUS,
                option("processing", "Processing", isSelected = true),
                option(OrderFilterOptionUiModel.DEFAULT_ALL_KEY, "All", isSelected = false)
            ),
            category(
                OrderListFilterCategory.DATE_RANGE,
                option("last_30_days", "Last 30 days", isSelected = true)
            )
        )

        val readable = sut.toReadableString(categories)

        assertThat(readable).isEqualTo("Processing, Last 30 days")
    }

    @Test
    fun `given a selection, when encoding then decoding, then the selection round-trips`() {
        val categories = listOf(
            category(
                OrderListFilterCategory.ORDER_STATUS,
                option("processing", "Processing", isSelected = true),
                option("completed", "Completed", isSelected = true)
            )
        )

        val payload = sut.toPayload(categories, customDateRangeStart = 111, customDateRangeEnd = 222)
        val decoded = sut.fromPayload(payload)

        assertThat(decoded?.selections).isEqualTo(
            mapOf(OrderListFilterCategory.ORDER_STATUS.name to listOf("completed", "processing"))
        )
        assertThat(decoded?.customDateRangeStart).isEqualTo(111)
        assertThat(decoded?.customDateRangeEnd).isEqualTo(222)
    }

    @Test
    fun `given selections in different orders, when encoded, then payloads are identical (canonical)`() {
        val a = listOf(
            category(
                OrderListFilterCategory.ORDER_STATUS,
                option("processing", "Processing", isSelected = true),
                option("completed", "Completed", isSelected = true)
            )
        )
        val b = listOf(
            category(
                OrderListFilterCategory.ORDER_STATUS,
                option("completed", "Completed", isSelected = true),
                option("processing", "Processing", isSelected = true)
            )
        )

        assertThat(sut.toPayload(a, 0, 0)).isEqualTo(sut.toPayload(b, 0, 0))
    }

    @Test
    fun `given an invalid payload, when decoding, then null is returned`() {
        assertThat(sut.fromPayload("not-json")).isNull()
    }

    @Test
    fun `given an empty json payload, when decoding, then defaults are returned`() {
        val decoded = sut.fromPayload("{}")

        assertThat(decoded).isNotNull
        assertThat(decoded?.selections).isEmpty()
        assertThat(decoded?.customDateRangeStart).isEqualTo(0)
        assertThat(decoded?.customDateRangeEnd).isEqualTo(0)
    }

    @Test
    fun `given a selected All option, when encoding, then it is excluded from readable string and payload`() {
        val categories = listOf(
            category(
                OrderListFilterCategory.ORDER_STATUS,
                option(OrderFilterOptionUiModel.DEFAULT_ALL_KEY, "All", isSelected = true),
                option("processing", "Processing", isSelected = true)
            )
        )

        assertThat(sut.toReadableString(categories)).isEqualTo("Processing")
        assertThat(sut.fromPayload(sut.toPayload(categories, 0, 0))?.selections).isEqualTo(
            mapOf(OrderListFilterCategory.ORDER_STATUS.name to listOf("processing"))
        )
    }

    @Test
    fun `given a category whose only selection is All, when encoding, then the category is absent from the payload`() {
        val categories = listOf(
            category(
                OrderListFilterCategory.ORDER_STATUS,
                option(OrderFilterOptionUiModel.DEFAULT_ALL_KEY, "All", isSelected = true)
            )
        )

        assertThat(sut.fromPayload(sut.toPayload(categories, 0, 0))?.selections).isEmpty()
    }

    @Test
    fun `given a selected option with a display value, when building readable, then the display value is used`() {
        val categories = listOf(
            category(
                OrderListFilterCategory.DATE_RANGE,
                OrderFilterOptionUiModel(
                    key = "custom_range",
                    displayName = "Custom Range",
                    displayValue = "Jan 1 - Jan 31",
                    isSelected = true
                )
            )
        )

        assertThat(sut.toReadableString(categories)).isEqualTo("Jan 1 - Jan 31")
    }

    private fun category(key: OrderListFilterCategory, vararg options: OrderFilterOptionUiModel) =
        OrderFilterCategoryUiModel(
            categoryKey = key,
            displayName = key.name,
            displayValue = "",
            orderFilterOptions = options.toList()
        )

    private fun option(key: String, displayName: String, isSelected: Boolean) =
        OrderFilterOptionUiModel(key = key, displayName = displayName, isSelected = isSelected)
}
