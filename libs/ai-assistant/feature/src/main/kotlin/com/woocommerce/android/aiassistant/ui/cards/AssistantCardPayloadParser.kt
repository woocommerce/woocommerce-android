package com.woocommerce.android.aiassistant.ui.cards

import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardPayload
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsUiStructured

internal object AssistantCardPayloadParser {
    fun parse(payload: ShowCardsUiStructured): List<AssistantCard> =
        payload.cards.mapNotNull(::parseCard)

    private fun parseCard(card: ShowCardPayload): AssistantCard? {
        if (card.family != ORDER_FAMILY) return null

        val remoteOrderId = card.id.toLongOrNull()?.takeIf { it > 0 } ?: return null
        val attributes = card.attributes
        val status = attributes["status"]
            ?: card.subtitle
            ?: card.badges.firstOrNull()
            ?: ""

        return AssistantCard.Order(
            remoteOrderId = remoteOrderId,
            number = card.title,
            status = status,
            total = listOfNotBlank(attributes["total"], attributes["currency"]).joinToString(" "),
            customerName = attributes["customer_name"].orEmpty(),
            date = attributes["date_created"].orEmpty(),
        )
    }

    private fun listOfNotBlank(vararg values: String?): List<String> =
        values.filterNotNull().filter { it.isNotBlank() }

    private const val ORDER_FAMILY = "order"
}
