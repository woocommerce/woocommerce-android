package com.woocommerce.android.aiassistant.ui.cards

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.aiassistant.R

internal data class AssistantCardGroupMetadata(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
)

internal fun List<AssistantCard>.toAssistantCardGroupMetadata(): AssistantCardGroupMetadata {
    val containsOrders = any { it is AssistantCard.Order }
    val containsProducts = any { it is AssistantCard.Product }
    val containsStats = any { it is AssistantCard.Stats }
    return when {
        containsOrders && !containsProducts && !containsStats -> AssistantCardGroupMetadata(
            titleRes = R.string.assistant_chat_card_group_orders,
            iconRes = R.drawable.ic_assistant_card_group_orders,
        )
        containsProducts && !containsOrders && !containsStats -> AssistantCardGroupMetadata(
            titleRes = R.string.assistant_chat_card_group_products,
            iconRes = R.drawable.ic_assistant_card_group_products,
        )
        containsStats && !containsOrders && !containsProducts -> AssistantCardGroupMetadata(
            titleRes = R.string.assistant_chat_card_group_stats,
            iconRes = R.drawable.ic_assistant_card_group_stats,
        )
        else -> AssistantCardGroupMetadata(
            titleRes = R.string.assistant_chat_card_group_generic,
            iconRes = R.drawable.ic_assistant_card_group_generic,
        )
    }
}
