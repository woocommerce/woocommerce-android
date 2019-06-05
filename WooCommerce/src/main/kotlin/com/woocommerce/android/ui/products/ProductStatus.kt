package com.woocommerce.android.ui.products

import android.content.Context
import androidx.annotation.StringRes
import com.woocommerce.android.R

/**
 * Similar to PostStatus except only draft, pending, private, and publish are supported
 */
enum class ProductStatus {
    PUBLISH,
    DRAFT,
    PENDING,
    PRIVATE;

    fun toString(context: Context): String {
        @StringRes val resId = when (this) {
            PUBLISH -> R.string.product_status_published
            DRAFT -> R.string.product_status_draft
            PENDING -> R.string.product_status_pending
            PRIVATE -> R.string.product_status_private
        }
        return context.getString(resId)
    }

    companion object {
        fun fromString(status: String): ProductStatus? {
            val statusLC = status.toLowerCase()
            ProductStatus.values().forEach { value ->
                if (value.toString().toLowerCase() == statusLC) return value
            }
            return null
        }
    }
}
