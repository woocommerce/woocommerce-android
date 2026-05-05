package com.woocommerce.android.aiassistant.ui.cards

import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.di.AiAssistantJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

internal class AssistantAnalyticsRevenueCardParser @Inject constructor(
    @AiAssistantJson private val json: Json,
) {
    fun parse(result: ToolResult.Success): AssistantCard.Stats? {
        val payload = runCatching {
            json.decodeFromJsonElement<AnalyticsRevenueStructured>(result.structured)
        }.getOrNull() ?: return null

        return payload.toStatsCard()
    }
}

@Serializable
private data class AnalyticsRevenueStructured(
    val after: String? = null,
    val before: String? = null,
    val currency: String? = null,
    val totals: JsonObject? = null,
    val revenue_chart: List<RevenueChartPoint> = emptyList(),
)

@Serializable
private data class RevenueChartPoint(
    val date: String? = null,
    val value: JsonElement? = null,
)

private fun AnalyticsRevenueStructured.toStatsCard(): AssistantCard.Stats? {
    val validAfter = after?.takeIf { it.isIsoLocalDate() } ?: return null
    val validBefore = before?.takeIf { it.isIsoLocalDate() } ?: return null
    return AssistantCard.Stats(
        after = validAfter,
        before = validBefore,
        revenueTotal = totals.stringValue(REVENUE_TOTAL_KEYS),
        revenueCurrency = currency.orEmpty(),
        orderCount = totals.stringValue(ORDER_COUNT_KEYS),
        chartPoints = revenue_chart.mapNotNull { it.toChartPoint() },
    )
}

private fun RevenueChartPoint.toChartPoint(): AssistantCard.Stats.ChartPoint? {
    val validDate = date?.takeIf { it.isIsoLocalDate() } ?: return null
    val numericValue = value?.numericStringOrNull()?.toDoubleOrNull() ?: return null
    return AssistantCard.Stats.ChartPoint(date = validDate, value = numericValue)
}

private fun JsonObject?.stringValue(keys: List<String>): String =
    keys.firstNotNullOfOrNull { key -> this?.get(key)?.numericStringOrNull() }.orEmpty()

private fun JsonElement.numericStringOrNull(): String? =
    runCatching { jsonPrimitive.content }.getOrNull()

private fun String.isIsoLocalDate(): Boolean =
    ISO_LOCAL_DATE_SHAPE.matches(this) &&
        runCatching { LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE) }.isSuccess

private val REVENUE_TOTAL_KEYS = listOf("net_revenue", "total_sales", "total_revenue", "gross_revenue")
private val ORDER_COUNT_KEYS = listOf("orders_count", "order_count", "orders")
private val ISO_LOCAL_DATE_SHAPE = Regex("\\d{4}-\\d{2}-\\d{2}")
