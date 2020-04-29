package com.woocommerce.android.ui.products.settings

import android.content.Context
import androidx.annotation.StringRes
import com.woocommerce.android.R
import java.util.Locale

enum class ProductVisibility {
    PUBLIC,
    PRIVATE,
    PASSWORD_PROTECTED;

    fun toLocalizedString(context: Context): String {
        @StringRes val resId = when (this) {
            PUBLIC -> R.string.product_visibility_public
            PRIVATE -> R.string.product_visibility_private
            PASSWORD_PROTECTED -> R.string.product_visibility_password_protected
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
