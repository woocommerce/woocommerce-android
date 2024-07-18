package com.woocommerce.android.ui.products.ai

import androidx.annotation.StringRes
import com.woocommerce.android.R

enum class AiTone(@StringRes val displayName: Int, val slug: String) {
    Casual(R.string.product_creation_ai_tone_casual, "Casual"),
    Formal(R.string.product_creation_ai_tone_formal, "Formal"),
    Flowery(R.string.product_creation_ai_tone_flowery, "Flowery"),
    Convincing(R.string.product_creation_ai_tone_convincing, "Convincing");

    companion object {
        fun fromString(source: String): AiTone =
            AiTone.values().firstOrNull { it.slug == source } ?: Casual
    }
}
