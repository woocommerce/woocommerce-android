package com.woocommerce.android.ui.orders.filters

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.woocommerce.android.ui.orders.filters.model.OrderFilterCategoryUiModel
import com.woocommerce.android.ui.orders.filters.model.OrderFilterOptionUiModel
import com.woocommerce.android.ui.orders.filters.model.OrderFilterOptionUiModel.Companion.DEFAULT_ALL_KEY
import javax.inject.Inject

/**
 * Encodes/decodes the order filter selection to and from the opaque `payload` string persisted in the
 * filter history table, and builds the human-readable label shown in the history list.
 *
 * The payload is a canonical JSON serialization of the active selection (categories and option keys
 * sorted) so that logically-identical selections dedup reliably. Decoding tolerates missing/unknown
 * fields, and applying a decoded selection silently drops option keys that no longer exist because the
 * filter screen only marks options selected when their key matches an available option.
 */
class OrderFilterHistoryMapper @Inject constructor(
    private val gson: Gson
) {
    fun toReadableString(categories: List<OrderFilterCategoryUiModel>): String =
        categories
            .flatMap { it.orderFilterOptions }
            .filter { it.isSelected && it.key != DEFAULT_ALL_KEY }
            .joinToString(separator = ", ") { it.readableLabel() }

    // Prefer displayValue when present so a custom date range shows its dates rather than the static
    // "Custom Range" label; other options have no displayValue and fall back to displayName.
    private fun OrderFilterOptionUiModel.readableLabel(): String =
        displayValue?.takeIf { it.isNotBlank() } ?: displayName

    fun toPayload(
        categories: List<OrderFilterCategoryUiModel>,
        customDateRangeStart: Long,
        customDateRangeEnd: Long
    ): String {
        val selections = categories
            .associate { category ->
                category.categoryKey.name to category.orderFilterOptions
                    .filter { it.isSelected && it.key != DEFAULT_ALL_KEY }
                    .map { it.key }
                    .sorted()
            }
            .filterValues { it.isNotEmpty() }
            .toSortedMap()
        return gson.toJson(
            OrderFilterHistoryData(
                selections = selections,
                customDateRangeStart = customDateRangeStart,
                customDateRangeEnd = customDateRangeEnd
            )
        )
    }

    fun fromPayload(payload: String): OrderFilterHistoryData? =
        runCatching { gson.fromJson(payload, OrderFilterHistoryData::class.java) }.getOrNull()

    data class OrderFilterHistoryData(
        @SerializedName("selections") val selections: Map<String, List<String>> = emptyMap(),
        @SerializedName("custom_date_range_start") val customDateRangeStart: Long = 0,
        @SerializedName("custom_date_range_end") val customDateRangeEnd: Long = 0
    )
}
