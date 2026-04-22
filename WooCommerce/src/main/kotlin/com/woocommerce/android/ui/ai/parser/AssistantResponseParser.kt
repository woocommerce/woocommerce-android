package com.woocommerce.android.ui.ai.parser

import com.woocommerce.android.ui.ai.model.MessageContent
import com.woocommerce.android.ui.ai.model.OrderCardData
import com.woocommerce.android.ui.ai.model.ProductCardData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object AssistantResponseParser {

    private val JSON = Json { ignoreUnknownKeys = true; isLenient = true }

    private val RICH_CONTENT_BLOCK_PATTERN = Regex(
        "```json:(\\w+)\\s*\\n(.*?)```",
        setOf(RegexOption.DOT_MATCHES_ALL)
    )

    fun parse(responseText: String): List<MessageContent> {
        val segments = mutableListOf<MessageContent>()
        var lastIndex = 0

        for (match in RICH_CONTENT_BLOCK_PATTERN.findAll(responseText)) {
            val textBefore = responseText.substring(lastIndex, match.range.first).trim()
            if (textBefore.isNotEmpty()) {
                segments.add(MessageContent.Text(textBefore))
            }

            val contentType = match.groupValues[1]
            val jsonContent = match.groupValues[2].trim()
            val richContent = tryParseRichContent(contentType, jsonContent)

            if (richContent != null) {
                segments.add(richContent)
            } else {
                segments.add(MessageContent.Text(match.value))
            }

            lastIndex = match.range.last + 1
        }

        val remaining = responseText.substring(lastIndex).trim()
        if (remaining.isNotEmpty()) {
            segments.add(MessageContent.Text(remaining))
        }

        if (segments.isEmpty()) {
            segments.add(MessageContent.Text(responseText))
        }

        return segments
    }

    private fun tryParseRichContent(type: String, jsonContent: String): MessageContent? {
        return when (type) {
            "orders" -> tryParseOrders(jsonContent)
            "products" -> tryParseProducts(jsonContent)
            else -> null
        }
    }

    private fun tryParseOrders(jsonContent: String): MessageContent.OrderCards? {
        return try {
            val apiOrders = JSON.decodeFromString<List<OrderApiData>>(jsonContent)
            if (apiOrders.isEmpty()) return null
            MessageContent.OrderCards(apiOrders.map { it.toOrderCardData() })
        } catch (_: Exception) {
            null
        }
    }

    private fun tryParseProducts(jsonContent: String): MessageContent.ProductCards? {
        return try {
            val apiProducts = JSON.decodeFromString<List<ProductApiData>>(jsonContent)
            if (apiProducts.isEmpty()) return null
            MessageContent.ProductCards(apiProducts.map { it.toProductCardData() })
        } catch (_: Exception) {
            null
        }
    }

    @Serializable
    private data class OrderApiData(
        val id: Long = 0L,
        val number: String = "",
        val status: String = "",
        @SerialName("billing") val billing: BillingData? = null,
        val total: String = "",
        @SerialName("date_created") val dateCreated: String = ""
    ) {
        fun toOrderCardData() = OrderCardData(
            id = id,
            number = "#$number",
            status = status.replace("-", " ").replaceFirstChar { it.uppercase() },
            statusColor = ProductCardData.orderStatusToColorRes(status),
            customerName = billing?.let { "${it.firstName} ${it.lastName}".trim() } ?: "",
            totalPrice = total,
            date = dateCreated.take(10)
        )
    }

    @Serializable
    private data class BillingData(
        @SerialName("first_name") val firstName: String = "",
        @SerialName("last_name") val lastName: String = ""
    )

    @Serializable
    private data class ProductApiData(
        val id: Long = 0L,
        val name: String = "",
        val price: String = "",
        val status: String = "",
        @SerialName("stock_status") val stockStatus: String = "",
        @SerialName("image_url") val imageUrl: String? = null
    ) {
        fun toProductCardData() = ProductCardData(
            id = id,
            name = name,
            price = price,
            status = status.replaceFirstChar { it.uppercase() },
            statusColor = ProductCardData.statusToColorRes(status),
            stockStatus = stockStatus.replace("_", " ").replaceFirstChar { it.uppercase() },
            imageUrl = imageUrl
        )
    }
}
