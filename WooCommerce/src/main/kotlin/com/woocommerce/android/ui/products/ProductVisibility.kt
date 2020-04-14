package com.woocommerce.android.ui.products

import android.content.Context
import androidx.annotation.StringRes
import com.woocommerce.android.R
import java.util.Locale

/**
 * Enum for a product's catalog visibility
 */
enum class ProductVisibility {
    VISIBLE,
    CATALOG,
    SEARCH,
    HIDDEN;

    fun toLocalizedString(context: Context): String {
        @StringRes val resId = when (this) {
            VISIBLE -> R.string.product_visibility_visible
            CATALOG -> R.string.product_visibility_catalog
            SEARCH -> R.string.product_visibility_search
            HIDDEN -> R.string.product_visibility_hidden
        }
        return context.getString(resId)
    }

    override fun toString(): String {
        return super.toString().toLowerCase(Locale.US)
    }

    companion object {
        fun fromString(visibility: String): ProductVisibility? {
            val lcVisibility = visibility.toLowerCase(Locale.US)
            values().forEach { value ->
                if (value.toString().toLowerCase(Locale.US) == lcVisibility) return value
            }
            return null
        }
    }
}
