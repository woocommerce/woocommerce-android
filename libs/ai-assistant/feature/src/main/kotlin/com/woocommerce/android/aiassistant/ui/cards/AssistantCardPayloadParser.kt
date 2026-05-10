package com.woocommerce.android.aiassistant.ui.cards

import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardDetails
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardPayload
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsUiStructured
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal object AssistantCardPayloadParser {
    fun parse(payload: ShowCardsUiStructured): List<AssistantCard> =
        parseEntries(payload).map { it.card }

    fun parseEntries(payload: ShowCardsUiStructured): List<AssistantCardEntry> =
        payload.cards.mapNotNull(::parseEntry)

    private fun parseEntry(card: ShowCardPayload): AssistantCardEntry? {
        val parsedCard = parseCard(card) ?: return null

        return AssistantCardEntry(
            key = AssistantCardKey(family = card.family, id = card.id),
            card = parsedCard,
        )
    }

    private fun parseCard(card: ShowCardPayload): AssistantCard? =
        when (card.family) {
            ORDER_FAMILY -> parseOrderCard(card)
            PRODUCT_FAMILY -> parseProductCard(card)
            VARIATION_FAMILY -> parseVariationCard(card)
            CUSTOMER_FAMILY -> parseCustomerCard(card)
            ANALYTICS_STATS_FAMILY -> parseStatsCard(card)
            else -> null
        }

    private fun parseOrderCard(card: ShowCardPayload): AssistantCard? {
        val remoteOrderId = card.id.toLongOrNull()?.takeIf { it > 0 } ?: return null
        val details = card.details as? ShowCardDetails.Order ?: return null

        return AssistantCard.Order(
            remoteOrderId = remoteOrderId,
            number = card.title,
            status = details.status.orEmpty(),
            total = details.total.orEmpty(),
            currency = details.currency.orEmpty(),
            customerName = details.customerName.orEmpty(),
            date = details.dateCreated.orEmpty(),
        )
    }

    private fun parseProductCard(card: ShowCardPayload): AssistantCard? {
        val remoteProductId = card.id.toLongOrNull()?.takeIf { it > 0 } ?: return null
        val details = card.details as? ShowCardDetails.Product ?: return null

        return AssistantCard.Product(
            remoteProductId = remoteProductId,
            name = card.title,
            sku = details.sku.orEmpty(),
            price = details.price.orEmpty(),
            stockStatus = details.stockStatus.orEmpty(),
            status = details.status.orEmpty(),
            imageUrl = details.imageUrl.orEmpty(),
        )
    }

    private fun parseVariationCard(card: ShowCardPayload): AssistantCard? {
        val details = card.details as? ShowCardDetails.Variation ?: return null
        val (parentProductId, variationId) = card.id.toVariationIdParts() ?: return null
        if (parentProductId != details.productId || variationId != details.variationId) return null

        return AssistantCard.Variation(
            parentProductId = parentProductId,
            variationId = variationId,
            name = details.name ?: card.title,
            sku = details.sku.orEmpty(),
            price = details.price.orEmpty(),
            stockStatus = details.stockStatus.orEmpty(),
            status = details.status.orEmpty(),
            imageUrl = details.imageUrl.orEmpty(),
            attributes = details.attributes.mapNotNull { attribute ->
                val name = attribute.name?.takeIf { it.isNotBlank() }
                val option = attribute.option?.takeIf { it.isNotBlank() }
                if (name != null && option != null) {
                    AssistantCard.Variation.Attribute(name = name, option = option)
                } else {
                    null
                }
            },
        )
    }

    private fun parseCustomerCard(card: ShowCardPayload): AssistantCard? {
        val remoteCustomerId = card.id.toLongOrNull()?.takeIf { it > 0 } ?: return null
        val details = card.details as? ShowCardDetails.Customer ?: return null

        return AssistantCard.Customer(
            remoteCustomerId = remoteCustomerId,
            name = card.title,
            email = details.email.orEmpty(),
        )
    }

    private fun parseStatsCard(card: ShowCardPayload): AssistantCard? =
        (card.details as? ShowCardDetails.AnalyticsStats)?.toStatsCard(card.id)

    private fun ShowCardDetails.AnalyticsStats.toStatsCard(id: String): AssistantCard? {
        val afterDate = after.takeIf { it.isIsoLocalDate() }
        val beforeDate = before.takeIf { it.isIsoLocalDate() }

        return if (afterDate != null && beforeDate != null) {
            AssistantCard.Stats(
                id = id,
                after = afterDate,
                before = beforeDate,
                currency = currency.orEmpty(),
                metrics = analyticsMetrics(),
            )
        } else {
            null
        }
    }

    private fun ShowCardDetails.AnalyticsStats.analyticsMetrics(): List<AssistantCard.Stats.Metric> =
        listOf(
            metric(AssistantCard.Stats.MetricType.TotalSales, TOTAL_SALES_KEYS),
            metric(AssistantCard.Stats.MetricType.NetSales, NET_SALES_KEYS),
            metric(AssistantCard.Stats.MetricType.TotalOrders, ORDERS_COUNT_KEYS),
            metric(AssistantCard.Stats.MetricType.AverageOrderValue, AVERAGE_ORDER_VALUE_KEYS),
        )

    private fun ShowCardDetails.AnalyticsStats.metric(
        type: AssistantCard.Stats.MetricType,
        keys: List<String>,
    ) = AssistantCard.Stats.Metric(
        type = type,
        value = totals.stringValue(keys),
        chartPoints = intervalSubtotals.mapNotNull { it.toChartPoint(keys) },
    )

    private fun JsonObject.toChartPoint(valueKeys: List<String>): AssistantCard.Stats.ChartPoint? {
        val date = chartDate() ?: return null
        val subtotals = get("subtotals") as? JsonObject ?: return null
        val value = subtotals.numericValue(valueKeys) ?: return null

        return AssistantCard.Stats.ChartPoint(date = date, value = value)
    }

    private fun JsonObject.chartDate(): String? {
        val intervalDate = stringValue("interval")?.takeIf { it.isIsoLocalDate() }
        if (intervalDate != null) return intervalDate

        return stringValue("date_start")
            ?.take(ISO_LOCAL_DATE_LENGTH)
            ?.takeIf { it.isIsoLocalDate() }
    }

    private fun JsonObject.stringValue(keys: List<String>): String =
        keys.firstNotNullOfOrNull { key -> get(key)?.stringContentOrNull() }.orEmpty()

    private fun JsonObject.numericValue(keys: List<String>): Double? =
        keys.firstNotNullOfOrNull { key -> get(key)?.stringContentOrNull()?.toDoubleOrNull() }

    private fun JsonObject.stringValue(key: String): String? =
        get(key)?.stringContentOrNull()

    private fun JsonElement.stringContentOrNull(): String? =
        takeUnless { it == JsonNull }
            ?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }

    private fun String.isIsoLocalDate(): Boolean =
        ISO_LOCAL_DATE_SHAPE.matches(this) &&
            runCatching { LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE) }.isSuccess

    private fun String.toVariationIdParts(): Pair<Long, Long>? {
        val parts = split("/")
        if (parts.size != 2) return null
        val parentProductId = parts[0].toLongOrNull()?.takeIf { it > 0L } ?: return null
        val variationId = parts[1].toLongOrNull()?.takeIf { it > 0L } ?: return null
        return parentProductId to variationId
    }

    private const val ORDER_FAMILY = "order"
    private const val PRODUCT_FAMILY = "product"
    private const val VARIATION_FAMILY = "variation"
    private const val CUSTOMER_FAMILY = "customer"
    private const val ANALYTICS_STATS_FAMILY = "analytics_stats"
    private const val ISO_LOCAL_DATE_LENGTH = 10
    private val TOTAL_SALES_KEYS = listOf("total_sales", "gross_sales")
    private val NET_SALES_KEYS = listOf("net_revenue")
    private val ORDERS_COUNT_KEYS = listOf("orders_count")
    private val AVERAGE_ORDER_VALUE_KEYS = listOf("avg_order_value")
    private val ISO_LOCAL_DATE_SHAPE = Regex("\\d{4}-\\d{2}-\\d{2}")
}
