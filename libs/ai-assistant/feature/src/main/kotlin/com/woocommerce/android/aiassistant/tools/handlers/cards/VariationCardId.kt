package com.woocommerce.android.aiassistant.tools.handlers.cards

internal data class VariationCardId(
    val productId: Long,
    val variationId: Long,
) {
    fun asShowCardsId(): String = "$productId/$variationId"

    companion object {
        private val SHAPE = Regex("[1-9]\\d*/[1-9]\\d*")

        fun parse(raw: String): VariationCardId? {
            if (!SHAPE.matches(raw)) return null
            val parts = raw.split("/")
            val productId = parts[0].toLongOrNull() ?: return null
            val variationId = parts[1].toLongOrNull() ?: return null
            return VariationCardId(productId = productId, variationId = variationId)
        }
    }
}
