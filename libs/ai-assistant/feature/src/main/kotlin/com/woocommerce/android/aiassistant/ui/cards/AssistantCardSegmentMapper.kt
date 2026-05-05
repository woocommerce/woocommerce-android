package com.woocommerce.android.aiassistant.ui.cards

import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsUiStructured
import com.woocommerce.android.aiassistant.ui.AssistantUiSegment

internal object AssistantCardSegmentMapper {
    fun toSegments(payload: ShowCardsUiStructured): List<AssistantUiSegment.Card> =
        AssistantCardPayloadParser.parse(payload).map(AssistantUiSegment::Card)
}
