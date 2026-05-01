package com.woocommerce.android.aiassistant.tools.handlers.cards

import kotlinx.serialization.json.JsonElement

internal const val MAX_SHOW_CARDS_REFS = 10

internal data class ShowCardsArguments(
    val references: List<JsonElement> = emptyList()
)

internal enum class ShowCardFamily(val serializedName: String) {
    Order("order"),
    Product("product");

    companion object {
        fun from(value: String): ShowCardFamily? =
            entries.firstOrNull { it.serializedName == value }
    }
}
