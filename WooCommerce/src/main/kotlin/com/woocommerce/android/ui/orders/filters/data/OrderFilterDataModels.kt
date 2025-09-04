package com.woocommerce.android.ui.orders.filters.data

enum class OrderListFilterCategory {
    ORDER_STATUS,
    DATE_RANGE,
    PRODUCT,
    CUSTOMER,
    SALES_CHANNEL
}

enum class SalesChannel(val key: String, val apiValue: String) {
    POS("pos", "pos-rest-api"),
    WEB_CHECKOUT("checkout", "checkout"),
    WP_ADMIN("admin", "admin");

    companion object {
        fun fromKey(key: String): SalesChannel? = entries.find { it.key == key }
    }
}

enum class DateRange(val filterKey: String) {
    TODAY("Today"),
    LAST_2_DAYS("Last2Days"),
    LAST_7_DAYS("Last7Days"),
    LAST_30_DAYS("Last30Days"),
    CUSTOM_RANGE("CustomRange");

    companion object {
        private val valueMap = values().associateBy(DateRange::filterKey)
        fun fromValue(filterKey: String) = valueMap[filterKey]
    }
}

sealed class OrderFilterOption {
    abstract val isSelected: Boolean
}

data class OrderStatusOption(
    val key: String,
    val label: String,
    val statusCount: Int,
    override val isSelected: Boolean
) : OrderFilterOption()

data class DateRangeFilterOption(
    val dateRange: DateRange,
    override val isSelected: Boolean,
    val startDate: Long,
    val endDate: Long,
) : OrderFilterOption()
