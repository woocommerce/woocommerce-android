package com.woocommerce.android.aiassistant.ui.cards

import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardDetails
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardPayload
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsUiStructured

internal object AssistantCardPayloadParser {
    fun parse(payload: ShowCardsUiStructured): List<AssistantCard> =
        payload.cards.mapNotNull(::parseCard)

    private fun parseCard(card: ShowCardPayload): AssistantCard? {
        if (card.family != ORDER_FAMILY) return null

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

    private const val ORDER_FAMILY = "order"
}
