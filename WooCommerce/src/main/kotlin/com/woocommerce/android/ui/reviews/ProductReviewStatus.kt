package com.woocommerce.android.ui.reviews

import android.content.Context
import com.woocommerce.android.R
import org.apache.commons.lang3.StringUtils
import java.util.Locale

enum class ProductReviewStatus {
    // Real status
    APPROVED,
    HOLD, // The product version of "unnapproved"
    SPAM,
    TRASH,

    // Used for filtering only
    ALL;

    override fun toString(): String {
        return this.name.toLowerCase(Locale.US)
    }

    companion object {
        fun fromString(string: String?): ProductReviewStatus {
            if (string != null) {
                for (v in values()) {
                    if (string.equals(v.name, ignoreCase = true)) {
                        return v
                    }
                }
            }
            return ALL
        }

        fun getLocalizedLabel(context: Context?, reviewStatus: ProductReviewStatus): String {
            return context?.let { ctx ->
                when (reviewStatus) {
                    APPROVED -> ctx.getString(R.string.wc_approved)
                    HOLD -> ctx.getString(R.string.wc_unapproved)
                    SPAM -> ctx.getString(R.string.wc_spam)
                    TRASH -> ctx.getString(R.string.wc_trash)
                    ALL -> ctx.getString(R.string.all)
                }
            } ?: StringUtils.EMPTY
        }
    }
}
