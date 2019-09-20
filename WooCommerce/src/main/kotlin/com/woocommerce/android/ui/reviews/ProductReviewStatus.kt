package com.woocommerce.android.ui.reviews

import java.util.Locale

enum class ProductReviewStatus {
    // Real status
    APPROVED,
    UNAPPROVED,
    SPAM,
    TRASH,
    DELETED,

    // Used for filtering
    ALL,

    // Used for editing
    UNSPAM, // Unmark the comment as spam. Will attempt to set it to the previous status.
    UNTRASH; // Untrash a comment. Only works when the comment is in the trash.

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
    }
}
