package com.woocommerce.android.aiassistant.ui.cards

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.aiassistant.R

internal data class AssistantCardGroupMetadata(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
)

internal fun List<AssistantCard>.toAssistantCardGroupMetadata(): AssistantCardGroupMetadata =
    map { it.groupKind }
        .distinct()
        .singleOrNull()
        ?.metadata
        ?: GENERIC_METADATA

private enum class AssistantCardGroupKind {
    Order,
    Product,
    Variation,
    Stats,
    Customer,
}

private val AssistantCard.groupKind: AssistantCardGroupKind
    get() = when (this) {
        is AssistantCard.Order -> AssistantCardGroupKind.Order
        is AssistantCard.Product -> AssistantCardGroupKind.Product
        is AssistantCard.Variation -> AssistantCardGroupKind.Variation
        is AssistantCard.Stats -> AssistantCardGroupKind.Stats
        is AssistantCard.Customer -> AssistantCardGroupKind.Customer
    }

private val AssistantCardGroupKind.metadata: AssistantCardGroupMetadata
    get() = when (this) {
        AssistantCardGroupKind.Order -> AssistantCardGroupMetadata(
            titleRes = R.string.assistant_chat_card_group_orders,
            iconRes = R.drawable.ic_assistant_card_group_orders,
        )
        AssistantCardGroupKind.Product -> AssistantCardGroupMetadata(
            titleRes = R.string.assistant_chat_card_group_products,
            iconRes = R.drawable.ic_assistant_card_group_products,
        )
        AssistantCardGroupKind.Variation -> AssistantCardGroupMetadata(
            titleRes = R.string.assistant_chat_card_group_variations,
            iconRes = R.drawable.ic_assistant_card_group_products,
        )
        AssistantCardGroupKind.Stats -> AssistantCardGroupMetadata(
            titleRes = R.string.assistant_chat_card_group_stats,
            iconRes = R.drawable.ic_assistant_card_group_stats,
        )
        AssistantCardGroupKind.Customer -> AssistantCardGroupMetadata(
            titleRes = R.string.assistant_chat_card_group_customers,
            iconRes = R.drawable.ic_assistant_card_group_customers,
        )
    }

private val GENERIC_METADATA = AssistantCardGroupMetadata(
    titleRes = R.string.assistant_chat_card_group_generic,
    iconRes = R.drawable.ic_assistant_card_group_generic,
)
