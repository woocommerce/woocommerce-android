package com.woocommerce.android.aiassistant.ui.cards

import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardDetails
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardPayload
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsUiStructured

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

    private const val ORDER_FAMILY = "order"
    private const val PRODUCT_FAMILY = "product"
}
