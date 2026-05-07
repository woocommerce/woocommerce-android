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

    private fun parseCustomerCard(card: ShowCardPayload): AssistantCard? {
        val remoteCustomerId = card.id.toLongOrNull()?.takeIf { it > 0 } ?: return null
        val details = card.details as? ShowCardDetails.Customer ?: return null

        return AssistantCard.Customer(
            remoteCustomerId = remoteCustomerId,
            name = card.title,
            email = details.email.orEmpty(),
        )
    }

    private fun parseStatsCard(card: ShowCardPayload): AssistantCard? {
        val details = card.details as? ShowCardDetails.AnalyticsStats ?: return null
        val after = details.after.takeIf { it.isIsoLocalDate() } ?: return null
        val before = details.before.takeIf { it.isIsoLocalDate() } ?: return null

        return AssistantCard.Stats(
            id = card.id,
            after = after,
            before = before,
            currency = details.currency.orEmpty(),
            totalSales = details.totals.stringValue(TOTAL_SALES_KEYS),
            netSales = details.totals.stringValue(NET_SALES_KEYS),
            totalSalesChartPoints = details.intervalSubtotals.mapNotNull {
                it.toChartPoint(TOTAL_SALES_KEYS)
            },
            netSalesChartPoints = details.intervalSubtotals.mapNotNull {
                it.toChartPoint(NET_SALES_KEYS)
            },
        )
    }

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

    private const val ORDER_FAMILY = "order"
    private const val PRODUCT_FAMILY = "product"
    private const val CUSTOMER_FAMILY = "customer"
    private const val ANALYTICS_STATS_FAMILY = "analytics_stats"
    private const val ISO_LOCAL_DATE_LENGTH = 10
    private val TOTAL_SALES_KEYS = listOf("total_sales", "gross_sales")
    private val NET_SALES_KEYS = listOf("net_revenue")
    private val ISO_LOCAL_DATE_SHAPE = Regex("\\d{4}-\\d{2}-\\d{2}")
}
