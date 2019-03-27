package com.woocommerce.android.extensions

import android.content.Context
import android.support.annotation.StringRes
import com.woocommerce.android.R
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.model.post.PostStatus.PENDING
import org.wordpress.android.fluxc.model.post.PostStatus.PRIVATE
import org.wordpress.android.fluxc.model.post.PostStatus.PUBLISHED
import org.wordpress.android.fluxc.model.post.PostStatus.SCHEDULED
import org.wordpress.android.fluxc.model.post.PostStatus.TRASHED
import org.wordpress.android.fluxc.model.post.PostStatus.UNKNOWN

fun PostStatus.toLocalizedString(context: Context): String {
    @StringRes val resId = when (this) {
        PUBLISHED -> R.string.post_status_published
        DRAFT -> R.string.post_status_draft
        PENDING -> R.string.post_status_pending
        PRIVATE -> R.string.post_status_private
        SCHEDULED -> R.string.post_status_scheduled
        TRASHED -> R.string.post_status_trashed
        UNKNOWN -> R.string.post_status_unknown
    }
    return context.getString(resId)
}
