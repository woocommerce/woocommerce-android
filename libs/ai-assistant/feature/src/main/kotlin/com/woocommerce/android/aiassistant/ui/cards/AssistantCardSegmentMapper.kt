package com.woocommerce.android.aiassistant.ui.cards

import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsUiStructured
import com.woocommerce.android.aiassistant.ui.AssistantUiSegment

internal object AssistantCardSegmentMapper {
    fun toSegments(payload: ShowCardsUiStructured): List<AssistantUiSegment.CardGroup> {
        val cards = AssistantCardPayloadParser.parse(payload)
        return if (cards.isEmpty()) {
            emptyList()
        } else {
            listOf(AssistantUiSegment.CardGroup(cards))
        }
    }
}
